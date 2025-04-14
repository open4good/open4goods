package org.open4goods.services.prompt.dto.openai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a message contained in a batch choice.
 */
 public class BatchMessage {
	    @JsonProperty("role") String role;
	    @JsonProperty("content") String content;
	    @JsonProperty("refusal") Object refusal;
	    @JsonProperty("annotations") List<Object> annotations; // Can be refined later if needed

	    
		public BatchMessage() {
			super();
		}
		public BatchMessage(String role, String content) {
			super();
			this.role = role;
			this.content = content;
		}
		public String getRole() {
			return role;
		}
		public void setRole(String role) {
			this.role = role;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public Object getRefusal() {
			return refusal;
		}
		public void setRefusal(Object refusal) {
			this.refusal = refusal;
		}
		public List<Object> getAnnotations() {
			return annotations;
		}
		public void setAnnotations(List<Object> annotations) {
			this.annotations = annotations;
		}
	    
	    
 }
