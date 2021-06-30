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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

    private final static int REQUEST_READ = 1;
    private final static int REQUEST_WRITE = 2;

    private File file;
    private Uri uri;

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

        if (!darkTheme)
            setTheme(R.style.AppTheme);

        setContentView(R.layout.editor);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        textView = findViewById(R.id.text);

        Intent intent = getIntent();
        uri = intent.getData();

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
                                              int after) {}

                // onTextChanged
                @Override
                public void onTextChanged(CharSequence s,
                                          int start,
                                          int before,
                                          int count) {}
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
                if (changed)
                {
                    CharSequence text = textView.getText();
                    write(text, file);
                }

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);

                // Return result
                setResult(RESULT_OK, null);
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

        if (changed)
            alertDialog(R.string.appName, R.string.changes,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                CharSequence text = textView.getText();
                write(text, file);
                setResult(RESULT_OK, null);
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                changed = false;
                setResult(RESULT_CANCELED, null);
                finish();
                break;
            }
        });

        else
        {
            setResult(RESULT_CANCELED, null);
            finish();
        }
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
        case REQUEST_WRITE:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .WRITE_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                {
                    // Granted, write
                    CharSequence text = textView.getText();
                    write(text, file);
                }
            break;

        case REQUEST_READ:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                {
                    // Granted, read
                    CharSequence text = read(uri);
                    textView.setText(text);
                }
            break;
        }
    }

    // resolveContent
    private Uri resolveContent(Uri uri)
    {
        String path = FileUtils.getPath(this, uri);

        if (path != null)
        {
            File file = new File(path);
            uri = Uri.fromFile(file);
        }

        return uri;
    }

    // alertDialog
    private void alertDialog(int title, int message,
                             int positiveButton, int negativeButton,
                             DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(positiveButton, listener);
        builder.setNegativeButton(negativeButton, listener);

        // Create the AlertDialog
        builder.show();
    }

    // alertDialog
    private void alertDialog(int title, String message,
                             int neutralButton)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the button
        builder.setNeutralButton(neutralButton, null);

        // Create the AlertDialog
        builder.show();
    }

    // read
    private CharSequence read(Uri uri)
    {
        StringBuilder stringBuilder = new StringBuilder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ);

                return stringBuilder;
            }
        }

        try (BufferedReader reader = new
             BufferedReader(new InputStreamReader(getContentResolver()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE);

                return;
            }
        }

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
                alertDialog(R.string.appName, e.getMessage(),
                            android.R.string.ok);

                e.printStackTrace();
            }
        }
    }
}
