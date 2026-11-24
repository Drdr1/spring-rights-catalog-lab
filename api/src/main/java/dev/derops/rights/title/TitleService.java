package dev.derops.rights.title;

import dev.derops.rights.common.ConflictException;
import dev.derops.rights.common.NotFoundException;
import dev.derops.rights.title.TitleDtos.CreateTitle;
import dev.derops.rights.title.TitleDtos.TitleSummary;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TitleService {

    private final TitleRepository titles;
    private final TitleQueries queries;

    public TitleService(TitleRepository titles, TitleQueries queries) {
        this.titles = titles;
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public List<TitleSummary> search(String term, int limit) {
        return queries.search(term == null ? "" : term.trim(), Math.max(1, Math.min(limit, 100)));
    }

    @Transactional(readOnly = true)
    public Title get(long id) {
        return titles.findById(id).orElseThrow(() -> new NotFoundException("Title " + id + " not found"));
    }

    @Transactional
    public Title create(CreateTitle request) {
        if (request.externalId() != null && titles.existsByExternalId(request.externalId())) {
            throw new ConflictException("A title with external id " + request.externalId() + " already exists");
        }
        return titles.save(new Title(request.name().trim(), request.kind(), request.releaseYear(),
                request.externalId()));
    }
}
