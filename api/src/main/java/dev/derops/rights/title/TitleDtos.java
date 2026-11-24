package dev.derops.rights.title;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TitleDtos {

    private TitleDtos() {
    }

    public record CreateTitle(
            @NotBlank @Size(max = 200) String name,
            @NotNull TitleKind kind,
            @Min(1888) @Max(2100) Integer releaseYear,
            @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String externalId) {
    }

    public record TitleView(Long id, String name, TitleKind kind, Integer releaseYear, String externalId) {
        static TitleView of(Title t) {
            return new TitleView(t.getId(), t.getName(), t.getKind(), t.getReleaseYear(), t.getExternalId());
        }
    }

    /** One row of the search result, read straight from SQL. */
    public record TitleSummary(Long id, String name, TitleKind kind, Integer releaseYear) {
    }
}
