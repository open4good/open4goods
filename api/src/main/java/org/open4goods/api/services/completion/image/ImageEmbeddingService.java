package org.open4goods.api.services.completion.image;
public interface ImageEmbeddingService {
    float[] embed(java.nio.file.Path imagePath) throws Exception;
}