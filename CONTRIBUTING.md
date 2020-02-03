# Contributing to KISS
Contributions are very welcome, feel free to open new issues or PR.

Project maintainers are @Neamar, @Dorvaryn, @saveman71 and @Pluggi.

## Translation [![Translation status](https://hosted.weblate.org/widgets/kiss/-/shields-badge.svg)](https://hosted.weblate.org/projects/kiss/strings/)

Want to help with the translation? Use https://hosted.weblate.org/projects/kiss/strings/ to collaborate on strings translation. Current progress:

[![Translation status](https://hosted.weblate.org/widgets/kiss/-/multi-auto.svg)](https://hosted.weblate.org/projects/kiss/strings/)

## Before contributing...
If you submit a pull request, thanks! There are a couple rules to follow though to make it manageable:

* The pull request should be atomic, i.e. contain only one feature. If it contains more, please submit multiple pull requests. Reviewing massive, 1000 loc+ pull requests is extremely hard.
* Likewise, if for one unique feature the pull request grows too large (more than 200 loc), please get in touch first.
* Please stick to the current coding style. It's important that the code uses a coherent style for readability.
* Do not include sylistic improvements ("housekeeping"). If you think one part deserves lots of housekeeping, use a separate pull request so as not to pollute the code.
* KISS has no automated tests, sadly. Ensure your changes work on all supported Android version!
* Update the readme accordingly.
* Last but not least: keep in mind what KISS's mindset is! Is your feature really needed for more than 10% of the users? Ask yourself what you're trying to achieve and if you can make it with fewer dependencies (for instance, that's why we're not using RecyclerView :()


## How does it work?

Different data types can be aggregated via KISS' simple interface: apps, contacts, settings...

Each data types uses four classes:

* A *loader*, which retrieves all available items at startup
* A *provider*, which knows all of its items (e.g. all contacts), and is responsible for filtering those records according to the query
* A *pojo*, which is a [POJO](https://en.wikipedia.org/wiki/Plain_Old_Java_Object) storing simple data for one item (e.g. contact name, display name, phone number, photo)
* A *result*, which ensures the *pojo* is properly displayed in the list

Controlling the workflow is *SummonActivity*, initializing the UI, dispatching the query to the providers and ordering the results according to their relevance and user search history.

### Adding new content sources
This is clearly not as easy as it ought to be.

You need to create a new loader, new provider, new pojo and new result. You also need to ensure `adapter.RecordAdapter.java` creates your own view type (see method `getViewTypeCount` and `getItemViewType`).

## Creating new Pull Request
Explain your changes, comment your code and make sure the CI is able to compile your code.

Sadly, there is no test suite yet :(
