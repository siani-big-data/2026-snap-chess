import torch.nn as nn
from torchvision import models


def build_model(num_classes: int = 7) -> nn.Module:
    model = models.resnet18(weights=None)
    num_features = model.fc.in_features
    model.fc = nn.Linear(num_features, num_classes)
    return model