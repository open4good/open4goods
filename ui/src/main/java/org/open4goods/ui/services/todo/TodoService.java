package org.open4goods.ui.services.todo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.commons.helper.DocumentHelper;
import org.open4goods.commons.helper.XpathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.annotation.PostConstruct;



/**
 * This service is a bit "special", because it is only involved from a post-step tests (see gitlab-ci.yml).
 * It allows to parse the taglist.xml in order to aggregate TODO's in git
 * @author goulven
 */
public class TodoService {

	private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

	private List<Todo> todos = new ArrayList<>();

	public TodoService(String string) {
		super();
		this.tagListPath = string;
	}

	private String tagListPath;	// A TOUDOU markup, in the form TOUDO(P1,0.5,design) : make a new feature 
	// the type, the priority, and the estimation can be swapped)
	
	public class Todo {
		private String component = "";
		private String priority = "";
		private String category = "";
		private String content = "";
		private String className = "";
		private String lineNumber = "";
		
		public Todo(String fileName, String lineNumber, String content, String component) {
			this.className = fileName;
			this.lineNumber = lineNumber;
			this.component = component;

			// Check the within parenthesis

			
			// Removing the potential ":" prefix 
			content = content.trim();
			if (content.startsWith(":")) {
				content = content.substring(1).trim();
			}
			
			int start = content.indexOf("(");
			int to = content.indexOf(")");

			// Checking if a valid (...), and ( first char
			if (start == -1 || start != 0  || to == -1) {
				this.content = content;
			} else {
				String[] frags = content.substring(start+1,to). split(",");
				
				if (frags.length != 2) {
					this.content = content;
				} else {
					// Testing each part
					for (int i = 0; i < frags.length; i++) {
						String tmp = frags[i].trim();
						
						Integer priority = getPriorityFrom(tmp);
						
						if (null != priority) {
							this.priority = String.valueOf(priority);
						} else {
							// Probably the category 
							this.category = tmp;
						}
					}
					
					
//			 		Setting the content
					this.content = content.substring(to+1).trim();
					if (this.content.startsWith(":")) {
						this.content=this.content.substring(1).trim();
					}
				}
			}

		}


		/**
		 * Try to extract a priority (from 1, ... P2,) 
		 * @param tmp
		 * @return
		 */
		private Integer getPriorityFrom(String tmp) {
			if (NumberUtils.isParsable(tmp)) {
				return Integer.valueOf(tmp);
			} else  {
				switch (tmp.toLowerCase()) {
				case "p1" : return 1;
				case "p2" : return 2; 
				case "p3" : return 3; 
				case "p4" : return 4; 
				case "p5" : return 5; 
				}
			}
			return null;
		}
		

		/**
		 * Handy method that generates the Github link
		 * @return
		 */
		public String href() {
			
			StringBuilder ret = new StringBuilder("https://github.com/open4good/open4goods/blob/main/");
			ret.append(component);
			ret.append("/src/main/java/");
			ret.append(className.replace('.', '/'));
			ret.append(".java#L");
			ret.append(lineNumber);
			return ret.toString();
			
		}

		public String getPriority() {
			return priority;
		}

		public void setPriority(String priority) {
			this.priority = priority;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getLineNumber() {
			return lineNumber;
		}

		public void setLineNumber(String lineNumber) {
			this.lineNumber = lineNumber;
		}

		public String getComponent() {
			return component;
		}

		public void setComponent(String component) {
			this.component = component;
		}


	}


	/**
	 * Download the taglist.xml file  
	 * @throws XPathExpressionException
	 * @throws IOException
	 * @throws Exception
	 */
	@PostConstruct
	public void process() throws XPathExpressionException, IOException, Exception {		
		File f =  File.createTempFile("todo", "todo");
		FileUtils.copyURLToFile(new URL( this.tagListPath), f);
		this.todos = loadTodos(f);		
		Files.delete(f.toPath());
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
	 * @return the count of TODO items by component
	 */
	public Map<String, Integer> byComponents() {
		   return todos.stream()
	                .collect(Collectors.groupingBy(
	                    Todo::getComponent, // Group by the component
	                    Collectors.summingInt(e -> 1) // Count each item
	                ));
	}
	
	/**
	 * 
	 * @return the count of TODO items by priority
	 */
	public Map<String, Integer> byPriority() {
		   return todos.stream()
	                .collect(Collectors.groupingBy(
	                    Todo::getPriority, // Group by the component
	                    Collectors.summingInt(e -> 1) // Count each item
	                ));
	}
	
	/**
	 * 
	 * @return the count of TODO items by component
	 */
	public Map<String, Integer> byCategory() {
		   return todos.stream()
	                .collect(Collectors.groupingBy(
	                    Todo::getCategory, // Group by the component
	                    Collectors.summingInt(e -> 1) // Count each item
	                ));
	}
	
	
	public List<Todo> getTodos() {
		return todos;
	}

	public void setTodos(List<Todo> todos) {
		this.todos = todos;
	}


}
