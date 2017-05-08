////////////////////////////////////////////////////////////////////////////////
//
//  Diary - Personal diary for Android
//
//  Copyright Â© 2017  Bill Farmer
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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.stacktips.view.CalendarListener;
import com.stacktips.view.CustomCalendarView;
import com.stacktips.view.DayDecorator;
import com.stacktips.view.DayView;

public class DiaryCalendar extends Activity
{
    public final static String TAG = "DiaryCalendar";

    private CustomCalendarView calendarView;
    private Date time;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        // Initialize CustomCalendarView from layout
        calendarView = (CustomCalendarView) findViewById(R.id.calendar);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        // Initialize calendar with date
        Calendar currentCalendar = Calendar.getInstance(Locale.getDefault());
        currentCalendar.setTimeInMillis(bundle.getLong(Diary.DATE));

        // Get date
        time = currentCalendar.getTime();
        setTitle(DateFormat.getDateInstance(DateFormat.FULL)
                 .format(time));

        // Show Monday as first date of week
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);

        // Show/hide overflow days of a month
        // calendarView.setShowOverflowDate(false);

        long longEntries[] = bundle.getLongArray(Diary.ENTRIES);
        List<Calendar> entries = new ArrayList<Calendar>();
        for (long time: longEntries)
        {
            Calendar entry = Calendar.getInstance();
            entry.setTimeInMillis(time);
            entries.add(entry);
        }
        List<DayDecorator> decorators = new ArrayList<DayDecorator>();
        decorators.add(new DateDecorator(entries));
        calendarView.setDecorators(decorators);

        // call refreshCalendar to update calendar the view
        calendarView.refreshCalendar(currentCalendar);

        // Handling custom calendar events
        calendarView.setCalendarListener(new CalendarListener()
            {
                @Override
                public void onDateSelected(Date date)
                {
                    time = date;
                    setTitle(DateFormat.getDateInstance(DateFormat.FULL)
                             .format(time));
                }

                @Override
                public void onMonthChanged(Date date) {}
            });

        Button button = (Button) findViewById(R.id.done);
        button.setOnClickListener(new View.OnClickListener()
            {
                // On click
                @Override
                public void onClick(View v)
                {
                    int id = v.getId();

                    switch(id)
                    {
                        // Done
                    case R.id.done:
                        // Return new date in intent
                        Intent intent = new Intent();
                        intent.putExtra(Diary.DATE, time.getTime());
                        setResult(RESULT_OK, intent);
                        finish();
                        break;
                    }
                }
            });
    }

    // DateDecorator
    private class DateDecorator
        implements DayDecorator
    {
        private List<Calendar> entries;

        private DateDecorator(List<Calendar> entries)
        {
            this.entries = entries;
        }
        // decorate
        @Override
        public void decorate(DayView dayView)
        {
            Calendar cellDate = Calendar.getInstance();
            cellDate.setTime(dayView.getDate());
            for (Calendar entry: entries)
                if (cellDate.get(Calendar.DATE) == entry.get(Calendar.DATE) &&
                    cellDate.get(Calendar.MONTH) == entry.get(Calendar.MONTH) &&
                    cellDate.get(Calendar.YEAR) == entry.get(Calendar.YEAR))
                    dayView.setBackgroundResource(R.drawable.diary_entry);
        }
    }
}
