package dev.derops.rights.availability;

import dev.derops.rights.availability.AvailabilityQueries.AvailableTitle;
import dev.derops.rights.license.Platform;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityQueries queries;

    public AvailabilityController(AvailabilityQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    public List<AvailableTitle> available(
            @RequestParam @Pattern(regexp = "^[A-Za-z]{2}$") String territory,
            @RequestParam Platform platform,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queries.available(territory.toUpperCase(Locale.ROOT), platform.name(), date);
    }
}
