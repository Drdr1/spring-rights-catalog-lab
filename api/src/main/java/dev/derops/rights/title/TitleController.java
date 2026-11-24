package dev.derops.rights.title;

import dev.derops.rights.metadata.MetadataClient;
import dev.derops.rights.metadata.TitleMetadata;
import dev.derops.rights.common.NotFoundException;
import dev.derops.rights.title.TitleDtos.CreateTitle;
import dev.derops.rights.title.TitleDtos.TitleSummary;
import dev.derops.rights.title.TitleDtos.TitleView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/titles")
public class TitleController {

    private final TitleService service;
    private final MetadataClient metadata;

    public TitleController(TitleService service, MetadataClient metadata) {
        this.service = service;
        this.metadata = metadata;
    }

    @GetMapping
    public List<TitleSummary> search(@RequestParam(defaultValue = "") String q,
                                     @RequestParam(defaultValue = "25") int limit) {
        return service.search(q, limit);
    }

    @GetMapping("/{id}")
    public TitleView get(@PathVariable long id) {
        return TitleView.of(service.get(id));
    }

    @PostMapping
    public ResponseEntity<TitleView> create(@Valid @RequestBody CreateTitle request) {
        Title created = service.create(request);
        return ResponseEntity.created(URI.create("/api/titles/" + created.getId())).body(TitleView.of(created));
    }

    /** Enriches a title from the external metadata service. */
    @GetMapping("/{id}/metadata")
    public TitleMetadata metadata(@PathVariable long id) {
        Title title = service.get(id);
        if (title.getExternalId() == null) {
            throw new NotFoundException("Title " + id + " has no external id");
        }
        return metadata.fetch(title.getExternalId());
    }
}
