package org.open4goods.nudgerfrontapi.config;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "devsec"})
public class AiMockConfig {

    @Bean
    public OpenAiApi openAiCustomApi() {
        return new OpenAiApi("dummy-key");
    }

    @Bean
    public OpenAiApi perplexityApi() {
        return new OpenAiApi("dummy-key");
    }
}
