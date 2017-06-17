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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
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

import org.billthefarmer.markdown.MarkdownView;

// Diary
public class Diary extends Activity
    implements DatePickerDialog.OnDateSetListener

{
    public final static int VERSION_NOUGAT = 24;

    private final static int DATE_DIALOG = 0;
    private final static int BUFFER_SIZE = 1024;

    private final static int DELAY = 500;

    public static final String PREF_ABOUT = "pref_about";
    public static final String PREF_CUSTOM = "pref_custom";
    public static final String PREF_MARKDOWN = "pref_markdown";

    public final static String TAG = "Diary";

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
    
    private boolean custom = true;
    private boolean markdown = true;

    private boolean dirty = true;
    private boolean shown = true;

    private float minScale = 1000;
    private boolean canSwipe = true;

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

        else
        {
            setDate(new GregorianCalendar(
                        (Integer) savedInstanceState.get(YEAR),
                        (Integer) savedInstanceState.get(MONTH),
                        (Integer) savedInstanceState.get(DAY)));

            shown = (Boolean) savedInstanceState.get(SHOWN);
        }

        // Copy help text to today's page if no entries
        if (prevEntry == null && nextEntry == null && textView.length() == 0)
            textView.setText(readAssetFile(HELP));
    }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        custom = preferences.getBoolean(PREF_CUSTOM, true);
        markdown = preferences.getBoolean(PREF_MARKDOWN, true);

        if (markdown && dirty)
        {
            // Get text
            String string = textView.getText().toString();
            markdownView.loadMarkdown(getBaseUrl(), string, getStyles());
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
            currEntry.get(Calendar.YEAR) != today.get(Calendar.YEAR) ||
            currEntry.get(Calendar.MONTH) != today.get(Calendar.MONTH) ||
            currEntry.get(Calendar.DATE) != today.get(Calendar.DATE));
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
        case R.id.prevEntry:
            changeDate(prevEntry);
            return true;
        case R.id.nextEntry:
            changeDate(nextEntry);
            return true;
        case R.id.today:
            today();
            return true;
        case R.id.goToDate:
            goToDate(currEntry);
            return true;
        case R.id.settings:
            settings();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        // Do nothing if cancelled
        if (resultCode != RESULT_OK)
            return;

        // Get date from intent
        Bundle extra = data.getExtras();
        long time = extra.getLong(DATE);
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(time);
        changeDate(calendar);
    }

    // onDateSet
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day)
    {
        changeDate(new GregorianCalendar(year, month, day));
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
                    public void afterTextChanged (Editable s) {}

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
                                               int count)
                    {
                        // Check markdown
                        if (markdown)
                            // Set flag
                            dirty = true;
                    }
                });

        if (markdownView != null)
            markdownView.setWebViewClient(new WebViewClient()
                {
                    // onScaleChanged
                    @Override
                    public void onScaleChanged (WebView view,
                                                float oldScale,
                                                float newScale)
                    {
                        if (minScale > oldScale)
                            minScale = oldScale;
                        canSwipe = (newScale == minScale);
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
                            String string = textView.getText().toString();
                            markdownView.loadMarkdown(getBaseUrl(), string,
                                                      getStyles());
                            // Clear flag
                            dirty = false;
                        }

                        // Animation
                        animateAccept();

                        // Set visibility
                        markdownView.setVisibility(View.VISIBLE);
                        scrollView.setVisibility(View.GONE);
                        accept.setVisibility(View.GONE);
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
                        markdownView.setVisibility(View.GONE);
                        scrollView.setVisibility(View.VISIBLE);
                        accept.setVisibility(View.VISIBLE);
                        edit.setVisibility(View.GONE);

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

    // getBaseUrl
    private String getBaseUrl()
    {
        try
        {
            return getCurrent().toURI().toURL().toString();
        }

        catch (Exception e) {}

        return null;
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
                scrollView.setVisibility(View.GONE);
                accept.setVisibility(View.GONE);
                edit.setVisibility(View.VISIBLE);
            }

            else
            {
                markdownView.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                accept.setVisibility(View.VISIBLE);
                edit.setVisibility(View.GONE);
            }
        }

        else
        {
            markdownView.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
            accept.setVisibility(View.GONE);
            edit.setVisibility(View.GONE);
        }
    }

    // goToDate
    private void goToDate(Calendar date)
    {
        if (custom)
        {
            Intent intent = new Intent(this, DiaryCalendar.class);
            Bundle bundle = new Bundle();
            bundle.putLong(DATE, date.getTimeInMillis());
            List<Calendar> entryList = getEntries();
            long entries[] = new long[entryList.size()];
            int i = 0;
            for (Calendar entry: entryList)
                entries[i++] = entry.getTimeInMillis();
            bundle.putLongArray(ENTRIES, entries);
            intent.putExtras(bundle);
            startActivityForResult(intent, DATE_DIALOG);
        }

        else
            showDatePickerDialog(date);
    }

    // showDatePickerDialog
    public void showDatePickerDialog(Calendar date)
    {
        DialogFragment fragment = DatePickerFragment.newInstance(date);
        fragment.show(getFragmentManager(), DATEPICKER);
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
        return new File(Environment.getExternalStorageDirectory(), DIARY);
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
                return Pattern.matches("^[0-9]{4}$", filename);
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
                return Pattern.matches("^[0-9]{2}$", filename);
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
                return Pattern.matches("^[0-9]{2}.txt$", filename);
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
            String string = textView.getText().toString();
            File file = getFile();
            if (string.length() == 0)
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
                file.getParentFile().mkdirs();
                try
                {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(string);
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
            char buf[] = new char[BUFFER_SIZE];
            int n;
            while ((n = fileReader.read(buf)) != -1)
                text.append(String.valueOf(buf, 0, n));
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

        catch (Exception e)
        {
            Log.d(TAG, "Error while reading file from assets", e);
            return null;
        }
    }

    // load
    private void load()
    {
        String string = read(getFile());
        textView.setText(string);
        if (markdown)
        {
            dirty = false;
            markdownView.loadMarkdown(getBaseUrl(), string, getStyles());
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

    // today
    private void today()
    {
        Calendar calendar = GregorianCalendar.getInstance();
        Calendar today = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                               calendar.get(Calendar.MONTH),
                                               calendar.get(Calendar.DATE));
        changeDate(today);
    }

    // getNextCalendarDay
    private Calendar getNextCalendarDay()
    {
        Calendar nextDay = new GregorianCalendar(currEntry.get(Calendar.YEAR),
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

        if (shown)
            animateSwipeLeft();
    }

    // onSwipeRight
    private void onSwipeRight()
    {
        if (!canSwipe && shown)
            return;

        Calendar prevDay = getPrevCalendarDay();
        changeDate(prevDay);

        if (shown)
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
}
