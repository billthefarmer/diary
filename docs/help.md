# ![Logo](https://github.com/billthefarmer/diary/raw/master/src/main/res/drawable-hdpi/ic_launcher.png) Diary
---
![Diary](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/diary/Diary-phone.png) ![Calendar](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/diary/Calendar-phone.png)

### Android personal diary.

* Entries saved in plain text files
* Browse entries
* English, Catalan, Spanish, Italian, Japanese, German, French,
  Polish, Brazilian Portuguese and simplified Chinese
* Choice of date picker calendars
* Diary entries may use markdown formatting
* Optional index page
* Display media stored in diary folders
* Display [OpenStreetMap](http://www.openstreetmap.org) maps
* Add media from media providers
* Receive media from other apps
* Receive geo uris from other apps
* Incremental search of diary entries
* Add events to calendar
* Dark or light theme for editing

## Toolbar
The toolbar icons are, from left to right:

* **Previous** - show the previous entry or today
* **Next** - show the next entry or today if next
* **Today** - show today's entry
* **Go to date** - show a date picker calendar to select a new date
* **Index** - go to index page if set
* **Search** - incremental search of diary entry
* **Find all** - find all diary entries containing search text
* **Add media** - show a media picker to select media
* **Edit styles** - show an editor to edit the custom styles
* **Settings** - show the settings

Depending on the device and orientation, some items may be on the
menu.

## Swipe left and right
Swipe left and right in the diary page will show the next or previous
day, or in the custom calendar will show the next or previous month.

## Swipe up and down
Swipe up and down with two fingers in the diary page will show the
previous or next month, or in the custom calendar will show the
previous or next year.

## Editing
In markdown mode the **Edit** button floating above the page allows
editing entries. The **Accept** button restores the formatted view. A
long touch on the button hides it until the device is rotated or a
long touch on the page.

See [Markdown](https://daringfireball.net/projects/markdown) for
markdown syntax.

## Search
You may search diary entries, the search will update as text is
entered into the search field. Use the search widget or keyboard
action button for find next.

## Find all
You may find all diary entries that contain the current search
text. This menu item will only appear while the search widget is
active. A dialog will pop up with a list of matching entries. Touch an
entry to open that entry. You may repeat this or refine the search
text to find the desired entry.

## Index
You may use an index page. If an index page is set the app will start
on that page unless receiving media from another app.

## Text
You may receive text clips from another app. A date picker will pop
up. The text will be appended to the entry selected.

## Media
You may store media in the diary storage folders and reference them in
diary entries, so markdown text `![cat](cat.jpg)` will display
`cat.jpg` stored in the current month folder which is
`Diary/<year>/<month>` on the sdcard. You may either add media from
media providers like file managers or image managers or receive media
sent by other apps. Media added will be added at the current cursor
position. Media sent by other apps will pop up a date picker. The
media will be appended to the selected page. Content URIs
(`content://`) sent by some media providers and apps will be resolved
to file URIs (`file:///`) if possible. Media will be added using
markdown image syntax (`![<name>](<url>)`), which will be parsed into
an HTML5 media player, text clips will be added as above, URLs will be
added as a link.

## Links
You may put links in diary pages, so
`[DuckDuckGo](https://duckduckgo.com)` will show a link to
DuckDuckGo. Links will be followed if touched. Use the back button in
the toolbar or the navigation bar to return to the diary page. Links
to other diary pages may either be a relative reference to the entry
file `[Sunday](25.txt)`, or use a date URI
`[Whenever](date:17/10/2017)`. Use your local convention for short
form numeric dates (UK: `dd/mm/yyyy`, US: `mm/dd/yyyy`). Use the back
button to return. The back button may be used to retrace your path
through diary entries and external web pages. Use the **Today** button
to clear the navigation entries.

## Maps
You may put [OpenStreetMap](http://www.openstreetmap.org) maps in
diary pages with the syntax `[<lat>,<lng>]`, or
`[osm:<lat>,<lng>]`. Because of differing conventions in different
locales, the app parser will accept a comma or a dot (`[,.]`) for the
decimal point, and a comma or a semicolon (`[,;]`) for the co-ordinate
separator. Use the correct convention for your locale. Geo uris
received from other apps will be converted to `[osm](<lat>,<lng>)`
syntax in the diary entry. Diary entries using `[<lat>,<lng>]` syntax
will be converted to geo uri syntax. **Caution** - geo uris use a
period (`[.]`) for a decimal point and a comma (`[,]`) for the
co-ordinate separator regardless of locale.

## Events
You may add an event to the calendar by adding a line to a diary entry
which begins with an 'at' symbol followed by a time and an event
title.

    @ 10:30 Dentist

An event will be added to the calendar when the entry is saved. A
colon will be added after the 'at' symbol when the event has been
added.

    @:10:30 Dentist

Use the Calendar app to remove or edit events. Do not remove the colon
from the diary entry or the event will be added again.

## Styles
You may add custom styles to the markdown formatting by placing a
`styles.css` file in the `Diary/css` folder, which will replace the
built in styles file which simply limits the width of images to the
page width. Use the built in editor to create a styles file.

**Caution** - There is no such thing as a markdown syntax error, but
syntax errors in a styles file may cause unpredictable results and
affect all diary pages. See
[CSS Tutorial](https://www.w3schools.com/Css).

You may include the built in styles file with an `@import` statement
`@import "file:///android_asset/styles.css";` or
`@import url("file:///android_asset/styles.css");`, which should be on
the first line.

## Settings
* **Use custom calendar** - Use custom calendar that shows diary
  entries rather than date picker calendar
* **Use markdown** - Use markdown formatting for diary entries
* **Folder** - Change diary entry storage folder. Caution - diary
  entries and styles will not be moved
* **Index** - Set an index page. Use the date picker to choose a date.
* **Copy images** - Copy images to current folder
* **Dark theme** - Use dark theme for editing
* **About** - Show app version, licence and credits
