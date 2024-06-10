package org.open4goods.config;

import org.open4goods.config.yml.IcecatConfiguration;
import org.open4goods.services.BrandService;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.IcecatService;
import org.open4goods.services.RemoteFileCachingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

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
    public IcecatService xmlMapper(@Autowired RemoteFileCachingService remoteFileCachingService, BrandService brandService,GoogleTaxonomyService googleTaxonomyService) throws SAXException {
    	IcecatConfiguration c = new IcecatConfiguration();
    	// TODO : Not windows OK
        return new IcecatService(new XmlMapper(), c, remoteFileCachingService, "/tmp",brandService, googleTaxonomyService);
    }
    
    
}