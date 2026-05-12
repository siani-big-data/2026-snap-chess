import torch
from torchvision import transforms
from PIL import Image
from app.model import build_model

CLASSES = ['b', 'empty', 'k', 'n', 'p', 'q', 'r']

TRANSFORM = transforms.Compose([
    transforms.Resize((64, 64)),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406],
                         [0.229, 0.224, 0.225])
])


class PieceClassifier:

    def __init__(self, model_path: str):
        self.model = build_model(num_classes=len(CLASSES))
        self.model.load_state_dict(torch.load(model_path, map_location='cpu'))
        self.model.eval()

    def predict(self, image: Image.Image) -> dict:
        tensor = TRANSFORM(image).unsqueeze(0)
        with torch.no_grad():
            output = self.model(tensor)
            probabilities = torch.softmax(output, dim=1)
            confidence, idx = probabilities.max(dim=1)
        return {
            "piece": CLASSES[idx.item()],
            "confidence": round(confidence.item(), 4)
        }