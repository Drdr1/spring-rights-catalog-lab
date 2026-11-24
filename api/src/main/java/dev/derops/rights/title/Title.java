package dev.derops.rights.title;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "title")
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TitleKind kind;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "external_id", length = 64, unique = true)
    private String externalId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Title() {
    }

    public Title(String name, TitleKind kind, Integer releaseYear, String externalId) {
        this.name = name;
        this.kind = kind;
        this.releaseYear = releaseYear;
        this.externalId = externalId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public TitleKind getKind() { return kind; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getExternalId() { return externalId; }
    public Instant getCreatedAt() { return createdAt; }
}
