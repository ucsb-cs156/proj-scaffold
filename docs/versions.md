# Updating versions of Java, Node and frontend dependencies

This file records where version numbers live in this repo and what broke (and how it
was fixed) the last time they were updated, so the next update can go straight to the
fix. Keep it up to date when you do the next one.

## Updating the Java version

Places that name the Java version:

* `pom.xml`: `<java.version>`. Also check that `jacoco-maven-plugin` and `pitest-maven`
  support the new Java version, since both read compiled class files. (Notes from the
  Java 21 to 25 move: Lombok needs `-proc:full`; `pitest-maven` is pinned to 1.22.1 because
  1.23.0+ moved the history file the incremental Pitest workflow relies on.)
* `.java-version` (used by the GitHub Actions workflows in `ucsb-cs156/workflows`).
* `Dockerfile` (the `openjdk-NN-jdk` apt package and `JAVA_HOME`).

## Updating the Node version

Places that name the Node version (all four must agree):

* `frontend/package.json`, `engines.node`. **This also controls CI**: the shared workflows
  in `ucsb-cs156/workflows` and this repo's Chromatic and gh-pages workflows call
  `actions/setup-node` with `node-version-file: frontend/package.json`, so no workflow edit
  is needed. This repo pins an exact version (no `^`) so that CI and local installs get the
  same Node patch release and therefore the same bundled npm major; a caret range has
  caused `npm ci` lockfile mismatches here before.
* `frontend/.nvmrc` (for `nvm use`; `frontend/nvm-pj.sh` reads `package.json` instead).
* `pom.xml`: the `app.frontend.nodeVersion` property, used by `frontend-maven-plugin` in
  the `integration` and `production` profiles.
* `Dockerfile`: `ENV NODE_VERSION=...` (installed with nvm for the Dokku image).

Then run `grep -rIn "<old version>" --exclude-dir=node_modules --exclude-dir=target .`
to catch anything else.

After changing the version, if a local `mvn` build fails inside `npm ci`/`npm install`
with `Class extends value undefined is not a constructor or null`, delete `target/node`
and `target/node_modules` (stale npm from the previous Node version left in the plugin's
install directory). CI is unaffected because it always starts clean.

## Updating frontend dependencies at the same time

Notes from the move to Node 24.21.0 (issue #130), useful as a checklist. The same
update was done first in `ucsb-cs156/proj-courses` (issue #355, PR #356); that repo
started from an older dependency set, so its notes cover things (react-router 6 to 7,
user-event 13 to 14, ESLint 9 vs 10 with `eslint-plugin-react`, recharts 3, Stryker's
`CallExpression` mutator) that did not come up here.

### Recipe

1. `nvm use` (reads `frontend/.nvmrc`), then `cd frontend && npm ci`.
2. `npm audit`, `npm outdated`, and `npm ci 2>&1 | grep -i "deprecated\|install-scripts"`
   show what needs attention.
3. Edit the versions in `package.json` (do it with a small script or by hand, not `sed`:
   a pattern like `"storybook": "..."` also matches the npm *script* named `storybook`),
   then regenerate the lockfile: `rm -rf node_modules package-lock.json && npm install`.
   A stale lockfile can otherwise produce a confusing `ERESOLVE` error.
4. `npm audit fix` for anything left that has a non-breaking fix.
5. npm 11 blocks install scripts by default and warns about the ones it skipped
   (`esbuild`, `msw`, `fsevents` here). Check the list is expected, then
   `npm install-scripts approve --all` and commit the resulting `allowScripts` block in
   `package.json`. Note the entries are pinned to exact versions, so they need re-approving
   after the next bump of those packages.
6. Verify with **all** of: `npm run lint`, `npm run check-format`, `npm test` (run it two
   or three times to catch flakiness), `npm run build`, `npm run build-storybook`, a Stryker
   run on every `src/main` file whose test you touched (see below), and
   `rm -rf target/node target/node_modules && mvn -Pproduction -DskipTests package`.
   Unit tests alone do not catch Storybook or Stryker breakage.

### Stryker and the CI mutation-testing job

The PR mutation job (`33-frontend-pr-mutation-testing`, from `ucsb-cs156/workflows`) runs
Stryker only on the `src/main` files changed in the PR **plus the `src/main` file matching
every changed test file**, and it aborts if any test fails in Stryker's initial dry run.
So editing a test file, even mechanically, exposes pre-existing survivors in that
component, and a timing-sensitive test anywhere can fail the job while `npm test` is green.
Before blaming the upgrade, run Stryker on the same files on a clean `main` checkout.

A quick smoke test that Stryker and vitest still talk to each other (about 20 seconds):

```
npx stryker run --mutate src/main/utils/dateUtils.js,src/main/utils/regexUtils.js
```

Expect 100% (7 killed). If it reports 0 covered or every mutant survives, the runner and
vitest versions are incompatible (see vitest 5 below).

### Breaking changes hit in this repo (Node 24.21.0 update)

* **jsdom 30** resolves `rem` to `px` in `getComputedStyle`, so
  `expect(el).toHaveStyle({ right: "0.75rem" })` fails with `right: 12px`. For rem-valued
  properties, assert on the inline declaration instead: `expect(el.style.right).toBe("0.75rem")`
  (`EnrollmentTabComponent` and `StaffTabComponent` tests).
* **msw-storybook-addon 3** removed `initialize()` and the root `mswLoader` export. In
  `.storybook/preview.jsx` use `import { mswLoader } from "msw-storybook-addon/csf3"` and
  `loaders: [mswLoader()]`, and drop the `initialize()` call. `npm run build-storybook`
  fails without this; unit tests do not catch it.
* **Storybook 10**: no other change was needed (`.storybook/main.js` is already ESM because
  `package.json` has `"type": "module"`, and `tsconfig` already uses
  `moduleResolution: "bundler"`). The `docs.autodocs` option in `main.js` was removed in
  Storybook 9 and was deleted; stories use `tags: ["autodocs"]`. Upgrade `storybook`,
  `@storybook/*`, `@chromatic-com/storybook` (5.x), `msw-storybook-addon` (3.x) and
  `chromatic` together.
* **Vite 8 (Rolldown)**: the TypeScript `vite.config.ts` loads fine as is. Vite 8 has
  built-in support for `tsconfig` `paths` (`resolve.tsconfigPaths: true`), so the
  `vite-tsconfig-paths` plugin was removed; it depended on the deprecated `tsconfck`, which
  was the last `npm ci` deprecation warning.
* **ESLint 10** works here because this repo does not use `eslint-plugin-react`
  (`typescript-eslint`, `eslint-plugin-react-hooks` 7 and `eslint-plugin-react-refresh` 0.5
  all declare ESLint 10 support). `build` and `storybook-static` were added to
  `globalIgnores` so a local build output does not produce lint warnings.
* **react-router 8**: no code changes. It removed `react-router-dom` (this repo already
  imports from `react-router`), requires React 19.2.7+ and Node 22.22+, and is ESM-only.
* **@testing-library/jest-dom 7**: no code changes (`@testing-library/dom` is now a required
  peer dependency; it is already installed via `@testing-library/react`).
* **@stryker-mutator/vitest-runner 10**: no config changes; the probe above still scores
  100%. Stryker 10 adds a `CallExpression` mutator (it deletes bare call statements); in
  proj-courses that produced new survivors and the mutator was excluded, but here every
  extra mutant was killed, so it is left enabled. `EnrollmentTabComponent.jsx` and
  `StaffTabComponent.jsx` (whose tests changed) have 3 and 4 survivors in the search-filter
  code; a run on a clean `main` worktree shows the same survivors (plus one more), so they
  are pre-existing. A quick way to get that baseline:

  ```
  git worktree add --detach /tmp/main-wt origin/main
  cd /tmp/main-wt/frontend && npm ci && npx stryker run --mutate <same files>
  ```

### Held back on purpose

* `vitest` and `@vitest/coverage-v8` stay on 4.x. In proj-courses, vitest 5 with Stryker
  10's vitest runner mapped no tests to mutants, so every mutant survived silently.
  Re-check with the Stryker smoke test above before taking vitest 5.
* `typescript` stays on 6.x: `typescript-eslint` 8.70 requires `typescript <6.1`.
* `@types/node` stays on 24.x to match the Node runtime (26.x exists).
* `@tanstack/react-table` stays on 8 (9 is a rewrite).
