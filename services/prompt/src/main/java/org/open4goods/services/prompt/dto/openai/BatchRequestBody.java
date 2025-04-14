package org.open4goods.services.prompt.dto.openai;

import java.util.List;

/**
 * Represents the body of a batch request entry.
 */
public class BatchRequestBody {
    private String model;
    private List<BatchMessage> messages;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

	public List<BatchMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<BatchMessage> messages) {
		this.messages = messages;
	}


}
