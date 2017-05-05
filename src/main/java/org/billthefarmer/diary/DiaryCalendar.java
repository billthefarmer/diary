//  Diary - Personal diary for Android
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

package org.billthefarmer.diary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.text.DateFormat;
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
    private CustomCalendarView calendarView;
    private Date time;

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

    private class DateDecorator
        implements DayDecorator
    {
        private List<Date> dates;

        private DateDecorator(List<Date> dates)
        {
            this.dates = dates;
        }

        public void decorate(DayView cell)
        {
            Date cellDate = cell.getDate();
            for (Date date: dates)
            {
                if (date.equals(cellDate))
                    cell.setBackgroundResource(R.drawable.diary_event);
            }
        }
    }
}
