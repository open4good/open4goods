package org.open4goods.api.controller.api;

import static org.mockito.ArgumentMatchers.any;
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
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Routes of {@link AggregationController} (GOU-2274): the vertical runs are kept and delegate to the paged
 * sanitisation of the facade, and the single-product run is unchanged. An unknown vertical is a 404 (GOU-2280).
 */
@ExtendWith(MockitoExtension.class)
class AggregationControllerTest {

	@Mock
	private AggregationFacadeService aggregationFacadeService;

	@Mock
	private VerticalsConfigService verticalsConfigService;

	@Mock
	private ProductRepository repository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AggregationController controller = new AggregationController(aggregationFacadeService, verticalsConfigService,
				repository);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
	}

	@Test
	void verticalRunDelegatesToTheSanitisationOfThatVertical() throws Exception {
		VerticalConfig washingMachine = new VerticalConfig();
		washingMachine.setId("washing-machine");
		when(verticalsConfigService.getConfigById("washing-machine")).thenReturn(washingMachine);

		mockMvc.perform(post("/aggregate/verticals/washing-machine")).andExpect(status().isOk());

		verify(aggregationFacadeService).sanitizeVertical(washingMachine);
	}

	@Test
	void unknownVerticalIsNotFound() throws Exception {
		mockMvc.perform(post("/aggregate/verticals/unknown-vertical")).andExpect(status().isNotFound());

		verifyNoInteractions(aggregationFacadeService);
	}

	@Test
	void allVerticalsRunDelegatesToTheSanitisationOfEachVertical() throws Exception {
		mockMvc.perform(post("/aggregate/verticals")).andExpect(status().isOk());

		verify(aggregationFacadeService).sanitizeAllVerticals();
	}

	@Test
	void singleProductRunIsKept() throws Exception {
		when(repository.getById(any(Long.class))).thenReturn(new Product());

		mockMvc.perform(post("/aggregate/gtin/7333394145372")).andExpect(status().isOk());

		verify(aggregationFacadeService).sanitizeAndSave(any());
	}
}
