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


def is_likely_leaf_by_color(image_bytes):
    """
    Heuristic check: a rice leaf image should have a significant amount of green/yellow-green pixels.
    This is used ONLY in mock mode when the real model is not loaded.
    Returns True if the image looks like it could be a plant leaf.
    """
    try:
        from PIL import Image
        import io
        img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        img = img.resize((64, 64))
        pixels = np.array(img)
        
        # Check for green-dominant pixels: G > R and G > B and G > 60
        r, g, b = pixels[:, :, 0], pixels[:, :, 1], pixels[:, :, 2]
        green_pixels = np.sum((g > r) & (g > b) & (g > 60))
        total_pixels = 64 * 64
        green_ratio = green_pixels / total_pixels
        
        # Also accept yellow-brown (rice field colors): high R and G, low B
        yellowbrown_pixels = np.sum((r > 80) & (g > 80) & (b < 100) & (r + g > b * 3))
        yellowbrown_ratio = yellowbrown_pixels / total_pixels

        return (green_ratio + yellowbrown_ratio) > 0.20  # At least 20% of image is greenish or leaf-colored
    except Exception:
        return True  # On error, allow it to pass


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
            # Real prediction using the trained VGG19 model
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
            # Mock mode: use color heuristic to simulate invalid image detection
            import random
            
            if not is_likely_leaf_by_color(image_bytes):
                return jsonify({
                    "isValidLeaf": False,
                    "message": "This does not appear to be a rice leaf. Please upload a clear image of a rice plant leaf.",
                    "confidence": round(random.random() * 30, 1),  # Low confidence for non-leaves
                    "mock": True
                })

            disease = random.choice(CLASS_NAMES)
            confidence = round(60 + random.random() * 35, 1)  # 60-95% for valid leaves
            return jsonify({
                "isValidLeaf": True,
                "disease": disease,
                "confidence": confidence,
                "mock": True
            })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "model_loaded": model is not None,
        "confidence_threshold": CONFIDENCE_THRESHOLD,
        "classes": CLASS_NAMES
    })


if __name__ == "__main__":
    load_model()
    app.run(host="0.0.0.0", port=5000, debug=True)



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
            # Real prediction
            img_array = preprocess_image(image_bytes)
            predictions = model.predict(img_array, verbose=0)
            predicted_index = np.argmax(predictions[0])
            confidence = float(predictions[0][predicted_index]) * 100

            return jsonify({
                "disease": CLASS_NAMES[predicted_index],
                "confidence": round(confidence, 1),
                "all_predictions": {
                    name: round(float(pred) * 100, 2)
                    for name, pred in zip(CLASS_NAMES, predictions[0])
                }
            })
        else:
            # Mock prediction (model not loaded)
            import random
            disease = random.choice(CLASS_NAMES)
            confidence = round(85 + random.random() * 15, 1)
            return jsonify({
                "disease": disease,
                "confidence": confidence,
                "mock": True
            })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "model_loaded": model is not None,
        "classes": CLASS_NAMES
    })


if __name__ == "__main__":
    load_model()
    app.run(host="0.0.0.0", port=5000, debug=True)
