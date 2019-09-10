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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class Editor extends Activity
{
    public final static String TAG = "Editor";
    public final static String CHANGED = "changed";
    public final static String CONTENT = "content";

    private File file;

    private EditText textView;

    private boolean changed = false;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        boolean darkTheme =
                preferences.getBoolean(Settings.PREF_DARK_THEME, false);

        if (darkTheme)
            setTheme(R.style.AppDarkTheme);

        setContentView(R.layout.editor);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        textView = findViewById(R.id.text);

        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri != null)
        {
            if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                uri = resolveContent(uri);

            String title = uri.getLastPathSegment();
            setTitle(title);

            if (!CONTENT.equalsIgnoreCase(uri.getScheme()))
                file = new File(uri.getPath());

            if (savedInstanceState == null)
            {
                CharSequence text = read(uri);
                textView.setText(text);
            }
        }

        setListeners();
    }

    // setListeners
    private void setListeners()
    {
        ImageButton accept = findViewById(R.id.accept);

        if (textView != null)
        {
            // Text changed
            textView.addTextChangedListener(new TextWatcher()
            {
                // afterTextChanged
                @Override
                public void afterTextChanged(Editable s)
                {
                    changed = true;
                    invalidateOptionsMenu();
                }

                // beforeTextChanged
                @Override
                public void beforeTextChanged(CharSequence s,
                                              int start,
                                              int count,
                                              int after)
                {
                }

                // onTextChanged
                @Override
                public void onTextChanged(CharSequence s,
                                          int start,
                                          int before,
                                          int count)
                {
                }
            });

            // On long click
            textView.setOnLongClickListener(v ->
            {
                // Reveal button
                accept.setVisibility(View.VISIBLE);
                return false;
            });
        }

        if (accept != null)
        {
            // On click
            accept.setOnClickListener(v ->
            {
                CharSequence text = textView.getText();
                if (changed)
                    write(text, file);

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);

                finish();
            });

            // On long click
            accept.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                return true;
            });
        }
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        changed = savedInstanceState.getBoolean(CHANGED);
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHANGED, changed);
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        else
        {
            return super.onOptionsItemSelected(item);
        }
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager)
            getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);

        finish();
    }

    // resolveContent
    private Uri resolveContent(Uri uri)
    {
        String path = FileUtils.getPath(this, uri);

        if (path != null)
        {
            File file = new File(path);
            if (file.canRead())
                uri = Uri.fromFile(file);
        }

        return uri;
    }

    // read
    private CharSequence read(Uri uri)
    {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader =
             new BufferedReader(new InputStreamReader(getContentResolver()
                                                      .openInputStream(uri))))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                stringBuilder.append(line);
                stringBuilder.append(System.getProperty("line.separator"));
            }
        }
        catch (IOException e)
        {
        }

        return stringBuilder;
    }

    // write
    private void write(CharSequence text, File file)
    {
        if (file != null)
        {
            file.getParentFile().mkdirs();
            try (FileWriter fileWriter = new FileWriter(file))
            {
                fileWriter.append(text);
                fileWriter.close();
            }
            catch (IOException e)
            {
            }
        }
    }
}
