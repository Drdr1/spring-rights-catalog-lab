package dev.derops.rights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.derops.rights.common.NotFoundException;
import dev.derops.rights.common.UpstreamException;
import dev.derops.rights.metadata.MetadataClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MetadataClientTest {

    MockRestServiceServer server;
    MetadataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://metadata.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MetadataClient(builder.build());
    }

    @Test
    void maps_a_successful_response() {
        server.expect(requestTo("http://metadata.test/titles/tt-42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"externalId":"tt-42","synopsis":"A heist.","runtimeMinutes":118,
                         "posterUrl":"https://img.test/tt-42.jpg"}""", MediaType.APPLICATION_JSON));
        var meta = client.fetch("tt-42");
        assertThat(meta.runtimeMinutes()).isEqualTo(118);
        assertThat(meta.synopsis()).isEqualTo("A heist.");
        server.verify();
    }

    @Test
    void provider_404_becomes_not_found() {
        server.expect(requestTo("http://metadata.test/titles/missing")).andRespond(withResourceNotFound());
        assertThatThrownBy(() -> client.fetch("missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void provider_5xx_becomes_upstream_error() {
        server.expect(requestTo("http://metadata.test/titles/tt-1")).andRespond(withServiceUnavailable());
        assertThatThrownBy(() -> client.fetch("tt-1")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void path_variables_are_encoded() {
        server.expect(requestTo("http://metadata.test/titles/a%2Fb")).andRespond(withResourceNotFound());
        assertThatThrownBy(() -> client.fetch("a/b")).isInstanceOf(NotFoundException.class);
        server.verify();
    }
}
