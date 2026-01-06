package org.open4goods.embedding.util;

/**
 * Utility methods for handling embedding vectors before indexing or querying.
 */
public final class EmbeddingVectorUtils
{
    private EmbeddingVectorUtils()
    {
        // Utility class.
    }

    /**
     * Applies L2 normalization to the provided vector in-place.
     *
     * @param vector embedding vector to normalize
     * @return the normalized vector, or {@code null} if the input is null
     */
    public static float[] normalizeL2(float[] vector)
    {
        if (vector == null || vector.length == 0)
        {
            return vector;
        }

        double sumSquares = 0.0;
        for (float value : vector)
        {
            sumSquares += (double) value * value;
        }

        if (sumSquares <= 0.0)
        {
            return vector;
        }

        double norm = Math.sqrt(sumSquares);
        for (int i = 0; i < vector.length; i++)
        {
            vector[i] = (float) (vector[i] / norm);
        }

        return vector;
    }
}
