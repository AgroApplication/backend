"""
AgroVision AI - Python Model Service
Lightweight Flask app that serves the trained VGG19 rice leaf disease detection model.

Usage:
    1. Place your trained model file (e.g., 'rice_disease_model.keras') in this directory
    2. Install dependencies: pip install -r requirements.txt
    3. Run: python app.py
    4. The service will start on http://localhost:5000

The Java backend calls POST /predict with an image file to get predictions.
"""

from flask import Flask, request, jsonify
import numpy as np
import os

app = Flask(__name__)

# Global model variable
model = None
CLASS_NAMES = ["Bacterial Leaf Blight", "Brown Spot", "Healthy", "Sheath Blight", "Rice Blast"]

# If model's top confidence is below this %, we reject the image as not a valid rice leaf
CONFIDENCE_THRESHOLD = 60.0


def load_model():
    """Load the trained VGG19 model."""
    global model
    model_path = os.environ.get("MODEL_PATH", "rice_disease_model.keras")

    if not os.path.exists(model_path):
        print(f"WARNING: Model file '{model_path}' not found.")
        print("The service will return mock predictions until a model is provided.")
        return False

    try:
        import tensorflow as tf
        model = tf.keras.models.load_model(model_path)
        print(f"Model loaded successfully from {model_path}")
        return True
    except Exception as e:
        print(f"Error loading model: {e}")
        return False


def preprocess_image(image_bytes):
    """Preprocess image for VGG19 input."""
    from PIL import Image
    import io
    from tensorflow.keras.applications.vgg19 import preprocess_input

    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    img = img.resize((224, 224))
    img_array = np.array(img, dtype=np.float32)
    img_array = np.expand_dims(img_array, axis=0)
    img_array = preprocess_input(img_array)
    return img_array


def get_deterministic_color_scores(image_bytes):
    """
    Deterministic heuristic: analyze actual pixel color distribution of the image.
    Returns a score dict based on real pixel data — same image = same result.
    """
    from PIL import Image
    import io

    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    img = img.resize((64, 64))
    pixels = np.array(img, dtype=np.float32)

    r = pixels[:, :, 0]
    g = pixels[:, :, 1]
    b = pixels[:, :, 2]
    total = 64 * 64

    # Healthy leaves: mostly green
    green_pixels = np.sum((g > r) & (g > b) & (g > 60))
    green_ratio = green_pixels / total

    # Brown Spot / Bacterial Blight: brownish-yellow patches
    brown_pixels = np.sum((r > 100) & (g > 80) & (b < 80) & (r > g))
    brown_ratio = brown_pixels / total

    # Sheath Blight: pale/grayish areas
    gray_pixels = np.sum((np.abs(r.astype(int) - g.astype(int)) < 20) &
                         (np.abs(g.astype(int) - b.astype(int)) < 20) & (r > 100))
    gray_ratio = gray_pixels / total

    # Rice Blast: dark spots
    dark_pixels = np.sum((r < 80) & (g < 80) & (b < 80))
    dark_ratio = dark_pixels / total

    # Bacterial Leaf Blight: yellowish streaks (high R+G, low B)
    yellow_pixels = np.sum((r > 120) & (g > 120) & (b < 80))
    yellow_ratio = yellow_pixels / total

    is_leaf = (green_ratio + brown_ratio + yellow_ratio) > 0.15

    # Build deterministic scores based on actual pixel ratios
    scores = {
        "Healthy": green_ratio,
        "Brown Spot": brown_ratio,
        "Sheath Blight": gray_ratio,
        "Rice Blast": dark_ratio,
        "Bacterial Leaf Blight": yellow_ratio,
    }

    return scores, is_leaf


@app.route("/predict", methods=["POST"])
def predict():
    """Predict disease from uploaded image."""
    if "file" not in request.files:
        return jsonify({"error": "No file provided"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "Empty filename"}), 400

    try:
        image_bytes = file.read()

        if model is not None:
            # ── REAL MODEL MODE ──────────────────────────────────────────────
            img_array = preprocess_image(image_bytes)
            predictions = model.predict(img_array, verbose=0)
            predicted_index = np.argmax(predictions[0])
            confidence = float(predictions[0][predicted_index]) * 100

            # If the model isn't confident, it's probably not a rice leaf
            if confidence < CONFIDENCE_THRESHOLD:
                return jsonify({
                    "isValidLeaf": False,
                    "message": "This does not appear to be a rice leaf. Please upload a clear image of a rice plant leaf.",
                    "confidence": round(confidence, 1),
                    "all_predictions": {
                        name: round(float(pred) * 100, 2)
                        for name, pred in zip(CLASS_NAMES, predictions[0])
                    }
                })

            return jsonify({
                "isValidLeaf": True,
                "disease": CLASS_NAMES[predicted_index],
                "confidence": round(confidence, 1),
                "all_predictions": {
                    name: round(float(pred) * 100, 2)
                    for name, pred in zip(CLASS_NAMES, predictions[0])
                }
            })

        else:
            # ── MOCK MODE: Deterministic based on actual image pixel data ──
            # NOTE: This is a heuristic fallback ONLY used when no model is loaded.
            # Results are deterministic — same image always gives same result.
            scores, is_leaf = get_deterministic_color_scores(image_bytes)

            if not is_leaf:
                return jsonify({
                    "isValidLeaf": False,
                    "message": "This does not appear to be a rice leaf. Please upload a clear image of a rice plant leaf.",
                    "confidence": 0.0,
                    "mock": True,
                    "warning": "Running in MOCK mode - no model loaded. Place rice_disease_model.keras in this directory."
                })

            total_score = sum(scores.values()) or 1.0
            normalized = {k: v / total_score for k, v in scores.items()}

            predicted_class = max(normalized, key=normalized.get)
            confidence = round(min(normalized[predicted_class] * 100, 95.0), 1)

            # Ensure a minimum confidence for valid leaves
            if confidence < 30.0:
                confidence = 30.0

            return jsonify({
                "isValidLeaf": True,
                "disease": predicted_class,
                "confidence": confidence,
                "all_predictions": {
                    name: round(score * 100 / total_score, 2)
                    for name, score in scores.items()
                },
                "mock": True,
                "warning": "Running in MOCK mode - no model loaded. Place rice_disease_model.keras in this directory."
            })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "model_loaded": model is not None,
        "mode": "real" if model is not None else "mock (deterministic heuristic)",
        "confidence_threshold": CONFIDENCE_THRESHOLD,
        "classes": CLASS_NAMES
    })


if __name__ == "__main__":
    load_model()
    app.run(host="0.0.0.0", port=5000, debug=False)
