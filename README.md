
# The project
| [![Beta](https://github.com/open4good/open4goods/actions/workflows/testAndPublishBeta.yml/badge.svg?branch=main)](https://github.com/open4good/open4goods/actions/workflows/testAndPublishBeta.yml) | [![Production](https://github.com/open4good/open4goods/actions/workflows/releaseDeployProd.yml/badge.svg?branch=main)](https://github.com/open4good/open4goods/actions/workflows/releaseDeployProd.yml) |
|--|--|


An open source online comparator  that operates ecological scoring, following a common good mindset.
* financially contributing to the environmental cause, [through a repayment of 20%](https://raw.githubusercontent.com/open4good/open4goods/main/LICENSE) of the company incomes.
* provide products datasets in [open data](https://www.data.gouv.fr/fr/datasets/base-de-codes-barres-noms-et-categories-produits/)
* 
  This project is for now deployed on  the frenchy [nudger.fr](https://nudger.fr)

Technical metrics and maven site is deployed on Github Pages:
* [https://open4good.github.io/open4goods/](https://open4good.github.io/open4goods/)

Open4goods (o4g) is an open-source and open-data product aggregator, search engine and comparator. More over, it is a stack aiming at handling large product datasets identified by GTIN's. It is build upon Maven, Java, SpringBoot, Elasticsearch, Redis, and some other cool libraries. It is mainly designed to :

* **crawl and ingest product based datas** (merchant offers, reviews, brands ratings). This is the job of the [crawler](crawler/) sub-project. Designed to ingest any kind of data, if having an UPC/GTIN code.

* **aggregate data fragments into well structured product-data**, by verticals. Features are -among others- scoring, attributes merging and conflict detection, comments NLP processing... The business intelligence of product construction is weared in the [aggregation](aggregation/) sub-project, and the orchestration and product data construction is operated through the scalable [API](api/) component.

* **Renders the data through API's, UI's, and through open data sets**, exposed through the [API](api/) component.

* Make the truth available for everyone on **officially supported websites**, (only french [nudger.fr](https://nudger.fr) for now), through the [UI](ui/) component. The affiliation revenue business model is used to deliver ecological compensation, maintain the user service and the open-data delivery
  

# <i class="icon-upload"></i> How to contribute

There are several ways to contribute to the project.

## Use it
By buying your products on official sites, you "create" money through the affiliation system, that allows to reverse the ["by law" 20% environmental compensation](https://raw.githubusercontent.com/open4good/open4goods/main/LICENSE). By using it you will also able to provide us some feedbacks

## Speak about it 
Communication will be the lack of this Odyssey, any kind of help is welcome. Talking about this the project to your granny or even to your dog could help us a lot.

## Feedback us
Bugs and ideas are greatly welcome !  We have a [specific feedback system](https://github.com/open4good/open4goods/blob/a35a37032218a20f3020e717773f1a6633b78122/commons/src/main/java/org/open4goods/services/FeedbackService.java#L19) that allows you to report feedback to [Github Issues](https://github.com/open4good/open4goods/blob/a35a37032218a20f3020e717773f1a6633b78122/commons/src/main/java/org/open4goods/services/FeedbackService.java#L19) directly from your navigation on our frontends.

## Eco-score and vertical definitions
Verticals (eg : tv's, washing machines, ...) are defined through yaml configuration files. The ecoscore is part of the verticals configurations.

Our [verticals definitions are totaly open](https://github.com/open4good/open4goods/tree/main/verticals/src/main/resources/verticals), meaning that you are very welcome to : 

* **inspect them**,  by reviewing the [verticals configuration](https://github.com/open4good/open4goods/tree/main/verticals/src/main/resources/verticals%29)
* **comment / question**, by posting [issues](https://github.com/open4good/open4goods/issues) with your concerns
* **contribute**, by creating new verticals or by updating existing ones with [Pull Requests](https://github.com/open4good/open4goods/pulls)


## Geek it ! 

Contribute to the websites, on the UI, on the content, or on the data aspects. Quiet simple to set-up, you can run open4goods website localy with the below instructions. 

 
# Run in dev mode
We will see here how to run open4goods frontends and API's on tour computer.

### Software Requirements

You will need :

- [Java 21+](https://adoptopenjdk.net/)
- [Maven](http://maven.apache.org/install.html)
- [Elasticsearch](https://github.com/elastic/elasticsearch) and [Redis](https://redis.io/), that can be transparently provided through [Docker](https://docs.docker.com/engine/install/) and [Docker Compose](https://docs.docker.com/compose/install/)

### running through docker-compose

Elasticsearch and Redis are used by the open4goods project. A [Kibana instance](https://www.elastic.co/fr/kibana) is commented in the docker-compose.yml file, if you want to browse data's by yourself. Go to the project root, then start the compose file

```
docker-compose up
```
> Docker compose is managed by spring-boot-docker-compose. That means that the manual launching of docker-compose is not mandatory, containers will be started by the application if not present.

Elastic, kibana and Redis should be available on :
* Elastic Search : [http://localhost:9200](http://localhost:9200)
* Kibana : [http://localhost:5601](http://localhost:5601)
* Redis : [http://localhost:6379](http://localhost:6379)
> Note that you could have to raise your max map  args to be able to rune the Elastic image, see the [Hint's section](https://github.com/open4good/open4goods?tab=readme-ov-file#elastic-max-map-count)


### Building the open4goods project from code base

Jars are not published to any central repo (nor planned to, it seems not to make any particular sense). To build, please go into the project folder. Then :

```
mvn install
```

This will build and run tests, hope in your terminal you'll get a

 
```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for parent 0.0.1-SNAPSHOT:
[INFO]
[INFO] parent ............................................. SUCCESS [ 2.509 s]
[INFO] commons ............................................ SUCCESS [ 27.046 s]
[INFO] crawler ............................................ SUCCESS [ 8.990 s]
[INFO] verticals .......................................... SUCCESS [ 7.143 s]
[INFO] api ................................................ SUCCESS [ 11.289 s]
[INFO] ui ................................................. SUCCESS [ 7.895 s]
[INFO] test ............................................... SUCCESS [ 1.496 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 01:07 min
[INFO] Finished at: 2021-01-31T15:20:30+01:00
[INFO] ------------------------------------------------------------------------
```

### Launching

The open4goods project is packaged under the form of several SpringBoot web applications. You will probably want to launch :

* [API](api) component : Play with the data aspect and the business logic
* [UI](ui) component : Play with the user interface part of the question

#### Command line

From the project root folder, you can now launch the aspects of the platform you are interested in :

**UI**

```
java -Dspring.profiles.active=dev -jar ui/target/ui-[VERSION].jar

```


You should be able to access the open4goods user interface at [http://localhost:8082](http://localhost:8082)

**Api**

```
java -Dspring.profiles.active=dev -jar api/target/api-[VERSION].jar

```
You should be able to access the open4goods API at [http://localhost:8081](http://localhost:8081)

**Crawler**

You should not need to run a separate crawler, since an embedded one is instanciated in the API. However, if you want to play, or register as a open4goods web scrapper and help us in crawling the world, you could setup an individual crawler node.

```
java -Dspring.profiles.active=dev -jar target/bin/open4goods-crawler.jar

```

You should be able to access the open4goods crawler interface at [http://localhost:8080](http://localhost:8080)

#### Using an IDE

If using any kind of IDE (tested with Eclipse and Intellij), please import as maven or SpringBoot project, then run or debug from the following Application classes :

* UI : [ui/src/main/java/com/open4goods/ui/Ui.java](ui/src/main/java/com/open4goods/ui/Ui.java)
* API : [api/src/main/java/com/open4goods/api/Api.java](ui/src/main/java/com/open4goods/ui/Api.java)
* Crawler : [crawler/src/main/java/com/open4goods/crawler/Crawler.java](ui/src/main/java/com/open4goods/crawler/Crawler.java)

> Don't forget to specify the profile to use (probably you'll want "dev" to run in development mode)

## Loading some datas

For now, you have an up and running open4good platform. You will like to play with some datasets, and like it's not so easy to get them, we provide some live datas directly from our websites.

### Products auto-loading

A [special service](https://github.com/open4good/open4goods/blob/main/commons/src/main/java/org/open4goods/helper/DevModeService.java) will automaticaly load sample datas on applications startup, allowing you to easily play with the project.


## A note on the work directory

> TODO : Document


## A note on Xwiki

> TODO : Document

# How to's
  
> TODO : Document
  
## Play with the UI

> TODO : Document
##  Play with the data

> TODO : Document

### Ingest new data

> TODO : Document

#### Transform data : the aggregation mechanism





# Hints   

## Report issues from code
TODO
That means that simple PR's to TODO can directly make new products of enhance the user experience of all open4goods users.

## Elastic max map count
You will probably need to raise your max map  args in order to be able to start elastic
```
sudo  sysctl -w vm.max_map_count=262144
```

To permanently set the max virtual memory :

1. edit /etc/sysctl.conf
2. add line vm.max_map_count=262144
3. sudo service sysctl restart

## Elastic space crash

After a space crash, elastic indexes are locked. You can get hand back on them by using :

```
curl -XPUT -H "Content-Type: application/json" http://localhost:9200/_all/_settings -d '{"index.blocks.read_only_allow_delete": null}'

```
