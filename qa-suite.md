# QA test suite

This document contains a small QA suite that can be run after big changes to the app.
As best as possible, only actual KISS code is tested, not standard Android system behavior.

> This document assumes all settings are at their default value when you start

### Get started...

- [ ] Loader appears when opening the app
- [ ] Help text ("search apps, contacts, ...") appears while app are loading
- [ ] After some time, loader disappears and is replaced by launcher icon
- [ ] Touching the search field displays the keyboard (automated)
- [ ] Searching for text displays results (automated)
- [ ] Clicking the launcher icon displays the list of apps (automated)
- [ ] When clicking on a search item, the corresponding intent is triggered (automated)
- [ ] When going back to KISS, search results have been cleared (automated)
- [ ] When going back to KISS, the item has been added to history (automated)
- [ ] History is displayed when search box is empty (automated)
- [ ] Three dots menu is displayed to the right (automated)
- [ ] Entering a search query replaces the three-dots menu with an "X" (automated)
- [ ] Clicking the "X" empties the search field, displays three dots menu and displays history (automated)
- [ ] When searching, pressing enter on the keyboard launches the first result (automated)
- [ ] When keyboard is displayed, scrolling the list down hides the keyboard
- [ ] When searching, pressing space as the first character does nothing (left-hand side trimming) (automated)
- [ ] Press kiss icon. App list is displayed, and kiss bar appears with a circular reveal animation
- [ ] Press kiss icon again. App list is hidden, and kiss bar disappears with a circular (un)reveal animation
- [ ] With keyboard open, press kiss icon. App list is displayed, and keyboard stays there
- [ ] With keyboard open, press kiss icon. The app list is displayed. When typing something on the keyboard, app list is hidden, and search results start appearing
- [ ] Search for something, press kiss icon. The app list is displayed. Press kiss icon again, search query has been emptied (automated)
- [ ] With KISS set as default launcher, pressing home empties the search field and displays history (automated)
- [ ] With KISS set as default launcher and app list displayed, pressing home hides the app list and displays history (automated)
- [ ] When app bar is displayed, pressing back hides the bar and displays history (automated)
- [ ] When search results are displayed, pressing back empties then search field and displays history (automated)
- [ ] When history is displayed, pressing back does not quit KISS (automated)
- [ ] When searching for something, touching the edit text moves the cursor

#### Menus

- [ ] Clicking the three dots menu open a popup (automated)
- [ ] Long-clicking the three dots menu open a popup (automated)
- [ ] If device has physical menu button, pressing menu displays the three-dots menu
- [ ] Long pressing a search result displays contextual menu (automated)
- [ ] When clicking three dots menu, pressing back dismisses the popup (automated)

### History

#### Standard history manipulation

- [ ] Reset history preference displays number of items in history if history length > 5
- [ ] Reset history clears existing history instantly
- [ ] Reset history summary does not display the old history length after reset
- [ ] Pressing cancel on reset history does not reset history
- [ ] Adding an excluded app hides the app from search results (automated)
- [ ] Adding an excluded app hides the app from history
- [ ] Adding an excluded app hides the app from app list (automated)
- [ ] Resetting excluded app removes all excluded apps
- [ ] Resetting excluded apps allows apps previously hidden to be displayed in app list
- [ ] No more than Max number of history results is displayed in history, and no more than Max number of search results (in Search settings) when searching (automated)
- [ ] Changing the value updates the history dynamically
- [ ] Freezing history prevents new items from being added to history (automated)
- [ ] Unchecking "Freeze history" ensures history is populated again (automated)
- [ ] TODO: history mode

#### Automated history

- [ ] When "Show incoming calls" is disabled, callers are not added to history
- [ ] When "Show incoming calls" is enabled, callers are added to history
- [ ] When "Show newly installed apps" is disabled, new apps are not added to history
- [ ] When "Show newly installed apps" is enabled, new apps are added to history

### Favorites

- [ ] Favorite bar is displayed automatically at startup
- [ ] Clicking on a favorite trigger the correct Intent (automated)
- [ ] When going back to KISS, the favorite has been added to history (automated)
- [ ] Long-clicking favorite displays the menu (automated)
- [ ] Long-click menu can be used to remove the favorite (automated)
- [ ] Empty favorites are not displayed (favorites takes all available space in the bar)
- [ ] When entering a search query, favorite bar is hidden (automated)
- [ ] When search query is removed, bar appears again
- [ ] When coming back from an application launched through search, bar is displayed again
- [ ] When kiss bar is opened, favorites bar is hidden
- [ ] When kiss bar is opened, internal favorites bar is hidden
- [ ] When kiss bar is opened, you can't click on the menu button behind the kiss bar (not even visible, doesn't respond to touch events either)
- [ ] When searching and pressing home, query is cleared, and favorites are displayed
- [ ] When adding a favorite, it appears automatically, and favorites are evenly spaced
- [ ] When removing a favorite, it disappears automatically, and favorites are evenly spaced
- [ ] When viewing the search list and adding an application to favorites, the app list remains visible and the favorite appears
- [ ] When searching and adding a result to favorites, the search remains visible and the favorite appears

#### Minimalistic mode on for favorites

- [ ] In settings, Home screen, enable "Minimalistic UI" and "Favorites bar hidden by default"
- [ ] Favorites bar is hidden by default (automated)
- [ ] Touching the screen on an empty area displays history and the favorites
- [ ] When entering a search query, favorite bar is hidden (automated)
- [ ] When search query is removed, favorite bar is hidden (automated)
- [ ] When searching and pressing home, favorite bar is hidden
- [ ] When coming back from an application launched through search, favorite bar is hidden (automated)
- [ ] When kiss bar is opened, favorites are displayed

#### When using the internal favorite bar

- [ ] In settings, Favorites & tags, disable "Show favorites above search bar", disable "Minimalistic UI" and "Favorites bar hidden by default"
- [ ] The external favorite bar is hidden by default (automated)
- [ ] When entering a search query, external favorite bar is hidden (automated)
- [ ] When search query is removed, external favorite bar is still hidden (automated)
- [ ] When coming back from an application launched through search, external favorite bar is hidden (automated)
- [ ] When kiss bar is opened, favorites are visible in the kiss bar (automated)
- [ ] When kiss bar is opened, favorites can be clicked (automated)
- [ ] When kiss bar is opened, favorites can be long-clicked (automated)
- [ ] When kiss bar is opened, favorites' context menu can be interacted with

#### When using the external favorite bar, with transparent favorite bar

- [ ] In settings, Favorites & tags, enable "Show favorites above search bar" and "Transparent favorites bar"
- [ ] The bar background is transparent

#### When using the internal favorite bar, with transparent favorite bar

- [ ] In settings, Favorites & tags, disable "Show favorites above search bar" and "Transparent favorites bar"
- [ ] The bar background is _not_ transparent, but still green.

#### First run

- [ ] On the first run, favorites are set by default (browser, contact, and phone)

### UI

#### Theming

- [ ] Picking a theme in the settings updates the settings UI (light or dark)
- [ ] Picking a theme in the settings updates the main screen theme
- [ ] Loader circle is properly tinted according to the primary color
- [ ] Launcher icon is properly tinted according to the primary color
- [ ] KISS bar background is properly tinted according to the primary color
- [ ] Status bar is of the selected color on the main screen theme

#### Icons pack

- [ ] TODO: theme icon packs

#### General UI

- [ ] Transparent search bar is displayed transparent
- [ ] Large search bar is... large
- [ ] When using large search bar and internal favorites bar, favorites are scaled appropriately
- [ ] When using large search bar and internal favorites bar, kiss bar is scaled appropriately (same height as search field)

### UX

#### Keyboard

- [ ] Disable keyboard on start (automated)
- [ ] Press home. Keyboard is not displayed (automated)
- [ ] Press back. Keyboard is not displayed (automated)
- [ ] Display app list. Hide app list, keyboard is not displayed (automated)
- [ ] Touch search field. Keyboard is displayed (automated)
- [ ] Open a search result. Press home, keyboard is not displayed
- [ ] Open a search result. Press back, keyboard is not displayed
- [ ] Enable keyboard on start (automated)
- [ ] Press home. Keyboard is displayed (automated)
- [ ] Display app list. Hide keyboard. Hide app list, keyboard is displayed
- [ ] Open a search result. Press home, keyboard is displayed
- [ ] Open a search result. Press back, keyboard is displayed
- [ ] (using Swiftkey keyboard) Disable "keyboard suggestions fix". Keyboard displays suggestions when typing
- [ ] (using Swiftkey keyboard) Enable "keyboard suggestions fix". Keyboard does not display suggestions when typing

#### Minimalistic mode

- [ ] When using minimalistic mode and pressing home, history is not displayed (automated)
- [ ] When using minimalistic mode and pressing the search bar, history is not displayed (automated)
- [ ] When using minimalistic mode and pressing anywhere else, history is not displayed (automated)
- [ ] When using minimalistic mode with history-touch and pressing anywhere else (outside of a widget), history is displayed (automated)
- [ ] When using minimalistic mode with history-touch, display history then press back, history is hidden (automated)
- [ ] When using minimalistic mode with history-touch, display history then press home, history is hidden (automated)
- [ ] When scrolling down on the history, the keyboard disappears
- [ ] Disable show keyboard on start
- [ ] When using immersive mode for the status bar, status bar disappears when pressing home
- [ ] When using immersive mode for the status bar, status bar appears when searching for a text
- [ ] When using immersive mode for the navigation bar, navigation bar disappears when pressing home

##### Widgets

- [ ] When using minimalistic mode, the three dot menu has an option to Add a widget
- [ ] Selecting the option displays a list of all available widgets
- [ ] Selecting a widget displays the widget when history is empty
- [ ] Widget is hidden when displaying app list
- [ ] Widget is hidden when searching
- [ ] When opening a search result and pressing back, Widget is displayed
- [ ] When opening a search result and pressing home, Widget is displayed
- [ ] Clicking on the widget opens the widget app
- [ ] Clicking outside of the widget with history-touch replace the widget with history
- [ ] A newly added widget taller than the remaining space keeps its requested size and is scrolled into view
- [ ] Vertical swipe down/up starting outside the widget area still triggers the configured gesture
- [ ] A two-finger drag starting anywhere over the widget area (widget or empty space) scrolls the widget area
- [ ] The widget context menu never opens during or after a two-finger scroll
- [ ] Normal single-finger long-press menus still open for widgets everywhere
- [ ] A single-finger drag never scrolls the widget area, even at the end of a widget's internal list
- [ ] A single tap anywhere on a widget reaches the widget (including the center of the screen; config-taps work)
- [ ] A widget containing its own vertical list (e.g. agenda/calendar) scrolls its inner list normally with one finger
- [ ] Gestures on empty areas behave exactly as in non-minimalistic KISS: tap reveals history/favorites as configured, double tap locks (or shows the accessibility prompt), long press and swipes trigger their configured actions (e.g. swipe down opens notifications)
- [ ] Visible spacing is present between stacked widgets
- [ ] Home screen setting "Space between widgets": changing it updates existing widget spacing immediately after leaving settings; the row is disabled (not clickable) while "Minimalistic UI" is off
- [ ] After scrolling, long-press menu actions (resize, move up/down, reconfigure, remove) apply to the correct widget
- [ ] Widget order is preserved after rotation and after process death

#### Portrait / landscape

- [ ] When portrait-locked, app can't pivot (automated)
- [ ] When not locked, app can pivot (automated)
- [ ] When not locked and searching, app can pivot and search remains (automated)
- [ ] When not locked and viewing app list, app can pivot and app list remains available (automated)

#### Apps

- [ ] App list is displayed alphabetically when A-Z is selected in "App list sort order"
- [ ] App list is displayed in reverse order when Z-A is selected in "App list sort order"
- [ ] With show app tags enabled, long press an app and add a tag. Tag is displayed
- [ ] Search for the tag you just added, app is displayed and tag is highlighted
- [ ] With show app tags disabled, long press an app and add a tag. Tag is not displayed
- [ ] Search for the tag you just added, the app is displayed with matching tag (automated)
- [ ] With hide app icons disabled, app icons are displayed (automated)
- [ ] With hide app icons enabled, app icons are not displayed (empty space) (automated)

#### Wallpaper

- [ ] Wallpaper reacts to touch events
- [ ] TODO: drag events

### Search settings

- [ ] Search for a contact. Make sure it is displayed as a result and appended to history on click
- [ ] Search for a device setting. Make sure it is displayed as a result and appended to history on click (automated)
- [ ] Search for a phone number. Make sure it is displayed as a result and appended to history on click (automated)
- [ ] Search for a shortcut (you'll probably need to create one from an app first, for instance, WhatsApp). Make sure it is displayed as a result and appended to history on click
- [ ] Search for a long text. Make sure a web search option is displayed as a result
- [ ] From Search sources, disable Contacts. From now on, they are not displayed anymore in search or history
- [ ] From Search sources, disable Device settings. From now on, they are not displayed anymore in search or history
- [ ] From Search sources, disable Direct phone calls. From now on, typing a phone number doesn't display a "Call" result (automated)
- [ ] From Search sources, disable Math formulae. From now on, typing `2+2` doesn't display a result (automated)
- [ ] From Search sources, disable Timers. From now on, typing `5m` doesn't display a result (automated)
- [ ] From Search sources, disable Shortcuts. From now on, they are not displayed anymore in search or history
- [ ] From Search sources, disable Web Search. From now on, they are not displayed anymore in search (automated)
- [ ] Set "Max number of search results" to 2. Searching never displays more than 2 results, history is not impacted (automated)
- [ ] Set "Max number of history results" (in History settings) to 2. History never displays more than 2 results, search is not impacted (automated)
- [ ] Add a search provider. Ensure it is available in the select search provider setting
- [ ] Select a search provider. Ensure it is available in search
- [ ] Reset search providers. Ensure only default providers are visible
- [ ] Delete a search provider. Ensure it is not available in search anymore
- [ ] Disable search providers. Disable minimalistic mode. Enter a query with no results. Help text is displayed. (automated)
- [ ] Disable search providers. Enable minimalistic mode. Enter a query with no results. Nothing is displayed

### Settings search

- [ ] Pressing the search icon in settings displays a search field, with an empty list
- [ ] Typing part of a setting title, summary or choice (e.g. "hexagon") lists matching settings, with the name of their screen
- [ ] Selecting a result opens its screen, scrolled to the setting
- [ ] Selecting a result linking to another screen (e.g. "Apps excluded from history") opens that screen with the right content
- [ ] From a search result, pressing back goes back to the main settings screen, and search is closed
- [ ] Rotating the device while searching doesn't crash (search is closed)

### Advanced settings

- [ ] Change default launcher option opens system dialog to pick a launcher
- [ ] TODO: root mode
- [ ] "Restart KISS" option closes the settings and reopen KISS

### Misc

- [ ] Rate the app settings appears if history has more than 300 items (automated)
- [ ] Help icon opens help website
