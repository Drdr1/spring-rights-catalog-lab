package dev.derops.rights;

import static org.assertj.core.api.Assertions.assertThat;

import dev.derops.rights.security.KeycloakRolesConverter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRolesConverterTest {

    private static Jwt token(Map<String, Object> claims) {
        var b = Jwt.withTokenValue("t").header("alg", "none").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).subject("u1");
        claims.forEach(b::claim);
        return b.build();
    }

    @Test
    void maps_realm_roles_to_spring_roles() {
        var roles = new KeycloakRolesConverter()
                .convert(token(Map.of("realm_access", Map.of("roles", List.of("viewer", "rights-manager")))));
        assertThat(roles).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_VIEWER", "ROLE_RIGHTS_MANAGER");
    }

    @Test
    void missing_or_malformed_claim_gives_no_roles() {
        var converter = new KeycloakRolesConverter();
        assertThat(converter.convert(token(Map.of("scope", "openid")))).isEmpty();
        assertThat(converter.convert(token(Map.of("realm_access", "viewer")))).isEmpty();
        assertThat(converter.convert(token(Map.of("realm_access", Map.of("roles", "viewer"))))).isEmpty();
    }
}
