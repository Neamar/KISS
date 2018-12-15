# QA test suite
This document contains a small QA suite that can be run after big changes to the app.
As best as possible, only actual KISS code is tested, not standard Android system behavior.

> This document assumes all settings are at their default value when you start

### Get started...
* [ ] Loader appears when opening the app
* [ ] Help text ("search apps, contacts, ...") appears while app are loading
* [ ] After some time, loader disappears and is replaced by launcher icon
* [ ] Touching the search field displays the keyboard
* [ ] Searching for text displays results
* [ ] Clicking the launcher icon displays the list of apps
* [ ] When clicking on a search item, the corresponding intent is triggered
* [ ] When going back to KISS, search results have been cleared
* [ ] When going back to KISS, the item has been added to history
* [ ] History is displayed when search box is empty
* [ ] Three dots menu is displayed to the right
* [ ] Entering a search query replaces the three-dots menu with an "X"
* [ ] Clicking the "X" empties the search field, displays three dots menu and displays history
* [ ] When searching, pressing enter on the keyboard launches the first result
* [ ] When keyboard is displayed, scrolling the list down hides the keyboard
* [ ] When searching, pressing space as the first character does nothing (left-hand side trimming)
* [ ] Press kiss icon. App list is displayed, and kiss bar appears with a circular reveal animation
* [ ] Press kiss icon again. App list is hidden, and kiss bar disappears with a circular (un)reveal animation
* [ ] With keyboard open, press kiss icon. App list is displayed, and keyboard stays there
* [ ] With keyboard open, press kiss icon. The app list is displayed. When typing something on the keyboard, app list is hidden, and search results start appearing
* [ ] Search for something, press kiss icon. The app list is displayed. Press kiss icon again, search query has been emptied
* [ ] With KISS set as default launcher, pressing home empties the search field and displays history
* [ ] With KISS set as default launcher and app list displayed, pressing home hides the app list and displays history
* [ ] When app bar is displayed, pressing back hides the bar and displays history
* [ ] When search results are displayed, pressing back empties then search field and displays history
* [ ] When history is displayed, pressing back does not quit KISS
* [ ] When searching for something, touching the edit text moves the cursor

#### Menus
* [ ] Clicking the three dots menu open a popup
* [ ] Long-clicking the three dots menu open a popup
* [ ] If device has physical menu button, pressing menu displays the three-dots menu
* [ ] Long pressing a search result displays contextual menu
* [ ] When clicking three dots menu, pressing back dismisses the popup

### History
#### Standard history manipulation
* [ ] Reset history preference displays number of items in history if history length > 5
* [ ] Reset history clears existing history instantly
* [ ] Reset history summary does not display the old history length after reset
* [ ] Pressing cancel on reset history does not reset history
* [ ] Adding an excluded app hides the app from search results
* [ ] Adding an excluded app hides the app from history
* [ ] Adding an excluded app hides the app from app list
* [ ] Resetting excluded app removes all excluded apps
* [ ] Resetting excluded apps allows apps previously hidden to be displayed in app list
* [ ] No more than Max number of results in search is displayed to the user
* [ ] Changing the value updates the history dynamically
* [ ] Freezing history prevents new items from being added to history
* [ ] Unchecking "Freeze history" ensures history is populated again
* [ ] TODO: history mode

#### Automated history
* [ ] When "Show incoming calls" is disabled, callers are not added to history
* [ ] When "Show incoming calls" is enabled, callers are added to history
* [ ] When "Show newly installed apps" is disabled, new apps are not added to history
* [ ] When "Show newly installed apps" is enabled, new apps are added to history

### Favorites
* [ ] Favorite bar is displayed automatically at startup
* [ ] Clicking on a favorite trigger the correct Intent
* [ ] When going back to KISS, the favorite has been added to history
* [ ] Long-clicking favorite displays the menu
* [ ] Long-click menu can be used to remove the favorite
* [ ] Empty favorites are not displayed (favorites takes all available space in the bar)
* [ ] When entering a search query, favorite bar is hidden
* [ ] When search query is removed, bar appears again
* [ ] When coming back from an application launched through search, bar is displayed again
* [ ] When kiss bar is opened, favorites bar is hidden
* [ ] When kiss bar is opened, internal favorites bar is hidden
* [ ] When kiss bar is opened, you can't click on the menu button behind the kiss bar (not even visible, doesn't respond to touch events either)
* [ ] When searching and pressing home, query is cleared, and favorites are displayed
* [ ] When adding a favorite, it appears automatically, and favorites are evenly spaced
* [ ] When removing a favorite, it disappears automatically, and favorites are evenly spaced
* [ ] When viewing the search list and adding an application to favorites, the app list remains visible and the favorite appears
* [ ] When searching and adding a result to favorites, the search remains visible and the favorite appears

#### Minimalistic mode on for favorites
* [ ] In settings, UX, enable Minimalistic mode and Minimalistic mode for favorites
* [ ] Favorites bar is hidden by default
* [ ] Touching the screen on an empty area displays history and the favorites
* [ ] When entering a search query, favorite bar is hidden
* [ ] When search query is removed, favorite bar is hidden
* [ ] When searching and pressing home, favorite bar is hidden
* [ ] When coming back from an application launched through search, favorite bar is hidden
* [ ] When kiss bar is opened, favorites are displayed

#### When using the internal favorite bar
* [ ] In settings, Favorites settings, disable "Show favorites above search bar", disable Minimalistic mode and Minimalistic mode for favorites
* [ ] The external favorite bar is hidden by default
* [ ] When entering a search query, external favorite bar is hidden
* [ ] When search query is removed, external favorite bar is still hidden
* [ ] When coming back from an application launched through search, external favorite bar is hidden
* [ ] When kiss bar is opened, favorites are visible in the kiss bar
* [ ] When kiss bar is opened, favorites can be clicked
* [ ] When kiss bar is opened, favorites can be long-clicked
* [ ] When kiss bar is opened, favorites' context menu can be interacted with

#### When using the external favorite bar, with transparent favorite bar
* [ ] In settings, Favorites settings, enable "Show favorites above search bar". In UI, enable "Transparent favorite bar"
* [ ] The bar background is transparent


#### When using the internal favorite bar, with transparent favorite bar
* [ ] In settings, Favorites settings, disable "Show favorites above search bar". In UI, enable "Transparent favorite bar"
* [ ] The bar background is *not* transparent, but still green.

#### First run
* [ ] On the first run, favorites are set by default (browser, contact, and phone)

### UI
#### Theming
* [ ] Picking a theme in the settings updates the settings UI (light or dark)
* [ ] Picking a theme in the settings updates the main screen theme
* [ ] Loader circle is properly tinted according to the primary color
* [ ] Launcher icon is properly tinted according to the primary color
* [ ] KISS bar background is properly tinted according to the primary color
* [ ] Notification bar is of the selected color on the main screen theme

#### Icons pack
* [ ] TODO: theme icon packs

#### General UI
* [ ] Transparent search bar is displayed transparent
* [ ] Large search bar is... large
* [ ] When using large search bar and internal favorites bar, favorites are scaled appropriately
* [ ] When using large search bar and internal favorites bar, kiss bar is scaled appropriately (same height as search field)

### UX
#### Keyboard
* [ ] Disable keyboard on start
* [ ] Press home. Keyboard is not displayed
* [ ] Press back. Keyboard is not displayed
* [ ] Display app list. Hide app list, keyboard is not displayed
* [ ] Touch search field. Keyboard is displayed
* [ ] Open a search result. Press home, keyboard is not displayed
* [ ] Open a search result. Press back, keyboard is not displayed
* [ ] Enable keyboard on start
* [ ] Press home. Keyboard is displayed
* [ ] Display app list. Hide keyboard. Hide app list, keyboard is displayed
* [ ] Open a search result. Press home, keyboard is displayed
* [ ] Open a search result. Press back, keyboard is displayed
* [ ] (using Swiftkey keyboard) Disable "keyboard suggestions fix". Keyboard displays suggestions when typing
* [ ] (using Swiftkey keyboard) Enable "keyboard suggestions fix". Keyboard does not display suggestions when typing

#### Minimalistic mode
* [ ] When using minimalistic mode and pressing home, history is not displayed
* [ ] When using minimalistic mode and pressing the search bar, history is not displayed
* [ ] When using minimalistic mode and pressing anywhere else, history is not displayed
* [ ] When using minimalistic mode with history-touch and pressing anywhere else (outside of a widget), history is displayed
* [ ] When using minimalistic mode with history-touch, display history then press back, history is hidden
* [ ] When using minimalistic mode with history-touch, display history then press home, history is hidden
* [ ] When scrolling down on the history, the keyboard disappears
* [ ] Disable show keyboard on start
* [ ] When using immersive mode for the notification bar, notification bar disappears when pressing home
* [ ] When using immersive mode for the notification bar, notification bar appears when searching for a text
* [ ] When using immersive mode for the navigation bar, navigation bar disappears when pressing home

##### Widgets
* [ ] When using minimalistic mode, the three dot menu has an option to Add a widget
* [ ] Selecting the option displays a list of all available widgets
* [ ] Selecting a widget displays the widget when history is empty
* [ ] Widget is hidden when displaying app list
* [ ] Widget is hidden when searching
* [ ] When opening a search result and pressing back, Widget is displayed
* [ ] When opening a search result and pressing home, Widget is displayed
* [ ] Clicking on the widget opens the widget app
* [ ] Clicking outside of the widget with history-touch replace the widget with history

#### Portrait / landscape
* [ ] When portrait-locked, app can't pivot
* [ ] When not locked, app can pivot
* [ ] When not locked and searching, app can pivot and search remains
* [ ] When not locked and viewing app list, app can pivot and app list remains available

#### Apps
* [ ] App list is displayed alphabetically when A-Z is selected in "App list sort order"
* [ ] App list is displayed in reverse order when Z-A is selected in "App list sort order"
* [ ] With show app tags enabled, long press an app and add a tag. Tag is displayed
* [ ] Search for the tag you just added, app is displayed and tag is highlighted
* [ ] With show app tags disabled, long press an app and add a tag. Tag is not displayed
* [ ] Search for the tag you just added, the app is displayed with matching tag
* [ ] With hide app icons disabled, app icons are displayed
* [ ] With hide app icons enabled, app icons are not displayed (empty space)

#### Wallpaper
* [ ] Wallpaper reacts to touch events
* [ ] TODO: drag events

### Providers settings
* [ ] Search for a contact. Make sure it is displayed as a result and appended to history on click
* [ ] Search for a device setting. Make sure it is displayed as a result and appended to history on click
* [ ] Search for a phone number. Make sure it is displayed as a result and appended to history on click
* [ ] Search for a shortcut (you'll probably need to create one from an app first, for instance, WhatsApp). Make sure it is displayed as a result and appended to history on click
* [ ] Search for a long text. Make sure a web search option is displayed as a result
* [ ] From Providers selection, disable Contacts. From now on, they are not displayed anymore in search or history
* [ ] From Providers selection, disable Device settings. From now on, they are not displayed anymore in search or history
* [ ] From Providers selection, disable Phone numbers. From now on, they are not displayed anymore in search or history
* [ ] From Providers selection, disable Shortcuts. From now on, they are not displayed anymore in search or history
* [ ] From Providers selection, disable Web Search. From now on, they are not displayed anymore in search
* [ ] Add a search provider. Ensure it is available in the select search provider setting
* [ ] Select a search provider. Ensure it is available in search
* [ ] Reset search providers. Ensure only default providers are visible
* [ ] Delete a search provider. Ensure it is not available in search anymore
* [ ] Disable search providers. Disable minimalistic mode. Enter a query with no results. Help text is displayed.
* [ ] Disable search providers. Enable minimalistic mode. Enter a query with no results. Nothing is displayed

### Advanced settings
* [ ] Change default launcher option opens system dialog to pick a launcher
* [ ] TODO: root mode
* [ ] "Restart KISS" option closes the settings and reopen KISS

### Misc
* [ ] Rate the app settings appears if history has more than 300 items
* [ ] Help icon opens help website
