# cache as most as possible in this multistage dockerfile.
FROM csanchez/maven:openjdk-15 as deps

WORKDIR /opt/capsule

#COPY --from=deps /root/.m2 /root/.m2
#COPY --from=deps /opt/capsule/ /opt/capsule

COPY commons/src /opt/capsule/commons/src
COPY commons/pom.xml /opt/capsule/commons/pom.xml

COPY test/src /opt/capsule/test/src
COPY test/pom.xml /opt/capsule/test/pom.xml

COPY crawler/src /opt/capsule/crawler/src
COPY crawler/pom.xml /opt/capsule/crawler/pom.xml

COPY ui/src /opt/capsule/ui/src
COPY ui/pom.xml /opt/capsule/ui/pom.xml

COPY api/src /opt/capsule/api/src
COPY api/pom.xml /opt/capsule/api/pom.xml

COPY aggregation/src /opt/capsule/aggregation/src
COPY aggregation/pom.xml /opt/capsule/aggregation/pom.xml

COPY pom.xml /opt/capsule/pom.xml

#COPY LICENCE /opt/capsule/LICENCE

# Layering commons & aggregation
RUN mvn -B -e -C -DexcludeArtifactIds=api,ui,crawler,test,openstartup org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline 

#Layering indicidually api, crawler and ui 
# TODO : validate effectiv layering
RUN mvn -B -e -C -DexcludeArtifactIds=api org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline 
RUN mvn -B -e -C -DexcludeArtifactIds=crawler org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline 
RUN mvn -B -e -C -DexcludeArtifactIds=ui org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline 


# if you have modules that depends each other, you may use -DexcludeArtifactIds as follows
# RUN mvn -B -e -C org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -DexcludeArtifactIds=module1

# Building javadoc 
#RUN mvn -f commons/pom.xml javadoc:javadoc

# Building mavensite, testing and packaging
RUN mvn -B  install

RUN mvn -B -DstagingDirectory=/opt/capsule/mavenSite site site:stage

#TODO : The Todo custom task
RUN mvn -B -pl crawler -Dit.test=TodoReporter#doReports test
 


#RUN mvn -B -e -C spring-boot:build-image 

VOLUME /tmp

COPY api/target/api-0.0.1-SNAPSHOT.jar /opt/capsule/capsule-api.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/capsule/capsule-api.jar"]