package dev.derops.rights.title;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TitleRepository extends JpaRepository<Title, Long> {
    boolean existsByExternalId(String externalId);
}
