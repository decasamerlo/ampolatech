# One repository for the backend and the web app

The backend and the web app live in this repository, in `backend/` and `web/`, with no monorepo tooling such as Nx or Turborepo: each folder keeps its own build. There is one developer and no mobile app, and most features span the API and its screens. In one repository, one issue and one pull request cover a feature, issue references stay local, and one Renovate config covers both builds.

nesto made the opposite choice: separate repositories for its backend, web and mobile apps, tied together by a meta-repo ([its ADR 003](https://github.com/decasamerlo/nesto/blob/main/docs/adr/003-polyrepo-over-monorepo.md)). That ADR records the cost: referencing an issue across repositories needs a commit trailer and a hook. AmpolaTech has no need for that separation.
