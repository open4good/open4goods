/**
 * This controller allow export operations.
 *
 * @author gof
 *
 */

package org.open4goods.api.controller.api;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.SerialisationService;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;

@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StatsController {

	@Autowired
	private DataFragmentRepository datafragmentsRepository;

	@Autowired
	private DataFragmentStoreService datafragmentsStoreService;


	@Autowired
	private GoogleTaxonomyService taxonomyService;

	@Autowired
	SerialisationService serialisationService;

	private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

	@GetMapping("/api/stats/count")
	public Long fragmentsCount() {
		return datafragmentsRepository.count();
	}

	@PostMapping(path =  "/api/taxonomy/unmapped")
	public void unmappedTaxonomy(HttpServletResponse response) throws IOException {

		final CsvMapper CSV_MAPPER = new CsvMapper();
		response.setContentType("text/csv;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		
		SequenceWriter seqW = CSV_MAPPER.writer().writeValues(response.getWriter());
		seqW.write(Arrays.asList("category", "count"));

		for (Entry<String, Long> entry : taxonomyService.getUnMappedCategories().entrySet()) {
			seqW.write(Arrays.asList(entry.getKey(), entry.getValue()));
		}
		seqW.close();

	}

}
