package org.open4goods.config;

import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.RemoteFileCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

    @Bean
    public RemoteFileCachingService remoteFileCachingService() {
    	// TODO : from env variable
        return new RemoteFileCachingService("/tmp");
    }
    
    @Bean
	public GoogleTaxonomyService googleTaxonomyService(@Autowired RemoteFileCachingService remoteFileCachingService) {
		return new GoogleTaxonomyService(remoteFileCachingService);
	}
}