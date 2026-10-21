# Java and Spring Boot, React and TypeScript, PostgreSQL

The backend is Java with Spring Boot, in a hexagonal layout: a domain module with no Spring or database dependency, plus persistence and API adapters, as in [nesto's backend](https://github.com/decasamerlo/nesto). Persistence uses Spring Data JPA with Liquibase; JPA entities stay inside the persistence adapter and are mapped to domain objects there. The web app is a single-page React app in TypeScript, built with Vite and pnpm, and responsive so the tablets use the same app: TanStack Router and TanStack Query, Tailwind with shadcn-style components on Radix, and react-hook-form with zod. The database is PostgreSQL. The author knows Java and Spring best and reviews every change, which outweighs what the alternatives offer.

Every dependency stays on its newest release: Java on the latest LTS (25 today), and Spring Boot, React, TypeScript, Vite, PostgreSQL and everything else on their latest stable release. Renovate opens the update pull requests.

## Considered options

- **TypeScript end to end** (e.g., NestJS): one language and types shared between the web app and the backend, but the backend would be in the stack the author knows less well.
- **Next.js**: its strengths, server rendering and search-engine visibility, don't matter for an app behind a login.
