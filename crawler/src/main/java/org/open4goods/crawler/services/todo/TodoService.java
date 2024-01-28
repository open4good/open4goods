package org.open4goods.crawler.services.todo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.crawler.extractors.Extractor;
import org.open4goods.helper.DocumentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;



@RestController
/**
 * This service is a bit "special", because it is only involved from a post-step tests (see gitlab-ci.yml).
 * It allows to parse the taglist.xml in order to aggregate TODO's in git
 * @author goulven
 */
public class TodoService {

	private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

	// A TOUDOU markup, in the form TOUDO(P1,0.5,design) : make a new feature 
	// the type, the priority, and the estimation can be swapped)
	public class Todo {
		private String component;
		private String priority;
		private String category;
		private String content;
		private String estimation;
		private String className;
		private String lineNumber;
		
		public Todo(String fileName, String lineNumber, String content) {
			this.className = fileName;
			this.lineNumber = lineNumber;

			// Check the within parenthesis

			int start = content.indexOf("(");
			int to = content.indexOf(")");

			if (start == -1 || to == -1) {
				this.content = content;
			} else {
				String[] frags = content.substring(start+1,to). split(",");
				for (int i = 0; i < frags.length; i++) {
					String tmp = frags[i].replace(',', '.').trim().toLowerCase();

					if (NumberUtils.isParsable(tmp)) {
						this.estimation = tmp;
					} else  {
                        switch (tmp) {
                            case "p1" -> priority = "1";
                            case "p2" -> priority = "2";
                            case "p3" -> priority = "3";
                            case "p4" -> priority = "4";
                            case "p5" -> priority = "5";
                            default -> category = tmp;
                        }
					}
				}

				// 		Setting the content
				this.content = content.substring(to+1).trim();
				if (this.content.startsWith(":")) {
					this.content=this.content.substring(1).trim();
				}
			}

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

		public String getEstimation() {
			return estimation;
		}

		public void setEstimation(String estimation) {
			this.estimation = estimation;
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
	 * Generates a CSV file
	 * @param todo
	 * @param destFile
	 * @throws Exception
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public void generateCsv(File todo, File destFile) throws Exception, IOException, XPathExpressionException {

		List<Todo> ret = getTodos(todo);

		// create mapper and schema
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Todo.class).withColumnSeparator(',').withHeader().withStrictHeaders(true).withUseHeader(true);
        // output writer
        ObjectWriter myObjectWriter = mapper.writer(schema);
        myObjectWriter.writeValue(destFile, ret);

	}



	/**
	 * Extracts the Todos from a taglist.cml
	 * @param taglistFile
	 * @return
	 * @throws Exception
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public List<Todo> getTodos(File taglistFile) throws Exception, IOException, XPathExpressionException {
		Document doc = DocumentHelper.cleanAndGetDocument(FileUtils.readFileToString(taglistFile, StandardCharsets.UTF_8));

		
		// Getting todos from taglist.txt
		NodeList files = Extractor.xpathMultipleEval(doc, "//file");

		// Iterating on each <file>
		List<Todo> ret = new ArrayList<>();
		for (int i = 0; i < files.getLength(); i++) {

			String fileName = files.item(i).getAttributes().getNamedItem("name").getTextContent();
			List<String> lineNumbers = Extractor.xpathMultipleEvalAsString(files.item(i), "./comments//lineNumber");
			List<String> comments = Extractor.xpathMultipleEvalAsString(files.item(i), "./comments//comment/comment");

			for (int j=0; j < lineNumbers.size(); j++) {
				ret.add(new Todo(fileName, lineNumbers.get(j) ,comments.get(j)));
			}
		}
		return ret;
	}


	/**
	 * 
	 * @param todos
	 * @return
	 */
	public TodoHistory historize(List<Todo> todos) {
		
		
		TodoHistory ret = new TodoHistory();
		ret.setDate(Date.from(Instant.now()));
		for (Todo todo : todos) {
			
			try {
				// Incrementing durations
				Double val;
				try {
					val = Double.valueOf(todo.getEstimation());
					int i;
				} catch (Exception e) {
					val=0.0;
				}
				
				String category = todo.getCategory();
				if (StringUtils.isEmpty(category)) {
					category = "UNDEFINED";
				}
				
				if (!ret.getEstimatedDurations().containsKey(category)) {
					ret.getEstimatedDurations().put(category, 0.0);
				}
				
				ret.getEstimatedDurations().put(category, ret.getEstimatedDurations().get(category) + val);
				ret.setTotalDuration(ret.getTotalDuration()+ val);
				
				
				// Incrementing counts
				if (!ret.getTasksCount().containsKey(category)) {
					ret.getTasksCount().put(category, 0);
				}
				
				ret.getTasksCount().put(category, ret.getTasksCount().get(category) + 1);
				ret.setTotalTasks(ret.getTotalTasks()+ 1);
			} catch (Exception e) {
				logger.error("Error in todo interpretation",e);
			}
			
						
		}
		
		return ret;
	}



}
