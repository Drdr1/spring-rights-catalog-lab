package dev.derops.rights.title;

import dev.derops.rights.title.TitleDtos.TitleSummary;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Hand-written SQL for the hot read path. The SQL is a public constant so the
 * query-plan test runs EXPLAIN on exactly the statement production uses.
 */
@Repository
public class TitleQueries {

    /**
     * Substring search. Served by the trigram index ix_title_name_trgm.
     * The earlier version, {@code lower(name) LIKE lower(...)}, wrapped the column
     * in a function and forced a sequential scan of every title.
     */
    public static final String SEARCH_SQL = """
            SELECT id, name, kind, release_year
            FROM title
            WHERE name ILIKE '%' || :term || '%'
            ORDER BY name
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public TitleQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<TitleSummary> search(String term, int limit) {
        return jdbc.sql(SEARCH_SQL)
                .param("term", escapeLike(term))
                .param("limit", limit)
                .query(TitleSummary.class)
                .list();
    }

    /** Treat %, _ and \ in user input literally instead of as wildcards. */
    static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
