////////////////////////////////////////////////////////////////////////////////
//
//  Diary - Personal diary for Android
//
//  Copyright © 2017  Bill Farmer
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
////////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.diary;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

import java.util.TimeZone;

// QueryHandler
public class QueryHandler extends AsyncQueryHandler
{
    private static final String TAG = "QueryHandler";

    // Projections
    private static final String[] CALENDAR_PROJECTION = new String[]
    {
        Calendars._ID
    };

    private static final String[] EVENT_PROJECTION = new String[]
    {
        Events.DTSTART, Events.TITLE, Events.DESCRIPTION
    };

    // Events selection
    private static final String EVENT_SELECTION =
        "((" + Events.CALENDAR_ID + " = ?) AND (" +
        Events.DTSTART + " > ?) AND (" + Events.DTSTART + " < ?))";

    // The indices for the projections above.
    private static final int CALENDAR_ID_INDEX = 0;
    private static final int EVENT_DTSTART_INDEX = 0;
    private static final int EVENT_TITLE_INDEX = 1;
    private static final int EVENT_DESCRIPTION_INDEX = 2;

    private static final int EVENT_QUERY = 0;
    private static final int EVENT_INSERT = 1;
    private static final int EVENT_REMINDER = 2;
    private static final int EVENT_REPORT = 3;
    private static final int EVENT_DISCARD = 4;

    private static QueryHandler queryHandler;

    // QueryHandler
    private QueryHandler(ContentResolver resolver)
    {
        super(resolver);
    }

    // queryEvents
    public static void queryEvents(Diary diary, long startTime, long endTime)
    {
        ContentResolver resolver = diary.getContentResolver();

        if (queryHandler == null)
            queryHandler = new QueryHandler(resolver);

        ContentValues values = new ContentValues();
        values.put(Events.DTSTART, startTime);
        values.put(Events.DTEND, endTime);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Calendar query start");

        queryHandler.startQuery(EVENT_QUERY, values, Calendars.CONTENT_URI,
                                CALENDAR_PROJECTION, null, null, null);
    }

    // insertEvent
    public static void insertEvent(Diary diary, long startTime,
                                   long endTime, String title)
    {
        ContentResolver resolver = diary.getContentResolver();

        if (queryHandler == null)
            queryHandler = new QueryHandler(resolver);

        ContentValues values = new ContentValues();
        values.put(Events.DTSTART, startTime);
        values.put(Events.DTEND, endTime);
        values.put(Events.TITLE, title);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Calendar query start");

        queryHandler.startQuery(EVENT_INSERT, values, Calendars.CONTENT_URI,
                                CALENDAR_PROJECTION, null, null, null);
    }

    // onQueryComplete
    @Override
    public void onQueryComplete(int token, Object object, Cursor cursor)
    {
        // Check rows
        if (cursor == null || cursor.getCount() == 0)
            return;

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Query complete");

        ContentValues values = (ContentValues) object;
        long calendarID = 0;

        switch (token)
        {
        case EVENT_QUERY:
            // Use the cursor to move through the returned records
            cursor.moveToFirst();
            // Get the field value
            calendarID = cursor.getLong(CALENDAR_ID_INDEX);
            String[] selectionArgs = new String[]
                {Long.toString(calendarID),
                 values.getAsString(Events.DTSTART),
                 values.getAsString(Events.DTSTART)};

            queryHandler.startQuery(EVENT_REPORT, values, Events.CONTENT_URI,
                                    EVENT_PROJECTION, EVENT_SELECTION,
                                    selectionArgs, null);
            break;

        case EVENT_INSERT:
            // Use the cursor to move through the returned records
            cursor.moveToFirst();
            // Get the field value
            calendarID = cursor.getLong(CALENDAR_ID_INDEX);
            values.put(Events.CALENDAR_ID, calendarID);
            values.put(Events.EVENT_TIMEZONE,
                       TimeZone.getDefault().getDisplayName());
            startInsert(EVENT_REMINDER, null, Events.CONTENT_URI, values);
            break;

        case EVENT_REPORT:
            while (cursor.moveToNext())
            {
                long startTime = cursor.getLong(EVENT_DTSTART_INDEX);
                String title = cursor.getString(EVENT_TITLE_INDEX);
                String description = cursor.getString(EVENT_DESCRIPTION_INDEX);
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Event " + startTime + ", " + title + ", " +
                          description);
            }
        }
    }

    // onInsertComplete
    @Override
    public void onInsertComplete(int token, Object object, Uri uri)
    {
        if (uri != null)
        {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Insert complete " + uri.getLastPathSegment());

            switch (token)
            {
            case EVENT_REMINDER:
                long eventID = Long.parseLong(uri.getLastPathSegment());
                ContentValues values = new ContentValues();
                values.put(Reminders.MINUTES, 10);
                values.put(Reminders.EVENT_ID, eventID);
                values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
                startInsert(EVENT_DISCARD, null, Reminders.CONTENT_URI, values);
                break;

            case EVENT_DISCARD:
                break;
            }
        }
    }
}
