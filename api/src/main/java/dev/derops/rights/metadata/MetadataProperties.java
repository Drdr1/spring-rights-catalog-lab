package dev.derops.rights.metadata;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rights.metadata")
public record MetadataProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
