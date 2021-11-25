# Add a new Datasource  

Platform supports [CSV data sources](commons/src/main/java/com/capsule/config/yml/datasource/CsvDataSourceProperties.java) and [HTML datasources](com.capsule/commons/src/main/java/com/capsule/config/yml/datasource/HtmlDataSourceProperties.java) . This is done through yaml configurations file, that have to be stored in the [datasource definition folder](api/src/main/resources/providers/ok/) . It's also possible to integrate API data enrichment or "non standard" data ingestion, but there is so far no clear contract around that.  
  
Datasources are regularly crawled (daily, weekly, monthly or custom cron). You can also force the API crawling from the API  
> TODO : Api link  
  
> **Good to now** : Non regression tests are operated through the scheduling of [DatasourceRegressionTest](api/src/test/java/com/capsule/api/datasources/DatasourceRegressionTest.java) to notify of data structure changes. To perform, the test section of your datasource is used to match the non-regression.  



  
## Add a CSV datasource  
* Please read the [add a CSV datasource](./add_CSV_datasource.md) documentation


## Add a HTML datasource  
* Please read the [add a HTML datasource](./add_HTML_datasource.md) documentation

  
#### Add a HTML datasource  
A HTML datasource is also a yaml configuration file, whom conforms to [HtmlDataSourceProperties](commons/src/main/java/com/capsule/config/yml/datasource/HtmlDataSourceProperties.java). It bridges HTML pages containing datas on products into the platform, mapping HTML pages and associated resources to [DataFragments](commons/src/main/java/com/capsule/model/data/DataFragment.java). To handle this, the datasource configuration files are based on xpath and json expressions, and contains all classical websites crawling rules.  
> TODO : Syntax pointers  
> Thanks crawl4j  
  
Please, refers to existing configurations for use as examples.  
  
> TODO : [cdiscount.com.yml](api/src/main/resources/providers/ok/cdiscount.com.yml) la conf d'exemple pas compatible avec (modele fermé / ouvert des confs). Heberger des pages de tests sur official websites pour crawling de test  



  
#### Setup and test your datasource  
  
- Place your new datasource file in the [datasource definition folder](api/src/main/resources/providers/ok/) .  
* Use the [DatasourceRegressionTest](api/src/test/java/com/capsule/api/datasources/DatasourceRegressionTest.java) to validate and tune your datasource(s), coma separated.  
* ``` mvn test -Dtest=DatasourceRegressionTest -Ddatasources=mysource1.yml,mysource2.yml ```  
  
  
> Yet, be aware that having a reasonable test section is really good to ensure non-regression on the datasource  
  
  
> TODO :bug build successful si datasources existent pas  
> TODO : detail verbosity params (WITH_DETAILS static variable)  
  
* That's it ! The datasource will be crawled periodically, according to configuration. You can manually trigger a datasource crawling from the orchestration API  
> TODO link  
  
* You should use your kibana dashboard or the "last seen endpoint" to validate and browse your data  
* Feel free to PR or submit your configuration file if you want to see it integrated in official websites  
  
  