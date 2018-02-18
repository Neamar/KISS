---
title: One of my contacts does not appear in KISS
categories:
  - UI
description: "What to do when a contact is not listed in KISS"
type: Document
---

You may notice that some of your contacts are missing, even though you can see them fine in the default *Contacts* app.

There can be multiple reasons for this:

### Contact was just added or edited
Some devices don't notify properly when the contact database is updated. You'll need to manually restart KISS, which will force a synchronisation with the contact database: `⋮, KISS Settings, Advanced settings, Restart KISS`.

### Contact doesn't have a phone number
Contacts without a phone number are not displayed in KISS, as KISS only provides tooling for phone and messages.

To access this contact, you'll need to search in KISS for "Contacts" and open the system app.

### Contact isn't part of your personal contact list
This will happen if your company uses Google Apps. The company directory will appear in contacts, but it isn't downloaded locally to the phone. As KISS doesn't have permission to access the Internet, it won't be able to display those contacts.

To fix this, you can open one of the missing contacts in the *Contacts* app and click on the "+" icon to add the contact to one of your local accounts.
