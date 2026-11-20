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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.documentfile.provider.DocumentFile;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.commonmark.Extension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;

// DiaryWidgetProvider
@SuppressWarnings("deprecation")
public class DiaryWidgetProvider extends AppWidgetProvider
{
    public final static String TAG = "DiaryWidgetProvider";
    public final static String WIDGET = "widget://";
    public final static String ENTRY = "org.billthefarmer.diary.ENTRY";
    public final static String PREF_WIDGET_ENTRY = "pref_widget_entry";

    public final static int PREV = 1;
    public final static int NEXT = 2;
    public final static int TODAY = 0;

    String folder;
    boolean markdown;
    Calendar entry;

    // onReceive
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onReceive " + intent);

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(context);
        // Get folder
        folder = preferences.getString(Settings.PREF_FOLDER, Diary.DIARY);
        markdown = preferences.getBoolean(Settings.PREF_MARKDOWN, true);

        entry = Calendar.getInstance();
        long date = intent.getLongExtra(ENTRY, new Date().getTime());

        if (date != TODAY)
            entry.setTimeInMillis(date);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREF_WIDGET_ENTRY, date);
        editor.apply();

        super.onReceive(context, intent);
    }

    // onAppWidgetOptionsChanged
    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions)
    {
        // Update widget, ignore options
        int[] appWidgetIds = {appWidgetId};
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    // onUpdate
    @Override
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds)
    {
        // Get date
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String date = format.format(entry.getTime());

        // Get text
        CharSequence text = getText(context);

        // Create an Intent to launch Diary
        Intent intent = new Intent(context, Diary.class);
        // This bit of jiggery hackery is to force the system to
        // keep a different intent for each widget
        Uri uri = Uri.parse(WIDGET + String.valueOf(TODAY));
        intent.setData(uri);
        intent.putExtra(ENTRY, entry.getTimeInMillis());
        //noinspection InlinedApi
        PendingIntent pendingIntent =
            PendingIntent.getActivity(context, 0, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);
        // Create an Intent to update widget
        Intent prev = new Intent(context, DiaryWidgetUpdate.class);
        // This bit of jiggery hackery is to force the system to
        // keep a different intent for each widget
        uri = Uri.parse(WIDGET + String.valueOf(PREV));
        prev.setData(uri);
        prev.putExtra(ENTRY, PREV);
        //noinspection InlinedApi
        PendingIntent prevIntent =
            PendingIntent.getActivity(context, 0, prev,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);
        // Create an Intent to update widget
        Intent next = new Intent(context, DiaryWidgetUpdate.class);
        // This bit of jiggery hackery is to force the system to
        // keep a different intent for each widget
        uri = Uri.parse(WIDGET + String.valueOf(NEXT));
        next.setData(uri);
        next.putExtra(ENTRY, NEXT);
        //noinspection InlinedApi
        PendingIntent nextIntent =
            PendingIntent.getActivity(context, 0, next,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);
        // Create an Intent to update widget
        Intent today = new Intent(context, DiaryWidgetUpdate.class);
        // This bit of jiggery hackery is to force the system to
        // keep a different intent for each widget
        uri = Uri.parse(WIDGET + String.valueOf(TODAY));
        intent.setData(uri);
        today.putExtra(ENTRY, TODAY);
        //noinspection InlinedApi
        PendingIntent todayIntent =
            PendingIntent.getActivity(context, 0, today,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);

        // Get the layout for the widget and attach an on-click
        // listener to the view.
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        views.setOnClickPendingIntent(R.id.prev, prevIntent);
        views.setOnClickPendingIntent(R.id.next, nextIntent);
        views.setOnClickPendingIntent(R.id.today, todayIntent);
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
        // Get text
        CharSequence text = Diary.read(context, getFile(entry, context));

        if (markdown)
        {
            // Use commonmark
            List<Extension> extensions =
                Arrays.asList(YamlFrontMatterExtension.create());
            Parser parser = Parser.builder().extensions(extensions).build();
            Node document = parser.parse(text.toString());
            HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(extensions).build();

            String html = renderer.render(document);
            text = Html.fromHtml(html);
        }

        return text;
    }

    /// getHome
    private DocumentFile getHome(Context context)
    {
        String folder = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(Settings.PREF_FOLDER, Diary.DIARY);

        if (folder == null || folder.isEmpty()
                || folder.equals(Diary.DIARY))
            return null;

        return DocumentFile.fromTreeUri(context, Uri.parse(folder));
    }

    // getYear
    private DocumentFile getYear(Context context, int year)
    {
        DocumentFile home = getHome(context);

        if (home == null)
            return null;

        String name = String.format(
                Locale.ENGLISH,
                Diary.YEAR_FORMAT,
                year);

        return home.findFile(name);
    }

    // getDay
    private DocumentFile getDay(Context context, int year, int month, int day)
    {
        DocumentFile home = getHome(context);

        if (home == null)
            return null;

        String yearName = String.format(
                Locale.ENGLISH, Diary.YEAR_FORMAT, year);

        DocumentFile yearFolder = home.findFile(yearName);

        if (yearFolder == null)
            return null;

        String monthName = String.format(
                Locale.ENGLISH, Diary.MONTH_FORMAT, month + 1);

        DocumentFile monthFolder = yearFolder.findFile(monthName);

        if (monthFolder == null)
            return null;

        String dayName = String.format(
                Locale.ENGLISH, Diary.DAY_FORMAT, day);

        DocumentFile file = monthFolder.findFile(dayName);

        if (file != null)
            return file;

        if (markdown)
        {
            dayName = String.format(
                    Locale.ENGLISH, Diary.MD_FORMAT, day);

            return monthFolder.findFile(dayName);
        }

        return null;
    }

    // getFile
    private DocumentFile getFile(Calendar entry, Context context)
    {
        return getDay(
                context,
                entry.get(Calendar.YEAR),
                entry.get(Calendar.MONTH),
                entry.get(Calendar.DATE));
    }

    // getFile
    private DocumentFile getFile(Context context)
    {
        return getFile(Calendar.getInstance(), context);
    }
}
