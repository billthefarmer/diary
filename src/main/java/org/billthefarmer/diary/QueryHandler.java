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
import android.provider.CalendarContract.Instances;
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

    private static final String[] INSTANCE_PROJECTION = new String[]
    {
        Instances.BEGIN, Instances.TITLE
    };

    private static final String INSTANCE_ORDERBY = Instances.BEGIN + " ASC";

    // The indices for the projections above.
    private static final int CALENDAR_ID_INDEX = 0;
    private static final int INSTANCE_BEGIN_INDEX = 0;
    private static final int INSTANCE_TITLE_INDEX = 1;

    private static final int INSTANCE_LISTEN = 0;
    private static final int EVENT_INSERT = 1;
    private static final int EVENT_REMIND = 2;
    private static final int EVENT_DONE = 3;

    private static QueryHandler queryHandler;
    private static EventListener listener;

    // QueryHandler
    private QueryHandler(ContentResolver resolver)
    {
        super(resolver);
    }

    // queryEvents
    public static void queryEvents(Context context, long startTime,
                                   long endTime, EventListener l)
    {
        ContentResolver resolver = context.getContentResolver();

        if (queryHandler == null)
            queryHandler = new QueryHandler(resolver);

        listener = l;

        Uri path = Instances.CONTENT_URI;
        path = Uri.withAppendedPath(path, String.valueOf(startTime));
        path = Uri.withAppendedPath(path, String.valueOf(endTime));

        if (BuildConfig.DEBUG)
            Log.d(TAG, String.format("Query with path %s", path));

        queryHandler.startQuery(INSTANCE_LISTEN, null, path,
                                INSTANCE_PROJECTION, null,
                                null, INSTANCE_ORDERBY);
    }

    // insertEvent
    public static void insertEvent(Context context, long startTime,
                                   long endTime, String title)
    {
        ContentResolver resolver = context.getContentResolver();

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
        case EVENT_INSERT:
            // Use the cursor to move through the returned records
            cursor.moveToFirst();
            // Get the field value
            calendarID = cursor.getLong(CALENDAR_ID_INDEX);
            values.put(Events.CALENDAR_ID, calendarID);
            values.put(Events.EVENT_TIMEZONE,
                       TimeZone.getDefault().getDisplayName());
            startInsert(EVENT_REMIND, null, Events.CONTENT_URI, values);
            break;

        case INSTANCE_LISTEN:
            // Use the cursor to move through the returned records
            while (cursor.moveToNext())
            {
                // Get the field values
                long startTime = cursor.getLong(INSTANCE_BEGIN_INDEX);
                String title = cursor.getString(INSTANCE_TITLE_INDEX);

                if (BuildConfig.DEBUG)
                    Log.d(TAG, String.format("Found event with title %s on %s",
                                             title, startTime));

                // Return values
                if (listener != null)
                    listener.onEvent(startTime, title);
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
            case EVENT_REMIND:
                long eventID = Long.parseLong(uri.getLastPathSegment());
                ContentValues values = new ContentValues();
                values.put(Reminders.MINUTES, 10);
                values.put(Reminders.EVENT_ID, eventID);
                values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
                startInsert(EVENT_DONE, null, Reminders.CONTENT_URI, values);
                break;

            case EVENT_DONE:
                break;
            }
        }
    }

    // EventListener
    public interface EventListener
    {
        public abstract void onEvent(long startTime, String title);
    }
}
