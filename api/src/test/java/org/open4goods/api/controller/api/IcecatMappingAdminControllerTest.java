package org.open4goods.api.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.open4goods.icecat.model.IcecatMappingCoverage;
import org.open4goods.icecat.model.IcecatMappingRebuildJob;
import org.open4goods.icecat.model.IcecatMappingRebuildState;
import org.open4goods.icecat.services.IcecatMappingCoverageService;
import org.open4goods.icecat.services.IcecatMappingRebuildService;
import org.open4goods.icecat.services.StaleRegistryProjectionException;
import org.open4goods.model.RolesConstants;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.server.ResponseStatusException;

class IcecatMappingAdminControllerTest {

    @Test
    void exposesOnlyAdminReadEndpointsAndAnAsynchronousRebuildCommand() {
        IcecatMappingCoverageService coverageService = mock(IcecatMappingCoverageService.class);
        IcecatMappingRebuildService rebuildService = mock(IcecatMappingRebuildService.class);
        IcecatMappingAdminController controller = new IcecatMappingAdminController(coverageService, rebuildService);
        IcecatMappingCoverage coverage = new IcecatMappingCoverage(4, "a".repeat(64), 3, 2, 1, java.util.Map.of("tv", 1L));
        IcecatMappingRebuildJob job = new IcecatMappingRebuildJob(UUID.randomUUID(), IcecatMappingRebuildState.QUEUED,
                coverage.registryHash(), Instant.parse("2026-09-12T10:00:00Z"), null, null);
        when(coverageService.coverage(LocalDate.of(2026, 9, 12))).thenReturn(coverage);
        when(rebuildService.submit("\"" + coverage.registryHash() + "\"")).thenReturn(job);

        assertThat(controller.coverage(LocalDate.of(2026, 9, 12))).isEqualTo(coverage);
        assertThat(controller.rebuild("\"" + coverage.registryHash() + "\"").getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
        assertThat(controller.rebuild("\"" + coverage.registryHash() + "\"").getBody()).isEqualTo(job);
        assertThat(IcecatMappingAdminController.class.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')");
        assertThat(List.of(IcecatMappingAdminController.class.getDeclaredMethods()))
                .noneMatch(method -> method.isAnnotationPresent(PutMapping.class)
                        || method.isAnnotationPresent(DeleteMapping.class));
    }

    @Test
    void convertsStaleRebuildPreconditionsToConflictProblemDetails() {
        IcecatMappingCoverageService coverageService = mock(IcecatMappingCoverageService.class);
        IcecatMappingRebuildService rebuildService = mock(IcecatMappingRebuildService.class);
        when(rebuildService.submit("stale")).thenThrow(new StaleRegistryProjectionException("stale", "current"));
        IcecatMappingAdminController controller = new IcecatMappingAdminController(coverageService, rebuildService);

        assertThatThrownBy(() -> controller.rebuild("stale"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void validatesTheUnmappedCandidatePageBound() {
        IcecatMappingCoverageService coverageService = mock(IcecatMappingCoverageService.class);
        IcecatMappingAdminController controller = new IcecatMappingAdminController(coverageService,
                mock(IcecatMappingRebuildService.class));

        assertThatThrownBy(() -> controller.unmappedCategories(LocalDate.of(2026, 9, 12), 501))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
