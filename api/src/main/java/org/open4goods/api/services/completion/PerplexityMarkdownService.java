package org.open4goods.api.services.completion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.ai.AiSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

public class PerplexityMarkdownService {

	protected static final Logger logger = LoggerFactory.getLogger(PerplexityMarkdownService.class);

	
	// Markdown parser
	protected Parser parser = Parser.builder().build();
	

	

	/**
	 * Parse the perplexity bloc response
	 * @param content
	 * @return
	 */
	public String parsebloc(String content) {
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
	public List<AiSource> parsesources(String sources) {
		
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
	protected String markdownToHtml(String markdown) {
	        
	        Node document = parser.parse(markdown);
	        return HtmlRenderer.builder().build().render(document);
	    }
	 
	 
	 /**
	  * Replace the links reference with corresponding html tags
	  * @param input
	  * @return
	  */
	protected static String replaceSources(String input) {
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

	 
}
