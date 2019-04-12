package com.cloudera.cem.efm.service.heartbeat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "efm.heartbeat")
public class HeartbeatProperties {

    private Duration cleanupInterval = Duration.ofSeconds(60);

    private Retention metadata = new Retention();

    private Retention content = new Retention();

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public Retention getMetadata() {
        return metadata;
    }

    public void setMetadata(Retention metadataRetention) {
        this.metadata = metadata;
    }

    public Retention getContent() {
        return content;
    }

    public void setContent(Retention contentRetention) {
        this.content = content;
    }

    public static class Retention {

        private Duration maxAgeToKeep;
        private Integer maxCountToKeep;

        public Duration getMaxAgeToKeep() {
            return maxAgeToKeep;
        }

        public void setMaxAgeToKeep(Duration maxAgeToKeep) {
            this.maxAgeToKeep = maxAgeToKeep;
        }

        public Integer getMaxCountToKeep() {
            return maxCountToKeep;
        }

        public void setMaxCountToKeep(Integer maxCountToKeep) {
            this.maxCountToKeep = maxCountToKeep;
        }

        boolean isZeroRetentionConfigured() {
            return (maxAgeToKeep != null && maxAgeToKeep.isZero()) || (maxCountToKeep != null && maxCountToKeep.equals(0));
        }
    }

}
