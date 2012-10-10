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

---------------------

Download bêta on the [https://play.google.com/store/apps/details?id=fr.neamar.summon.lite](Play Store).
(https://lh4.ggpht.com/DI0efjo_g4Y8gy-7qeODOM8XG-r6aNdL389Kkmd6wayHT42kXrXDsBebsBCskxar6A)

![Preview](https://lh4.ggpht.com/DI0efjo_g4Y8gy-7qeODOM8XG-r6aNdL389Kkmd6wayHT42kXrXDsBebsBCskxar6A)
![Preview](https://lh4.ggpht.com/_fXNqsbJkVOuj1kToJyFEderQ8oJ50uduHiWQqB7ac-w5HNVm32fcomFhz7id1pKpQ)
![Preview](https://lh4.ggpht.com/_B28M9qIOoUmrQZIrhN1V6iI8f9pTxF-843LOJWdMPidIYkeI9uyzjx1EMJWq5XvhPg)