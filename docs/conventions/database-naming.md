# Database naming

How AmpolaTech names its PostgreSQL tables and the names built from them. It adapts nesto's [database-naming convention](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/database-naming.md), which gives the reasons for singular names, and departs from it on reserved nouns.

## Tables are singular

A table is named for one of its rows, in snake_case: `pharmacy`. A name built from a table embeds it unchanged: `pharmacy_pkey`, `fk_app_user_pharmacy_id`, `app_user_role_check`.

Name every table explicitly, in the Liquibase changeset and in the entity's `@Table`. Left implicit, Spring Boot derives the name from the entity class, so `PharmacyEntity` would map to `pharmacy_entity`.

## A reserved noun gets the `app_` prefix

PostgreSQL reserves `user` and `order` ([Appendix C](https://www.postgresql.org/docs/current/sql-keywords-appendix.html)), and a reserved name works only double-quoted, everywhere it appears. When a table's natural noun is reserved, and only then, the table takes the `app_` prefix: `app_user` and `app_order`, but `pharmacy`.

The prefix belongs to the table's name and the names built from it, such as `app_user_pkey`. Everything else keeps the plain noun: the domain (`User`, `Order`), the JPA entity that maps the table (`UserEntity`, whose `@Table` names `app_user`), and a column that points at the table (`user_id`, the column JPA derives from a `user` field).

nesto picks another noun instead, `account` for `user` and `purchase` for `order`. Neither works here: `account` would be read as the Google account a user signs in with ([ADR 0004](../adr/0004-google-sign-in.md)), and the glossary lists purchase, with request and sale, among the words to avoid for an order.
