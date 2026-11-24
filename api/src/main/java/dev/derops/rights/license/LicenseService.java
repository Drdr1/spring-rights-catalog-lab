package dev.derops.rights.license;

import dev.derops.rights.common.BadRequestException;
import dev.derops.rights.common.ConflictException;
import dev.derops.rights.license.LicenseDtos.CreateWindow;
import dev.derops.rights.title.Title;
import dev.derops.rights.title.TitleService;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenseService {

    private final LicenseRepository windows;
    private final TitleService titles;

    public LicenseService(LicenseRepository windows, TitleService titles) {
        this.windows = windows;
        this.titles = titles;
    }

    @Transactional(readOnly = true)
    public List<LicenseWindow> forTitle(long titleId) {
        titles.get(titleId);
        return windows.findByTitleIdOrderByStartDate(titleId);
    }

    /**
     * Adds a window. The overlap check gives a clear 409 message in the normal case; the
     * database exclusion constraint (ex_exclusive_overlap) still stops two concurrent
     * exclusive windows that both passed this check.
     */
    @Transactional
    public LicenseWindow add(long titleId, CreateWindow request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException("startDate must be on or before endDate");
        }
        Title title = titles.get(titleId);
        String territory = request.territory().toUpperCase(Locale.ROOT);
        if (windows.existsConflict(titleId, territory, request.platform(), request.startDate(),
                request.endDate(), request.exclusive())) {
            throw new ConflictException("Overlaps an existing window for " + territory + "/" + request.platform()
                    + " where one of them is exclusive");
        }
        return windows.saveAndFlush(new LicenseWindow(title, territory, request.platform(),
                request.startDate(), request.endDate(), request.exclusive()));
    }
}
