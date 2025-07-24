package org.open4goods.staticserver.config;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "static")
public class StaticUiConfig {

    private String rootFolder = File.separator + "opt" + File.separator + "open4goods" + File.separator;

    public File uiJarFile() {
        return new File(rootFolder + File.separator + "bin" + File.separator + "latest" + File.separator + "ui-latest.jar");
    }

    public String getRemoteCachingFolder() {
        return rootFolder + File.separator + ".cached" + File.separator;
    }

    public String getDatasourcesfolder() {
        return rootFolder + File.separator + "config" + File.separator + "datasources" + File.separator;
    }

    public String logsFolder() {
        return rootFolder + File.separator + "logs" + File.separator;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }
}
