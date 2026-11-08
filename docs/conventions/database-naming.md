# Database naming

How AmpolaTech names its PostgreSQL tables, the names built from them, and the columns its JPA entities map.

## Tables are singular

A table is named for one of its rows, in snake_case: `pharmacy`. A name built from a table embeds it unchanged: `pharmacy_pkey`, `fk_app_user_pharmacy_id`, `app_user_role_check`.

## Every table and column is named explicitly

An entity names its table in `@Table`, and every column it maps in the annotation that maps it: `@Column` for a field and `@JoinColumn` for an association, `id` and `name` included. Each name is spelled exactly as the Liquibase changeset spells it: `@Table(name = "app_user")`, `@Column(name = "pharmacy_id")`.

Left out, a name is derived. A table's comes from the entity class, so `PharmacyEntity` would map to `pharmacy_entity`. A column's comes from its field, through the physical naming strategy Spring Boot sets: `pharmacyId` maps to `pharmacy_id`, the column a reader expects. A column's name is written out all the same, for these reasons:

- **A derived name ties the column to the field.** Renaming `PharmacyEntity.name` to `displayName`, with no name in its `@Column`, points the mapping at a `display_name` column the schema doesn't have, and the app refuses to start. With `@Column(name = "name")` kept, the app starts after the same rename. The written name keeps the column, not the queries: a Spring Data method such as `findByIdAndPharmacyId` names the field, so the app still refuses to start after `UserEntity.pharmacyId` is renamed.
- **A reader can't always predict a derived name, and it has changed between versions.** At the Spring Boot version `backend/gradle/libs.versions.toml` pins, `addressLine1` maps to `address_line1`, `httpURL` to `httpurl` and `pointA` to `pointa`. An earlier Spring Boot mapped `line2Street` to `line2street` and `cep8Digits` to `cep8digits`, where the pinned one gives `line2_street` and `cep8_digits`. [author-values.md](author-values.md#explicit-over-magic) writes out a default a reader can't predict every time, and once one field of an entity needs a written name, writes out every field of it.
- **A written name can be searched for.** A search for `pharmacy_id` from its changeset lands on the field that maps it.

The naming strategy rewrites a written name too: `@Column(name = "pharmacyId")` still maps to `pharmacy_id`, and `@Table(name = "AppUser")` to `app_user`. The app starts with either, but a search for `pharmacy_id` or `app_user` misses both, hence the changeset's spelling.

## A reserved noun gets the `app_` prefix

PostgreSQL reserves `user` and `order` ([Appendix C](https://www.postgresql.org/docs/current/sql-keywords-appendix.html)), and a reserved name works only double-quoted, everywhere it appears. When a table's natural noun is reserved, and only then, the table takes the `app_` prefix: `app_user` and `app_order`, but `pharmacy`.

The prefix belongs to the table's name and the names built from it, such as `app_user_pkey`. Everything else keeps the plain noun: the domain (`User`, `Order`), the JPA entity that maps the table (`UserEntity`, whose `@Table` names `app_user`), and a column that points at the table (`user_id`, named in the entity that maps it, as `UserEntity` names `pharmacy_id`).

Another noun, `account` for `user` or `purchase` for `order`, would avoid the prefix. Neither works here: `account` would be read as the Google account a user signs in with ([ADR 0004](../adr/0004-google-sign-in.md)), and the glossary lists purchase, with request and sale, among the words to avoid for an order.
