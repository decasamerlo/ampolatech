# AmpolaTech

Software for compounding pharmacies that make sterile injectables and hormone pellets to order, each against a doctor's prescription.

The backend, in `backend/`, and the web app, in `web/`, keep their own builds ([ADR 0005](docs/adr/0005-one-repository.md)) and share the API's contract, `api/openapi.yaml` ([ADR 0006](docs/adr/0006-openapi-contract-first.md)). Each folder's README says how to build and run what it holds: [backend/README.md](backend/README.md) and [web/README.md](web/README.md). `docs/adr/` holds the decisions, and `docs/conventions/` the rules the code, the commits and the docs follow.

## Run the whole app

With Docker installed, from the repository root:

```bash
docker compose up --build
```

It builds the backend's and the web app's images from the checkout and starts three services:

| Service | On | Port variable |
| --- | --- | --- |
| `web`: nginx, serving the built app and proxying `/api/` to the backend, so the two share an origin ([ADR 0008](docs/adr/0008-nginx-serves-the-web-app.md)) | <http://localhost:8081> | `AMPOLATECH_WEB_PORT` |
| `backend`, under the `local` profile | <http://localhost:8080> | `AMPOLATECH_BACKEND_PORT` |
| `postgres` | `localhost:5432` | `AMPOLATECH_POSTGRES_PORT` |

The `local` profile seeds a demo pharmacy and its sales agent, whom the demo sign-in signs everyone in as: the web app shows that agent, and `curl localhost:8080/api/me` answers with them.

`docker compose up --build --wait` starts the same stack in the background and returns once the backend accepts connections and the web app answers, so a request sent right after it gets an answer. `docker compose down` stops it, and `docker compose down -v` also deletes the database's volume, so the next start creates it afresh. When another process holds one of the ports, set its variable to a free port in your shell.

Compose names the project after the folder that holds `compose.yaml`: the folder's name in lower case, with every character but `a`–`z`, `0`–`9`, `-` and `_` dropped. The project's name prefixes its containers, built images, network and volume. A checkout in a folder Compose names differently, such as a worktree, runs a stack of its own, on a database of its own: with its ports set as above, the stack runs beside one already running, and `docker compose down -v --rmi local` there removes it, its built images included. Two checkouts in folders Compose names alike, such as `ampolatech` and `AmpolaTech`, or two that `git clone` named `ampolatech`, are one project and share its containers and database: give every command in the second `-p <name>`, as in `docker compose -p <name> up`.
