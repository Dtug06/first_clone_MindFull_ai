-- Align the PostgreSQL column with the JPA mapping and the H2 test schema.
-- The original V6 migration used "value", which is reserved by H2; the domain
-- model consistently uses optionValue/option_value.

ALTER TABLE daily_question_options
    RENAME COLUMN value TO option_value;
