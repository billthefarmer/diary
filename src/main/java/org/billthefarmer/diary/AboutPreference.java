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

import android.content.Context;
import android.preference.DialogPreference;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;

// AboutPreference class
public class AboutPreference extends DialogPreference {
    // Constructor
    public AboutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // On bind dialog view
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // Get version text view
        TextView version = view.findViewById(R.id.about);

        // Set version in text view
        if (version != null) {
            String v = (String) version.getText();
            String s = String.format(v, BuildConfig.VERSION_NAME);
            version.setText(s);
        }

        // Get built text view
        TextView built = view.findViewById(R.id.built);

        // Set built date in text view
        if (built != null) {
            String d = (String) built.getText();
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            String s =
                    String.format(d, dateFormat.format(BuildConfig.BUILT));
            built.setText(s);
        }

        // Get copyright text view
        TextView copyright = view.findViewById(R.id.copyright);

        // Set movement method
        copyright.setMovementMethod(LinkMovementMethod.getInstance());

        // Get licence text view
        TextView licence = view.findViewById(R.id.licence);

        // Set movement method
        licence.setMovementMethod(LinkMovementMethod.getInstance());

    }
}
