package org.open4goods.crawler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev","docker"})
public class CrawlerConfigDev {


}
