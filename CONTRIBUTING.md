# Contributing to KISS

Contributions are very welcome, feel free to open new issues or PRs.

Project maintainers are @Neamar and @marunjar.

## Translation [![Translation status](https://hosted.weblate.org/widgets/kiss/-/shields-badge.svg)](https://hosted.weblate.org/projects/kiss/strings/)

Want to help with the translation? Use https://hosted.weblate.org/projects/kiss/strings/ to collaborate on strings translation. Current progress:

[![Translation status](https://hosted.weblate.org/widgets/kiss/-/multi-auto.svg)](https://hosted.weblate.org/projects/kiss/strings/)

## Before contributing...

If you submit a pull request, thanks! There are a couple rules to follow though to make it manageable:

- The pull request should be atomic, i.e. contain only one feature. If it contains more, please submit multiple pull requests.
- Likewise, if for one unique feature the pull request grows too large (more than 300 loc), please get in touch first.
- Please stick to the current coding style. It's important that the code uses a coherent style for readability.
- Do not include stylistic improvements. If you think one part deserves lots of housekeeping, use a separate pull request so as not to pollute the code.
- Ensure your changes work on all supported Android version!
- Last but not least: keep in mind what KISS's mindset is! Is your feature really needed for more than 10% of the users? How to make it simpler? Can you do it without an external library?

## How does it work?

Different data types can be aggregated via KISS' simple interface: apps, contacts, settings...

Each data type uses four classes:

- A _loader_, which retrieves all available items at startup
- A _provider_, which knows all of its items (e.g. all contacts), and is responsible for filtering those records according to the query
- A _pojo_, which is a [POJO](https://en.wikipedia.org/wiki/Plain_Old_Java_Object) storing simple data for one item (e.g. contact name, display name, phone number, photo)
- A _result_, which ensures the _pojo_ is properly displayed in the list

Controlling the workflow is _MainActivity_, initializing the UI. Searching runs through the `Searcher` classes, and UI features live in `forwarder/`.

### Adding new content sources

This is clearly not as easy as it ought to be.

You need to create a new loader, new provider, new pojo and new result. You also need to ensure `adapter.RecordAdapter.java` creates your own view type (see method `getViewTypeCount` and `getItemViewType`).

## Creating new Pull Request

Explain your changes, comment your code and make sure the CI is able to compile your code.
