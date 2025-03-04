package org.open4goods.ui.controllers.ui;

import java.net.URLDecoder;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.serialisation.service.SerialisationService;
import org.open4goods.ui.services.BlogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller maps the classical verticals pages
 *
 * @author gof
 *
 */
public class VerticalBrandsController  extends AbstractVerticalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalBrandsController.class);

	public VerticalBrandsController( VerticalsConfigService verticalService, SearchService searchService, UiService uiService, String vertical, BlogService blogService, SerialisationService serialisationService) {
		super(verticalService, searchService, uiService, vertical, blogService, serialisationService);
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		ModelAndView ret = uiService.defaultModelAndView(("vertical-home"), request);

		String brand = request.getServletPath().substring(request.getServletPath().lastIndexOf('/')+1);
		
		if (StringUtils.isEmpty(brand)) {
			// TODO :(p2, design) : Throw invalid parameter / 404
			return null;
		}
		
		brand = URLDecoder.decode(brand,"UTF-8").toUpperCase();

		VerticalConfig config = verticalService.getConfigById(this.vertical);

		// TODO : strategy of injection of products for nativ SEO

		VerticalSearchRequest vRequest = buildDefaultRequest(ret, config);
		
		
		// Get the default subset, to have texts and so on
		VerticalSubset brandSubset = serialisationService.clone(config.getBrandsSubset());
		brandSubset.getCriterias().add(new SubsetCriteria("attributes.referentielAttributes.BRAND",SubsetCriteriaOperator.EQUALS, brand));
		// TODO(p3,i18n)
		brandSubset.getDescription().put("fr", getBrandDescription());
		brandSubset.getTitle().put("fr", config.i18n("fr").getVerticalHomeTitle() + " " + WordUtils.capitalizeFully(brand));
		
		vRequest.setBrandsSubset(brandSubset);
		
		// Searching the products
		VerticalSearchResponse vResponse = searchService.verticalSearch(config,vRequest);
		
		// Complete the view with standards verticals attributes
		completeResponse(request, ret, config, vResponse);
		ret.addObject("subset",brandSubset);

		return ret;
	}

	
	// TODO
	public String getBrandDescription () {
		
		
		return """
				
<div>
    <p><strong>Présentation de la marque Samsung</strong></p>
    <p>
        Fondée en 1938 en Corée du Sud, <strong>Samsung</strong> est l'une des entreprises les plus reconnues 
        dans le domaine de l'électronique grand public. Active dans de nombreux secteurs tels que les smartphones, 
        les appareils électroménagers et les téléviseurs, la marque est devenue un acteur majeur de l'industrie 
        technologique mondiale.
    </p>

    <p><strong>Parts de marchés et innovations dans les téléviseurs</strong></p>
    <p>
        Samsung domine depuis plusieurs années le marché mondial des téléviseurs, occupant régulièrement 
        la première place avec une part de marché significative, avoisinant les <em>20 à 30 %</em>, selon les études. 
        La marque a été à l'avant-garde des innovations dans ce domaine, introduisant des technologies telles que 
        les écrans <u>QLED</u>, qui améliorent la luminosité et les couleurs, et les téléviseurs <em>8K</em>, offrant 
        une résolution ultra-haute. Samsung s'illustre également par le design de ses modèles, avec des gammes comme 
        "The Frame" qui allient esthétique et performance.
    </p>

    <p><strong>Lieux de production</strong></p>
    <p>
        Les téléviseurs Samsung sont fabriqués dans plusieurs usines à travers le monde. Les principaux sites de production 
        se situent en <strong>Corée du Sud</strong>, mais également dans des pays comme le <strong>Vietnam</strong>, 
        <strong>Slovaquie</strong>, <strong>Hongrie</strong>, <strong>Inde</strong> et <strong>Mexique</strong>. 
        Cette répartition stratégique permet à Samsung de répondre efficacement à la demande mondiale et de réduire 
        les coûts logistiques.
    </p>
</div>
				
				
				
				""";
		
	}
	
}