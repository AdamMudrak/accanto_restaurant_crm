# Demo artwork originals

These images are editable source artwork for the demo restaurant. They are kept
outside `src/main/resources` so the application JAR does not contain a duplicate
copy. Runtime images are stored in the database; Liquibase migration
`103-seed-images.sql` already embeds byte-for-byte identical demo image data.

Do not rewrite an applied Liquibase changeset to update artwork. Use an additive
migration or the image upload APIs for subsequent changes.
