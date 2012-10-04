Summon
======

Summon is a *blazingly* fast launcher for android requiring nearly no memory to run.

Expect to be even more productive. Stop losing time with this stupid drawer.

How does it works ?
--------------------
Different data types can be aggregated via Summon simple interface : apps, contacts, settings...

Each data types uses three classes :

* A *provider*, which knows all of its items (e.g. all contacts), and responsible for filtering those records according to the query
* A *holder*, which is a POJO storing simple data for one item (e.g. contact name, display name, phone number, photo)
* A *record*, which ensure the *holder* is properly displayed in the list

Controlling the workflow is *SummonActivity*, intializing the UI, dispatching the query to the providers and ordering the results according to their relevance and user search history.