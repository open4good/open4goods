package org.open4goods.services.prompt.dto;

import java.util.List;

/**
 * Represents the body of a batch request entry.
 */
public class BatchRequestBody {
    private String model;
    private List<Message> messages;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
