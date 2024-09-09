package org.open4goods.ui.services.todo;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.open4goods.commons.helper.DocumentHelper;
import org.open4goods.commons.helper.XpathHelper;
import org.open4goods.ui.model.Todo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.annotation.PostConstruct;



/**
 * A service quick and dirty, that allows parsing of T O D O's (maven taglist.xml)
 * @author goulven
 */
public class TodoService implements HealthIndicator{

	private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

	private List<Todo> todos = new ArrayList<>();

	public TodoService(String tagList) {
		super();
		this.tagListPath = tagList;
	}

	private String tagListPath;	// A TOUDOU markup, in the form TOUDO(P1,design) : make a new feature 
	// the type, the priority, and the estimation can be swapped)
	



	/**
	 * Download the taglist.xml file  
	 * @throws XPathExpressionException
	 * @throws IOException
	 * @throws Exception
	 */
	@PostConstruct
	public void process() {
		File f = null;
		try {
			f =  File.createTempFile("todo", "todo");
			FileUtils.copyURLToFile(new URL( this.tagListPath), f);
			this.todos = loadTodos(f);		
		} catch (Exception e) {
			logger.error("Error while loading taglist file at {} ",tagListPath, e);
		} finally {
			if (null != f) {
				FileUtils.deleteQuietly(f);
			}
		}
	}

	/**
	 * Extracts the Todos from a taglist.cml
	 * @param taglistFile
	 * @return
	 * @throws Exception
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public List<Todo> loadTodos(File taglistFile) throws Exception, IOException, XPathExpressionException {
		Document doc = DocumentHelper.cleanAndGetDocument(FileUtils.readFileToString(taglistFile, StandardCharsets.UTF_8));

		
		// Getting todos from taglist.txt
		List<Node> todoNodes = XpathHelper.xpathMultipleEval(doc, "//file");

		// Iterating on each <file>
		List<Todo> ret = new ArrayList<>();
		for (int i = 0; i < todoNodes.size(); i++) {

			String fileName = todoNodes.get(i).getAttributes().getNamedItem("name").getTextContent();
			
			List<String> lineNumbers = XpathHelper.xpathMultipleEvalString(todoNodes.get(i), "./comments//lineNumber");
			List<String> comments = XpathHelper.xpathMultipleEvalString(todoNodes.get(i), "./comments//comment/comment");

			for (int j=0; j < lineNumbers.size(); j++) {
				ret.add(new Todo(fileName, lineNumbers.get(j) ,comments.get(j), detectComponent(fileName) ));
			}
		}
		return ret;
	}

	/**
	 * Extract the component from the filename
	 * NOTE : Must be updated in case of new sub-projects
	 * @param fileName
	 * @return
	 */
	private String detectComponent(String fileName) {
		
		String component = "";
		
		if (fileName.startsWith("org.open4goods.ui")) {
			component = "ui";
		} else if (fileName.startsWith("org.open4goods.commons")){
			component = "commons";
		} else if (fileName.startsWith("org.open4goods.api")){
			component = "api";
		} else if (fileName.startsWith("org.open4goods.admin")){
			component = "admin";
		} else if (fileName.startsWith("org.open4goods.crawler")){
			component = "crawler";
		} else {
			logger.warn("Cannot get TODO component for {}",fileName);
		}
		
		return component;
	}

	/**
	 * 
	 * @return the count of items by component
	 */
	public Map<String, Integer> byComponents() {
		   return todos.stream()
	                .collect(Collectors.groupingBy(
	                    Todo::getComponentOrUndefined, // Group by the component
	                    Collectors.summingInt(e -> 1) // Count each item
	                ));
	}
	
	/**
	 * 
	 * @return the count of items by priority
	 */
	public Map<String, Integer> byPriority() {
		   return todos.stream()
	                .collect(Collectors.groupingBy(
	                    Todo::getPriorityOrUndefined, // Group by the component
	                    Collectors.summingInt(e -> 1) // Count each item
	                ));
	}
	
	/**
	 * 
	 * @return the count of items by category
	 */
	public Map<String, Integer> byCategory() {
		   return todos.stream()
				   	
	                .collect(Collectors.groupingBy(
	                    Todo::getCategoryOrUndefined, // Group by the component
	                    Collectors.summingInt(e -> 1) // Count each item
	                ));
	}
	
	
	public List<Todo> getTodos() {
		return todos;
	}

	public void setTodos(List<Todo> todos) {
		this.todos = todos;
	}

	/**
	 * Custom healthcheck, simply goes to DOWN if 0 T O DOs, that could never happens.
	 */
	@Override
	public Health health() {
		
		Builder health;
		
		int eCount = todos.size();
		
		if (0 == eCount ) {
			health =  Health.down();
		} else {
			health =  Health.up();
		}

		return health.withDetail("todos_count", eCount).build();
	}


	
	
}
