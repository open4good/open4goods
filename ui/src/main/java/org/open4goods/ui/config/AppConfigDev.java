package org.open4goods.ui.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev","docker"})
public class AppConfigDev {


}
