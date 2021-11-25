# Add a HTML datasource  
A HTML datasource is also a yaml configuration file, whom conforms to [HtmlDataSourceProperties](commons/src/main/java/com/capsule/config/yml/datasource/HtmlDataSourceProperties.java). It bridges HTML pages containing datas on products into the platform, mapping HTML pages and associated resources to [DataFragments](commons/src/main/java/com/capsule/model/data/DataFragment.java). To handle this, the datasource configuration files are based on xpath and json expressions, and contains all classical websites crawling rules.  
> TODO : Syntax pointers  
> Thanks crawl4j  
  
Please, refers to existing configurations for use as examples.  
  
> TODO : [cdiscount.com.yml](api/src/main/resources/providers/ok/cdiscount.com.yml) la conf d'exemple pas compatible avec (modele fermé / ouvert des confs). Heberger des pages de tests sur official websites pour crawling de test  
 
 