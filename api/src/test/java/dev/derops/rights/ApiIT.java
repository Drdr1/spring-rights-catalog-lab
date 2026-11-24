package dev.derops.rights;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
class ApiIT extends PostgresTestBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    static RequestPostProcessor as(String... realmRoles) {
        return jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of(realmRoles))))
                .authorities(new dev.derops.rights.security.KeycloakRolesConverter());
    }

    @BeforeEach
    void clean() {
        db.execute("TRUNCATE license_window, title RESTART IDENTITY CASCADE");
    }

    private MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private long createTitle(String name, String externalId) throws Exception {
        var res = mvc.perform(postJson("/api/titles",
                        Map.of("name", name, "kind", "FILM", "releaseYear", 2024, "externalId", externalId))
                        .with(as("editor")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = json.readTree(res.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Map<String, Object> window(String territory, String platform, String start, String end, boolean excl) {
        return Map.of("territory", territory, "platform", platform, "startDate", start, "endDate", end,
                "exclusive", excl);
    }

    // ---------------------------------------------------------------- security

    @Test
    void unauthenticated_requests_get_401() throws Exception {
        mvc.perform(get("/api/titles")).andExpect(status().isUnauthorized());
    }

    @Test
    void health_is_public() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void viewer_can_read_but_not_create() throws Exception {
        mvc.perform(get("/api/titles").with(as("viewer"))).andExpect(status().isOk());
        mvc.perform(postJson("/api/titles", Map.of("name", "X", "kind", "FILM")).with(as("viewer")))
                .andExpect(status().isForbidden());
    }

    @Test
    void editor_cannot_add_license_windows() throws Exception {
        long id = createTitle("Editor Only", "ed-1");
        mvc.perform(postJson("/api/titles/" + id + "/licenses", window("EG", "SVOD", "2026-01-01", "2026-06-30", true))
                        .with(as("editor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void token_without_known_roles_is_forbidden() throws Exception {
        mvc.perform(get("/api/titles").with(as("offline_access"))).andExpect(status().isForbidden());
    }

    @Test
    void unmapped_methods_are_denied() throws Exception {
        long id = createTitle("Locked", "lock-1");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/titles/" + id).with(as("editor", "rights-manager")))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- titles

    @Test
    void create_returns_location_and_validates_input() throws Exception {
        mvc.perform(postJson("/api/titles", Map.of("name", "Dune", "kind", "FILM", "releaseYear", 2021))
                        .with(as("editor")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/titles/1"))
                .andExpect(jsonPath("$.name").value("Dune"));

        mvc.perform(postJson("/api/titles", Map.of("name", " ", "kind", "FILM")).with(as("editor")))
                .andExpect(status().isBadRequest());
        mvc.perform(postJson("/api/titles", Map.of("name", "Old", "kind", "FILM", "releaseYear", 1700))
                        .with(as("editor")))
                .andExpect(status().isBadRequest());
        mvc.perform(postJson("/api/titles", Map.of("name", "Bad kind", "kind", "PODCAST")).with(as("editor")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicate_external_id_is_a_conflict() throws Exception {
        createTitle("First", "dup-1");
        mvc.perform(postJson("/api/titles", Map.of("name", "Second", "kind", "FILM", "externalId", "dup-1"))
                        .with(as("editor")))
                .andExpect(status().isConflict());
    }

    @Test
    void search_is_case_insensitive_substring_and_treats_wildcards_literally() throws Exception {
        createTitle("The Marigold Affair", "m-1");
        createTitle("Marigold Returns", "m-2");
        createTitle("100% Proof", "p-1");
        createTitle("Unrelated", "u-1");

        mvc.perform(get("/api/titles").param("q", "MARIGOLD").with(as("viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Marigold Returns"))
                .andExpect(jsonPath("$[0].kind").value("FILM"));

        mvc.perform(get("/api/titles").param("q", "%").with(as("viewer")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("100% Proof"));
    }

    @Test
    void missing_title_is_404() throws Exception {
        mvc.perform(get("/api/titles/999").with(as("viewer"))).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- license windows

    @Test
    void exclusive_windows_cannot_overlap_but_can_touch() throws Exception {
        long id = createTitle("Exclusive Film", "ex-1");
        String url = "/api/titles/" + id + "/licenses";
        var manager = as("rights-manager");

        mvc.perform(postJson(url, window("eg", "SVOD", "2026-01-01", "2026-06-30", true)).with(manager))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.territory").value("EG"));
        // Overlaps by one day (June 30 is inclusive).
        mvc.perform(postJson(url, window("EG", "SVOD", "2026-06-30", "2026-12-31", true)).with(manager))
                .andExpect(status().isConflict());
        // A non-exclusive window may not overlap an exclusive one either.
        mvc.perform(postJson(url, window("EG", "SVOD", "2026-03-01", "2026-03-31", false)).with(manager))
                .andExpect(status().isConflict());
        // Starts the day after: allowed.
        mvc.perform(postJson(url, window("EG", "SVOD", "2026-07-01", "2026-12-31", true)).with(manager))
                .andExpect(status().isCreated());
        // Same dates, other platform or territory: allowed.
        mvc.perform(postJson(url, window("EG", "AVOD", "2026-01-01", "2026-06-30", true)).with(manager))
                .andExpect(status().isCreated());
        mvc.perform(postJson(url, window("SA", "SVOD", "2026-01-01", "2026-06-30", true)).with(manager))
                .andExpect(status().isCreated());

        mvc.perform(get(url).with(as("viewer")))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].startDate").value("2026-01-01"));
    }

    @Test
    void non_exclusive_windows_may_overlap_each_other() throws Exception {
        long id = createTitle("Shared Film", "sh-1");
        String url = "/api/titles/" + id + "/licenses";
        mvc.perform(postJson(url, window("US", "TVOD", "2026-01-01", "2026-12-31", false)).with(as("rights-manager")))
                .andExpect(status().isCreated());
        mvc.perform(postJson(url, window("US", "TVOD", "2026-06-01", "2027-05-31", false)).with(as("rights-manager")))
                .andExpect(status().isCreated());
    }

    @Test
    void invalid_windows_are_rejected() throws Exception {
        long id = createTitle("Bad Dates", "bd-1");
        String url = "/api/titles/" + id + "/licenses";
        mvc.perform(postJson(url, window("EG", "SVOD", "2026-06-30", "2026-01-01", false)).with(as("rights-manager")))
                .andExpect(status().isBadRequest());
        mvc.perform(postJson(url, window("EGY", "SVOD", "2026-01-01", "2026-02-01", false)).with(as("rights-manager")))
                .andExpect(status().isBadRequest());
        mvc.perform(postJson("/api/titles/999/licenses", window("EG", "SVOD", "2026-01-01", "2026-02-01", false))
                        .with(as("rights-manager")))
                .andExpect(status().isNotFound());
    }

    @Test
    void database_blocks_overlapping_exclusive_windows_even_without_the_app_check() throws Exception {
        long id = createTitle("Race Film", "race-1");
        db.update("INSERT INTO license_window (title_id, territory, platform, start_date, end_date, exclusive) "
                + "VALUES (?, 'EG', 'SVOD', '2026-01-01', '2026-06-30', true)", id);
        try {
            db.update("INSERT INTO license_window (title_id, territory, platform, start_date, end_date, exclusive) "
                    + "VALUES (?, 'EG', 'SVOD', '2026-06-01', '2026-12-31', true)", id);
            throw new AssertionError("the exclusion constraint should have rejected the second window");
        } catch (org.springframework.dao.DataIntegrityViolationException expected) {
            // ex_exclusive_overlap
        }
    }

    // ---------------------------------------------------------------- availability

    @Test
    void availability_uses_inclusive_dates_and_exact_territory_platform() throws Exception {
        long a = createTitle("Alpha", "a-1");
        long b = createTitle("Bravo", "b-1");
        var manager = as("rights-manager");
        mvc.perform(postJson("/api/titles/" + a + "/licenses", window("EG", "SVOD", "2026-01-01", "2026-01-31", true))
                .with(manager)).andExpect(status().isCreated());
        mvc.perform(postJson("/api/titles/" + b + "/licenses", window("EG", "SVOD", "2026-01-15", "2026-02-15", false))
                .with(manager)).andExpect(status().isCreated());
        mvc.perform(postJson("/api/titles/" + b + "/licenses", window("EG", "AVOD", "2026-01-01", "2026-12-31", false))
                .with(manager)).andExpect(status().isCreated());

        mvc.perform(get("/api/availability").param("territory", "eg").param("platform", "SVOD")
                        .param("date", "2026-01-31").with(as("viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[0].exclusive").value(true))
                .andExpect(jsonPath("$[1].name").value("Bravo"));

        mvc.perform(get("/api/availability").param("territory", "EG").param("platform", "SVOD")
                        .param("date", "2026-02-01").with(as("viewer")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Bravo"));

        mvc.perform(get("/api/availability").param("territory", "EG").param("platform", "SVOD")
                        .param("date", "2025-12-31").with(as("viewer")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void availability_validates_parameters() throws Exception {
        mvc.perform(get("/api/availability").param("territory", "EGY").param("platform", "SVOD")
                .param("date", "2026-01-01").with(as("viewer"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/availability").param("territory", "EG").param("platform", "RADIO")
                .param("date", "2026-01-01").with(as("viewer"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/availability").param("territory", "EG").param("platform", "SVOD")
                .param("date", "01/02/2026").with(as("viewer"))).andExpect(status().isBadRequest());
    }
}
