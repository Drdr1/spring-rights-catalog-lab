package dev.derops.rights.license;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseRepository extends JpaRepository<LicenseWindow, Long> {

    List<LicenseWindow> findByTitleIdOrderByStartDate(Long titleId);

    /**
     * True if the proposed window overlaps an existing window for the same title, territory
     * and platform where either of the two is exclusive. Two non-exclusive windows may overlap.
     */
    @Query("""
            select count(l) > 0 from LicenseWindow l
            where l.title.id = :titleId
              and l.territory = :territory
              and l.platform = :platform
              and l.startDate <= :endDate
              and l.endDate >= :startDate
              and (l.exclusive = true or :exclusive = true)
            """)
    boolean existsConflict(@Param("titleId") Long titleId,
                           @Param("territory") String territory,
                           @Param("platform") Platform platform,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate,
                           @Param("exclusive") boolean exclusive);
}
