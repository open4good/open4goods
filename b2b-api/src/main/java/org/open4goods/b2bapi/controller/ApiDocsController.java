package org.open4goods.b2bapi.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Lightweight documentation viewer entry points for the generated OpenAPI spec.
 */
@Hidden
@Controller
public class ApiDocsController {

    @GetMapping(path = "/redoc", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> redoc() {
        return ResponseEntity.ok("""
                <!doctype html>
                <html lang="en">
                <head><title>Product Data API - Redoc</title></head>
                <body>
                  <redoc spec-url="/v3/api-docs"></redoc>
                  <script src="https://cdn.redoc.ly/redoc/latest/bundles/redoc.standalone.js"></script>
                </body>
                </html>
                """);
    }

    @GetMapping(path = "/scalar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> scalar() {
        return ResponseEntity.ok("""
                <!doctype html>
                <html lang="en">
                <head><title>Product Data API - Scalar</title></head>
                <body>
                  <script id="api-reference" data-url="/v3/api-docs"></script>
                  <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                </body>
                </html>
                """);
    }
}
