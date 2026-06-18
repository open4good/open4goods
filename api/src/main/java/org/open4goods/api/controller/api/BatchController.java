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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for batch operations (full-scan reprocessing, cleanup, AI batch job status).
 * Scoring and aggregation are in {@link ScoreController} and {@link AggregationController}.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Batch", description = "Trigger the full scoring and aggregation batch pipeline, data-cleanup routines, "
        + "and monitor AI (OpenAI) batch job status. "
        + "Scoring-only and aggregation-only endpoints are in the Scoring and Aggregation groups respectively.")
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
    @Operation(
            summary = "Run the data-cleanup (tweak) routine",
            description = "Executes the ad-hoc data-cleanup routine implemented in BatchService.clean(). "
                    + "The exact operations performed depend on the current implementation and are typically "
                    + "one-shot fixes, deduplication passes or orphan-record removals. "
                    + "Check the BatchService source for the current behaviour.")
    @ApiResponse(responseCode = "200", description = "Cleanup routine completed")
    public void batchCleanup() throws InvalidParameterException, IOException, InterruptedException {
        batchService.clean();
    }

    @PostMapping("/batch/clean-resources")
    @Operation(
            summary = "Run resource garbage collection",
            description = "Scans stored resources (images, PDFs, cached responses) and removes any orphaned entries "
                    + "that no longer have a corresponding product document in Elasticsearch. "
                    + "Safe to run at any time; it does not affect live product data.")
    @ApiResponse(responseCode = "200", description = "Orphaned resource cleanup completed")
    public void cleanOrphanResources() {
        batchService.cleanOrphanResources();
    }

    @PostMapping("/batch")
    @Operation(
            summary = "Run the full batch pipeline",
            description = "Executes scoring, aggregation and AI completion batch in the same order as the nightly "
                    + "@Scheduled run. Use this to trigger a full refresh outside the normal schedule "
                    + "(e.g. after a major configuration change or a manual data import). "
                    + "Prefer /batch/{vertical} to limit the scope to a single category.")
    @ApiResponse(responseCode = "200", description = "Full batch pipeline completed")
    public void batch() throws InvalidParameterException, IOException, InterruptedException {
        batchService.batch();
    }

    @PostMapping("/batch/{vertical}")
    @Operation(
            summary = "Run the full batch pipeline for a specific vertical",
            description = "Executes the complete batch pipeline (feed fetch, scoring, aggregation, AI completion) "
                    + "scoped to the named vertical only. Faster than a global /batch run when only one category "
                    + "needs to be refreshed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch pipeline completed for the vertical"),
            @ApiResponse(responseCode = "404", description = "Vertical not found")
    })
    public void batchVertical(
            @Parameter(description = "Vertical identifier (e.g. 'tv', 'laptop', 'air-conditioner')", required = true)
            @PathVariable @NotBlank final String vertical)
            throws InvalidParameterException, IOException, InterruptedException {
        batchService.batch(verticalConfigService.getConfigById(vertical));
    }

    @GetMapping("/batch/ai-job/{jobId}/status")
    @Operation(
            summary = "Check the status of an AI batch job",
            description = "Queries the AI provider (OpenAI Batch API) for the current status of the batch job "
                    + "identified by jobId. Returns a BatchJobResponse containing status, "
                    + "progress counters and result file references when the job has completed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch job status returned"),
            @ApiResponse(responseCode = "404", description = "No batch job found for the given jobId")
    })
    public BatchJobResponse checkAiBatchJobStatus(
            @Parameter(description = "AI provider batch job identifier as returned by the review or scoring batch submission endpoint", required = true)
            @PathVariable String jobId) {
        return batchPromptService.checkStatus(jobId);
    }
}
