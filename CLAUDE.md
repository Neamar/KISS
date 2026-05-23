# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

KISS is an Android launcher (package `fr.neamar.kiss`). It is search-first: users type a few letters and get apps, contacts, shortcuts, settings, search engines, phone numbers, calculator results, etc. App-browsing is secondary. Minimum SDK 21, target/compile SDK 36, Java 11 source. Build requires JDK 21.

## Common commands

All commands run from the repo root via the wrapper. CI runs the first two.

- Lint: `./gradlew lint`
- Unit tests (JUnit 5, JVM only): `./gradlew testDebugUnitTest`
- Single test class: `./gradlew testDebugUnitTest --tests fr.neamar.kiss.utils.calculator.CalculatorTest`
- Single test method: `./gradlew testDebugUnitTest --tests 'fr.neamar.kiss.utils.fuzzy.FuzzyScoreV1Test.someMethod'`
- Debug APK (installable side-by-side, applicationId suffix `.debug`): `./gradlew assembleDebug`
- Release APK: `./gradlew assembleRelease`

Both `debug` and `release` build types enable `minifyEnabled` + `shrinkResources` with ProGuard, so don't assume reflection-only references survive — check `app/proguard-rules.pro` when adding them.

## Architecture: the four-class data-type pattern

The core abstraction is the *data type* (apps, contacts, shortcuts, …). Each one is implemented as **four cooperating classes**, and adding a new content source means adding all four plus wiring the adapter:

- `loader/Load<Type>Pojos` — `AsyncTask` that enumerates all items once at startup.
- `dataprovider/<Type>Provider` — Android `Service` (extends `Provider<T extends Pojo>`) holding the loaded list and filtering it against a query. Bound via `DataHandler` and listed by name in `DataHandler.PROVIDER_NAMES` (`"app"`, `"contacts"`, `"shortcuts"`).
- `pojo/<Type>Pojo` — plain data holder (extends `Pojo` or `PojoWithTags`).
- `result/<Type>Result` — knows how to render that pojo as a list row and handle clicks/long-clicks (extends `Result` / `ResultWithTags`).

When adding a new data type you must also extend `adapter/RecordAdapter`'s `getViewTypeCount` / `getItemViewType` so the new result has its own view type. This is called out explicitly in `CONTRIBUTING.md` as the not-easy part.

For lightweight, in-memory-only sources (search engines, calculator, phone-number parsing, settings shortcuts, timers, tags), use `dataprovider/simpleprovider/SimpleProvider` instead — no loader, no `Service`, no async startup.

`DataHandler` is the central glue: it owns the `Map<String, ProviderEntry>` of bound providers, dispatches queries to them, listens for `SharedPreferences` changes that toggle providers, and exposes history (`db/DBHelper`, `db/DB`). `MainActivity` drives the UI; query execution flows through a `searcher/Searcher` subclass (`QuerySearcher`, `HistorySearcher`, `ApplicationsSearcher`, `TagsSearcher`, `UntaggedSearcher`, `NullSearcher`, `PojoWithTagSearcher`) which collects results from providers and orders them.

UI-level behaviors that aren't part of search proper (favorites bar, widgets, notification badges, tags menu, live wallpaper interactions, Oreo shortcuts pinning, "experience tweaks") are split into `forwarder/` classes coordinated by `ForwarderManager`. Look there before adding logic into `MainActivity`.

Icon handling: `IconsHandler` + `icons/` (icon packs, adaptive icon synthesis, caching via `utils/IconPackCache`). Theming/colors via `UIColors`.

## Testing reality

There are only JVM unit tests under `app/src/test/` (string normalization, calculator, fuzzy scoring, calculator provider). There is no instrumented test runner exercising the launcher itself — `CONTRIBUTING.md` and `qa-suite.md` both note this. `qa-suite.md` is a manual checklist to run after significant UI changes; refer to it rather than inventing ad-hoc QA steps. The `app/src/test/java/android/text/TextUtils.java` shim exists because tests run on the JVM without Android framework classes — keep that workaround in mind when writing new tests that touch framework-y utilities.

## Contributing rules to respect

From `CONTRIBUTING.md` — these are reviewer expectations, not generic advice:

- Pull requests must be atomic (one feature) and small (>200 LOC means talk first).
- No standalone "housekeeping" / stylistic refactors mixed into feature PRs.
- Match existing coding style.
- Before adding a feature, ask whether it's needed by >10% of users and whether it can be done with fewer dependencies — the project deliberately avoids heavyweight libs (e.g. no `RecyclerView` despite the androidx dependency being available).
- Update the README when behavior changes.

## Translations

User-facing strings live in `app/src/main/res/values*/strings.xml` and are managed via Weblate. Don't hand-edit non-English `strings.xml` files — they round-trip through Weblate (see merge commits from `weblate/master`).
