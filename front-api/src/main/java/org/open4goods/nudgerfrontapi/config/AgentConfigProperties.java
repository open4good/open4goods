package org.open4goods.nudgerfrontapi.config;

import org.open4goods.nudgerfrontapi.config.properties.AgentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class enabling binding for frontend agent properties.
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfigProperties {

}
