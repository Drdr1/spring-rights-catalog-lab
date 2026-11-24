package dev.derops.rights.license;

import dev.derops.rights.license.LicenseDtos.CreateWindow;
import dev.derops.rights.license.LicenseDtos.WindowView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/titles/{titleId}/licenses")
public class LicenseController {

    private final LicenseService service;

    public LicenseController(LicenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<WindowView> list(@PathVariable long titleId) {
        return service.forTitle(titleId).stream().map(WindowView::of).toList();
    }

    @PostMapping
    public ResponseEntity<WindowView> add(@PathVariable long titleId, @Valid @RequestBody CreateWindow request) {
        LicenseWindow created = service.add(titleId, request);
        return ResponseEntity
                .created(URI.create("/api/titles/" + titleId + "/licenses/" + created.getId()))
                .body(WindowView.of(created));
    }
}
