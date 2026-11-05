# nginx serves the web app and proxies the API, on one origin

The web app has an image of its own: nginx serves the built app and proxies `/api/` to the backend, so the browser sees one origin, which the session cookie of [ADR 0004](0004-google-sign-in.md) needs. This replaces what #2 first planned, Spring Boot serving the built web app. The backend serves the API alone, its image builds the backend alone, and each folder keeps its own build, as [ADR 0005](0005-one-repository.md) says. In development, Vite's dev server proxies `/api` the same way.

nginx does what a single-page app asks of its server with a few lines of config, in `web/nginx.conf`: a file of the build is served as a file, every other path outside `/assets/` answers `index.html`, so a page reloaded or opened from a link reaches the app's router, and each kind of answer gets the caching it allows.

## Considered options

- **Spring Boot serving the built app**, as #2 first planned. Spring Boot answers a path it doesn't map with a 404 problem, so a reloaded `/orders` gets JSON, and its support for client-side routing is an open issue ([spring-boot#27554](https://github.com/spring-projects/spring-boot/issues/27554)). It takes a `RouterFunction` that answers `index.html` outside `/api/**`, `/error` and `/assets/**`. The Spring Framework 7.0.10 reference's own example excludes only `/api/**` and `/error`, and on 2026-11-04 it answered the bundle with `index.html` as `text/html`, so the app never started. Its image would also need Node and pnpm to build the web app, or the backend's build would have to run the web app's.

## Consequences

- Two images to build and run instead of one. Compose builds both, and starts nginx once the backend is healthy.
- The backend gets each browser request from nginx, with the `Host` the browser asked for, `X-Forwarded-For` and `X-Forwarded-Proto`. No setting of the backend's turns on Spring Boot's handling of forwarded headers.
- What sits in front of nginx once deployed, TLS included, is #42's to decide.
