package org.open4goods.model.vertical;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageGenerationConfig {

    private String prompt;
    private boolean forceOverride;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isForceOverride() {
        return forceOverride;
    }

    public void setForceOverride(boolean forceOverride) {
        this.forceOverride = forceOverride;
    }
}
