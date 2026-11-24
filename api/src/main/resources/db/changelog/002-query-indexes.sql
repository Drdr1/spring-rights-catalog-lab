--liquibase formatted sql

-- See docs/query-tuning.md for the before/after plans.

--changeset derops:002-title-search
-- Title search is a substring match (ILIKE '%term%'). A btree index cannot serve a
-- leading wildcard; a trigram GIN index can.
CREATE INDEX ix_title_name_trgm ON title USING gin (name gin_trgm_ops);
--rollback DROP INDEX ix_title_name_trgm;

--changeset derops:002-availability
-- Availability filters on equality columns first (territory, platform), then the date range.
CREATE INDEX ix_license_lookup ON license_window (territory, platform, start_date, end_date);
CREATE INDEX ix_license_title ON license_window (title_id);
--rollback DROP INDEX ix_license_lookup; DROP INDEX ix_license_title;
