

package org.open4goods.api.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.services.prompt.dto.openai.BatchJobResponse;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Controller exposing batch AI job management endpoints.
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class GenAiController {

    private final BatchPromptService batchAiService;

    public GenAiController(BatchPromptService batchAiService) {
        this.batchAiService = batchAiService;
    }

    /**
     * Checks the current status of a batch job.
     *
     * @param jobId the identifier of the batch job.
     * @return the BatchJobResponse containing the job status.
     */
    @GetMapping("/batch/checkStatus")
    @Operation(summary = "Check batch job status by jobId")
    public BatchJobResponse checkStatus(@RequestParam String jobId) {
        return batchAiService.checkStatus(jobId);
    }
}
