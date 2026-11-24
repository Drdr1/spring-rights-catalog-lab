package dev.derops.rights.availability;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AvailabilityQueries {

    public record AvailableTitle(Long titleId, String name, LocalDate endDate, boolean exclusive) {
    }

    /**
     * Titles licensed in a territory on a platform on a given day. Served by ix_license_lookup
     * (territory, platform, start_date, end_date). The earlier version compared
     * {@code upper(territory)} and used {@code :day BETWEEN start_date AND end_date} with no
     * index, so every availability check read the whole license table.
     */
    public static final String AVAILABLE_SQL = """
            SELECT t.id AS title_id, t.name, l.end_date, l.exclusive
            FROM license_window l
            JOIN title t ON t.id = l.title_id
            WHERE l.territory = :territory
              AND l.platform = :platform
              AND l.start_date <= :day
              AND l.end_date >= :day
            ORDER BY t.name
            """;

    private final JdbcClient jdbc;

    public AvailabilityQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<AvailableTitle> available(String territory, String platform, LocalDate day) {
        return jdbc.sql(AVAILABLE_SQL)
                .param("territory", territory)
                .param("platform", platform)
                .param("day", day)
                .query(AvailableTitle.class)
                .list();
    }
}
