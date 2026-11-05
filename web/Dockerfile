# syntax=docker/dockerfile:1

# Built from the repository root, since the build generates the API's client
# from api/openapi.yaml. Dockerfile.dockerignore sends only api/ and web/.

# The Node image has neither corepack nor pnpm, so pnpm comes from the image
# pnpm publishes, as https://pnpm.io/docker shows for a Debian base.
FROM node:26.13.0-slim AS build
COPY --from=ghcr.io/pnpm/pnpm:12.16.1 /opt/pnpm /opt/pnpm
RUN ln -s /opt/pnpm/pnpm /usr/local/bin/pnpm
WORKDIR /workspace/web
COPY web/package.json web/pnpm-lock.yaml web/pnpm-workspace.yaml ./
RUN --mount=type=cache,id=pnpm,target=/var/cache/pnpm \
    pnpm install --frozen-lockfile --store-dir /var/cache/pnpm
COPY api/ /workspace/api/
COPY web/ /workspace/web/
RUN pnpm build

FROM nginx:1.30.6-alpine
COPY web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/web/dist/ /usr/share/nginx/html/
