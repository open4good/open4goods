package org.open4goods.api.config;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.dao.ProductRepository;
import org.open4goods.helper.DevModeService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev","docker"})
public class ApiConfigDev {


}
