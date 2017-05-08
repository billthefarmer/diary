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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

// DatePickerFragment
public class DatePickerFragment extends DialogFragment
{

    // newInstance
    public static final DatePickerFragment newInstance(Calendar date)
    {
        DatePickerFragment fragment = new DatePickerFragment();

        Bundle args = new Bundle();
        args.putLong(Diary.DATE, date.getTimeInMillis());
        fragment.setArguments(args);

        return fragment;
    }

    // onCreateDialog
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle bundle = getArguments();
        long time = bundle.getLong(Diary.DATE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        // Create a new instance of DatePickerDialog
        DatePickerDialog dialog =
            new DatePickerDialog(getActivity(),
                                 (DatePickerDialog.OnDateSetListener)
                                 getActivity(),
                                 calendar.get(Calendar.YEAR),
                                 calendar.get(Calendar.MONTH),
                                 calendar.get(Calendar.DATE));


        if (Build.VERSION.SDK_INT < Diary.VERSION_NOUGAT)
        {
            DatePicker picker = dialog.getDatePicker();
            Configuration config = getResources().getConfiguration();
            switch (config.screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK)
            {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                picker.setCalendarViewShown(true);
                picker.setSpinnersShown(false);
                break;

            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                switch (config.orientation)
                {
                case Configuration.ORIENTATION_PORTRAIT:
                    picker.setCalendarViewShown(true);
                    picker.setSpinnersShown(false);
                    break;

                case Configuration.ORIENTATION_LANDSCAPE:
                    picker.setCalendarViewShown(true);
                    break;
                }
                break;

            default:
                picker.setCalendarViewShown(true);
                break;
            }
        }
        return dialog;
    }
}
