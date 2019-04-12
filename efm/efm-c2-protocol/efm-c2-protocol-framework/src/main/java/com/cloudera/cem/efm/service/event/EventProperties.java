package com.cloudera.cem.efm.service.event;

import com.cloudera.cem.efm.model.EventSeverity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "efm.event")
public class EventProperties {

    private Duration cleanupInterval = Duration.ofSeconds(30);

    private Map<EventSeverity, Duration> maxAgeToKeep = new LinkedHashMap<>();

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    /*
     * Zero duration indicates do not persist at all.
     * Null indicates keep forever.
     */
    public Duration getMaxAgeToKeep(EventSeverity level) {
        return maxAgeToKeep.get(level);
    }

    public Map<EventSeverity, Duration> getMaxAgeToKeep() {
        return maxAgeToKeep;
    }

    public void setMaxAgeToKeep(Map<EventSeverity, Duration> maxAgeToKeep) {
        if (maxAgeToKeep == null) {
            throw new IllegalArgumentException("maxAgeToKeep cannot be set to null");
        }
        this.maxAgeToKeep = maxAgeToKeep;
    }


}
