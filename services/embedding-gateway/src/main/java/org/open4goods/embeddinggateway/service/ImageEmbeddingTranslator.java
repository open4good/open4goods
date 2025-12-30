package org.open4goods.embeddinggateway.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/**
 * Translator converting an {@link Image} into a normalised embedding-ready tensor.
 */
public class ImageEmbeddingTranslator implements Translator<Image, float[]>
{
    private static final float[] MEAN = { 0.485f, 0.456f, 0.406f };
    private static final float[] STD = { 0.229f, 0.224f, 0.225f };

    private final int imageSize;

    public ImageEmbeddingTranslator(int imageSize)
    {
        this.imageSize = imageSize;
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input)
    {
        Image processed = preprocess(input);
        BufferedImage buffered = toBufferedImage(processed);
        float[] data = toNormalizedCHW(buffered);

        NDArray modelInput = ctx.getNDManager().create(data, new Shape(1, 3, imageSize, imageSize));
        return new NDList(modelInput);
    }

    @Override
    public float[] processOutput(TranslatorContext ctx, NDList list)
    {
        NDArray embedding = list.get(0);
        float[] vec = embedding.toFloatArray();

        double sumSq = 0.0d;
        for (float value : vec)
        {
            sumSq += (double) value * value;
        }
        double norm = Math.sqrt(sumSq) + 1e-12;
        for (int i = 0; i < vec.length; i++)
        {
            vec[i] = (float) (vec[i] / norm);
        }
        return vec;
    }

    @Override
    public Batchifier getBatchifier()
    {
        return null;
    }

    private Image preprocess(Image input)
    {
        int width = input.getWidth();
        int height = input.getHeight();
        Image working = input;

        if (width != imageSize || height != imageSize)
        {
            float scale = (float) imageSize / Math.min(width, height);
            int newWidth = Math.max(1, Math.round(width * scale));
            int newHeight = Math.max(1, Math.round(height * scale));
            working = input.resize(newWidth, newHeight, true);
        }

        int cropX = Math.max(0, (working.getWidth() - imageSize) / 2);
        int cropY = Math.max(0, (working.getHeight() - imageSize) / 2);
        if (cropX != 0 || cropY != 0 || working.getWidth() != imageSize || working.getHeight() != imageSize)
        {
            working = working.getSubImage(cropX, cropY, imageSize, imageSize);
        }
        return working;
    }

    private BufferedImage toBufferedImage(Image image)
    {
        Object wrapped = image.getWrappedImage();
        if (wrapped instanceof BufferedImage bufferedImage)
        {
            return bufferedImage;
        }
        if (wrapped instanceof java.awt.Image awtImage)
        {
            BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = converted.createGraphics();
            try
            {
                graphics.drawImage(awtImage, 0, 0, null);
            }
            finally
            {
                graphics.dispose();
            }
            return converted;
        }
        throw new IllegalArgumentException("Unsupported image wrapper: " + Objects.toString(wrapped));
    }

    private float[] toNormalizedCHW(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        int channelArea = width * height;
        float[] data = new float[3 * channelArea];

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int rgb = image.getRGB(x, y);
                float r = ((rgb >> 16) & 0xFF) / 255f;
                float g = ((rgb >> 8) & 0xFF) / 255f;
                float b = (rgb & 0xFF) / 255f;

                int offset = y * width + x;
                data[offset] = normalize(r, 0);
                data[channelArea + offset] = normalize(g, 1);
                data[2 * channelArea + offset] = normalize(b, 2);
            }
        }
        return data;
    }

    private float normalize(float value, int channel)
    {
        return (value - MEAN[channel]) / STD[channel];
    }
}
