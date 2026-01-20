

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.dto.openai.BatchJobResponse;
import org.open4goods.services.prompt.dto.openai.BatchOutput;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * TODO : Should split, into scoringController / resourceController, ....
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class GenAiController {


	private PromptService aiService;
	private BatchPromptService batchAiService;
	private ProductRepository repository;
	private VerticalsConfigService verticalsConfigservice;
	private ReviewGenerationService reviewGenerationService;



	private VerticalsConfigService verticalsConfigService;


	public GenAiController(PromptService aiService,  VerticalsConfigService verticalsConfigService, BatchPromptService batchAiService, ProductRepository repository, VerticalsConfigService verticalsConfigservice, ReviewGenerationService reviewGenerationService) {
		this.aiService = aiService;
		this.verticalsConfigService = verticalsConfigService;
		this.batchAiService = batchAiService;
		this.repository = repository;
		this.verticalsConfigservice = verticalsConfigService;
		this.reviewGenerationService = reviewGenerationService;
	}



	/**
     * Endpoint to check the current status of a batch job.
     *
     * @param jobId the identifier of the batch job.
     * @return the BatchJobResponse containing the job status.
     */
    @GetMapping("/batch/checkStatus")
    @Operation(summary = "Check batch job status by jobId")
    public BatchJobResponse checkStatus(@RequestParam String jobId) {
        return batchAiService.checkStatus(jobId);
    }


	/**
     * Endpoint to check the current status of a batch job.
     *
     * @param jobId the identifier of the batch job.
     * @return the BatchJobResponse containing the job status.
	 * @throws IOException
	 * @throws ResourceNotFoundException
     */
    @GetMapping("/batch/processResponse")
    @Operation(summary = "Process the response for a given jobId")
    public void trigger(@RequestParam String jobId) throws ResourceNotFoundException, IOException {
         reviewGenerationService.triggerResponseHandling(jobId);
    }


    /**
     * Endpoint to process and retrieve the response of a completed batch job.
     *
     * @param jobId the identifier of the batch job.
     * @return a PromptResponse containing a list of BatchOutput objects.
     */
    @GetMapping("/batch/processresponse")
    @Operation(summary = "Process and retrieve batch job response by jobId")
    public void processResponse() {
        reviewGenerationService.checkBatchJobStatuses();


    }

}
