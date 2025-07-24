package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataMetaDto;
import org.open4goods.nudgerfrontapi.service.OpenDataService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints exposing dataset metadata and downloads.
 */
@RestController
@RequestMapping("/opendata")
@Tag(name = "OpenData", description = "Dataset metadata and download")
public class OpenDataController {

    private final OpenDataService service;

    public OpenDataController(OpenDataService service) {
        this.service = service;
    }

    @GetMapping("/meta")
    @Operation(summary = "Dataset metadata", responses = {
            @ApiResponse(responseCode = "200", description = "Metadata returned")
    })
    public ResponseEntity<OpenDataMetaDto> meta() {
        OpenDataMetaDto body = new OpenDataMetaDto(
                service.countGtin(),
                service.countIsbn(),
                service.gtinFileSize(),
                service.isbnFileSize(),
                service.gtinLastUpdated(),
                service.isbnLastUpdated());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/gtin-open-data.zip")
    @Operation(summary = "Download GTIN dataset", responses = {
            @ApiResponse(responseCode = "200", description = "File download")
    })
    public ResponseEntity<Resource> downloadGtin() throws Exception {
        Resource res = service.gtinResource();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"gtin-open-data.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
    }

    @GetMapping("/isbn-open-data.zip")
    @Operation(summary = "Download ISBN dataset", responses = {
            @ApiResponse(responseCode = "200", description = "File download")
    })
    public ResponseEntity<Resource> downloadIsbn() throws Exception {
        Resource res = service.isbnResource();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"isbn-open-data.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
    }
}
