package org.open4goods.ui.interceptors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.commons.services.ResourceService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
