package org.open4goods.services.remotefilecaching.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for {@code RemoteFileCachingService}.
 */
@Component
@ConfigurationProperties(prefix = "remote-file-caching")
public class RemoteFileCachingProperties {

    /** Connection timeout in milliseconds. */
    private int connectionTimeout = 30000;

    /** Read timeout in milliseconds. */
    private int readTimeout = 30000;

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
