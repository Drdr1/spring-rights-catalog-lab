package dev.derops.rights.metadata;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MetadataConfig {

    /** Short timeouts so a slow provider cannot tie up request threads. */
    @Bean
    RestClient metadataRestClient(RestClient.Builder builder, MetadataProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return builder.baseUrl(props.baseUrl()).requestFactory(factory).build();
    }
}
