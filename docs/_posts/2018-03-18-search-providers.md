---
title: Customize search providers
categories:
  - Advanced
description: "How to use other search providers than Google?"
type: Document
---

KISS will, by default, enable Google search. If you'd rather use another search provider, you can customize your experience by adding other search engines (Duck Duck Go, Bing, etc.) or even create your own provider!

## Use an alternative search provider
Navigate to `⋮, KISS Settings, Providers Selection`. Select `Select web search providers`. By default, the list will contain DuckDuckGo, Google and Bing. Enable the providers you want to use (you can have more than one!)

From now on, search will use your new search providers:

![Multiple search providers](/screenshots/multiple-search-providers.png)

## Create a new search provider
If the default list does not fit your need, you can create a new search engine fairly easily.

Navigate to `⋮, KISS Settings, Providers Selection`. Select `Add search provider`. This will open a new dialog, you'll need to enter:

* a **name** for your provider. This can be anything you want, but make sure you remember what it is!
* a **URL** for your provider. This has to be a valid URL, use `%s` as a placeholder for the actual query.

> For instance, if you want to add Wikipedia as a search provider, you'll first need to find the search URL. When going to Wikipedia, use the search box, enter `kiss launcher`. You'll be redirected to `https://en.wikipedia.org/w/index.php?search=kiss+launcher`, so the URL you need to use is `https://en.wikipedia.org/w/index.php?search=%s`

Enter both values in the dialog:

![Wikipedia example](/screenshots/add-search-provider-1.png)

From now on, you'll be able to select your new provider in the list when you select `Select web search providers`:

![Select Wikipedia](/screenshots/add-search-provider-2.png)

You're all set! You can now search on your new provider:

![Example search](/screenshots/add-search-provider-3.png)

> If you want to remove your custom search providers, access `⋮, KISS Settings, Providers Selection` and select `Delete search providers`.

## Sample search providers URLs

* Qwant: `https://www.qwant.com/?q=%s`
* Wikipedia: `https://en.wikipedia.org/w/index.php?search=%s&title=Special:Search&fulltext=1`

## How to open Google in the browser?
By default, Google searches will open in the Google app if you have it installed.

If you'd rather open your default browser, you can setup a new search engine -- call it for instance "Google Browser" and use `https://google.com/search?q=%s` as described above in "Create a new search provider".
