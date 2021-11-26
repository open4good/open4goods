

package org.open4goods.api.controller.api;

import java.util.Date;
import java.util.Map;

import javax.validation.constraints.NotBlank;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.data.DataFragment;
import org.open4goods.services.DataSourceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class DataSourceConfigurationsController {

	private @Autowired DataSourceConfigService datasourceConfigService;

	public @Autowired IndexationService indexationService;

	@GetMapping(path=UrlConstants.MASTER_API_DATASOURCES_CONFIG)
	@Operation(summary="List all availlable datasource configurations")
	public Map<String, DataSourceProperties> datasources () {
		return datasourceConfigService.getDatasourceConfigs();
	}

	@GetMapping(path=UrlConstants.MASTER_API_DATASOURCE_CONFIG_PREFIX+"{datasourceName}")
	@Operation(summary ="Get a specific datasource configuration")
	public DataSourceProperties datasource (@PathVariable @NotBlank final String datasourceName) {
		return datasourceConfigService.getDatasourceConfig(datasourceName);
	}

	@GetMapping(path=UrlConstants.MASTER_API_DATASOURCE_CONFIG_PREFIX+"{datasourceName}/lastindexed")
	@Operation(summary="Get the last indexed datafragment for a specific datasource")
	public DataFragment datasourceLastIndexed (@PathVariable @NotBlank final String datasourceName) {
		return indexationService.getLastIndexed(datasourceName);
	}

	@GetMapping(path=UrlConstants.MASTER_API_DATASOURCE_CONFIG_PREFIX+"{datasourceName}"+UrlConstants.MASTER_API_DATASOURCE_CONFIG_NEXT_SCHEDULE_SUFFIX)
	@Operation(summary="Get a specific datasource configuration")
	public Date datasourceNextSchedule ( @PathVariable @NotBlank final String datasourceName) {
		return datasourceConfigService.getNextSchedule(datasourceName);
	}

}
