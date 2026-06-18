package org.open4goods.api.controller.api;

import java.util.Date;
import java.util.Map;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Admin endpoints for inspecting datasource (feed provider) configurations.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Datasources", description = "Inspect the configuration of all registered feed datasources "
        + "and query their next scheduled fetch time.")
public class DataSourceConfigurationsController {

	private final DataSourceConfigService datasourceConfigService;

	public DataSourceConfigurationsController(DataSourceConfigService datasourceConfigService) {
		this.datasourceConfigService = datasourceConfigService;
	}

	@GetMapping(path = UrlConstants.MASTER_API_DATASOURCES_CONFIG)
	@Operation(
			summary = "List all available datasource configurations",
			description = "Returns a map of datasource name to DataSourceProperties for every feed provider "
					+ "registered in the application configuration. "
					+ "Each entry describes the provider's URL patterns, parsing rules and scheduling parameters.")
	@ApiResponse(responseCode = "200", description = "Map of datasource name to configuration properties")
	public Map<String, DataSourceProperties> datasources() {
		return datasourceConfigService.datasourceConfigs();
	}

	@GetMapping(path = UrlConstants.MASTER_API_DATASOURCE_CONFIG_PREFIX + "{datasourceName}")
	@Operation(
			summary = "Get a specific datasource configuration",
			description = "Returns the DataSourceProperties for the named feed datasource. "
					+ "Properties include feed URL templates, parsing options and scheduling intervals.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Datasource configuration found and returned"),
			@ApiResponse(responseCode = "404", description = "No datasource with the given name is registered")
	})
	public DataSourceProperties datasource(
			@Parameter(description = "Exact datasource name as registered in the application configuration", required = true)
			@PathVariable @NotBlank final String datasourceName) {
		return datasourceConfigService.getDatasourceConfig(datasourceName);
	}

	@GetMapping(path = UrlConstants.MASTER_API_DATASOURCE_CONFIG_PREFIX + "{datasourceName}" + UrlConstants.MASTER_API_DATASOURCE_CONFIG_NEXT_SCHEDULE_SUFFIX)
	@Operation(
			summary = "Get the next scheduled fetch time for a datasource",
			description = "Returns the Date at which the scheduler will next trigger a feed fetch for the named datasource. "
					+ "Useful for diagnosing delayed or stuck feeds.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Next scheduled fetch date for the datasource"),
			@ApiResponse(responseCode = "404", description = "No datasource with the given name is registered")
	})
	public Date datasourceNextSchedule(
			@Parameter(description = "Exact datasource name as registered in the application configuration", required = true)
			@PathVariable @NotBlank final String datasourceName) {
		return datasourceConfigService.getNextSchedule(datasourceName);
	}
}
