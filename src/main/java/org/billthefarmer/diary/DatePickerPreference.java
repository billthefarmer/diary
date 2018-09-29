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

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.GregorianCalendar;

// DatePickerPreference
public class DatePickerPreference extends DialogPreference
{
    protected final static long DEFAULT_VALUE = 946684800000L;

    private long value = DEFAULT_VALUE;

    // Constructor
    public DatePickerPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    // On create dialog view
    @Override
    protected View onCreateDialogView()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value);

        DatePicker picker = new DatePicker(getContext());
        // onDateChanged
        picker.init(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DATE),
                    (view, year, monthOfYear, dayOfMonth) ->
        {
            Calendar calendar1 = new
            GregorianCalendar(year, monthOfYear,
                              dayOfMonth);
            value = calendar1.getTimeInMillis();
        });
        return picker;
    }

    // On get default value
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        return DEFAULT_VALUE;
    }

    // On set initial value
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defaultValue)
    {
        if (restorePersistedValue)
        {
            // Restore existing state
            value = getPersistedLong(DEFAULT_VALUE);
        }
        else
        {
            // Set default state from the XML attribute
            value = (Long) defaultValue;
            persistLong(value);
        }
    }

    // On dialog closed
    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        // When the user selects "OK", persist the new value
        if (positiveResult)
        {
            persistLong(value);
        }
    }

    // Get value
    protected long getValue()
    {
        return value;
    }
}
