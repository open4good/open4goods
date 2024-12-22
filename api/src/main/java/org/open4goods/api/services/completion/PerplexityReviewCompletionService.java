package org.open4goods.api.services.completion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.AiReview;
import org.open4goods.commons.model.AiSource;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.ai.GenAiService;
import org.open4goods.commons.services.ai.PromptResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class PerplexityReviewCompletionService  extends AbstractCompletionService{

	protected static final Logger logger = LoggerFactory.getLogger(PerplexityReviewCompletionService.class);

	// Markdown parser
	private Parser parser = Parser.builder().build();
	
	private GenAiService aiService;

	
	public PerplexityReviewCompletionService( GenAiService aiService, ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties) {
		// TODO(p3,design) : Should set a specific log level here (not "aggregation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());		
		this.aiService = aiService;
	}

	public void processProduct(VerticalConfig vertical, Product data) {
		
		try {
			logger.info("Completing reviews for {}",data);
			completePerplexity(vertical, data);
		} catch (Exception e) {
			logger.error("Error while compelting reviews with perplexity for {}",data, e);
		}
	}

	/**
	 * Operates the complexity completion process
	 * @param vConf
	 * @param data
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 */
	private void completePerplexity(VerticalConfig vConf, Product data) throws JsonParseException, JsonMappingException, ResourceNotFoundException, IOException {
		
		Map<String,Object> context = new HashMap<>();
		
		// Templates context feeding
		context.put("VERTICAL_NAME", (vConf.getI18n().get("fr").getVerticalHomeTitle()));
		context.put("PRODUCT_NAME", data.shortestOfferName());
		context.put("PRODUCT_BRAND", data.brand());
		context.put("PRODUCT_MODEL", data.model());
		context.put("PRODUCT_GTIN", data.gtin());

		// AI Prompting
		PromptResponse<CallResponseSpec> response = aiService.prompt("perplexity-review", context);
		
		AiReview review = new AiReview();
		String[] frags  = response.getRaw().split("\n##");
		review.setDescription(parsebloc(frags[0]));
		review.setPros(parsebloc(frags[1]));
		review.setCons(parsebloc(frags[2]));
		review.setReview(parsebloc(frags[3]));
		review.setDataQuality(parsebloc(frags[4]));
		review.setSources(parsesources(frags[5]));
		
		// TODO(p2, i18n) : internationalisation
		data.getAiReviews().put("fr", review);
		data.getDatasourceCodes().put(this.getClass().getSimpleName(), System.currentTimeMillis());
		
	}

	
	
	

	/**
	 * Parse the perplexity bloc response
	 * @param content
	 * @return
	 */
	private String parsebloc(String content) {
		String ret = null;
		
		int pos = content.indexOf("\n\n");
		if (pos ==-1) {
			ret = content;
		} else {
			ret = content.substring(pos);
		}
		
		ret = markdownToHtml(ret);
		
		ret = replaceSources(ret);
		return ret;
	}


	/**
	 * Tweaky method that parses the markdown sources
	 * @param sources
	 * @return
	 */
	private List<AiSource> parsesources(String sources) {
		
		List<AiSource> ret = new ArrayList<AiSource>();
		
		List<String> frags = Arrays.asList(sources.split("\n\\|"));
		frags = frags.subList(3, frags.size());
		
		frags.forEach(frag -> {
			
			String[] items = frag.split("\\|");
			
			Integer num = null;
			try {
				num = Integer.valueOf(items[0].replace("[", "").replace("]", "").trim());
			} catch (NumberFormatException e) {
				logger.error("Cannot parse source number",e);
			}
			String name = null;
			try {
				name = items[1];
			} catch (Exception e) {
				logger.error("Cannot parse source name",e);
			}
			
			String url = null;
			try {
				url = items[2];
			} catch (Exception e) {
				logger.error("Cannot parse source url",e);
			}
			String description = null;
			try {
				description = items[3];
			} catch (Exception e) {
				logger.error("Cannot parse source description",e);
			}
			
			
			AiSource source = new AiSource();
			source.setName(name);
			source.setDescription(description);
			source.setUrl(url);
			source.setNumber(num);
			
			ret.add(source);
		});
		
		
		return ret;
	}
	
	/**
	 * Markdown to html conversion
	 * @param markdown
	 * @return
	 */
	 public String markdownToHtml(String markdown) {
	        
	        Node document = parser.parse(markdown);
	        return HtmlRenderer.builder().build().render(document);
	    }
	 
	 
	 /**
	  * Replace the links reference with corresponding html tags
	  * @param input
	  * @return
	  */
	 public static String replaceSources(String input) {
	        // Regular expression to match [N] where N is an integer
	        String regex = "\\[(\\d+)\\]";
	        Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(input);

	        // Using StringBuffer to append replacements
	        StringBuffer result = new StringBuffer();

	        while (matcher.find()) {
	            String number = matcher.group(1); // Captures the number inside [N]
	            // Replace with the desired HTML structure
	            String replacement = String.format(
	                "<span class=\"source-span\">[<a class=\"source-link\" href=\"#review-source-%s\">%s</a>]</span>",
	                number, number
	            );
	            matcher.appendReplacement(result, replacement);
	        }
	        matcher.appendTail(result);

	        return result.toString();
	    }

	 
	 
	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		// TODO(p1, cost) : Review cache is working
		// TODO(p1, cost) : Add a filter on year or creationDate (do not process too much recent items)
		if (data.getDatasourceCodes().containsKey(this.getClass().getSimpleName())) {
			logger.info("Skipping perplexity review gen service, already exists");
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String getDatasourceName() {
		return this.getClass().getSimpleName();
	}
}
