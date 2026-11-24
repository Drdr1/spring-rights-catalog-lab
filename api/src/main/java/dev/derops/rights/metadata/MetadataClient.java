package dev.derops.rights.metadata;

import dev.derops.rights.common.NotFoundException;
import dev.derops.rights.common.UpstreamException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client for the external metadata REST service (timeouts are set in MetadataConfig).
 * A provider 404 becomes our 404; any other failure or a timeout becomes a 502.
 */
@Component
public class MetadataClient {

    private final RestClient http;

    public MetadataClient(RestClient metadataRestClient) {
        this.http = metadataRestClient;
    }

    public TitleMetadata fetch(String externalId) {
        try {
            TitleMetadata body = http.get()
                    .uri("/titles/{id}", externalId)
                    .retrieve()
                    .body(TitleMetadata.class);
            if (body == null) {
                throw new UpstreamException("Metadata provider returned an empty body", null);
            }
            return body;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new NotFoundException("No metadata for " + externalId);
            }
            throw new UpstreamException("Metadata provider answered " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new UpstreamException("Metadata provider unreachable or too slow", e);
        }
    }
}
