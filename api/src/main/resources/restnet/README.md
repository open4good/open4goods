---
license: apache-2.0
pipeline_tag: image-classification
---

ONNX port of [microsoft/resnet-50](https://huggingface.co/microsoft/resnet-50).

This model is intended to be used for image classification and similarity searches.

You can find the ONNX port implementation [here](https://github.com/qdrant/fastembed/blob/main/experiments/Example.%20Convert%20Resnet50%20to%20ONNX.ipynb)

### Usage

Here's an example of performing inference using the model with [FastEmbed](https://github.com/qdrant/fastembed).

```py
from fastembed import ImageEmbedding

images = [
    "./path/to/image1.jpg",
    "./path/to/image2.jpg",
]

model = ImageEmbedding(model_name="Qdrant/resnet50-onnx")
embeddings = list(model.embed(images))

# [
#   array([-0.1115,  0.0097,  0.0052,  0.0195, ...], dtype=float32),
#   array([-0.1019,  0.0635, -0.0332,  0.0522, ...], dtype=float32)
# ]
```