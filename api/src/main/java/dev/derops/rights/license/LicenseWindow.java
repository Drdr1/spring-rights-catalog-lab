package dev.derops.rights.license;

import dev.derops.rights.title.Title;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** A period during which a title may be shown in one territory on one platform. Dates are inclusive. */
@Entity
@Table(name = "license_window")
public class LicenseWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private Title title;

    @Column(nullable = false, length = 2)
    private String territory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean exclusive;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected LicenseWindow() {
    }

    public LicenseWindow(Title title, String territory, Platform platform, LocalDate startDate,
                         LocalDate endDate, boolean exclusive) {
        this.title = title;
        this.territory = territory;
        this.platform = platform;
        this.startDate = startDate;
        this.endDate = endDate;
        this.exclusive = exclusive;
    }

    public Long getId() { return id; }
    public Title getTitle() { return title; }
    public String getTerritory() { return territory; }
    public Platform getPlatform() { return platform; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isExclusive() { return exclusive; }
}
