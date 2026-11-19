---
title: KISS keeps crashing, how do I send a crash log?
categories:
  - Bugs
description: "Capture a crash log and report it on GitHub so it can be fixed"
type: Document
---

When KISS crashes, Android records *why* it crashed in a crash log (also called a "stack trace"). Without this log, it is usually impossible for developers to find and fix the problem, so please include one in your report!

> If KISS crashes on startup and only works again after you clear its data, **capture the crash log before clearing the data**. Once the data is gone, the crash usually can't be reproduced anymore.

## Option 1: from the crash dialog (no computer needed)

When KISS crashes, Android sometimes shows a dialog such as "KISS keeps stopping". If you see a `Send feedback` or `Report` button, tap it, then look for `Details` or `More` and an entry called `Stack trace`.

Take screenshots of the whole stack trace, or copy the text if your phone allows it.

Not all phones offer this button: if yours doesn't, use one of the options below.

## Option 2: with a bug report (no computer needed)

1. [Enable developer options](https://developer.android.com/studio/debug/dev-options#enable).
2. Make KISS crash again.
3. Go to `Settings, System, Developer options, Take bug report`, and pick `Interactive report`.
4. After a few minutes, a notification lets you share the report (a `.zip` file).

> A bug report contains a lot of information about your phone, some of it private. Rather than posting it publicly, open the `.zip` file, find the `FATAL EXCEPTION` section mentioning `fr.neamar.kiss` in the main text file, and only copy that part.

## Option 3: with a computer (adb)

This gives the cleanest log.

1. On your phone, [enable developer options](https://developer.android.com/studio/debug/dev-options#enable) and turn on `USB debugging`.
2. On your computer, install [Android platform tools](https://developer.android.com/tools/releases/platform-tools), which include `adb`.
3. Connect your phone with a USB cable and accept the "Allow USB debugging?" prompt on the phone.
4. Make KISS crash again.
5. Run this command on your computer:

```bash
adb logcat -d -b crash > kiss-crash.txt
```

The file `kiss-crash.txt` now contains the recent crashes. Look for the lines mentioning `fr.neamar.kiss`.

## Open an issue on GitHub

Once you have the crash log, [open a crash report on GitHub](https://github.com/Neamar/KISS/issues/new?template=crash-report.md) (you'll need a free GitHub account). Please include:

* the crash log, pasted between two lines of three backticks (```` ``` ````) so it stays readable;
* what you were doing when KISS crashed, and whether you can make it crash again;
* your KISS version (`Settings, Apps, KISS`), where you installed it from (Play Store, F-Droid…), your phone model and Android version;
* when the crashes started, for instance after an update of KISS or of your phone.

Then keep an eye on the issue: developers may have follow-up questions!
