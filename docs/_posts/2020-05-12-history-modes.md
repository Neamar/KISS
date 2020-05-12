---
title: What are the different history modes?
categories:
  - History
description: "What is the difference between frequently, frecency, recently, etc."
type: Document
---

By default, KISS history will display the most recently used items first.

You can customize this behavior from `⋮, KISS Settings, History Settings`. Select `History mode`.

> Note that changing this mode will not change the search results.

![History modes](/screenshots/history-modes.png)


## Accessed recently first
This is the default option: the first item (closest to your thumb) will be the last one used.

## Accessed frequently
Most used items will be displayed first.
This ranking is the slowest to change; you'll be able to get used to it, but if you stop using an app you'll still see it in your history for a very long time.

Note that your history is automatically cleaned every 3.000 uses, so at some point, old apps will disappear.

## Adapted to user usage
This option will display apps sorted by frequency, but only over the last 36 hours of usage.
This means apps frequently used recently will appear first, and soon disappear if they're not used anymore (for instance, weekend apps vs. workdays apps)

## Alphabetically
The names says it all ;)
This will retrieve all elements accessed recently, and display them sorted alphabetically.

## Accessed frequently recently
Probably the most advanced sorting method.

This one will sort apps based on your frequency, but also factor in time since last use.

Formula is `frequency * recency` where `frequency = #launches_for_app / #all_launches` and `recency = 1 / position_of_app_in_recent_history`.
