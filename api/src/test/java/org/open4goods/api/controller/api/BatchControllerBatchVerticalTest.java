package org.open4goods.api.controller.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.DatasourcePurgeService;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@code POST /batch/{vertical}}: runs the batch of a known vertical, and answers 404 for an unknown one instead of
 * failing with a 500 on a null configuration (GOU-2280).
 */
@ExtendWith(MockitoExtension.class)
class BatchControllerBatchVerticalTest {

	@Mock
	private BatchService batchService;

	@Mock
	private AggregationFacadeService aggregationFacadeService;

	@Mock
	private VerticalsConfigService verticalsConfigService;

	@Mock
	private BatchPromptService batchPromptService;

	@Mock
	private DatasourcePurgeService datasourcePurgeService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		BatchController controller = new BatchController(batchService, aggregationFacadeService,
				verticalsConfigService, batchPromptService, datasourcePurgeService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
	}

	@Test
	void knownVerticalIsBatched() throws Exception {
		VerticalConfig washingMachine = new VerticalConfig();
		washingMachine.setId("washing-machine");
		when(verticalsConfigService.getConfigById("washing-machine")).thenReturn(washingMachine);

		mockMvc.perform(post("/batch/washing-machine")).andExpect(status().isOk());

		verify(batchService).batch(washingMachine);
	}

	@Test
	void unknownVerticalIsNotFound() throws Exception {
		mockMvc.perform(post("/batch/unknown-vertical")).andExpect(status().isNotFound());

		verifyNoInteractions(batchService);
	}
}
