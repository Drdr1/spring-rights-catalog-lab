package dev.derops.rights;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One real PostgreSQL 16 for every integration test class (started once, reused).
 * Liquibase migrates it on startup and Hibernate validates the mappings against it,
 * so a mismatch between entities and migrations fails the build.
 */
@SpringBootTest
// Tests authenticate with mock JWTs (SecurityMockMvcRequestPostProcessors.jwt()), so the
// issuer's JWKS is never fetched: Boot's issuer-based decoder is created lazily.
@TestPropertySource(properties = "rights.metadata.base-url=http://localhost:1")
public abstract class PostgresTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
