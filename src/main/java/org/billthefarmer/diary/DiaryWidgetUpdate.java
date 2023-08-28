////////////////////////////////////////////////////////////////////////////////
//
//  Diary - Personal diary for Android
//
//  Copyright (C) 2017	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.diary;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

// DiaryWidgetUpdate
@SuppressWarnings("deprecation")
public class DiaryWidgetUpdate extends Activity
{
    public static final String TAG = "DiaryWidgetUpdate";

    String folder;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (BuildConfig.DEBUG)
            Log.d(TAG, "onCreate " + intent);

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        // Get folder
        folder = preferences.getString(Settings.PREF_FOLDER, Diary.DIARY);
        // Get widget entry
        long entry = preferences.getLong(DiaryWidgetProvider.PREF_WIDGET_ENTRY,
                                         new Date().getTime());

        // Get calendar
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(entry);

        // Get action
        int action = intent.getIntExtra(DiaryWidgetProvider.ENTRY,
                                        DiaryWidgetProvider.TODAY);
        switch (action)
        {
        case DiaryWidgetProvider.TODAY:
            entry = new Date().getTime();
            break;

        case DiaryWidgetProvider.PREV:
            Calendar prev = getPrevEntry(calendar.get(Calendar.YEAR),
                                         calendar.get(Calendar.MONTH),
                                         calendar.get(Calendar.DATE));
            if (prev == null)
                entry = calendar.getTimeInMillis();

            else
                entry = prev.getTimeInMillis();
            break;

        case DiaryWidgetProvider.NEXT:
            Calendar next = getNextEntry(calendar.get(Calendar.YEAR),
                                         calendar.get(Calendar.MONTH),
                                         calendar.get(Calendar.DATE));
            if (next == null)
                entry = new Date().getTime();

            else
                entry = next.getTimeInMillis();
            break;
        }

        // Get manager
        AppWidgetManager appWidgetManager =
            AppWidgetManager.getInstance(this);
        ComponentName provider = new
            ComponentName(this, DiaryWidgetProvider.class);

        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(provider);
        Intent broadcast = new
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        broadcast.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                           appWidgetIds);
        broadcast.putExtra(DiaryWidgetProvider.ENTRY, entry);
        sendBroadcast(broadcast);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "sendBroadcast " + broadcast);

        finish();
    }

    // getHome
    private File getHome()
    {
        File file = new File(folder);
        if (file.isAbsolute() && file.isDirectory() && file.canWrite())
            return file;

        return new File(Environment.getExternalStorageDirectory(), folder);
    }

    // getYear
    private File getYear(int year)
    {
        return new File(getHome(), String.format(Locale.ENGLISH,
                                                 Diary.YEAR_FORMAT,
                                                 year));
    }

    // getMonth
    private File getMonth(int year, int month)
    {
        return new File(getYear(year), String.format(Locale.ENGLISH,
                                                     Diary.MONTH_FORMAT,
                                                     month + 1));
    }

    // prevYear
    private int prevYear(int year)
    {
        int prev = -1;
        for (File yearDir : Diary.listYears(getHome()))
        {
            int n = Diary.yearValue(yearDir);
            if (n < year && n > prev)
                prev = n;
        }
        return prev;
    }

    // prevMonth
    private int prevMonth(int year, int month)
    {
        int prev = -1;
        for (File monthDir : Diary.listMonths(getYear(year)))
        {
            int n = Diary.monthValue(monthDir);
            if (n < month && n > prev)
                prev = n;
        }
        return prev;
    }

    // prevDay
    private int prevDay(int year, int month, int day)
    {
        int prev = -1;
        for (File dayFile : Diary.listDays(getMonth(year, month)))
        {
            int n = Diary.dayValue(dayFile);
            if (n < day && n > prev)
                prev = n;
        }
        return prev;
    }

    // getPrevEntry
    private Calendar getPrevEntry(int year, int month, int day)
    {
        int prev;
        if ((prev = prevDay(year, month, day)) == -1)
        {
            if ((prev = prevMonth(year, month)) == -1)
            {
                if ((prev = prevYear(year)) == -1)
                    return null;
                return getPrevEntry(prev, Calendar.DECEMBER, 32);
            }
            return getPrevEntry(year, prev, 32);
        }
        return new GregorianCalendar(year, month, prev);
    }

    // nextYear
    private int nextYear(int year)
    {
        int next = -1;
        for (File yearDir : Diary.listYears(getHome()))
        {
            int n = Diary.yearValue(yearDir);
            if (n > year && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // nextMonth
    private int nextMonth(int year, int month)
    {
        int next = -1;
        for (File monthDir : Diary.listMonths(getYear(year)))
        {
            int n = Diary.monthValue(monthDir);
            if (n > month && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // nextDay
    private int nextDay(int year, int month, int day)
    {
        int next = -1;
        for (File dayFile : Diary.listDays(getMonth(year, month)))
        {
            int n = Diary.dayValue(dayFile);
            if (n > day && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // getNextEntry
    private Calendar getNextEntry(int year, int month, int day)
    {
        int next;
        if ((next = nextDay(year, month, day)) == -1)
        {
            if ((next = nextMonth(year, month)) == -1)
            {
                if ((next = nextYear(year)) == -1)
                    return null;
                return getNextEntry(next, Calendar.JANUARY, -1);
            }
            return getNextEntry(year, next, -1);
        }
        return new GregorianCalendar(year, month, next);
    }
}
