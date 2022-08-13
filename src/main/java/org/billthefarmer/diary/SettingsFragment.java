////////////////////////////////////////////////////////////////////////////////
//
//  Diary - Personal diary for Android
//
//  Copyright (C) 2017	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
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

import android.app.ActionBar;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.text.DateFormat;
import java.util.Date;

// SettingsFragment class
@SuppressWarnings("deprecation")
public class SettingsFragment extends android.preference.PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    // On create
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Get folder summary
        EditTextPreference folder =
            (EditTextPreference) findPreference(Settings.PREF_FOLDER);

        // Set folder in text view
        folder.setSummary(preferences.getString(Settings.PREF_FOLDER,
                                                Diary.DIARY));
        // Get index preference
        DatePickerPreference entry =
            (DatePickerPreference) findPreference(Settings.PREF_INDEX_PAGE);

        // Get value
        long value = preferences.getLong(Settings.PREF_INDEX_PAGE,
                                         DatePickerPreference.DEFAULT_VALUE);
        Date date = new Date(value);

        // Set summary
        DateFormat format = DateFormat.getDateInstance();
        String s = format.format(date);
        entry.setSummary(s);

        // Get link template preference
        EditTextPreference link =
            (EditTextPreference) findPreference(Settings.PREF_INDEX_TEMPLATE);

        // Set template in text view
        link.setSummary(preferences.getString(Settings.PREF_INDEX_TEMPLATE,
                                              Diary.INDEX_TEMPLATE));
        // Get template preference
        entry =
            (DatePickerPreference) findPreference(Settings.PREF_TEMPLATE_PAGE);

        // Get value
        value = preferences.getLong(Settings.PREF_TEMPLATE_PAGE,
                                    DatePickerPreference.DEFAULT_VALUE);
        date = new Date(value);

        // Set summary
        s = format.format(date);
        entry.setSummary(s);

        // Get about summary
        Preference about = findPreference(Settings.PREF_ABOUT);
        String sum = about.getSummary().toString();

        // Set version in text view
        s = String.format(sum, BuildConfig.VERSION_NAME);
        about.setSummary(s);
    }

    // on Resume
    @Override
    public void onResume()
    {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this);
    }

    // on Pause
    @Override
    public void onPause()
    {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this);
    }

    // On preference tree click
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference)
    {
        boolean result =
            super.onPreferenceTreeClick(preferenceScreen, preference);

        // Set home as up
        if (preference instanceof PreferenceScreen)
        {
            Dialog dialog = ((PreferenceScreen) preference).getDialog();
            ActionBar actionBar = dialog.getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        return result;
    }

    // On shared preference changed
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key)
    {
        if (key.equals(Settings.PREF_FOLDER))
        {
            // Get folder summary
            EditTextPreference folder =
                (EditTextPreference) findPreference(key);

            // Set folder in text view
            folder.setSummary(preferences.getString(key, Diary.DIARY));
        }

        if (key.equals(Settings.PREF_INDEX_PAGE))
        {
            // Get index preference
            DatePickerPreference entry =
                (DatePickerPreference) findPreference(key);

            // Get value
            long value =
                preferences.getLong(key, DatePickerPreference.DEFAULT_VALUE);
            Date date = new Date(value);

            // Set summary
            DateFormat format = DateFormat.getDateInstance();
            String s = format.format(date);
            entry.setSummary(s);
        }

        if (key.equals(Settings.PREF_INDEX_TEMPLATE))
        {
            // Get template summary
            EditTextPreference link =
                (EditTextPreference) findPreference(key);

            // Set folder in text view
            link.setSummary(preferences.getString(key, Diary.INDEX_TEMPLATE));
        }

        if (key.equals(Settings.PREF_TEMPLATE_PAGE))
        {
            // Get template preference
            DatePickerPreference entry =
                (DatePickerPreference) findPreference(key);

            // Get value
            long value =
                preferences.getLong(key, DatePickerPreference.DEFAULT_VALUE);
            Date date = new Date(value);

            // Set summary
            DateFormat format = DateFormat.getDateInstance();
            String s = format.format(date);
            entry.setSummary(s);
        }

        if (key.equals(Settings.PREF_DARK_THEME))
        {
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
                getActivity().recreate();
        }
    }
}
