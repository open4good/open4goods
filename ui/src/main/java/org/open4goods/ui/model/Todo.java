package org.open4goods.ui.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
/**
 * An out of business object, because it represents T O D O in the code, as a  mapping from taglist.xml items.
 * Contains the (dirty) parsing logic of our customs (priority and category),  and helpers for manipulation from service and templates  
 */
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

		
		/**
		 * Stupid helper for helping restitution
		 * @return
		 */
		public String getCategoryOrUndefined () {
			return StringUtils.isEmpty(category) ? "undefined" : category;
		}
		
		/**
		 * Stupid helper for helping restitution
		 * @return
		 */
		public String getPriorityOrUndefined () {
			return StringUtils.isEmpty(priority) ? "undefined" : priority;
		}
		
		/**
		 * Stupid helper for helping restitution
		 * @return
		 */
		public String getComponentOrUndefined () {
			return StringUtils.isEmpty(component) ? "undefined" : component;
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