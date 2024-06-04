package org.open4goods.config;

import org.open4goods.config.yml.IcecatConfiguration;
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
<<<<<<< Upstream, based on origin/main
<<<<<<< Upstream, based on origin/main
    public IcecatService xmlMapper(@Autowired RemoteFileCachingService remoteFileCachingService) throws SAXException {
    	IcecatConfiguration c = new IcecatConfiguration();
    	// TODO : Not windows OK
<<<<<<< Upstream, based on origin/main
        return new IcecatService(new XmlMapper(), c, remoteFileCachingService, "/tmp");
=======
    public IcecatFeatureService xmlMapper(@Autowired RemoteFileCachingService remoteFileCachingService) {
    	IcecatFeatureConfiguration c = new IcecatFeatureConfiguration();
=======
    public IcecatService xmlMapper(@Autowired RemoteFileCachingService remoteFileCachingService) throws SAXException {
    	IcecatConfiguration c = new IcecatConfiguration();
>>>>>>> 464b249 icecat
    	// TODO : Not windows OK
<<<<<<< Upstream, based on origin/main
        return new IcecatFeatureService(new XmlMapper(), c, remoteFileCachingService, "/tmp");
>>>>>>> 666f12d First working taxonomy identification. But have to rework the design, (shared redis / scheduled loads)
=======
        return new IcecatService(new XmlMapper(), c, remoteFileCachingService, "/tmp");
>>>>>>> 464b249 icecat
=======
        return new IcecatService(new XmlMapper(), c, remoteFileCachingService, "/tmp",null);
>>>>>>> abed882 Brands : layout, icecat preloading, resolution mechanism against sustainalytics (first approach, buggy)
    }
    
    
}