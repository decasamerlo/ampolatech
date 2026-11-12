# A public repository, without a licence

`decasamerlo/ampolatech` is private, on GitHub's free plan, and the plan falls short twice. CI spends the plan's included Actions usage, while standard runners cost nothing on a public repository ([billing for GitHub Actions](https://docs.github.com/en/billing/managing-billing-for-your-products/managing-billing-for-github-actions/about-billing-for-github-actions)). And GitHub refuses a ruleset on `main`: on 2026-11-12, `gh api repos/decasamerlo/ampolatech/rulesets` answered 403, "Upgrade to GitHub Pro or make this repository public to enable this feature". The project spends no money while a free way exists, so the repository goes public (#127), and CI (#10) and a ruleset on `main` cost nothing. It goes public without a licence.

- **What goes public** is everything the repository holds, its history included:
  - the files;
  - every commit on `main`;
  - each pull request's head, `refs/pull/N/head`;
  - the commits that force-pushes left behind, which stay fetchable by SHA. Pull request timelines link them, and the repository's activity log lists the ones pushed before their pull request existed;
  - every issue and pull request, with its comments, its reviews, the edit history of each body, and its old titles;
  - from #10 on, the Actions logs and artifacts.

  Before the switch, a sweep reads all of it for anything `CLAUDE.md`'s *Non-public sources* keeps out of published text.
- **What anyone can do:** open issues and pull requests, comment, and fork. Only the author merges, since nobody without write access can push to the repository. The repository stays open to all of this, and answers each:
  - **Agents act on the author's text only.** An agent runs as the author, with the author's access, so text anyone else writes could steer it. A malicious issue in a public repository has already made an agent publish private data in a public pull request ([Invariant Labs, 2025-05-26](https://invariantlabs.ai/blog/mcp-github-vulnerability)). [`docs/agents/issue-tracker.md`](../agents/issue-tracker.md) says how an agent reads an issue, to work it and to triage it.
  - **Code from anyone else runs only in CI**, never on the author's machine. CI on a fork's pull request waits for the author's approval.
  - **Security findings go to private vulnerability reporting**, not to a public issue.
- **No licence, and no contributor licence agreement.**
  - **The repository has no `LICENSE`.** The author retains all rights, and "no one may reproduce, distribute, or create derivative works" ([GitHub's docs on licensing a repository](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository)). GitHub's terms still let others view and fork a public repository.
  - **Why not grant one now:** a licence can be granted later, but a grant on code already published can't be taken back.
  - **Contributions get no licence either.** GitHub's terms license a contribution under the repository's terms only when the repository contains "notice of a license" ([Terms of Service, D.6](https://docs.github.com/en/site-policy/github-terms/github-terms-of-service)). So a pull request from anyone else isn't merged, and the first one the author wants to merge reopens the choice between a licence and a contributor licence agreement.
  - **Mend's OSS plan for Renovate stays closed**, since it asks for an OSI-approved licence ([dependency updates](../research/dependency-updates.md)). Its free plan still runs #11.
- **The vendored skills leave the repository.**
  - **Where they came from:** the 27 skills in `.claude/skills/` came from [`mattpocock/skills`](https://github.com/mattpocock/skills) under the MIT licence. The *Summary* of `pr` reproduced HumanLayer's `show-me`, from [`humanlayer/skills`](https://github.com/humanlayer/skills), also MIT.
  - **What was missing:** MIT asks that its copyright and permission notice travel with every copy, and no copy here carried it.
  - **Why a notice inside them wouldn't last:** `npx skills add` replaces a skill's folder whole, so a file added there is gone after the next run, as version 1.8.1 of the `skills` CLI did on 2026-11-12.
  - **Why removing them costs the author nothing:** the author's agents load these skills from the user level, which wins over a project's copy ([Claude Code's skills docs](https://code.claude.com/docs/en/skills)).
  - **So** the repository carries none, and `CLAUDE.md` names their source. The commits that carried the copies stay as they are.
  - **The Gradle wrapper needs nothing:** it carries its own Apache notice, in the headers of `gradlew` and `gradlew.bat` and in its jar's `META-INF/LICENSE`.
- **No check in this repository for non-public text**, as nesto decided in its [ADR 012](https://github.com/decasamerlo/nesto/blob/main/docs/adr/012-precedent-without-evidence.md). A list of names to check against, committed here, "publishes what it protects", and without one a check could match only `owner/repo`-shaped names, missing the prose, the quotations and the counts. `CLAUDE.md`'s *Non-public sources* is the rule, applied when text is drafted and again in review.

## Considered options

- **GitHub Pro**, which allows a ruleset on a private repository, and costs money.
- **Path conditions in CI**, running each job only when its files change, to spend less of the included usage. They leave checks unrun, and give no ruleset.
- **A public mirror, or a new public repository.** Neither takes an issue or a pull request along, each breaks every `#N` link, and the working repository stays without a ruleset.
