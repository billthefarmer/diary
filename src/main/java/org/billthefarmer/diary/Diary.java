//  Diary - Personal diary for Android
//  Copyright © 2012  Josep Portella Florit <hola@josep-portella.com>
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
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.billthefarmer.markdown.MarkdownView;

import org.billthefarmer.view.CustomCalendarDialog;
import org.billthefarmer.view.CustomCalendarView;
import org.billthefarmer.view.DayDecorator;
import org.billthefarmer.view.DayView;

// Diary
public class Diary extends Activity
    implements DatePickerDialog.OnDateSetListener,
               CustomCalendarDialog.OnDateSetListener
{
    public final static int VERSION_NOUGAT = 24;

    private final static int ADD_MEDIA = 1;

    private final static int BUFFER_SIZE = 1024;
    private final static int SCALE_RATIO = 100;

    public final static String PREF_ABOUT = "pref_about";
    public final static String PREF_CUSTOM = "pref_custom";
    public final static String PREF_FOLDER = "pref_folder";
    public final static String PREF_MARKDOWN = "pref_markdown";

    private final static String TAG = "Diary";

    public final static String DIARY = "Diary";
    public final static String STRING = "string";
    public final static String DATE = "date";
    public final static String ENTRIES = "entries";
    public final static String DATEPICKER = "datePicker";

    private final static String YEAR = "year";
    private final static String MONTH = "month";
    private final static String DAY = "day";

    private final static String SHOWN = "shown";

    private final static String HELP = "help.md";
    private final static String STYLES = "file:///android_asset/styles.css";
    private final static String CSS = "css/styles.css";
    private final static String IMAGE_TEMPLATE = "![%s](%s)\n";
    private final static String LINK_TEMPLATE = "[%s](%s)\n";    
    private final static String AUDIO_TEMPLATE =
        "<audio controls>\n  <source src=\"%s\" type=\"%s\" />\n</audio>\n";
    private final static String VIDEO_TEMPLATE =
        "<video controls>\n  <source src=\"%s\" type=\"%s\" />\n</video>\n";
    private final static String FILE = "file:///";
    private final static String PATTERN = "^@ *(\\d{1,2}:\\d{2}) +(.+)$";
    private final static String ANDROID_DATA = "Android/data";
    private final static String HTTP = "http";
    private final static String HTTPS = "https";
    private final static String CONTENT = "content";
    private final static String TEXT_PLAIN = "text/plain";
    private final static String IMAGE_WILD = "image/*";
    private final static String WILD_WILD = "*/*";
    private final static String IMAGE = "image/";
    private final static String AUDIO = "audio/";
    private final static String VIDEO = "video/";

    private boolean custom = true;
    private boolean markdown = true;

    private boolean dirty = true;
    private boolean shown = true;

    private float minScale = 1000;
    private boolean canSwipe = true;
    private boolean haveFile = false;

    private String folder = DIARY;

    private Calendar prevEntry;
    private Calendar currEntry;
    private Calendar nextEntry;

    private EditText textView;
    private ScrollView scrollView;

    private MarkdownView markdownView;

    private GestureDetector gestureDetector;

    private View accept;
    private View edit;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        textView = (EditText) findViewById(R.id.text);
        scrollView = (ScrollView) findViewById(R.id.scroll);
        markdownView = (MarkdownView) findViewById(R.id.markdown);

        accept = findViewById(R.id.accept);
        edit = findViewById(R.id.edit);

        WebSettings settings = markdownView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        setListeners();

        gestureDetector =
            new GestureDetector(this, new GestureListener());

        if (savedInstanceState == null)
            today();

        // Get preferences
        getPreferences();

        // Check for sent files
        fileCheck();
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        setDate(new GregorianCalendar((Integer) savedInstanceState.get(YEAR),
                                      (Integer) savedInstanceState.get(MONTH),
                                      (Integer) savedInstanceState.get(DAY)));

        shown = (Boolean) savedInstanceState.get(SHOWN);
    }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();

        // Get preferences
        getPreferences();
        setDate(currEntry);

        // Copy help text to today's page if no entries
        if (prevEntry == null && nextEntry == null && textView.length() == 0)
            textView.setText(readAssetFile(HELP));

        if (markdown && dirty)
        {
            // Get text
            String text = textView.getText().toString();
            markdownView.loadMarkdown(getBaseUrl(), text, getStyles());
        }

        setVisibility();
    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        if (currEntry != null)
        {
            outState.putInt(YEAR, currEntry.get(Calendar.YEAR));
            outState.putInt(MONTH, currEntry.get(Calendar.MONTH));
            outState.putInt(DAY, currEntry.get(Calendar.DATE));

            outState.putBoolean(SHOWN, shown);
        }
        super.onSaveInstanceState(outState);
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();
        save();
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        Calendar today = GregorianCalendar.getInstance();
        menu.findItem(R.id.today).setEnabled(currEntry == null ||
                                             currEntry.get(Calendar.YEAR) !=
                                             today.get(Calendar.YEAR) ||
                                             currEntry.get(Calendar.MONTH) !=
                                             today.get(Calendar.MONTH) ||
                                             currEntry.get(Calendar.DATE) !=
                                             today.get(Calendar.DATE));
        menu.findItem(R.id.nextEntry).setEnabled(nextEntry != null);
        menu.findItem(R.id.prevEntry).setEnabled(prevEntry != null);
        return true;
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case android.R.id.home:
            onBackPressed();
            break;
        case R.id.prevEntry:
            prevEntry();
            break;
        case R.id.nextEntry:
            nextEntry();
            break;
        case R.id.today:
            today();
            break;
        case R.id.goToDate:
            goToDate(currEntry);
            break;
        case R.id.addImage:
            addImage();
            break;
        case R.id.editStyles:
            editStyles();
            break;
        case R.id.settings:
            settings();
        default:
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        if (markdownView.canGoBack())
        {
            markdownView.goBack();

            if (!markdownView.canGoBack())
            {
                getActionBar().setDisplayHomeAsUpEnabled(false);
                String text = textView.getText().toString();
                markdownView.loadMarkdown(getBaseUrl(), text, getStyles());
            }
       }

        else
            super.onBackPressed();
    }

    // onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        // Do nothing if cancelled
        if (resultCode != RESULT_OK)
            return;

        if (BuildConfig.DEBUG)
        {
            Bundle bundle = data.getExtras();
            if (bundle != null)
            {
                for (String key : bundle.keySet())
                {
                    Object value = bundle.get(key);
                    Log.d(TAG, String.format("%s %s (%s)", key,
                                             value.toString(),
                                             value.getClass().getName()));
                }
            }

            else
                Log.d(TAG, data.toString());
        }

        // if (data.getType() == null)
        //     return;

        switch (requestCode)
        {
        case ADD_MEDIA:
            Uri uri = data.getData();
            // String type = data.getType();

            // if (type.startsWith(IMAGE))
                addImage(uri, false);

            // else if (type.startsWith(AUDIO))
            //     addAudio(uri, type, false);

            // else if (type.startsWith(VIDEO))
            //     addVideo(uri, type, false);
            break;
        }
    }

    // onDateSet
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day)
    {
        changeDate(new GregorianCalendar(year, month, day));

        if (haveFile)
            fileAdd(getIntent());
    }

    // onDateSet
    @Override
    public void onDateSet(CustomCalendarView view, int year,
                          int month, int day)
    {
        changeDate(new GregorianCalendar(year, month, day));

        if (haveFile)
            fileAdd(getIntent());
    }

    // dispatchTouchEvent
    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private void setListeners()
    {
        if (textView != null)
            textView.addTextChangedListener(new TextWatcher()
        {
            // afterTextChanged
            @Override
            public void afterTextChanged (Editable s)
            {
                // Check markdown
                if (markdown)
                    dirty = true;
            }

            // beforeTextChanged
            @Override
            public void beforeTextChanged (CharSequence s,
                                           int start,
                                           int count,
                                           int after) {}
            // onTextChanged
            @Override
            public void onTextChanged (CharSequence s,
                                       int start,
                                       int before,
                                       int count) {}
        });

        if (markdownView != null)
            markdownView.setWebViewClient(new WebViewClient()
        {
            // onPageFinished
            @Override
            public void onPageFinished (WebView view, 
                                        String url)
            {
                if (view.canGoBack() && !url.startsWith(FILE))
                {
                    getActionBar().setDisplayHomeAsUpEnabled(true);
                    dirty = true;
                }

                else
                {
                    getActionBar().setDisplayHomeAsUpEnabled(false);
                    markdownView.clearHistory();
                }
            }

            // onScaleChanged
            @Override
            public void onScaleChanged (WebView view,
                                        float oldScale,
                                        float newScale)
            {
                if (minScale > oldScale)
                    minScale = oldScale;
                canSwipe = (Math.abs(newScale - minScale) <
                            minScale / SCALE_RATIO);
            }
        });

        if (accept != null)
            accept.setOnClickListener(new View.OnClickListener()
        {
            // On click
            @Override
            public void onClick(View v)
            {
                // Check flag
                if (dirty)
                {
                    // Get text
                    String text = textView.getText().toString();
                    markdownView.loadMarkdown(getBaseUrl(), text,
                                              getStyles());
                    // Clear flag
                    dirty = false;
                }

                // Animation
                animateAccept();

                // Set visibility
                markdownView.setVisibility(View.VISIBLE);
                scrollView.setVisibility(View.INVISIBLE);
                accept.setVisibility(View.INVISIBLE);
                edit.setVisibility(View.VISIBLE);

                shown = true;
            }
        });

        if (edit != null)
            edit.setOnClickListener(new View.OnClickListener()
        {
            // On click
            @Override
            public void onClick(View v)
            {

                // Animation
                animateEdit();

                // Set visibility
                markdownView.setVisibility(View.INVISIBLE);
                scrollView.setVisibility(View.VISIBLE);
                accept.setVisibility(View.VISIBLE);
                edit.setVisibility(View.INVISIBLE);

                getActionBar().setDisplayHomeAsUpEnabled(false);
                markdownView.clearHistory();
                shown = false;
            }
        });
    }

    // animateAccept
    public void animateAccept()
    {
        // Animation
        Animation viewClose =
            AnimationUtils.loadAnimation(this,
                                         R.anim.activity_close_exit);
        Animation viewOpen =
            AnimationUtils.loadAnimation(this,
                                         R.anim.activity_open_enter);

        scrollView.startAnimation(viewClose);
        markdownView.startAnimation(viewOpen);

        Animation buttonFlipOut =
            AnimationUtils.loadAnimation(this, R.anim.flip_out);
        Animation buttonFlipIn =
            AnimationUtils.loadAnimation(this, R.anim.flip_in);

        accept.startAnimation(buttonFlipOut);
        edit.startAnimation(buttonFlipIn);
    }

    // animateEdit
    private void animateEdit()
    {
        Animation viewClose =
            AnimationUtils.loadAnimation(this, R.anim.activity_close_exit);
        Animation viewOpen =
            AnimationUtils.loadAnimation(this, R.anim.activity_open_enter);

        markdownView.startAnimation(viewClose);
        scrollView.startAnimation(viewOpen);

        Animation buttonFlipOut =
            AnimationUtils.loadAnimation(this, R.anim.flip_out);
        Animation buttonFlipIn =
            AnimationUtils.loadAnimation(this, R.anim.flip_in);

        edit.startAnimation(buttonFlipOut);
        accept.startAnimation(buttonFlipIn);
    }

    // getPreferences
    private void getPreferences()
    {
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        custom = preferences.getBoolean(PREF_CUSTOM, true);
        markdown = preferences.getBoolean(PREF_MARKDOWN, true);

        folder = preferences.getString(PREF_FOLDER, DIARY);
    }

    // fileCheck
    private void fileCheck()
    {
        // Check for sent files
        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_SEND) ||
            intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
        {
            haveFile = true;
            goToDate(currEntry);
        }
    }

    // eventCheck
    private String eventCheck(String text)
    {
        StringBuilder builder = new StringBuilder(text);
        int index = 1;

        Pattern pattern = Pattern.compile(PATTERN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        // Find matches
        while (matcher.find())
        {
            // Parse time
            Date date = null;
            try
            {
                date = dateFormat.parse(matcher.group(1));
            }

            catch (Exception e)
            {
                continue;
            }

            Calendar time = Calendar.getInstance();
            time.setTime(date);

            Calendar startTime =
                new GregorianCalendar(currEntry.get(Calendar.YEAR),
                                      currEntry.get(Calendar.MONTH),
                                      currEntry.get(Calendar.DATE),
                                      time.get(Calendar.HOUR_OF_DAY),
                                      time.get(Calendar.MINUTE));
            Calendar endTime =
                new GregorianCalendar(currEntry.get(Calendar.YEAR),
                                      currEntry.get(Calendar.MONTH),
                                      currEntry.get(Calendar.DATE),
                                      time.get(Calendar.HOUR_OF_DAY),
                                      time.get(Calendar.MINUTE));
            // Add an hour
            endTime.add(Calendar.HOUR, 1);

            String title = matcher.group(2);

            QueryHandler.insertEvent(this, startTime.getTimeInMillis(),
                                     endTime.getTimeInMillis(), title);
            // Insert ':' char
            builder.insert(matcher.start() + index++, ':');
        }

        return builder.toString();
    }

    // fileAdd
    private void fileAdd(Intent intent)
    {
        if (BuildConfig.DEBUG)
        {
            Bundle bundle = intent.getExtras();
            if (bundle != null)
            {
                for (String key : bundle.keySet())
                {
                    Object value = bundle.get(key);
                    Log.d(TAG, String.format("%s %s (%s)", key,
                                             value.toString(),
                                             value.getClass().getName()));
                }
            }
        }

        if (intent.getType().equals(TEXT_PLAIN))
        {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            Uri uri = Uri.parse(text);
            if ((uri != null) && (uri.getScheme() != null) &&
                (uri.getScheme().equalsIgnoreCase(HTTP) ||
                 uri.getScheme().equalsIgnoreCase(HTTPS)))
                addLink(text, intent.getStringExtra(Intent.EXTRA_TITLE));

            else
                textView.append(text);
        }

        else if (intent.getType().startsWith(IMAGE))
        {
            if (intent.getAction().equals(Intent.ACTION_SEND))
            {
                Uri image =
                    intent.getParcelableExtra(Intent.EXTRA_STREAM);

                // Attempt to get web uri from clip 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    // Get clip
                    ClipData clip = intent.getClipData();

                    // Get the first item from the clipboard data
                    ClipData.Item item = clip.getItemAt(0);

                    // Try to get the item's contents as an uri
                    Uri uri = Uri.parse(item.getText().toString());

                    if ((uri != null) && (uri.getScheme() != null) &&
                        (uri.getScheme().equalsIgnoreCase(HTTP) ||
                         uri.getScheme().equalsIgnoreCase(HTTPS)))
                        image = uri;
                }

                addImage(image, true);
            }

            else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
            {
                try
                {
                    ArrayList<Uri> images =
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    for (Uri image : images)
                        addImage(image, true);
                }

                catch (Exception e) {}
            }
        }

        else if (intent.getType().startsWith(AUDIO))
        {
                Uri audio =
                    intent.getParcelableExtra(Intent.EXTRA_STREAM);
                String type = intent.getType();

                addAudio(audio, type, true);
        }

        else if (intent.getType().startsWith(VIDEO))
        {
                Uri video =
                    intent.getParcelableExtra(Intent.EXTRA_STREAM);
                String type = intent.getType();

                addVideo(video, type, true);
        }

        haveFile = false;
        intent.setAction(Intent.ACTION_DEFAULT);
    }

    // getBaseUrl
    private String getBaseUrl()
    {
        StringBuilder url = new
            StringBuilder(Uri.fromFile(getCurrent()).toString());
        return url.append(File.separator).toString();
    }

    // getCurrent
    private File getCurrent()
    {
        return getMonth(currEntry.get(Calendar.YEAR),
                        currEntry.get(Calendar.MONTH));
    }

    // getStyles
    private String getStyles()
    {
        File cssFile = new File(getHome(), CSS);

        if (cssFile.exists())
        {
            try
            {
                return cssFile.toURI().toURL().toString();
            }

            catch (Exception e) {}
        }

        return STYLES;
    }

    // setVisibility
    private void setVisibility()
    {
        if (markdown)
        {
            // Check if shown
            if (shown)
            {
                markdownView.setVisibility(View.VISIBLE);
                scrollView.setVisibility(View.INVISIBLE);
                accept.setVisibility(View.INVISIBLE);
                edit.setVisibility(View.VISIBLE);
            }

            else
            {
                markdownView.setVisibility(View.INVISIBLE);
                scrollView.setVisibility(View.VISIBLE);
                accept.setVisibility(View.VISIBLE);
                edit.setVisibility(View.INVISIBLE);
            }
        }

        else
        {
            markdownView.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.VISIBLE);
            accept.setVisibility(View.INVISIBLE);
            edit.setVisibility(View.INVISIBLE);
        }
    }

    // goToDate
    private void goToDate(Calendar date)
    {
        if (custom)
            showCustomCalendarDialog(date);

        else
            showDatePickerDialog(date);
    }

    // showCustomCalendarDialog
    private void showCustomCalendarDialog(Calendar date)
    {
        CustomCalendarDialog dialog = new
            CustomCalendarDialog(this, this,
                                 date.get(Calendar.YEAR),
                                 date.get(Calendar.MONTH),
                                 date.get(Calendar.DATE));

        List<DayDecorator> decorators = new ArrayList<DayDecorator>();
        decorators.add(new EntryDecorator(getEntries()));
        CustomCalendarView calendarView = dialog.getCalendarView();
        calendarView.setDecorators(decorators);
        dialog.show();
    }

    // showDatePickerDialog
    private void showDatePickerDialog(Calendar date)
    {
        DatePickerDialog dialog = new
            DatePickerDialog(this, this,
                             date.get(Calendar.YEAR),
                             date.get(Calendar.MONTH),
                             date.get(Calendar.DATE));
        dialog.show();
    }

    // alertDialog
    private AlertDialog alertDialog(int title, int message,
                                    DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);

        // Create the AlertDialog
        return builder.create();
    }

    // addImage
    public void addImage()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(WILD_WILD);
        startActivityForResult(Intent.createChooser(intent, null), ADD_MEDIA);
    }

    // editStyles
    public void editStyles()
    {
        File file = new File(getHome(), CSS);
        Uri uri = Uri.fromFile(file);

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, "text/css");
        startActivity(intent);
    }

    // settings
    private void settings()
    {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    // getHome
    private File getHome()
    {
        return new File(Environment.getExternalStorageDirectory(), folder);
    }

    // getYear
    private File getYear(int year)
    {
        return new File(getHome(), String.format(Locale.getDefault(),
                        "%04d", year));
    }

    // getMonth
    private File getMonth(int year, int month)
    {
        return new File(getYear(year), String.format(Locale.getDefault(),
                        "%02d", month + 1));
    }

    // getDay
    private File getDay(int year, int month, int day)
    {
        return new
               File(getMonth(year, month), String.format(Locale.getDefault(),
                       "%02d.txt", day));
    }

    // getFile
    private File getFile()
    {
        return getDay(currEntry.get(Calendar.YEAR),
                      currEntry.get(Calendar.MONTH),
                      currEntry.get(Calendar.DATE));
    }

    // sortFiles
    private static File[] sortFiles(File[] files)
    {
        if (files == null)
            return new File[0];
        Arrays.sort(files, new Comparator<File> ()
        {
            // compare
            @Override
            public int compare(File file1, File file2)
            {
                return file2.getName().compareTo(file1.getName());
            }
        });
        return files;
    }

    // listYears
    private static File[] listYears(File home)
    {
        return sortFiles(home.listFiles(new FilenameFilter()
        {
            // accept
            @Override
            public boolean accept(File dir, String filename)
            {
                return filename.matches("^[0-9]{4}$");
            }
        }));
    }

    // listMonths
    private static File[] listMonths(File yearDir)
    {
        return sortFiles(yearDir.listFiles(new FilenameFilter()
        {
            // accept
            @Override
            public boolean accept(File dir, String filename)
            {
                return filename.matches("^[0-9]{2}$");
            }
        }));
    }

    // listDays
    private static File[] listDays(File monthDir)
    {
        return sortFiles(monthDir.listFiles(new FilenameFilter()
        {
            // accept
            @Override
            public boolean accept(File dir, String filename)
            {
                return filename.matches("^[0-9]{2}.txt$");
            }
        }));
    }

    // yearValue
    private static int yearValue(File yearDir)
    {
        return Integer.parseInt(yearDir.getName());
    }

    // monthValue
    private static int monthValue(File monthDir)
    {
        return Integer.parseInt(monthDir.getName()) - 1;
    }

    // dayValue
    private static int dayValue(File dayFile)
    {
        return Integer.parseInt(dayFile.getName().split("\\.")[0]);
    }

    // prevYear
    private int prevYear(int year)
    {
        int prev = -1;
        for (File yearDir : listYears(getHome()))
        {
            int n = yearValue(yearDir);
            if (n < year && n > prev)
                prev = n;
        }
        return prev;
    }

    // prevMonth
    private int prevMonth(int year, int month)
    {
        int prev = -1;
        for (File monthDir : listMonths(getYear(year)))
        {
            int n = monthValue(monthDir);
            if (n < month && n > prev)
                prev = n;
        }
        return prev;
    }

    // prevDay
    private int prevDay(int year, int month, int day)
    {
        int prev = -1;
        for (File dayFile : listDays(getMonth(year, month)))
        {
            int n = dayValue(dayFile);
            if (n < day && n > prev)
                prev = n;
        }
        return prev;
    }

    // getPrevEntry
    private Calendar getPrevEntry(int year, int month, int day)
    {
        int prev;
        if ((prev = prevDay(year, month, day)) == -1)
        {
            if ((prev = prevMonth(year, month)) == -1)
            {
                if ((prev = prevYear(year)) == -1)
                    return null;
                return getPrevEntry(prev, Calendar.DECEMBER, 32);
            }
            return getPrevEntry(year, prev, 32);
        }
        return new GregorianCalendar(year, month, prev);
    }

    // nextYear
    private int nextYear(int year)
    {
        int next = -1;
        for (File yearDir : listYears(getHome()))
        {
            int n = yearValue(yearDir);
            if (n > year && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // nextMonth
    private int nextMonth(int year, int month)
    {
        int next = -1;
        for (File monthDir : listMonths(getYear(year)))
        {
            int n = monthValue(monthDir);
            if (n > month && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // nextDay
    private int nextDay(int year, int month, int day)
    {
        int next = -1;
        for (File dayFile : listDays(getMonth(year, month)))
        {
            int n = dayValue(dayFile);
            if (n > day && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // getNextEntry
    private Calendar getNextEntry(int year, int month, int day)
    {
        int next;
        if ((next = nextDay(year, month, day)) == -1)
        {
            if ((next = nextMonth(year, month)) == -1)
            {
                if ((next = nextYear(year)) == -1)
                    return null;
                return getNextEntry(next, Calendar.JANUARY, -1);
            }
            return getNextEntry(year, next, -1);
        }
        return new GregorianCalendar(year, month, next);
    }

    // getEntries
    private List<Calendar> getEntries()
    {
        List<Calendar> list = new ArrayList<Calendar>();
        Calendar entry = getNextEntry(1970, Calendar.JANUARY, 1);
        while (entry != null)
        {
            list.add(entry);
            entry = getNextEntry(entry.get(Calendar.YEAR),
                                 entry.get(Calendar.MONTH),
                                 entry.get(Calendar.DATE));
        }

        return list;
    }

    // save
    private void save()
    {
        if (currEntry != null)
        {
            String text = textView.getText().toString();
            File file = getFile();
            if (text.length() == 0)
            {
                if (file.exists())
                    file.delete();
                File parent = file.getParentFile();
                if (parent.exists() && parent.list().length == 0)
                {
                    parent.delete();
                    File grandParent = parent.getParentFile();
                    if (grandParent.exists()
                        && grandParent.list().length == 0)
                        grandParent.delete();
                }
            }

            else
            {
                // Check for events
                text = eventCheck(text);

                file.getParentFile().mkdirs();
                try
                {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(text);
                    fileWriter.close();
                }
                catch (Exception e) {}
            }
        }
    }

    // read
    private static String read(File file)
    {
        StringBuilder text = new StringBuilder();
        try
        {
            FileReader fileReader = new FileReader(file);
            char buffer[] = new char[BUFFER_SIZE];
            int n;
            while ((n = fileReader.read(buffer)) != -1)
                text.append(String.valueOf(buffer, 0, n));
            fileReader.close();
        }

        catch (Exception e) {}

        return text.toString();
    }

    // readAssetFile
    private String readAssetFile(String file)
    {
        try
        {
            // Open help file
            InputStream input = getResources().getAssets().open(file);
            try
            {
                BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(input));
                StringBuilder content =
                    new StringBuilder(input.available());
                String line;
                while ((line = bufferedReader.readLine()) != null)
                {
                    content.append(line);
                    content.append(System.getProperty("line.separator"));
                }

                return content.toString();
            }

            finally
            {
                input.close();
            }
        }

        catch (Exception e) {}

        return null;
    }

    // load
    private void load()
    {
        String text = read(getFile());
        textView.setText(text);
        if (markdown)
        {
            dirty = false;
            markdownView.loadMarkdown(getBaseUrl(), text, getStyles());
        }
        textView.setSelection(0);
    }

    // setDate
    private void setDate(Calendar date)
    {
        setTitleDate(new Date(date.getTimeInMillis()));

        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DATE);

        Calendar calendar = GregorianCalendar.getInstance();
        Calendar today = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                               calendar.get(Calendar.MONTH),
                                               calendar.get(Calendar.DATE));

        prevEntry = getPrevEntry(year, month, day);
        if ((prevEntry == null || today.compareTo(prevEntry) > 0) &&
                today.compareTo(date) < 0)
            prevEntry = today;
        currEntry = date;
        nextEntry = getNextEntry(year, month, day);
        if ((nextEntry == null || today.compareTo(nextEntry) < 0) &&
                today.compareTo(date) > 0)
            nextEntry = today;

        invalidateOptionsMenu();
    }

    // setTitleDate
    private void setTitleDate(Date date)
    {
        Configuration config = getResources().getConfiguration();
        switch (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
        {
        case Configuration.SCREENLAYOUT_SIZE_SMALL:
            setTitle(DateFormat.getDateInstance(DateFormat.MEDIUM)
                     .format(date));
            break;

        case Configuration.SCREENLAYOUT_SIZE_NORMAL:
            switch (config.orientation)
            {
            case Configuration.ORIENTATION_PORTRAIT:
                setTitle(DateFormat.getDateInstance(DateFormat.MEDIUM)
                         .format(date));
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                setTitle(DateFormat.getDateInstance(DateFormat.FULL)
                         .format(date));
                break;
            }
            break;

        default:
            setTitle(DateFormat.getDateInstance(DateFormat.FULL)
                     .format(date));
            break;
        }
    }

    // changeDate
    private void changeDate(Calendar date)
    {
        save();
        setDate(date);
        load();
    }

    // prevEntry
    private void prevEntry()
    {
        changeDate(prevEntry);
    }

    // nextEntry
    private void nextEntry()
    {
        changeDate(nextEntry);
    }

    // today
    private void today()
    {
        Calendar calendar = GregorianCalendar.getInstance();
        Calendar today = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                               calendar.get(Calendar.MONTH),
                                               calendar.get(Calendar.DATE));
        changeDate(today);
    }

    // addImage
    private  void addImage(Uri image, boolean append)
    {
        if (image.getScheme().equalsIgnoreCase(CONTENT))
            image = resolveContent(image);

        String imageText = String.format(IMAGE_TEMPLATE,
                                         image.getLastPathSegment(),
                                         image.toString());
        if (append)
            textView.append(imageText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, imageText);
        }

        String text = textView.getText().toString();
        markdownView.loadMarkdown(getBaseUrl(), text, getStyles());
    }

    // addLink
    private void addLink(String url, String title)
    {
        if (title == null)
            title = url;

        String linkText = String.format(LINK_TEMPLATE, title, url);
        textView.append(linkText);

        String text = textView.getText().toString();
        markdownView.loadMarkdown(getBaseUrl(), text, getStyles());
    }

    // addAudio
    private void addAudio(Uri audio, String type, boolean append)
    {
        if (audio.getScheme().equalsIgnoreCase(CONTENT))
            audio = resolveContent(audio);

        String audioText = String.format(AUDIO_TEMPLATE,
                                         audio.toString(), type);
        if (append)
            textView.append(audioText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, audioText);
        }

        String text = textView.getText().toString();
        markdownView.loadMarkdown(getBaseUrl(), text, getStyles());
    }

    // addVideo
    private void addVideo(Uri video, String type, boolean append)
    {
        if (video.getScheme().equalsIgnoreCase(CONTENT))
            video = resolveContent(video);

        String videoText = String.format(VIDEO_TEMPLATE,
                                         video.toString(), type);
        if (append)
            textView.append(videoText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, videoText);
        }

        String text = textView.getText().toString();
        markdownView.loadMarkdown(getBaseUrl(), text, getStyles());
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

    // getNextCalendarDay
    private Calendar getNextCalendarDay()
    {
        Calendar nextDay =
            new GregorianCalendar(currEntry.get(Calendar.YEAR),
                                  currEntry.get(Calendar.MONTH),
                                  currEntry.get(Calendar.DATE));
        nextDay.add(Calendar.DATE, 1);
        return nextDay;
    }

    // getPrevCalendarDay
    private Calendar getPrevCalendarDay()
    {
        Calendar prevDay =
            new GregorianCalendar(currEntry.get(Calendar.YEAR),
                                  currEntry.get(Calendar.MONTH),
                                  currEntry.get(Calendar.DATE));

        prevDay.add(Calendar.DATE, -1);
        return prevDay;
    }

    // animateSwipeLeft
    private void animateSwipeLeft()
    {
        Animation viewSwipeIn =
            AnimationUtils.loadAnimation(this, R.anim.swipe_left_in);

        markdownView.startAnimation(viewSwipeIn);
    }

    // animateSwipeRight
    private void animateSwipeRight()
    {
        Animation viewSwipeIn =
            AnimationUtils.loadAnimation(this, R.anim.swipe_right_in);

        markdownView.startAnimation(viewSwipeIn);
    }

    // onSwipeLeft
    private void onSwipeLeft()
    {
        if (!canSwipe && shown)
            return;

        Calendar nextDay = getNextCalendarDay();
        changeDate(nextDay);

        if (markdown && shown)
            animateSwipeLeft();
    }

    // onSwipeRight
    private void onSwipeRight()
    {
        if (!canSwipe && shown)
            return;

        Calendar prevDay = getPrevCalendarDay();
        changeDate(prevDay);

        if (markdown && shown)
            animateSwipeRight();
    }

    // GestureListener
    private class GestureListener
        extends GestureDetector.SimpleOnGestureListener
    {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        // onDown
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        // onFling
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY)
        {
            boolean result = false;

            try
            {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY))
                {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                            Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD)
                    {
                        if (diffX > 0)
                        {
                            onSwipeRight();
                        }

                        else
                        {
                            onSwipeLeft();
                        }
                    }

                    result = true;
                }
            }

            catch (Exception e) {}

            return result;
        }
    }

    // EntryDecorator
    private class EntryDecorator
        implements DayDecorator
    {
        private List<Calendar> entries;

        private EntryDecorator(List<Calendar> entries)
        {
            this.entries = entries;
        }

        // decorate
        @Override
        public void decorate(DayView dayView)
        {
            Calendar cellDate = dayView.getDate();
            for (Calendar entry : entries)
                if (cellDate.get(Calendar.DATE) == entry.get(Calendar.DATE) &&
                    cellDate.get(Calendar.MONTH) == entry.get(Calendar.MONTH) &&
                    cellDate.get(Calendar.YEAR) == entry.get(Calendar.YEAR))
                    dayView.setBackgroundResource(R.drawable.diary_entry);
        }
    }
}
