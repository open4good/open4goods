package org.open4goods.config;

import org.open4goods.config.yml.IcecatFeatureConfiguration;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.IcecatFeatureService;
import org.open4goods.services.RemoteFileCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

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
    

    @Bean
    public IcecatFeatureService xmlMapper(@Autowired RemoteFileCachingService remoteFileCachingService) {
    	IcecatFeatureConfiguration c = new IcecatFeatureConfiguration();
    	// TODO : Not windows OK
        return new IcecatFeatureService(new XmlMapper(), c, remoteFileCachingService, "/tmp");
    }
    
    
}