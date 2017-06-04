# ![Logo](src/main/res/drawable-mdpi/ic_launcher.png) Diary [![Build Status](https://travis-ci.org/billthefarmer/diary.svg?branch=master)](https://travis-ci.org/billthefarmer/diary)

Android personal diary. The app is available on [F-Droid](https://f-droid.org/repository/browse/?fdid=org.billthefarmer.diary)
and [here](https://github.com/billthefarmer/diary/releases).

![Diary](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/diary/Diary-phone.png) ![Calendar](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/diary/Calendar-phone.png)

![Calendar](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/diary/Calendar-landscape.png)

There is a
[help](https://github.com/billthefarmer/diary/blob/master/data/help.md)
and a
[test](https://github.com/billthefarmer/diary/blob/master/data/test.md)
file, which may be copied in to an entry for reference if required.

* Entries saved in plain text files
* Browse entries
* English, Catalan, Spanish, Japanese and German
* Choice of date picker calendars
* Diary entries may use markdown formatting

## Toolbar
The toolbar icons are, from left to right:

* **Previous** - show the previous entry or today
* **Next** - show the next entry or today if next
* **Today** - show today's entry
* **Go to date…** - show a date picker dialog to select a new date
* **Settings** - show the settings

Depending on the device and orientation, some items may be on the
menu.

## Swipe left and right
Swipe left and right in the diary will show the next or previous day
or in the custom calendar will show the next or previous month.

## Editing
In markdown mode the **Edit** button floating above the page allows
editing entries. The **Done** button restores the formatted view.

You may store images in the diary storage folders and reference them
in diary entries so markdown text `![cat](cat.jpg)` will display
`cat.jpg` stored in the current month folder which is
`Diary/<year>/<month>` on the sdcard.

## Settings
* **Use custom calendar** - Use custom calendar that shows diary
  entries rather than date picker calendar
* **Use markdown** - Use markdown formatting for diary entries
* **About** - Show app version, licence and credits
