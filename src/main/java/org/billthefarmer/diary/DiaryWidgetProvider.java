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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class DiaryWidgetProvider extends AppWidgetProvider
{
    String folder;
    boolean markdown;

    // onAppWidgetOptionsChanged
    @Override
    @SuppressLint("InlinedApi")
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions)
    {
        // Get date
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String date = format.format(new Date());

        // Get text
        CharSequence text = getText(context);

        // Create an Intent to launch Diary
        Intent intent = new Intent(context, Diary.class);
        PendingIntent pendingIntent =
            PendingIntent.getActivity(context, 0, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);

        // Get the views
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        views.setTextViewText(R.id.header, date);
        views.setTextViewText(R.id.entry, text);

        // Tell the AppWidgetManager to perform an update on the
        // current app widget.
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // onUpdate
    @Override
    @SuppressLint("InlinedApi")
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds)
    {
        // Get date
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String date = format.format(new Date());

        // Get text
        CharSequence text = getText(context);

        // Create an Intent to launch Diary
        Intent intent = new Intent(context, Diary.class);
        PendingIntent pendingIntent =
            PendingIntent.getActivity(context, 0, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);

        // Get the layout for the widget and attach an on-click
        // listener to the view.
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        views.setTextViewText(R.id.header, date);
        views.setTextViewText(R.id.entry, text);

        // Tell the AppWidgetManager to perform an update on the app
        // widgets.
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    // getText
    @SuppressWarnings("deprecation")
    private CharSequence getText(Context context)
    {
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(context);
        // Get folder
        folder = preferences.getString(Settings.PREF_FOLDER, Diary.DIARY);
        markdown = preferences.getBoolean(Settings.PREF_MARKDOWN, true);

        // Get date
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String date = format.format(new Date());

        // Get text
        CharSequence text = Diary.read(getFile());

        if (markdown)
        {
            // Use commonmark
            Parser parser = Parser.builder().build();
            Node document = parser.parse(text.toString());
            HtmlRenderer renderer = HtmlRenderer.builder().build();

            String html = renderer.render(document);
            text = Html.fromHtml(html);
        }

        return text;
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
                                                 Diary.YEAR_FORMAT, year));
    }

    // getMonth
    private File getMonth(int year, int month)
    {
        return new File(getYear(year),
                        String.format(Locale.ENGLISH,
                                      Diary.MONTH_FORMAT, month + 1));
    }

    // getDay
    private File getDay(int year, int month, int day)
    {
        File folder = getMonth(year, month);
        File file = new File(folder, String.format(Locale.ENGLISH,
                                                   Diary.DAY_FORMAT, day));
        if (file.exists())
            return file;

        else if (markdown)
            return new File(folder, String.format(Locale.ENGLISH,
                                                  Diary.MD_FORMAT, day));
        else
            return file;
    }

    // getFile
    private File getFile(Calendar entry)
    {
        return getDay(entry.get(Calendar.YEAR),
                      entry.get(Calendar.MONTH),
                      entry.get(Calendar.DATE));
    }

    // getFile
    private File getFile()
    {
        Calendar calendar = Calendar.getInstance();
        return getFile(calendar);
    }
}
