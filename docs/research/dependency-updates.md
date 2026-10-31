# Dependency updates: Dependabot or Renovate

Date: 2026-10-20

**Summary.** Renovate covers this repo better. Both tools update the Gradle version catalog and wrapper, npm and pnpm, Dockerfiles, Compose files and GitHub Actions. Dependabot does not update three places where AmpolaTech will pin Java: `mise.toml`, `JavaLanguageVersion.of(25)` in `build.gradle.kts`, and the `java-version` input of `actions/setup-java`. It also offers no way to add them. Renovate updates `mise.toml` and `setup-java` natively and reaches the toolchain through a regex manager. It also ships a rule that keeps Java runtimes on LTS releases. On a scratch repo with AmpolaTech's layout, that rule moved Java 21 to 25 (not 27) in all four places, in a single PR. Renovate also pins Docker and Actions digests, keeps a dashboard, and can hold an update until someone approves it.

The cost is trust and configuration. Renovate's hosted app is free for private repos (Mend Renovate Community Cloud), but Mend is a third party, its app has write access to code and workflows, and it runs one job at a time every four hours. Its `config:best-practices` preset also needs four explicit overrides before it does what the owner asked:

- npm's 3-day age wait raised to 7 days;
- Java exempted from the age wait, because its datasource has no release dates;
- the toolchain kept to a bare major version;
- `build` set as the commit type.

For security, keep Dependabot alerts on as the source of vulnerability data, turn Dependabot security updates off, and let Renovate raise the fix PRs.

How to read this file:

- Every claim links to the source it was checked against on 2026-10-20: docs.github.com, docs.renovatebot.com, GitHub Marketplace, the GitHub API, or source code. Code links are permalinks to the commit read that day: [renovatebot/renovate@7f7021a](https://github.com/renovatebot/renovate/tree/7f7021a63094d36a2f8f1b41ce5a263c71c406af) and [dependabot/dependabot-core@fda701f](https://github.com/dependabot/dependabot-core/tree/fda701f49c7d6ba5807e98a06549fdb935e3bd2e).
- **Observed** marks a result from running Renovate 44.172.0 locally (`--platform=local --dry-run=lookup`) on a scratch repo with AmpolaTech's file layout: a Gradle version catalog, the wrapper, `build.gradle.kts` with a toolchain, `mise.toml`, a Dockerfile, a Compose file, a workflow and a pnpm `package.json`. Three limits of that run:
  - Renovate warned that the local Node 24.6.0 is below its required `^24.11.0`.
  - Lookups that need a GitHub token were skipped (`actions/checkout`, Gradle in `mise.toml`).
  - Docker Hub refused unauthenticated tag pagination with 403s, so the run had no Docker release dates.
- Dependabot was not run. Its claims come from GitHub's docs and dependabot-core's source.
- Text marked **analysis** is reasoning from the cited facts, not a sourced claim.

## 1. Coverage

| Where the version lives | Dependabot | Renovate |
| --- | --- | --- |
| `gradle/libs.versions.toml` | Yes, without running Gradle ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#gradle)); parses `[libraries]` and `[plugins]` ([source](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/gradle/lib/dependabot/gradle/file_parser.rb#L278-L282)) | Yes, `gradle` manager, `*.versions.toml` and `gradle/*.toml` ([docs](https://docs.renovatebot.com/java/)) |
| Gradle wrapper | Yes, runs Gradle and updates `gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#gradle)) | Yes, `gradle-wrapper` manager, same four files ([docs](https://docs.renovatebot.com/java/)); conditions below |
| `JavaLanguageVersion.of(25)` | No | Not natively; a regex custom manager does it (observed, sketch in [section 5](#renovate)) |
| `mise.toml` (Java, Gradle, Node) | No; [feature request #12320](https://github.com/dependabot/dependabot-core/issues/12320) open | Yes, `mise` manager ([docs](https://docs.renovatebot.com/modules/manager/mise/)) |
| `package.json` + lockfile, npm | Yes, npm v7–v11 ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories)) | Yes, `npm` manager ([docs](https://docs.renovatebot.com/javascript/)) |
| Same, pnpm | Docs say v7–v10; the code also accepts 11 and 12 (below) | Yes; uses the pnpm version the repo asks for (below) |
| Dockerfile `FROM` tags | Yes, `docker` | Yes, `dockerfile` ([docs](https://docs.renovatebot.com/modules/manager/dockerfile/)) |
| `docker-compose.yml` image tags | Yes, `docker-compose` ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#docker-compose)) | Yes, `docker-compose` ([docs](https://docs.renovatebot.com/modules/manager/docker-compose/)) |
| Docker digest pinning | Updates a digest already present; adding one is not a documented option (below) | Adds and updates digests: `docker:pinDigests` is in `config:best-practices` ([preset](https://docs.renovatebot.com/presets-config/#configbest-practices)) |
| GitHub Actions `uses:` | Yes; updates refs already pinned to a SHA and a version comment on the same line ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#github-actions)) | Yes; `helpers:pinGitHubActionDigests`, in `config:best-practices`, pins to SHAs and keeps them current ([docs](https://docs.renovatebot.com/modules/manager/github-actions/)) |
| `actions/setup-java` `java-version:` | No; [feature request #15550](https://github.com/dependabot/dependabot-core/issues/15550) open | Yes, for the `temurin` and `adopt` distributions ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/manager/github-actions/known-actions/java-version-dynamic.ts#L12)) |

### Gaps and workarounds

- **Dependabot has no escape hatch.** The [options reference](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference) has no custom or regex matcher. When a contributor opened a pull request adding mise, a dependabot-core committer said it could not go ahead: "Our current approach for new ecosystems requires backing from the upstream maintainers to ensure long-term support, and since the Mise team isn't able to take that on right now, we can't proceed at this stage" ([comment on #14500](https://github.com/dependabot/dependabot-core/pull/14500#issuecomment-4245738799), 2026-04-14). A request for a plugin mechanism is open ([#15939](https://github.com/dependabot/dependabot-core/issues/15939)). With Dependabot, the Java version in the toolchain, `mise.toml` and `setup-java` is bumped by hand, and so are Node and Gradle in `mise.toml`.
- **Renovate's custom managers.** `customManagers` takes `regex` or `jsonata` matchers that extract a datasource, a package name and the current value from any file ([docs](https://docs.renovatebot.com/configuration-options/#custommanagers)). Renovate's Gradle parser already reads `JavaLanguageVersion.of(...)` ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/manager/gradle/parser/language-version.ts)). It uses that value only to pick the JDK that runs the wrapper ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/manager/gradle-wrapper/utils.ts#L54-L93)), not to update it. Observed: a regex manager on `build.gradle.kts` with the `java-version` datasource proposed `JavaLanguageVersion.of(25)` from 21, once `semver-partial` versioning was forced, as in the sketch. Without it, the LTS rule's versioning proposed `25.0.4+101.0.LTS`, which would not compile.
- **Renovate runs the wrapper only where allowed.** It updates the wrapper by running `./gradlew :wrapper` and rewrites `distributionSha256Sum` when the properties file has one ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/manager/gradle-wrapper/artifacts.ts#L156-L185)). Both happen only if `gradleWrapper` is in the global `allowedUnsafeExecutions` ([docs](https://docs.renovatebot.com/self-hosted-configuration/#allowedunsafeexecutions)), whose default is empty ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/options/index.ts#L3716-L3731)). When it is not allowed, the update skips this step without an error; an [open issue](https://github.com/renovatebot/renovate/issues/44232) proposes reporting it. Mend does not document the hosted app's value. A hosted job log a user quoted in June 2026 shows `"allowedUnsafeExecutions": ["gradleWrapper"]` ([discussion #43562](https://github.com/renovatebot/renovate/discussions/43562#discussioncomment-17264664)). That is secondary evidence; check the first wrapper PR. Self-hosted, the administrator sets it.
- **What Renovate's `mise` manager maps.** Java goes to the `java-version` datasource as `java-jdk`, from forms such as `25`, `temurin-25` or `corretto-25` ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/manager/mise/upgradeable-tooling.ts#L68-L81)). Node goes to `node-version`. Gradle resolves through the mise registry; observed, it was looked up as GitHub tags of `gradle/gradle-distributions`. Only the first version listed for a tool is updated ([docs](https://docs.renovatebot.com/modules/manager/mise/)).
- **pnpm versions.** GitHub's docs list pnpm v7–v10 ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories)). dependabot-core's source lists 11 and 12 as supported ([source](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/npm_and_yarn/lib/dependabot/npm_and_yarn/pnpm_package_manager.rb#L20-L25)): 11 arrived as beta on 2026-07-28 and 12 on 2026-09-15 ([history](https://github.com/dependabot/dependabot-core/commits/main/npm_and_yarn/lib/dependabot/npm_and_yarn/pnpm_package_manager.rb)). An [open issue](https://github.com/dependabot/dependabot-core/issues/16397) reports the docs as outdated. pnpm 12.0.0 was published on 2026-08-26 ([npm registry](https://registry.npmjs.org/pnpm), `time["12.0.0"]`), so Dependabot's support followed about three weeks later. Renovate installs the pnpm version named by `packageManager` or `engines` in `package.json`, or else implied by the lockfile ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/manager/npm/post-update/pnpm.ts#L55-L65)).
- **Dependabot and digests.** GitHub's docs say nothing about Docker digests. In the source, Dependabot updates a digest that is already pinned. It adds one only when an internal experiment flag, `docker_pin_digests`, is on ([source](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/docker/lib/dependabot/docker/update_checker.rb#L168-L178), [flag](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/docker/lib/dependabot/docker/update_checker.rb#L1055-L1057)). The repo cannot set that flag. To pin with Dependabot, write `image:tag@sha256:…` by hand once.
- **Actions lockfile.** Renovate also keeps GitHub's `.github/workflows/actions.lock` in sync, an alternative to inline SHAs that GitHub calls a public preview. Renovate marks this support as experimental ([docs](https://docs.renovatebot.com/modules/manager/github-actions/)).

## 2. Controls

| Control | Dependabot | Renovate |
| --- | --- | --- |
| Grouping | `groups` per ecosystem by `patterns`, `exclude-patterns`, `dependency-type`, `update-types`; `multi-ecosystem-groups` across ecosystems ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#groups--)) | `groupName` in any `packageRules` entry, across managers; `config:recommended` adds curated groups, including Spring Boot, Spring Core and React ([preset](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/group.preset.ts#L641-L653)) |
| Schedule | `schedule.interval` (`daily` to `yearly`, or `cron`), `day`, `time`, `timezone`; time is random by default ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#schedule-)) | `schedule` (cron recommended) and `timezone`, a window inside the times the bot runs at all ([docs](https://docs.renovatebot.com/key-concepts/scheduling/)) |
| Minimum release age | `cooldown`, version updates only ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#cooldown-)) | `minimumReleaseAge` ([docs](https://docs.renovatebot.com/configuration-options/#minimumreleaseage)); details below |
| Conventional Commits | `commit-message.prefix`, `prefix-development`, `include: scope` ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#commit-message--)) | `semanticCommits`, `semanticCommitType`, `semanticCommitScope` ([docs](https://docs.renovatebot.com/configuration-options/#semanticcommits)); see [section 5](#commit-messages) |
| Labels | `dependencies` plus an ecosystem label by default; `labels` replaces them ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#labels--)) | `labels`, `addLabels` ([docs](https://docs.renovatebot.com/configuration-options/#labels)) |
| Automerge | No built-in option; the documented route is a workflow with `dependabot/fetch-metadata` and `gh pr merge --auto` ([docs](https://docs.github.com/en/code-security/tutorials/secure-your-dependencies/automate-dependabot-with-actions)) | `automerge` per rule; uses GitHub's own auto-merge by default (`platformAutomerge`) ([docs](https://docs.renovatebot.com/configuration-options/#automerge)) |
| Dashboard | None. Job logs are listed from the dependency graph ([docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-job-logs)) | Dependency Dashboard issue, on in `config:recommended`: pending, open, closed and errored updates; force or retry from checkboxes ([docs](https://docs.renovatebot.com/configuration-options/#dependencydashboard)) |
| Hold until approved | No | `dependencyDashboardApproval` per package, manager or update type ([docs](https://docs.renovatebot.com/configuration-options/#dependencydashboardapproval)) |
| Open-PR limit | 5 version-update PRs by default; security PRs have a separate fixed limit of 10 ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#open-pull-requests-limit-)) | `prConcurrentLimit` 10, `prHourlyLimit` 2 by default ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/options/index.ts#L2524-L2537)) |

### Minimum release age

**Dependabot `cooldown` exists.** It delays version updates, not security updates, by a number of days. It takes `default-days`, `semver-major-days`, `semver-minor-days` and `semver-patch-days`, plus `include` and `exclude` lists of up to 150 names each ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#cooldown-)). The details that matter here:

- Even unconfigured, it applies "a default cooldown period of 3 days to version updates", and "this default cooldown does not apply to security updates" (same page).
- The per-SemVer-level days are not supported for Docker, Docker Compose or GitHub Actions; only `default-days` is (same page).
- For Docker, dependabot-core reads publication dates only from Docker Hub's `tag_last_pushed`. When no verified date exists, "Dependabot retains its existing policy: allow the update and record a `cooldown_date_unavailable` warning" ([README](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/docker/README.md#cooldown-publication-dates)). Without a date, Dependabot lets the update through.

**Renovate `minimumReleaseAge`.** Renovate checks the release date of each version separately. A branch is not created until the version passes, because `internalChecksFilter` defaults to `strict` ([docs](https://docs.renovatebot.com/key-concepts/minimum-release-age/)). Since Renovate 42, a release without a date counts as not yet old enough (`minimumReleaseAgeBehaviour=timestamp-required`, [default](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/options/index.ts#L2430-L2436)): without a date, the update is held. Two consequences for this repo:

- **Java never passes.** The `java-version` datasource, which serves Java in `mise.toml`, `setup-java` and the toolchain manager, publishes no release dates. It does not set `releaseTimestampSupport` ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/datasource/java-version/index.ts)), whose base default is `false` ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/datasource/datasource.ts#L71)). Observed: with a 7-day age, Renovate logged "Marking … release(s) as pending, as they do not have a releaseTimestamp" for every Java update. They became ordinary updates once `java-version` was exempted. Maven Central, Docker Hub, npm, Node and GitHub tags provide dates ([table](https://docs.renovatebot.com/key-concepts/minimum-release-age/#which-registries-support-release-timestamps)), and so does the Gradle version list ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/modules/datasource/gradle-version/index.ts#L24)). For Docker, that holds only on Docker Hub.
- **npm gets 3 days unless overridden.** `config:best-practices` includes `security:minimumReleaseAgeNpm`, a package rule that sets npm to 3 days ([preset](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/security.preset.ts#L24-L28), [rule](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/security.preset.ts#L84-L98)). Package rules override top-level settings. Observed: with a top-level `"minimumReleaseAge": "7 days"`, Renovate proposed react 19.3.1, published six days earlier. After adding an npm rule with 7 days, it proposed 19.3.0 instead.

Renovate also passes `--before=<date>` to npm when it regenerates a lockfile, so transitive packages respect the age too. For other package managers, its docs recommend also setting the age in the package manager's own config ([docs](https://docs.renovatebot.com/key-concepts/minimum-release-age/)).

### Separating majors and staying on the Java LTS line

**Renovate.** Renovate opens a separate branch for a major when a minor also exists (`separateMajorMinor`, default `true`). If you close a major's PR, Renovate does not reopen it until you make that upgrade yourself ([docs](https://docs.renovatebot.com/configuration-options/#separatemajorminor)). Observed: the Spring Boot plugin got `renovate/spring-boot` for 3.5.16 and `renovate/major-spring-boot` for 4.1.1.

For Java, `config:recommended`, and so `config:best-practices`, includes `workarounds:javaLTSVersions` ([docs](https://docs.renovatebot.com/java/)). It limits `allowedVersions` to `/^(?:8|11|17|21|25)(?:\.|-|$)/` for the Java runtime images on the Docker datasource (`eclipse-temurin`, `amazoncorretto` and others) and for the `java-version` datasource. It also keeps major-only tags like `25-jre` from turning into full versions ([preset](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/workarounds.preset.ts#L194-L301)). Observed: Adoptium reports 27 as the newest feature release and 25 as the newest LTS ([API](https://api.adoptium.net/v3/info/available_releases)). From 21, Renovate proposed 25 for the Dockerfile, `mise.toml`, `setup-java` and the toolchain, and with one `groupName` rule all four went into one branch, `renovate/major-java`.

The LTS list is written into the preset by hand:

- 25 was added on 2025-08-13 ([#37475](https://github.com/renovatebot/renovate/pull/37475)), before Java 25's GA in September 2025.
- 21 was added on 2023-10-12 ([#25175](https://github.com/renovatebot/renovate/pull/25175)), after Java 21's GA in September 2023.

Oracle says "the next planned LTS release is Java 29 in September 2027" ([roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)). If the list lags, Renovate keeps the repo on 25, which is safe. Adding `workarounds:javaLTSVersions` to `ignorePresets` turns the rule off.

**Dependabot.** Dependabot opens one PR per dependency unless `groups` say otherwise. A group's `update-types` keeps majors apart from minors and patches ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#update-types-groups)). Dependabot has no notion of LTS. `ignore` takes a dependency name plus `versions` ranges or `update-types` such as `version-update:semver-major`; Docker ranges use Bundler syntax ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#ignore--)). Docker requirements split on commas, so `">= 26, < 29"` is one range ([source](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/docker/lib/dependabot/docker/requirement.rb#L29-L37)). That keeps `eclipse-temurin` on 25 until 29 ships. Someone has to widen the range by hand every LTS cycle, and it covers only image tags, the one Java location Dependabot manages.

The other way to hold a version is a comment command such as `@dependabot ignore this major version`. That stores an ignore condition outside `dependabot.yml`; you read it back with `@dependabot show <name> ignore conditions` ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-pull-request-comment-commands)).

**PostgreSQL majors.** Neither tool knows that a new major of the `postgres` image cannot open the old data directory. PostgreSQL says "for major releases of PostgreSQL, the internal data storage format is subject to change". Moving the data takes `pg_dumpall` and restore, `pg_upgrade`, or logical replication ([docs](https://www.postgresql.org/docs/current/upgrading.html)). Renovate can make that PR wait for approval on the dashboard. With Dependabot, the PR simply opens.

## 3. Security updates

**Dependabot alerts and security updates.** Dependabot alerts are available for user-owned repositories ([docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-alerts)), and security updates for "all repositories on GitHub" ([docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-security-updates)). An alert triggers a PR "to the minimum version that includes the patch", and only for dependencies "specified in a manifest or lock file" (same page). Security PRs ignore `cooldown`. They have their own fixed limit of 10 open PRs ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#open-pull-requests-limit-)). Per ecosystem, for this repo:

- **Docker and Docker Compose:** no security updates ([table](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories)).
- **Gradle:** GitHub's dependency graph lists no Gradle file it reads statically; Gradle data comes only from automatic dependency submission ([table](https://docs.github.com/en/code-security/reference/supply-chain-security/dependency-graph-supported-package-ecosystems)). For security updates, "Gradle support is limited to manual uploads of the dependency graph data using the dependency submission API" ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#gradle)). That runs a workflow GitHub manages on every manifest change. It "counts toward your GitHub Actions minutes" ([docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependency-graph-data#automatic-dependency-submission)), which costs only while the repo is private. Even then, Dependabot cannot fix an alert on a transitive Gradle dependency, because it "isn't able to find the vulnerable dependency in the repository" ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#gradle)).
- **npm and pnpm:** GitHub reads `package-lock.json` and `pnpm-lock.yaml` statically, transitive dependencies included ([table](https://docs.github.com/en/code-security/reference/supply-chain-security/dependency-graph-supported-package-ecosystems)).
- **GitHub Actions:** "alerts are only generated for actions that use semantic versioning, not SHA versioning" ([docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-alerts)). Pinning actions to SHAs, as `config:best-practices` does, gives up GitHub's alerts for actions.

**Renovate.** By default, Renovate reads the repo's Dependabot alerts and raises fix PRs. That requires the dependency graph and Dependabot alerts to be on, and the app to have read access to Dependabot alerts ([docs](https://docs.renovatebot.com/configuration-options/#vulnerabilityalerts)); the Mend app requests it (section 4). Fix PRs ignore `schedule` and the hourly limits, have no concurrency limit by default, and go to the lowest fixed version unless `vulnerabilityFixStrategy` is `highest` (same page). Separately, `osvVulnerabilityAlerts: true` checks direct dependencies against a local copy of the OSV database, for `maven` and `npm` among other datasources ([docs](https://docs.renovatebot.com/configuration-options/#osvvulnerabilityalerts)). It does not need GitHub's Gradle graph. Neither source covers base-image vulnerabilities; that needs an image scanner.

**Using both.** Renovate's alert integration needs Dependabot alerts on, so the two coexist by design. GitHub's own docs say: "If your repository already uses an integration for dependency management, you will need to disable this before enabling Dependabot" ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#supported-ecosystems-maintained-by-github)). **Analysis:** keep Dependabot alerts on, with no version updates and no security updates, and let Renovate open the fixes. Otherwise both bots would open a PR for the same alert.

## 4. Operational cost and trust

**Dependabot** is part of GitHub. Version and security updates are available on every repository ([docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-version-updates), [docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-security-updates)). Its jobs run on GitHub Actions, and on standard runners they do not count toward included minutes ([docs](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-on-actions)). Workflows triggered by its PRs get a read-only `GITHUB_TOKEN` and only Dependabot secrets, not Actions secrets ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-on-actions)). GitHub treats them "as though they are from a forked repository" ([docs](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull-request-events-for-forked-repositories)). On a private personal repo it needs a `.github/dependabot.yml` and, for alerts, the dependency graph and Dependabot alerts switched on.

**Renovate, hosted by Mend.**

- **Name and price.** The GitHub App is "Renovate" by `mend`; its Marketplace listing reads "Mend Renovate | Dependency Update Automation". Its one plan, "Renovate Community Cloud", costs $0 and covers "Personal and org accounts" and "Public and private repos" ([Marketplace](https://github.com/marketplace/renovate)). Mend's docs call it Mend Renovate Community Cloud, "a generous free tier, available for all across an unlimited number of public and private repositories" ([docs](https://docs.renovatebot.com/mend-hosted/overview/)).
- **Limits.** One concurrent job per organization, active repos scheduled every 4 hours, 1 vCPU, 3 GB of memory, 15 GB of disk, a 30-minute job timeout, and no helpdesk support (same page). Repos that are onboarded but have no merged Renovate PR yet run daily ([docs](https://docs.renovatebot.com/mend-hosted/job-scheduling/)).
- **The OSS plan.** Once AmpolaTech is public under an OSI-approved license, it can request the Community (OSS) plan: 2 concurrent jobs, 6 GB, a 60-minute timeout, and Merge Confidence workflows. Acceptance is at Mend's discretion (same page).
- **Permissions.** The app's declared permissions are administration, emails, members, metadata, packages and vulnerability alerts: read; checks, contents, issues, pull requests, statuses and workflows: write ([GitHub API](https://api.github.com/apps/renovate)). Renovate's docs explain most of them, for example "Workflows: … Explicit permission needed to update workflows" ([docs](https://docs.renovatebot.com/security-and-permissions/#permissions)).
- **Data.** Mend clones the code for each job and does not keep it. Its database keeps "a list of dependencies and versions per repo", and anonymized, aggregated update results feed Merge Confidence. Mend says it is ISO 27001 and SOC2 certified (same page).
- **Defaults and version.** The hosted app adds `mergeConfidence:all-badges` to every repo. Its Renovate version "can lag a few hours to a week behind the open source version" ([docs](https://docs.renovatebot.com/mend-hosted/hosted-apps-config/)).
- **What free users cannot change.** They can set any repository option through environment variables, but not `allowedUnsafeExecutions` or `allowedCommands`; those are for paying customers ([docs](https://docs.renovatebot.com/mend-hosted/environment-variables/)). Free users also cannot add `postUpgradeTasks` commands ([docs](https://docs.renovatebot.com/mend-hosted/faq/)).
- **What it needs on a private personal repo.** Install the app on that repo only. "Selected repositories" always produces an onboarding PR ([docs](https://docs.renovatebot.com/mend-hosted/hosted-apps-config/)). Add `renovate.json`, keep Issues enabled for the dashboard, and turn on Dependabot alerts for vulnerability PRs.

**Renovate, self-hosted.** The [`renovatebot/github-action`](https://github.com/renovatebot/github-action) runs Renovate in this repo's own Actions. It needs a classic PAT with `repo` and `workflow` scopes, or a GitHub App you create. `GITHUB_TOKEN` also works, but workflows on Renovate's PRs then need manual approval (README). The administrator must also put `gradleWrapper` in `allowedUnsafeExecutions` for wrapper updates. On a private repo, the runs spend Actions minutes: GitHub Free includes 2,000 a month, and public repos on standard runners are free ([docs](https://docs.github.com/en/billing/concepts/product-billing/github-actions)).

**Analysis: the trust difference.** Renovate pushes its branches into the repository itself, and GitHub's fork-style restrictions are documented only for Dependabot. So workflows on Renovate PRs presumably run with the repo's normal Actions secrets and token permissions. A malicious release that reaches CI on a Renovate PR could read whatever secrets those workflows can read. The 7-day age wait shrinks that window, and keeping deploy secrets out of pull-request workflows closes it.

## 5. Config surface

Paths below are placeholders: Gradle at the root, the web app in `/web`, Dockerfiles in `/backend` and `/web`, Compose in `/deploy`.

### Dependabot

This sketch was not run. Its keys were checked against the [options reference](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference).

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"          # catalog, build scripts, wrapper
    directory: "/"
    schedule: { interval: "weekly" }
    cooldown: { default-days: 7 }
    commit-message: { prefix: "build", include: "scope" }
    groups:
      spring-boot:
        patterns: ["org.springframework.boot*"]
  - package-ecosystem: "npm"             # npm and pnpm
    directory: "/web"
    schedule: { interval: "weekly" }
    cooldown: { default-days: 7 }
    commit-message: { prefix: "build", include: "scope" }
    groups:
      react:
        patterns: ["react", "react-dom", "@types/react", "@types/react-dom"]
  - package-ecosystem: "docker"
    directories: ["/backend", "/web"]
    schedule: { interval: "weekly" }
    cooldown: { default-days: 7 }
    commit-message: { prefix: "build", include: "scope" }
    ignore:
      - dependency-name: "eclipse-temurin"
        versions: [">= 26, < 29"]         # non-LTS; widen by hand each LTS cycle
  - package-ecosystem: "docker-compose"
    directory: "/deploy"
    schedule: { interval: "weekly" }
    cooldown: { default-days: 7 }
    commit-message: { prefix: "build", include: "scope" }
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule: { interval: "weekly" }
    cooldown: { default-days: 7 }
    commit-message: { prefix: "build", include: "scope" }
# Not reachable: mise.toml, JavaLanguageVersion.of(25), setup-java's java-version.
```

### Renovate

Renovate's own validator accepted this file (`renovate-config-validator --strict`, 44.172.0). The lookup run described in "How to read this file" used it as the repo config.

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": ["config:best-practices", ":semanticCommitTypeAll(build)"],
  "semanticCommits": "enabled",
  "labels": ["dependencies"],
  "minimumReleaseAge": "7 days",
  "customManagers": [
    {
      "customType": "regex",
      "description": "Java toolchain in Gradle build scripts",
      "managerFilePatterns": ["/(^|/)build\\.gradle\\.kts$/"],
      "matchStrings": ["JavaLanguageVersion\\.of\\((?<currentValue>\\d+)\\)"],
      "depNameTemplate": "java",
      "packageNameTemplate": "java-jdk",
      "datasourceTemplate": "java-version"
    }
  ],
  "packageRules": [
    {
      "description": "config:best-practices sets 3 days for npm; raise it to the repo-wide 7",
      "matchDatasources": ["npm"],
      "matchUpdateTypes": ["major", "minor", "patch"],
      "minimumReleaseAge": "7 days"
    },
    {
      "description": "java-version has no release timestamps; without this, Java updates stay pending forever",
      "matchDatasources": ["java-version"],
      "minimumReleaseAge": null
    },
    {
      "description": "Keep the toolchain a bare major: JavaLanguageVersion.of(25)",
      "matchManagers": ["custom.regex"],
      "matchDepNames": ["java"],
      "versioning": "semver-partial"
    },
    {
      "description": "Move Java everywhere in one PR: toolchain, mise, setup-java, base images",
      "matchDatasources": ["java-version"],
      "groupName": "java"
    },
    {
      "matchDatasources": ["docker"],
      "matchPackageNames": ["eclipse-temurin"],
      "groupName": "java"
    },
    {
      "description": "A PostgreSQL major needs pg_upgrade or dump/restore; approve it from the dashboard",
      "matchDatasources": ["docker"],
      "matchPackageNames": ["postgres"],
      "matchUpdateTypes": ["major"],
      "dependencyDashboardApproval": true
    }
  ]
}
```

The Spring grouping the owner wants comes from `config:recommended`. Observed, the Spring Boot plugin landed in the `spring boot` group; the same preset puts `org.springframework.boot:**` libraries there ([preset](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/group.preset.ts#L641-L653)). With Boot's dependency management, Spring Framework versions are not declared separately (**analysis**).

### Commit messages

- **Dependabot.** With `prefix: "build"` and `include: "scope"`, the prefix is `build(deps): ` for production dependencies and `build(deps-dev): ` for development ones ([source](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/common/lib/dependabot/pull_request_creator/pr_name_prefixer.rb#L121-L128), [scope](https://github.com/dependabot/dependabot-core/blob/fda701f49c7d6ba5807e98a06549fdb935e3bd2e/common/lib/dependabot/pull_request_creator/pr_name_prefixer.rb#L179-L181)). For `build(deps): ` on every update, set `prefix: "build(deps)"` and leave out `include`. Without `commit-message`, Dependabot copies the style of the repo's recent commits ([docs](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#commit-message--)). <!-- markdownlint-disable-line MD038 -->
- **Renovate.** Renovate's defaults are type `chore` and scope `deps` ([source](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/options/index.ts#L2322-L2342)). But `config:recommended` includes `:semanticPrefixFixDepsChoreOthers`, package rules that use `fix` for production dependencies and `chore` for everything else ([preset](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/default.preset.ts#L561-L603)). Production dependencies there include npm `dependencies` and Gradle's `dependencies` dep type. Because package rules override top-level settings, as observed with the release age, a top-level `semanticCommitType: "build"` would lose to that preset (**analysis**). `:semanticCommitTypeAll(build)` adds a catch-all rule after it ([preset](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/default.preset.ts#L541-L550)). The result reads `build(deps): update dependency vite to v8`. `"semanticCommits": "enabled"` replaces Renovate's guess from commit history ([docs](https://docs.renovatebot.com/configuration-options/#semanticcommits)).

## Recommendation for AmpolaTech (analysis)

- **Use Renovate, through Mend Renovate Community Cloud.** It is the only one of the two that reaches every place a version lives here, and its LTS rule matches the "latest LTS" policy. That policy is [ADR 0002](../adr/0002-stack.md), which already names Renovate; this note is the evidence behind it. Apply for the OSS plan once the repo is public under an OSI license; it buys concurrency and longer jobs, and nothing in the config changes.
- **Start from the Renovate sketch above.** Keep the `description` on every override so the reason lives in the file. Each override corrects a preset default that contradicts the owner's stated policy, and the reason is not obvious from the setting alone.
- **Keep the Gradle version in one place.** If `mise.toml` also pins Gradle, there are two Gradle versions, and Renovate updates them through different datasources. `group:gradle` matches the wrapper's `gradle-version` datasource and Docker images ([preset](https://github.com/renovatebot/renovate/blob/7f7021a63094d36a2f8f1b41ce5a263c71c406af/lib/config/presets/internal/group.preset.ts#L208-L220)), not mise's GitHub-tags lookup, so the two arrive as separate PRs. Letting the wrapper own Gradle removes that.
- **Security.** In the repo settings, turn on the dependency graph and Dependabot alerts, leave Dependabot security updates off, and do not add a `dependabot.yml`. Renovate raises the fixes from those alerts. For Gradle alerts, turn on automatic dependency submission; it is free once the repo is public. Or set `osvVulnerabilityAlerts: true` and skip GitHub's Gradle graph; how its PRs interact with the GitHub-alert PRs was not tested.
- **Pin actions by SHA,** as `config:best-practices` does. Renovate keeps the SHAs and their version comments current, and GitHub stops alerting on SHA-pinned actions.
- **Keep deploy credentials out of pull-request workflows,** for example in a GitHub environment used only by the deploy job on `main`, because Renovate's PRs run CI with the repository's secrets.
- **When Dependabot would still be the right call:** if no third-party app may hold write access to this repo. The price is bumping Java by hand in the toolchain, `mise.toml` and `setup-java` every LTS cycle, Node and Gradle in `mise.toml` by hand, widening the `eclipse-temurin` ignore range by hand, and having no approval gate for PostgreSQL majors.
