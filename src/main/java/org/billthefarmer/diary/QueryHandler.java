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
import android.os.Message;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.net.Uri;
import android.util.Log;

import java.util.TimeZone;

// QueryHandler
public class QueryHandler extends AsyncQueryHandler
{
    private static final String TAG = "QueryHandler";

    private static final String REMINDER_SELECT =
        "((" + Reminders.EVENT_ID + "=?) AND (" + Reminders.METHOD + "=?))";

    // Projection arrays
    private static final String[] CALENDAR_PROJECTION = new String[]
        {
            Calendars._ID
        };

    // The indices for the projection array above.
    private static final int CALENDAR_ID_INDEX = 0;

    private static final int CALENDAR = -1;
    private static final int EVENT    = -2;
    private static final int REMINDER = -3;

    private static QueryHandler queryHandler;

    // QueryHandler
    public QueryHandler(ContentResolver resolver)
    {
        super(resolver);
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

        Log.d(TAG, "Calendar query start");

        queryHandler.startQuery(CALENDAR, values, Calendars.CONTENT_URI,
                                CALENDAR_PROJECTION, null, null, null);
    }

    // onQueryComplete
    @Override
    public void onQueryComplete(int token, Object object, Cursor cursor)
    {
        // Use the cursor to move through the returned records
        cursor.moveToFirst();

        // Get the field values
        long calendarID = cursor.getLong(CALENDAR_ID_INDEX);

        Log.d(TAG, "Calendar query complete " + calendarID);

        ContentValues values = (ContentValues) object;
        values.put(Events.CALENDAR_ID, calendarID);
        values.put(Events.EVENT_TIMEZONE,
                   TimeZone.getDefault().getDisplayName());

        startInsert(EVENT, null, Events.CONTENT_URI, values);
    }

    // onInsertComplete
    @Override
    public void onInsertComplete(int token, Object object, Uri uri)
    {
        Log.d(TAG, "Event insert complete " + uri.getLastPathSegment());

        Message msg =
            obtainMessage(REMINDER, uri.getLastPathSegment());
        sendMessageDelayed(msg, 60000);
    }

    // onDeleteComplete
    @Override
    public void onDeleteComplete(int token, Object object, int result)
    {
        Log.d(TAG, "Reminder delete complete " + result);
    }

    // handleMessage
    @Override
    public void handleMessage(Message msg)
    {
        switch (msg.what)
        {
        case REMINDER:
            Log.d(TAG, "Reminder delete start");

            String selectionArgs[] = 
                {(String) msg.obj, String.valueOf(Reminders.METHOD_EMAIL)};

            startDelete(REMINDER, null, Reminders.CONTENT_URI,
                        REMINDER_SELECT, selectionArgs);
            break;

        default:
            super.handleMessage(msg);
        }
    }
}
