Summon
======
Summon is a *blazingly* fast launcher for android requiring nearly no memory to run.

Download for free: [https://play.google.com/store/apps/details?id=fr.neamar.summon.lite](Play Store).
What is it?
------------
Summon is a fast launcher for your Android.

Search through you app, contacts and settings lightning fast.
No more time spent trying to find the app you need to launch : enter a few characters from the name and press enter.
Need to phone someone ? Don't meddle with the call log, juste give three letters of his name and push the "phone" button.

Summon becomes smarter and smarter as you uses it, pushing forward results you're more likely to select.

You can use this app as a widget on your default launcher, or replace your launcher with Summon for an even faster experience.

Get ready to be amazed.

Previews
---------------------


![Preview](https://lh5.ggpht.com/ncNrAB5Z3-sY8nk6KyEaX71aS5hJtbqrgKu5_ovaBEAizmJa-x78dzsE43gmQD8tmA)
![Preview](https://lh6.ggpht.com/ai8ByZXcGLV62lAiskbcUaW27fOD4dprqtkvn6BwVs-50bR6BvzWgXiqdIK65a6Xruhv)
![Preview](https://lh6.ggpht.com/XG1p9WAmjnvxsYLQIXPvJMbzPVVydBlOCi20nzMCwGEIJ1Ft_otrts5uCXGwy-582w)


How does it works?
-------------------
Different data types can be aggregated via Summon simple interface : apps, contacts, settings...

Each data types uses three classes :

* A *provider*, which knows all of its items (e.g. all contacts), and responsible for filtering those records according to the query
* A *holder*, which is a POJO storing simple data for one item (e.g. contact name, display name, phone number, photo)
* A *record*, which ensure the *holder* is properly displayed in the list

Controlling the workflow is *SummonActivity*, intializing the UI, dispatching the query to the providers and ordering the results according to their relevance and user search history.

