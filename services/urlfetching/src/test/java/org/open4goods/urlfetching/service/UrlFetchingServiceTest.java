// TODO : Disabled because selenium not working on CI / CD
// package org.open4goods.urlfetching.service;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import com.sun.net.httpserver.HttpServer;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.open4goods.services.urlfetching.dto.FetchResponse;
//import org.open4goods.services.urlfetching.service.UrlFetchingService;
//import org.springframework.boot.SpringBootConfiguration;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.net.InetSocketAddress;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
///**
// * Integration test for the UrlFetchingService using a test YAML configuration.
// *
// * Since this module is intended for inclusion in other applications and does not have its own main
// * application class, we define a minimal test configuration here.
// */
//@SpringBootTest(classes = UrlFetchingServiceTest.TestConfig.class)
//@ActiveProfiles("test")
//public class UrlFetchingServiceTest {
//
//    @Autowired
//    private UrlFetchingService urlFetchingService;
//
//    // Lightweight HTTP server for testing purposes
//    private HttpServer server;
//    // Port where the test server will run
//    private int port;
//
//    /**
//     * Minimal test configuration to bootstrap the Spring context.
//     */
//    @SpringBootConfiguration
//    @EnableAutoConfiguration
//    @ComponentScan(basePackages = {"org.open4goods.services.urlfetching"})
//    public static class TestConfig {
//        // This class remains empty; its purpose is to trigger component scanning in the
//        // org.open4goods.services.urlfetching package and enable auto-configuration.
//    }
//
//    /**
//     * Starts an embedded HTTP server before each test.
//     *
//     * @throws IOException if the server cannot be started
//     */
//    @BeforeEach
//    public void setUp() throws IOException {
//        // Create a server on a random available port.
//        server = HttpServer.create(new InetSocketAddress(0), 0);
//        port = server.getAddress().getPort();
//
//        // Define a simple context that returns a fixed HTML response.
//        server.createContext("/test", exchange -> {
//            String response = "<html><body><h1>Hello, World!</h1></body></html>";
//            exchange.sendResponseHeaders(200, response.getBytes().length);
//            try (OutputStream os = exchange.getResponseBody()) {
//                os.write(response.getBytes());
//            }
//        });
//        server.start();
//    }
//
//    /**
//     * Stops the embedded HTTP server after each test.
//     */
//    @AfterEach
//    public void tearDown() {
//        if (server != null) {
//            server.stop(0);
//        }
//    }
//
//    /**
//     * Tests that the UrlFetchingService fetches content from the test server using simple url retriving
//     * @throws Exception if the fetch operation fails
//     */
//    @Test
//    public void testFetchSimpleUrl() throws Exception {
//        // Build the test URL using the local server address.
//        String testUrl = "http://localhost:" + port + "/test";
//
//        // Call the service to fetch the URL.
//        CompletableFuture<FetchResponse> futureResponse = urlFetchingService.fetchUrl(testUrl);
//
//        // Wait for the response (timeout after 5 seconds).
//        FetchResponse response = futureResponse.get(5, TimeUnit.SECONDS);
//
//        // Verify that the response has a 200 status code.
//        assertEquals(200, response.statusCode(), "Expected HTTP status 200");
//
//        // Check that the HTML contains our expected content.
//        assertTrue(response.htmlContent().contains("Hello, World!"),
//                "Expected HTML content to contain 'Hello, World!'");
//
//        // Optionally, verify that the markdown conversion contains expected text.
//        assertTrue(response.markdownContent().contains("Hello, World!"),
//                "Expected Markdown content to contain 'Hello, World!'");
//    }
//    
//    
//    /**
//     * Tests that the UrlFetchingService fetches content from the test server using selenium url retriving
//     * @throws Exception if the fetch operation fails
//     * TODO : Disabled because difficulties to install selenium on CI/CD 
//     */
////    @Test
////    public void testFetchSimpleSelenium() throws Exception {
////        // Build the test URL using the local server address.
////        String testUrl = "http://127.0.0.1:" + port + "/test";
////
////        // Call the service to fetch the URL.
////        CompletableFuture<FetchResponse> futureResponse = urlFetchingService.fetchUrl(testUrl);
////
////        // Wait for the response (timeout after 5 seconds).
////        FetchResponse response = futureResponse.get(5, TimeUnit.SECONDS);
////
////        // Verify that the response has a 200 status code.
////        assertEquals(200, response.statusCode(), "Expected HTTP status 200");
////
////        // Check that the HTML contains our expected content.
////        assertTrue(response.htmlContent().contains("Hello, World!"),
////                "Expected HTML content to contain 'Hello, World!'");
////
////        // Optionally, verify that the markdown conversion contains expected text.
////        assertTrue(response.markdownContent().contains("Hello, World!"),
////                "Expected Markdown content to contain 'Hello, World!'");
////    }
//}
