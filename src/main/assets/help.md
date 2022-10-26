# Diary
---
## Android personal diary.

* Entries saved in plain text files
* Browse entries
* English, Catalan, Spanish, Italian, Japanese, German, French,
  Polish, Brazilian Portuguese, Dutch and simplified Chinese
* Choice of date picker calendars
* Diary entries may use markdown formatting
* Optional index page
* Index link template
* Optional entry template
* Display media stored in diary folders
* Display [OpenStreetMap](https://www.openstreetmap.org) maps
* Print diary entries
* Share diary entries
* Add media from media providers
* Receive media from other apps
* Receive geo uris from other apps
* Incremental search of diary entries
* Add events to calendar
* Add events from calendar
* Show today's entry in app widget
* Dark or light theme for editing
* Back up entries to a zip file
* Optional edit cursor position control

## Toolbar
The toolbar buttons are, from left to right:

* **Cancel** &ndash; cancel edits of current entry
* **Previous** &ndash; show the previous entry or today
* **Next** &ndash; show the next entry or today if next
* **Today** &ndash; show today's entry
* **Go to date** &ndash; show a date picker calendar to select a new date
* **Index** &ndash; go to index page if set
* **Add link** &ndash; add link to current entry to index page
* **Search** &ndash; incremental search of diary entry
* **Find all** &ndash; find all diary entries containing search text
* **Print** &ndash; print current diary entry
* **Share** &ndash; share current diary entry
* **Use template** &ndash; use template for current entry if empty
* **Add time** &ndash; Add the current time to diary entry
* **Add events** &ndash; Add calendar events to diary entry
* **Add media** &ndash; show a media picker to select media
* **Edit styles** &ndash; show an editor to edit the custom styles
* **Edit script** &ndash; show an editor to edit custom javascript
* **Backup** &ndash; backup entries to a zip file
* **Settings** &ndash; show the settings

Depending on the device and orientation, some items may be on the
menu.

## Swipe left and right
Swipe left and right with two fingers in the diary page will show the
next or previous day, or in the custom calendar will show the next or
previous month.

## Swipe up and down
Swipe up and down with two fingers in the diary page will show the
previous or next month, or in the custom calendar will show the
previous or next year.

## Double tap
In the formatted view a double tap on the screen will switch to the
edit view in approximately the same position in the markdown text. The
accuracy is dependent on the text formatting and media in the entry.

## Editing
In markdown mode the **Edit** button floating above the page allows
editing entries. The **Accept** button restores the formatted view. A
long touch on the button hides it until the device is rotated or a
long touch on the page. The **Cancel** button cancels edits to the
current entry. It will appear only when there are changes to cancel.

See [Markdown](https://daringfireball.net/projects/markdown) for
markdown syntax.

## Scrolling
Scrolling the page up will temporarily hide the floating
button. Scrolling down restores it.

## Search
You may search diary entries, the search will update as text is
entered into the search field. Use the search widget or keyboard
action button to find the next match. To find and edit text, search in
the markdown view, and then double tap where you want to edit. This
will switch to the edit view at about the right place.

## Find all
You may find all diary entries that contain the current search
text. This menu item will only appear while the search widget is
active. A dialog will pop up with a list of matching entries. Touch an
entry to open that entry. You may repeat this or refine the search
text to find the desired entry.

## Index
You may use an index page. If an index page is set the app will start
on that page unless receiving media from another app. See **Links**
for the syntax for links to other diary entries and external
sites. The **Add link** menu item will add a link, using the index
template, to the index.


## Index template
The index template defaults to `<<date>>`, which gives an entry that
looks like [Sunday 14 August 2022](#). The template uses the same
rules as the entry template date code or pattern below. Text outside
the angle brackets will be copied unchanged, text inside the angle
brackets will be interpreted as a date code or date pattern, as below.

## Entry template
You may use an entry template. If a template is set it will be copied
to today's entry if it is empty. If the template contains a date code
`<<date>>` or date pattern `<<date EEEE d MMMM yyyy HH:mm>>` the
current date/time will be inserted in the diary entry. See
[here](https://developer.android.com/reference/java/text/SimpleDateFormat#date-and-time-patterns)
for the pattern documentation.

## Widget
The widget will show as much of the entry as will fit in the size
chosen. Images, maps, and other media will not display because of
android widget limitations. Bullet points do not work very well, but
can be constructed using HTML entities so they appear the same in the
widget:
```html
&nbsp; &bullet;&nbsp; Test item<br>
```

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
added as a link. Media added from removable SD cards not part of the
device storage may work but may not be persistent and is not supported.

## Markdown
Diary supports several markdown extensions including autolink, tables
using vertical bar characters, heading anchor, underlining using
`++underline++`, strikethrough using `~~strike~~`, YAML front matter,
task lists using `- [x] task #1`, `- [ ] task #2`, superscript using
`^super^`, subscript using `~sub~`.

## LaTeX
Diary doesn't support [LaTeX](https://en.wikipedia.org/wiki/LaTeX),
but it is possible to use an external site to add LaTeX formatted
images.

    ![Math](http://www.sciweavers.org/tex2img.php?eq=\sum_{i=1}^{n}x_{i}^{2}&bc=cornsilk&fc=black&im=jpg&fs=24&ff=arev)

    \sum_{i=1}^{n}x_{i}^{2}

![Math](http://www.sciweavers.org/tex2img.php?eq=\sum_{i=1}^{n}x_{i}^{2}&bc=cornsilk&fc=black&im=jpg&fs=24&ff=arev)

If you don't intend to change the image, you can copy it and use that
instead, which will then work offline.

## Svgbob
Diary doesn't support text diagrams, but will display SVG images
produced by an online editor [Svgbob
Editor](https://svgbob-editor.netlify.app/).

![Svgbob](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/diary/bob.svg)

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

## Help file
You may put a link to the embedded [help
file](file:///android_asset/help.md),
`[Help](file:///android_asset/help.md)` in an entry. Selecting it will load
a copy into the markdown view. Long touch on a word and choose
**Select all** on the pop up menu. Select **Share**, and **Diary** in
the Share dialog. Choose a date in the date picker, and you will have
a copy in the chosen entry.

## Maps
You may put [OpenStreetMap](https://www.openstreetmap.org) maps in
diary pages with a geo URI `![osm](geo:<lat>,<lng>)`. Geo uris
received from other apps will be converted to
`![osm](geo:<lat>,<lng>)` syntax in the diary entry.

## Events
You may add an event to the calendar by adding a line to a diary entry
which begins with an 'at' symbol followed by a time and an event title
`@ 10:30 Dentist`. An event will be added to the calendar when the
entry is saved. A colon will be added after the 'at' symbol when the
event has been added `@:10:30 Dentist`. Use the Calendar app to remove
or edit events. Do not remove the colon from the diary entry or the
event will be added again. You may add events from the calendar to the
current diary entry. Events will be added from the relevant calendar
date.

## App widgets
You may put Diary app widgets on the home screen. This will show
today's entry. The widget will be updated when today's entry is
updated.

## Cursor position
You may put a line in an entry to control or remember the edit cursor
position. This will not appear in the markdown view. Put `[<]: #` on a
line for the start of an entry, `[>]: #` for the end of an entry. Put
`[#]: #` for the cursor position to be remembered. There should be no
other text on the line. The current cursor position will be added when
the entry is saved `[#]: # (nnn)`. Because entries are only saved if
they are changed, moving the cursor with no other change will not move
the saved position.

**Note** &ndash; Using this functionality will not stop reference type links
to the top of the current entry, `[Top][#]` working. However you can
more simply use an inline link `[Top](#)`.

## Styles
You may add custom styles to the markdown formatting by placing a
`styles.css` file in the `Diary/css` folder, which will replace the
built in styles file which simply limits the width of images to the
page width. Use the built in editor to create a styles file. You must
use the editor `accept` tick button to save the edits.

**Caution** &ndash; There is no such thing as a markdown syntax error, but
syntax errors in a styles file may cause unpredictable results and
affect all diary pages. See
[CSS Tutorial](https://www.w3schools.com/Css).

You may include the built in styles file with an `@import` statement
`@import "file:///android_asset/styles.css";` or
`@import url("file:///android_asset/styles.css");`, which should be on
the first line.

## Javascript
You may add custom javascript to be loaded into all diary pages by
placing a `script.js` file in the `Diary/js` folder. Use the built in
editor to create a script file. You must use the editor `accept` tick
button to save the edits. Errors in the script will be logged by the
[WebView](https://developer.android.com/reference/android/webkit/WebView)
which displays the page. See
[Javascript tutorial](https://www.w3schools.com/js).

If you want to use javascript libraries or write a large script it
might be a good idea to use the Google
[Closure Compiler](https://developers.google.com/closure/compiler) to
check and minimise your code. It will handle multiple input files.

## Emojies
There is a script,
[emojies.js](https://github.com/billthefarmer/diary/blob/master/data/emojies.js)
in the data folder and a minimised version,
[emojies.min.js](https://github.com/billthefarmer/diary/blob/master/data/emojies.min.js). Either
may be copied to the `script.js` file as above. The script is a work
in progress, it could do with some TLC from an expert.

## Backup
You may create a backup of all your entries in a zip file. The file
will have the same name as the diary folder, default `Diary.zip`.

## Sync
Android cloud storage apps when last tested appeared not to be capable
of syncing a real storage folder on the device. However
[Syncthing](https://syncthing.net) does just that and can sync your
diary folder with other devices and desktop computers.

## Removable SD cards
Android allows removable SD cards to be used like a USB stick or as
part of the device storage. Storing diary entries on a removable SD
card not part of the device storage may work on some devices, but is
not supported. Adding media may also work, but may not be persistent.

## Settings
* **Use custom calendar** &ndash; Use custom calendar that shows diary
    entries rather than date picker calendar
* **Use markdown** &ndash; Use markdown formatting for diary entries
* **Folder** &ndash; Change diary entry storage folder. Caution &ndash;
    diary entries and styles will not be moved
* **Index** &ndash; Set an index page. Use the date picker to choose a date.
* **Template** &ndash; Set a template page. Use the date picker to
    choose a date.
* **Browser** &ndash; Use browser for external links
* **Copy images** &ndash; Copy images to current folder
* **Dark theme** &ndash; Use dark theme for editing
* **About** &ndash; Show app version, licence and credits
