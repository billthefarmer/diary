# Diary
---
### Android personal diary.

* Entries saved in plain text files
* Browse entries
* English, Catalan, Spanish, Japanese and German
* Choice of date picker calendars
* Diary entries may use markdown formatting
* Display images stored in diary folders
* Add images from media providers
* Receive images from other apps

## Toolbar
The toolbar icons are, from left to right:

* **Previous** - show the previous entry or today
* **Next** - show the next entry or today if next
* **Today** - show today's entry
* **Go to date** - show a date picker calendar to select a new date
* **Add image** - show a media picker to select an image
* **Settings** - show the settings

Depending on the device and orientation, some items may be on the
menu.

## Swipe left and right
Swipe left and right in the diary will show the next or previous day
or in the custom calendar will show the next or previous month.

## Editing
In markdown mode the floating **Edit** button allows editing
entries. The **Accept** button restores the formatted view. You may
cut and paste this text into another page if you don't want it here.

See [Markdown](https://daringfireball.net/projects/markdown) for
markdown syntax.

## Images
You may store images in the diary storage folders and reference them
in diary entries so markdown text `![cat](cat.jpg)` will display
`cat.jpg` stored in the current month folder which is
`Diary/<year>/<month>` on the sdcard.

You may either add images from media providers like file managers or
image managers or receive images sent by other apps. Images added will
be added at the current cursor position. Images sent by other apps
will pop up a date picker. The image or images will be appended to the
selected page. Content URIs (`content://`) sent by some media
providers will be resolved to file URIs (`file:///`) if possible.

## Styles
You may add custom styles to the markdown formatting by placing a
`styles.css` file in the `Diary/css` folder, which will replace the
built in styles file which simply limits the width of images to the
page width. The simplest way to create a styles file is to write it on
a diary page, and then use a file manager to move and rename it.

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
* **About** - Show app version, licence and credits
