package dev.derops.rights;

import static org.assertj.core.api.Assertions.assertThat;

import dev.derops.rights.availability.AvailabilityQueries;
import dev.derops.rights.title.TitleQueries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proves the two tuned queries are served by their indexes on realistic data volumes
 * (200,000 titles, 80,000 license windows).
 * EXPLAIN runs on the exact SQL constants the application uses, with literal values
 * substituted for the named parameters.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryPlanIT extends PostgresTestBase {

    @Autowired JdbcTemplate db;

    @BeforeAll
    void seed() {
        db.execute("TRUNCATE license_window, title RESTART IDENTITY CASCADE");
        // 200,000 titles (a realistic catalog size; on a tiny table a full scan is cheaper
        // than any index, so the planner would rightly ignore it) plus one we search for.
        db.execute("""
                INSERT INTO title (name, kind, release_year)
                SELECT 'Catalog Title ' || md5(g::text), 'FILM', 1990 + (g % 35)
                FROM generate_series(1, 200000) g
                """);
        db.execute("INSERT INTO title (name, kind) VALUES ('The Marigold Affair', 'FILM')");
        // 80,000 windows across 40 territories x 4 platforms, spread over ten years.
        db.execute("""
                INSERT INTO license_window (title_id, territory, platform, start_date, end_date, exclusive)
                SELECT 1 + (g % 20000),
                       chr(65 + (g % 40) / 26) || chr(65 + (g % 40) % 26),
                       (ARRAY['SVOD','AVOD','TVOD','LINEAR'])[1 + (g / 40) % 4],
                       DATE '2020-01-01' + (g % 3650),
                       DATE '2020-01-01' + (g % 3650) + 90,
                       false
                FROM generate_series(1, 80000) g
                """);
        db.execute("ANALYZE title");
        db.execute("ANALYZE license_window");
    }

    private String plan(String sql) {
        return String.join("\n", db.queryForList("EXPLAIN " + sql, String.class));
    }

    @Test
    void title_search_uses_the_trigram_index() {
        String sql = TitleQueries.SEARCH_SQL
                .replace(":term", "'marigold'")
                .replace(":limit", "25");
        String plan = plan(sql);
        assertThat(plan).as(plan).contains("ix_title_name_trgm");
    }

    @Test
    void the_old_title_search_could_not_use_any_index() {
        String old = "SELECT id, name FROM title WHERE lower(name) LIKE lower('%marigold%') ORDER BY name LIMIT 25";
        String plan = plan(old);
        assertThat(plan).as(plan).contains("Seq Scan on title").doesNotContain("ix_title_name_trgm");
    }

    @Test
    void availability_uses_the_lookup_index() {
        String sql = AvailabilityQueries.AVAILABLE_SQL
                .replace(":territory", "'AB'")
                .replace(":platform", "'SVOD'")
                .replace(":day", "DATE '2024-06-01'");
        String plan = plan(sql);
        assertThat(plan).as(plan).contains("ix_license_lookup");
    }

    @Test
    void the_old_availability_query_scanned_the_whole_table() {
        String old = """
                SELECT t.id, t.name FROM license_window l JOIN title t ON t.id = l.title_id
                WHERE upper(l.territory) = 'AB' AND lower(l.platform) = 'svod'
                  AND DATE '2024-06-01' BETWEEN l.start_date AND l.end_date
                """;
        String plan = plan(old);
        assertThat(plan).as(plan).contains("Seq Scan on license_window").doesNotContain("ix_license_lookup");
    }
}
