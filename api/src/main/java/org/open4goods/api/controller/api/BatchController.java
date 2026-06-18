package org.open4goods.api.controller.api;

import java.io.IOException;

import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.BatchService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.services.prompt.dto.openai.BatchJobResponse;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for batch operations (full-scan reprocessing, cleanup, AI batch job status).
 * Scoring and aggregation are in {@link ScoreController} and {@link AggregationController}.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class BatchController {

    private final VerticalsConfigService verticalConfigService;
    private final AggregationFacadeService aggregationFacadeService;
    private final BatchService batchService;
    private final BatchPromptService batchPromptService;

    public BatchController(BatchService batchService,
                           AggregationFacadeService aggregationFacadeService,
                           VerticalsConfigService verticalsConfigService,
                           BatchPromptService batchPromptService) {
        this.verticalConfigService = verticalsConfigService;
        this.aggregationFacadeService = aggregationFacadeService;
        this.batchService = batchService;
        this.batchPromptService = batchPromptService;
    }

    @PostMapping("/batch/tweak")
    @Operation(summary = "Launch the data cleanup routine, if implemented")
    public void batchCleanup() throws InvalidParameterException, IOException, InterruptedException {
        batchService.clean();
    }

    @PostMapping("/batch/clean-resources")
    @Operation(summary = "Launch the resource garbage collection routine to clean orphaned resources")
    public void cleanOrphanResources() {
        batchService.cleanOrphanResources();
    }

    @PostMapping("/batch")
    @Operation(summary = "Launch the full batch (scoring, aggregation, completion batch), iso has @Scheduled")
    public void batch() throws InvalidParameterException, IOException, InterruptedException {
        batchService.batch();
    }

    @PostMapping("/batch/{vertical}")
    @Operation(summary = "Batch a specific vertical")
    public void batchVertical(@PathVariable @NotBlank final String vertical)
            throws InvalidParameterException, IOException, InterruptedException {
        batchService.batch(verticalConfigService.getConfigById(vertical));
    }

    @GetMapping("/batch/ai-job/{jobId}/status")
    @Operation(summary = "Check AI batch job status by jobId")
    public BatchJobResponse checkAiBatchJobStatus(@PathVariable String jobId) {
        return batchPromptService.checkStatus(jobId);
    }
}
