package org.open4goods.api.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.OpenDataGenerationScheduler;
import org.open4goods.model.RolesConstants;
import org.open4goods.services.opendata.model.OpenDataGenerationStatus;
import org.open4goods.services.opendata.service.OpenDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;

class OpenDataControllerTest {

	private final OpenDataService openDataService = mock(OpenDataService.class);
	private final OpenDataController controller = new OpenDataController(openDataService);

	@Test
	void generateAnswersAcceptedWhenThePassStarts() {
		when(openDataService.launchGeneration()).thenReturn(true);
		when(openDataService.status()).thenReturn(OpenDataGenerationStatus.idle());

		ResponseEntity<OpenDataGenerationStatus> response = controller.generate();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		verify(openDataService).launchGeneration();
	}

	@Test
	void generateAnswersConflictWhenRunningOrDisabled() {
		when(openDataService.launchGeneration()).thenReturn(false);
		when(openDataService.status()).thenReturn(OpenDataGenerationStatus.idle());

		assertThat(controller.generate().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void endpointsAreAdminOnly() {
		PreAuthorize preAuthorize = OpenDataController.class.getAnnotation(PreAuthorize.class);

		assertThat(preAuthorize.value()).isEqualTo("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')");
	}

	@Test
	void schedulerStartsTheWeeklyPassOnSundayNightByDefault() throws Exception {
		Scheduled scheduled = OpenDataGenerationScheduler.class.getMethod("generateWeekly").getAnnotation(Scheduled.class);
		new OpenDataGenerationScheduler(openDataService).generateWeekly();

		assertThat(scheduled.cron()).isEqualTo("${open-data-config.generation-cron:0 30 1 * * SUN}");
		assertThat(scheduled.zone()).isEqualTo("${open-data-config.generation-zone:Europe/Paris}");
		verify(openDataService).launchGeneration();
	}
}
