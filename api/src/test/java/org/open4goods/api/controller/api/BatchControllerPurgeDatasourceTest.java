package org.open4goods.api.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.BatchService;
import org.open4goods.api.services.DatasourcePurgeReport;
import org.open4goods.api.services.DatasourcePurgeScope;
import org.open4goods.api.services.DatasourcePurgeService;
import org.open4goods.api.services.DatasourcePurgeStatus;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Contract of {@code POST /batch/purge-datasource} (GOU-2101): the scope defaults to OFFERS, the least destructive,
 * and ALL must be asked explicitly.
 */
@ExtendWith(MockitoExtension.class)
class BatchControllerPurgeDatasourceTest {

	private static final String RAKUTEN = "rakuten.com";

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
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void scopeDefaultsToOffers() throws Exception {
		when(datasourcePurgeService.start(RAKUTEN, DatasourcePurgeScope.OFFERS, true))
				.thenReturn(running(DatasourcePurgeScope.OFFERS, true));

		mockMvc.perform(post("/batch/purge-datasource").param("datasource", RAKUTEN))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.state").value("RUNNING"))
				.andExpect(jsonPath("$.scope").value("OFFERS"))
				.andExpect(jsonPath("$.report.scope").value("OFFERS"));

		verify(datasourcePurgeService).start(RAKUTEN, DatasourcePurgeScope.OFFERS, true);
	}

	@Test
	void scopeAllIsPassedThrough() throws Exception {
		when(datasourcePurgeService.start(RAKUTEN, DatasourcePurgeScope.ALL, false))
				.thenReturn(running(DatasourcePurgeScope.ALL, false));

		mockMvc.perform(post("/batch/purge-datasource").param("datasource", RAKUTEN).param("scope", "ALL")
				.param("dryRun", "false"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.scope").value("ALL"));

		verify(datasourcePurgeService).start(RAKUTEN, DatasourcePurgeScope.ALL, false);
	}

	@Test
	void unknownScopeIsRejected() throws Exception {
		mockMvc.perform(post("/batch/purge-datasource").param("datasource", RAKUTEN).param("scope", "ATTRIBUTES"))
				.andExpect(status().isBadRequest());

		verify(datasourcePurgeService, never()).start(anyString(), any(), anyBoolean());
	}

	private static DatasourcePurgeStatus running(DatasourcePurgeScope scope, boolean dryRun) {
		Instant now = Instant.now();
		return new DatasourcePurgeStatus(DatasourcePurgeStatus.State.RUNNING, RAKUTEN, scope, dryRun, now, now, null,
				new DatasourcePurgeReport(RAKUTEN, scope, dryRun, 0, 0, 0, 0, 0, List.of(), 0), null);
	}
}
