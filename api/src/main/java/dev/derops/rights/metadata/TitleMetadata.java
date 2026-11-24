package dev.derops.rights.metadata;

/** What the external metadata provider returns for a title. */
public record TitleMetadata(String externalId, String synopsis, Integer runtimeMinutes, String posterUrl) {
}
