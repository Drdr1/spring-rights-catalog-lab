package dev.derops.rights.license;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public final class LicenseDtos {

    private LicenseDtos() {
    }

    public record CreateWindow(
            @NotNull @Pattern(regexp = "^[A-Za-z]{2}$") String territory,
            @NotNull Platform platform,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            boolean exclusive) {
    }

    public record WindowView(Long id, Long titleId, String territory, Platform platform,
                             LocalDate startDate, LocalDate endDate, boolean exclusive) {
        static WindowView of(LicenseWindow w) {
            return new WindowView(w.getId(), w.getTitle().getId(), w.getTerritory(), w.getPlatform(),
                    w.getStartDate(), w.getEndDate(), w.isExclusive());
        }
    }
}
