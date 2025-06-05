

# The project: Spring Boot Starter - o4g-xwiki

Spring Boot Starter O4G-XWIKI offers services to retrieve xwiki objects via XWIKI REST api.


# Enabling the O4G-XWIKI starter

To add the o4g-xwiki starter to a Maven-based project,
add the following dependency:


	<dependencies>
		<dependency>
			<groupId>org.open4goods</groupId>
			<artifactId>o4g-xwiki-spring-boot-starter</artifactId>
			<version>0.0.1-SNAPSHOT</version
		</dependency>
	</dependencies>
----

For Gradle, use the following declaration:

	dependencies {
		implementation 'org.open4goods:o4g-xwiki-spring-boot-starter'
	}
----


# Data Model

# Services

* **getPages** TODO:

* **getPageList** TODO:
  
# Properties 

  The following properties should be set in the calling app
  
  Properties to set

// pour les appel autres que rest ex: /bin/view
xwiki.baseUrl= https://wiki.nudger.fr
// to access rest resources
xwiki.api.entryPoint= https://wiki.nudger.fr/rest
// targeted wiki
xwiki.api.wiki=xwiki


xwiki.httpsOnly= true
xwiki.media= json
  
  