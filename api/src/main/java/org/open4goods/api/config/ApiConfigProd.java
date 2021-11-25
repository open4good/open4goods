package org.open4goods.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"ic","prod"})
public class ApiConfigProd {


}
