package org.open4goods.ui.interceptors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.resource.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

class ImageResizeInterceptorTest {

    private ResourceService resourceService;
    private Set<String> allowedResize;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        allowedResize = Set.of("200", "200x100");
    }

    @Test
    void parseDimensionsSupportsWidthAndHeight() {
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        assertThat(interceptor.parseDimensions("image-200x300.webp")).containsExactly(200, 300);
    }

    @Test
    void parseDimensionsDefaultsHeightToWidth() {
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        assertThat(interceptor.parseDimensions("image-150.webp")).containsExactly(150, 150);
    }

    @Test
    void parseDimensionsReturnsNullOnInvalidPattern() {
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        assertThat(interceptor.parseDimensions("image.webp")).isNull();
    }

    @Test
    void resizeImageMaintainsAspectRatioAndUsesRequestedWidth() {
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");
        BufferedImage original = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);

        BufferedImage resized = interceptor.resizeImage(original, 50, 50);

        assertThat(resized.getWidth()).isEqualTo(50);
        assertThat(resized.getHeight()).isEqualTo(25);
    }

    @Test
    void findSourceImageSupportsOriginalWebp() throws IOException {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com") {
            @Override
            BufferedImage fetchImageFromURL(String urlString) {
                requestedUrl.set(urlString);
                if (urlString.endsWith(".webp")) {
                    return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                }
                return null;
            }
        };

        BufferedImage image = interceptor.findSourceImage("/images/sample.webp", false);

        assertThat(image).isNotNull();
        assertThat(requestedUrl.get()).endsWith("sample.webp");
    }

    @Test
    void fetchImageFromUrlClosesConnectionAndStream() throws IOException {
        byte[] imageBytes = createPngBytes();
        TrackingInputStream trackingInputStream = new TrackingInputStream(new ByteArrayInputStream(imageBytes));
        MockHttpURLConnection connection = new MockHttpURLConnection(trackingInputStream, HttpURLConnection.HTTP_OK);
        ImageResizeInterceptor interceptor = new TestableInterceptor(resourceService, allowedResize, "http://example.com", connection);

        BufferedImage image = interceptor.fetchImageFromURL("http://example.com/image.webp");

        assertThat(image).isNotNull();
        assertThat(trackingInputStream.closed).isTrue();
        assertThat(connection.disconnected).isTrue();
        assertThat(connection.requestProperties).containsEntry(ImageResizeInterceptor.BYPASS_HEADER, ImageResizeInterceptor.BYPASS_HEADER);
    }

    @Test
    void preHandleBypassesWhenInternalHeaderPresent() throws Exception {
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/images/example.webp");
        request.addHeader(ImageResizeInterceptor.BYPASS_HEADER, ImageResizeInterceptor.BYPASS_HEADER);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean shouldContinue = interceptor.preHandle(request, response, new Object());

        assertThat(shouldContinue).isTrue();
        verifyNoInteractions(resourceService);
    }

    @Test
    void preHandleReturns404WhenServeImageFails() throws Exception {
         ResourceService resourceService = mock(ResourceService.class);
         
         ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com") {
             @Override
             Resource buildResource(String requestURI) {
                 Resource resource = new Resource();
                 resource.setFileName(requestURI);
                 resource.setCacheKey("cached-file.webp");
                 return resource;
             }
         };

         MockHttpServletRequest request = new MockHttpServletRequest();
         request.setRequestURI("/images/example.webp");
         
         MockHttpServletResponse response = mock(MockHttpServletResponse.class);
         when(response.getOutputStream()).thenThrow(new IOException("Disk error"));

         java.io.File mockFile = mock(java.io.File.class);
         when(resourceService.getCacheFile(any(Resource.class))).thenReturn(mockFile);
         when(mockFile.exists()).thenReturn(true);
         when(mockFile.getAbsolutePath()).thenReturn("/tmp/cached-file.webp");

         boolean result = interceptor.preHandle(request, response, new Object());

         assertThat(result).isFalse();
         verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
    }


    @Test
    void saveToCacheReturnsFalseWhenParentFolderCannotBeCreated() throws IOException {
        File tempDir = java.nio.file.Files.createTempDirectory("image-cache-test").toFile();
        File blockingFile = new File(tempDir, "blocking-file");
        assertThat(blockingFile.createNewFile()).isTrue();
        File cachedFile = new File(new File(blockingFile, "C/H/E"), "cached.webp");

        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        boolean saved = interceptor.saveToCache(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), cachedFile);

        assertThat(saved).isFalse();
        assertThat(cachedFile.exists()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("imagesTheWebpWriterRejects")
    void saveToCacheConvertsImagesTheWebpWriterRejects(String label, BufferedImage source) throws IOException {
        // GOU-1155: unresized originals of these kinds answered 404 in prod
        File cachedFile = new File(java.nio.file.Files.createTempDirectory("image-cache-test").toFile(), "C/H/E/key.cache.webp");
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        assertThat(interceptor.saveToCache(source, cachedFile)).isTrue();
        BufferedImage written = ImageIO.read(cachedFile);
        assertThat(written.getWidth()).isEqualTo(source.getWidth());
        assertThat(written.getHeight()).isEqualTo(source.getHeight());
    }

    static Stream<Arguments> imagesTheWebpWriterRejects() {
        return Stream.of(
                Arguments.of("8-bit grayscale JPEG", new BufferedImage(7, 3, BufferedImage.TYPE_BYTE_GRAY)),
                Arguments.of("16-bit grayscale PNG", new BufferedImage(7, 3, BufferedImage.TYPE_USHORT_GRAY)),
                Arguments.of("16-bit RGB PNG", rgb48Image(7, 3)));
    }

    @Test
    void saveToCacheWritesUsualJpegLayoutWithoutConversion() throws IOException {
        // Layout ImageIO decodes a colour JPEG into, which the WebP writer accepts as is
        File cachedFile = new File(java.nio.file.Files.createTempDirectory("image-cache-test").toFile(), "key.cache.webp");
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        assertThat(interceptor.saveToCache(new BufferedImage(7, 3, BufferedImage.TYPE_3BYTE_BGR), cachedFile)).isTrue();
    }

    /** Same layout as ImageIO decodes a 16 bits per channel RGB PNG into (custom type, 48 bits per pixel). */
    private static BufferedImage rgb48Image(int width, int height) {
        ComponentColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                new int[] { 16, 16, 16 }, false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);
        return new BufferedImage(colorModel, raster, false, null);
    }

    @Test
    void preHandleReturns404AndResetsContentTypeWhenCachedFileIsMissing() throws Exception {
        ResourceService resourceService = mock(ResourceService.class);
        File missingFile = new File("/tmp/this-file-does-not-exist.webp");

        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, new HashSet<>(), "http://example.com") {
            @Override
            Resource buildResource(String requestURI) {
                Resource resource = new Resource();
                resource.setFileName(requestURI);
                resource.setCacheKey("cached-file.webp");
                return resource;
            }

            @Override
            boolean saveToCache(BufferedImage sourceImage, File cachedFile) {
                return true;
            }

            @Override
            BufferedImage findSourceImage(String baseImageName, boolean hasDimensionsArgument) {
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/images/example.webp");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(resourceService.getCacheFile(any(Resource.class))).thenReturn(missingFile);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        assertThat(response.getContentType()).isEqualTo("application/json");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encodedFormats")
    void decodeBoundedSubsamplesImagesLargerThanTheBound(String format) throws IOException {
        // GOU-1952: the decoded raster depends on the bound, not on the source size
        byte[] encoded = encode(new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB), format);
        ImageResizeInterceptor interceptor = boundedInterceptor(10_000, 2, Duration.ofSeconds(1));

        BufferedImage decoded = interceptor.decodeBounded(new ByteArrayInputStream(encoded));

        assertThat((long) decoded.getWidth() * decoded.getHeight()).isLessThanOrEqualTo(10_000);
        assertThat(decoded.getWidth()).isGreaterThan(decoded.getHeight());
    }

    static Stream<String> encodedFormats() {
        return Stream.of("png", "jpg", "webp");
    }

    @Test
    void decodeBoundedKeepsImagesUnderTheBoundAtFullSize() throws IOException {
        byte[] encoded = encode(new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB), "png");
        ImageResizeInterceptor interceptor = boundedInterceptor(10_000, 2, Duration.ofSeconds(1));

        BufferedImage decoded = interceptor.decodeBounded(new ByteArrayInputStream(encoded));

        assertThat(decoded.getWidth()).isEqualTo(120);
        assertThat(decoded.getHeight()).isEqualTo(80);
    }

    @Test
    void defaultBoundSubsamplesAHugeSource() throws IOException {
        // 6000 x 6000 = 36 MP: a full decode would allocate 144 MB per ARGB copy.
        // One bit per pixel keeps the test source itself small.
        byte[] encoded = encode(new BufferedImage(6000, 6000, BufferedImage.TYPE_BYTE_BINARY), "png");
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        BufferedImage decoded = interceptor.decodeBounded(new ByteArrayInputStream(encoded));

        assertThat((long) decoded.getWidth() * decoded.getHeight())
                .isLessThanOrEqualTo(ImageResizeInterceptor.MAX_DECODED_PIXELS);
        assertThat(decoded.getWidth()).isEqualTo(3000);
    }

    @Test
    void decodeBoundedReturnsNullForUnknownFormat() throws IOException {
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com");

        assertThat(interceptor.decodeBounded(new ByteArrayInputStream("not an image".getBytes()))).isNull();
    }

    @Test
    void fetchImageFromUrlDecodesWithinTheBound() throws IOException {
        byte[] imageBytes = encode(new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB), "png");
        MockHttpURLConnection connection = new MockHttpURLConnection(new ByteArrayInputStream(imageBytes), HttpURLConnection.HTTP_OK);
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com",
                10_000, 2, Duration.ofSeconds(1)) {
            @Override
            HttpURLConnection openConnection(String urlString) {
                return connection;
            }
        };

        BufferedImage image = interceptor.fetchImageFromURL("http://example.com/image.png");

        assertThat((long) image.getWidth() * image.getHeight()).isLessThanOrEqualTo(10_000);
    }

    @Test
    void subsamplingStepKeepsEveryDecodedSideUnderTheWebpLimit() {
        // A very long and thin image fits the pixel bound but not the WebP encoder
        int step = ImageResizeInterceptor.subsamplingStep(40_000, 100, ImageResizeInterceptor.MAX_DECODED_PIXELS);

        assertThat(Math.ceilDiv(40_000, step)).isLessThanOrEqualTo(ImageResizeInterceptor.WEBP_MAX_DIMENSION);
        assertThat(ImageResizeInterceptor.subsamplingStep(4096, 4096, ImageResizeInterceptor.MAX_DECODED_PIXELS)).isEqualTo(1);
        assertThat(ImageResizeInterceptor.subsamplingStep(4097, 4096, ImageResizeInterceptor.MAX_DECODED_PIXELS)).isEqualTo(2);
    }

    @Test
    void preHandleAnswers503WhenEveryConversionSlotIsBusy() throws Exception {
        // GOU-1952: concurrent cache misses must not each hold a decoded image
        File cacheDir = java.nio.file.Files.createTempDirectory("image-cache-test").toFile();
        when(resourceService.getCacheFile(any(Resource.class)))
                .thenAnswer(invocation -> new File(cacheDir, ((Resource) invocation.getArgument(0)).getCacheKey()));
        CountDownLatch converting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ImageResizeInterceptor interceptor = new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com",
                10_000, 1, Duration.ofMillis(200)) {
            @Override
            BufferedImage findSourceImage(String baseImageName, boolean hasDimensionsArgument) {
                converting.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<MockHttpServletResponse> first = executor.submit(() -> {
                MockHttpServletResponse response = new MockHttpServletResponse();
                interceptor.preHandle(webpRequest("/images/first.webp"), response, new Object());
                return response;
            });
            assertThat(converting.await(5, TimeUnit.SECONDS)).isTrue();

            MockHttpServletResponse second = new MockHttpServletResponse();
            boolean result = interceptor.preHandle(webpRequest("/images/second.webp"), second, new Object());

            assertThat(result).isFalse();
            assertThat(second.getStatus()).isEqualTo(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            assertThat(second.getHeader("Retry-After")).isNotNull();

            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private ImageResizeInterceptor boundedInterceptor(long maxPixels, int conversions, Duration wait) {
        return new ImageResizeInterceptor(resourceService, allowedResize, "http://example.com", maxPixels, conversions, wait);
    }

    private static MockHttpServletRequest webpRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, outputStream)).isTrue();
        return outputStream.toByteArray();
    }

    private byte[] createPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static class TrackingInputStream extends InputStream {

        private final InputStream delegate;
        private boolean closed;

        private TrackingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return delegate.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }
    }

    private static class MockHttpURLConnection extends HttpURLConnection {

        private final InputStream inputStream;
        private final int responseCode;
        private boolean disconnected;
        private final Map<String, String> requestProperties = new HashMap<>();

        protected MockHttpURLConnection(InputStream inputStream, int responseCode) {
            super((URL) null);
            this.inputStream = inputStream;
            this.responseCode = responseCode;
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
            // No-op for mock
        }

        @Override
        public int getResponseCode() throws IOException {
            return responseCode;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.put(key, value);
        }
    }

    private static class TestableInterceptor extends ImageResizeInterceptor {

        private final HttpURLConnection connection;

        TestableInterceptor(ResourceService resourceService, Set<String> allowedResize, String imageBaseUrl, HttpURLConnection connection) {
            super(resourceService, allowedResize, imageBaseUrl);
            this.connection = connection;
        }

        @Override
        HttpURLConnection openConnection(String urlString) throws IOException {
            return connection;
        }
    }
}
