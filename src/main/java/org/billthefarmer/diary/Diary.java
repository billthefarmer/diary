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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;

import org.billthefarmer.markdown.MarkdownView;
import org.billthefarmer.view.CustomCalendarDialog;
import org.billthefarmer.view.CustomCalendarView;
import org.billthefarmer.view.DayDecorator;
import org.billthefarmer.view.DayView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Diary
public class Diary extends Activity
    implements DatePickerDialog.OnDateSetListener,
    CustomCalendarDialog.OnDateSetListener
{
    private final static int ADD_MEDIA = 1;

    private final static int BUFFER_SIZE = 1024;
    private final static int SCALE_RATIO = 128;
    private final static int FIND_DELAY = 256;
    private final static int VERSION_M = 23;

    private final static String TAG = "Diary";

    public final static String PREF_ABOUT = "pref_about";
    public final static String PREF_CUSTOM = "pref_custom";
    public final static String PREF_FOLDER = "pref_folder";
    public final static String PREF_EXTERNAL = "pref_external";
    public final static String PREF_MARKDOWN = "pref_markdown";
    public final static String PREF_USE_INDEX = "pref_use_index";
    public final static String PREF_INDEX_PAGE = "pref_index_page";
    public final static String PREF_COPY_MEDIA = "pref_copy_media";
    public final static String PREF_DARK_THEME = "pref_dark_theme";

    public final static String DIARY = "Diary";

    private final static String YEAR = "year";
    private final static String MONTH = "month";
    private final static String DAY = "day";

    private final static String SAVED = "saved";
    private final static String SHOWN = "shown";
    private final static String ENTRY = "entry";

    private final static String HELP = "help.md";
    private final static String STYLES = "file:///android_asset/styles.css";
    private final static String SCRIPT = "file:///android_asset/script.js";
    private final static String CSS_STYLES = "css/styles.css";
    private final static String JS_SCRIPT = "js/script.js";
    private final static String MEDIA_PATTERN = "!\\[(.*)\\]\\((.+)\\)";
    private final static String MEDIA_TEMPLATE = "![%s](%s)\n";
    private final static String LINK_TEMPLATE = "[%s](%s)\n";
    private final static String AUDIO_TEMPLATE =
        "<audio controls src=\"%s\"></audio>\n";
    private final static String VIDEO_TEMPLATE =
        "<video controls src=\"%s\"></video>\n";
    private final static String EVENT_PATTERN = "^@ *(\\d{1,2}:\\d{2}) +(.+)$";
    private final static String EVENT_TEMPLATE = "@:$1 $2";
    private final static String MAP_PATTERN =
        "\\[(?:osm:)?(-?\\d+[,.]\\d+)[,;] ?(-?\\d+[,.]\\d+)\\]";
    private final static String MAP_TEMPLATE =
        "<iframe width=\"560\" height=\"420\" " +
        "src=\"http://www.openstreetmap.org/export/embed.html?" +
        "bbox=%f,%f,%f,%f&amp;layer=mapnik\">" +
        "</iframe><br/><small>" +
        "<a href=\"http://www.openstreetmap.org/#map=16/%f/%f\">" +
        "View Larger Map</a></small>\n";
    private final static String GEO_PATTERN =
        "geo:(-?\\d+[.]\\d+), ?(-?\\d+[.]\\d+).*";
    private final static String GEO_TEMPLATE =
        "![osm](geo:%f,%f)";
    private final static String DATE_PATTERN =
        "\\[(.+)\\]\\(date:(\\d+.\\d+.\\d+)\\)";
    public final static String PATTERN_CHARS =
        "[\\(\\)\\[\\]\\{\\}\\<\\>\"'`]";
    public final static String BRACKET_CHARS = "([{<";
    private final static String GEO = "geo";
    private final static String OSM = "osm";
    private final static String HTTP = "http";
    private final static String HTTPS = "https";
    private final static String CONTENT = "content";
    private final static String TEXT_PLAIN = "text/plain";
    private final static String WILD_WILD = "*/*";
    private final static String IMAGE = "image";
    private final static String AUDIO = "audio";
    private final static String VIDEO = "video";

    private boolean custom = true;
    private boolean markdown = true;
    private boolean external = false;
    private boolean useIndex = false;
    private boolean copyMedia = false;
    private boolean darkTheme = false;

    private boolean changed = false;
    private boolean shown = true;

    private boolean multi = false;
    private boolean entry = false;

    private long saved = 0;

    private float minScale = 1000;
    private boolean canSwipe = true;
    private boolean haveMedia = false;

    private String folder = DIARY;

    private Calendar prevEntry;
    private Calendar currEntry;
    private Calendar nextEntry;
    private Calendar indexPage;

    private EditText textView;
    private ScrollView scrollView;

    private MarkdownView markdownView;

    private SearchView searchView;
    private MenuItem searchItem;

    private GestureDetector gestureDetector;

    private Deque<Calendar> entryStack;

    private View accept;
    private View edit;

    // sortFiles
    private static File[] sortFiles(File[] files)
    {
        if (files == null)
            return new File[0];
        // compare
        Arrays.sort(files, (file1, file2) ->
                    file2.getName().compareTo(file1.getName()));
        return files;
    }

    // listYears
    private static File[] listYears(File home)
    {
        // accept
        return sortFiles(home.listFiles((dir, filename) ->
                                        filename.matches("^[0-9]{4}$")));
    }

    // listMonths
    private static File[] listMonths(File yearDir)
    {
        // accept
        return sortFiles(yearDir.listFiles((dir, filename) ->
                                           filename.matches("^[0-9]{2}$")));
    }

    // listDays
    private static File[] listDays(File monthDir)
    {
        // accept
        return sortFiles(monthDir.listFiles((dir, filename) ->
                                            filename.matches("^[0-9]{2}.txt$")));
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

    // read
    private static String read(File file)
    {
        StringBuilder text = new StringBuilder();
        try
        {
            try (FileReader fileReader = new FileReader(file))
            {
                BufferedReader reader =
                    new BufferedReader(fileReader);

                String line;
                while ((line = reader.readLine()) != null)
                {
                    text.append(line);
                    text.append(System.getProperty("line.separator"));
                }

                return text.toString();
            }
        }
        catch (Exception e)
        {
        }

        return null;
    }

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        getPreferences();

        if (darkTheme)
            setTheme(R.style.AppDarkTheme);

        setContentView(R.layout.main);

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.scroll);
        markdownView = findViewById(R.id.markdown);

        accept = findViewById(R.id.accept);
        edit = findViewById(R.id.edit);

        WebSettings settings = markdownView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        setListeners();

        gestureDetector =
            new GestureDetector(this, new GestureListener());

        entryStack = new ArrayDeque<>();

        // Check startup
        if (savedInstanceState == null)
        {
            Intent intent = getIntent();

            // Check index and start from launcher
            if (useIndex && intent.getAction().equals(Intent.ACTION_MAIN))
                index();

            // Set the date
            else
                today();

            // Check for sent media
            mediaCheck(intent);
        }
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        markdownView.restoreState(savedInstanceState);

        setDate(new GregorianCalendar(savedInstanceState.getInt(YEAR),
                                      savedInstanceState.getInt(MONTH),
                                      savedInstanceState.getInt(DAY)));

        shown = savedInstanceState.getBoolean(SHOWN);
        entry = savedInstanceState.getBoolean(ENTRY);
        saved = savedInstanceState.getLong(SAVED);
    }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();

        boolean dark = darkTheme;

        // Get preferences
        getPreferences();

        // Recreate
        if (dark != darkTheme && Build.VERSION.SDK_INT != VERSION_M)
            recreate();

        // Set date
        setDate(currEntry);

        // Reload if modified
        if (getFile().lastModified() > saved)
            load();

        // Clear cache
        markdownView.clearCache(true);

        // Copy help text to today's page if no entries
        if (prevEntry == null && nextEntry == null && textView.length() == 0)
            textView.setText(readAssetFile(HELP));

        if (markdown && changed)
            loadMarkdown();

        setVisibility();
    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        markdownView.saveState(outState);

        if (currEntry != null)
        {
            outState.putInt(YEAR, currEntry.get(Calendar.YEAR));
            outState.putInt(MONTH, currEntry.get(Calendar.MONTH));
            outState.putInt(DAY, currEntry.get(Calendar.DATE));

            outState.putBoolean(SHOWN, shown);
            outState.putBoolean(ENTRY, entry);
            outState.putLong(SAVED, saved);
        }
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();

        if (changed)
            save();

        saved = getFile().lastModified();
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        searchItem = menu.findItem(R.id.search);

        // Set up search view and action expand listener
        if (searchItem != null)
        {
            searchView = (SearchView) searchItem.getActionView();
            searchItem.setOnActionExpandListener(new MenuItem
                                                 .OnActionExpandListener()
            {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item)
                {
                    invalidateOptionsMenu();
                    return true;
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item)
                {
                    return true;
                }
            });
        }

        // Set up search view options and listener
        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener());
        }

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
        menu.findItem(R.id.index).setVisible(useIndex);

        // Show find all item
        if (menu.findItem(R.id.search).isActionViewExpanded())
            menu.findItem(R.id.findAll).setVisible(true);
        else
            menu.findItem(R.id.findAll).setVisible(false);

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
        case R.id.index:
            index();
            break;
        case R.id.findAll:
            findAll();
            break;
        case R.id.addTime:
            addTime();
            break;
        case R.id.addMedia:
            addMedia();
            break;
        case R.id.editStyles:
            editStyles();
            break;
        case R.id.editScript:
            editScript();
            break;
        case R.id.settings:
            settings();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }

        // Close text search
        if (searchItem.isActionViewExpanded() &&
                item.getItemId() != R.id.findAll)
            searchItem.collapseActionView();

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        // Calendar entry
        if (entry)
        {
            if (!entryStack.isEmpty())
                changeDate(entryStack.pop());

            else
                super.onBackPressed();
        }

        // External
        else
        {
            if (markdownView.canGoBack())
            {
                markdownView.goBack();

                if (!markdownView.canGoBack())
                    changeDate(currEntry);
            }

            else
                super.onBackPressed();
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

        switch (requestCode)
        {
        case ADD_MEDIA:
            // Get uri
            Uri uri = data.getData();

            // Resolve content uri
            if (uri.getScheme().equalsIgnoreCase(CONTENT))
                uri = resolveContent(uri);

            if (uri != null)
            {
                // Get type
                String type = FileUtils.getMimeType(this, uri);

                if (type == null)
                    addLink(uri, uri.getLastPathSegment(), false);

                else if (type.startsWith(IMAGE) ||
                         type.startsWith(AUDIO) ||
                         type.startsWith(VIDEO))
                    addMedia(uri, false);

                else
                    addLink(uri, uri.getLastPathSegment(), false);
            }
            break;
        }
    }

    // onDateSet
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day)
    {
        entryStack.push(currEntry);
        changeDate(new GregorianCalendar(year, month, day));

        if (haveMedia)
            addMedia(getIntent());
    }

    // onDateSet
    @Override
    public void onDateSet(CustomCalendarView view, int year, int month, int day)
    {
        entryStack.push(currEntry);
        changeDate(new GregorianCalendar(year, month, day));

        if (haveMedia)
            addMedia(getIntent());
    }

    // dispatchTouchEvent
    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (event.getPointerCount() > 1)
            multi = true;

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
            public void afterTextChanged(Editable s)
            {
                // Text changed
                changed = true;
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

        if (markdownView != null)
        {
            markdownView.setWebViewClient(new WebViewClient()
            {
                // onPageFinished
                @Override
                public void onPageFinished(WebView view, String url)
                {
                    // Check if entry
                    if (entry)
                    {
                        if (entryStack.isEmpty())
                            getActionBar().setDisplayHomeAsUpEnabled(false);

                        else
                            getActionBar().setDisplayHomeAsUpEnabled(true);

                        setTitleDate(currEntry.getTime());
                        view.clearHistory();
                    }
                    else
                    {
                        if (view.canGoBack())
                        {
                            getActionBar().setDisplayHomeAsUpEnabled(true);

                            // Get page title
                            if (view.getTitle() != null)
                                setTitle(view.getTitle());
                        }
                        else
                        {
                            getActionBar().setDisplayHomeAsUpEnabled(false);
                            setTitleDate(currEntry.getTime());
                        }
                    }
                }

                // onScaleChanged
                @Override
                public void onScaleChanged(WebView view,
                                           float oldScale,
                                           float newScale)
                {
                    if (minScale > oldScale)
                        minScale = oldScale;
                    canSwipe = (Math.abs(newScale - minScale) <
                                minScale / SCALE_RATIO);
                }

                // shouldOverrideUrlLoading
                @Override
                @SuppressWarnings("deprecation")
                public boolean shouldOverrideUrlLoading(WebView view,
                                                        String url)
                {
                    Calendar calendar = diaryEntry(url);
                    // Diary entry
                    if (calendar != null)
                    {
                        entryStack.push(currEntry);
                        changeDate(calendar);
                        return true;
                    }

                    // Use external browser
                    if (external)
                    {
                        Uri uri = Uri.parse(url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        if (intent.resolveActivity(getPackageManager()) != null)
                            startActivity(intent);
                        return true;
                    }

                    entry = false;
                    return false;
                }
            });

            // On long click
            markdownView.setOnLongClickListener(v ->
            {
                // Reveal button
                edit.setVisibility(View.VISIBLE);
                return false;
            });
        }

        if (accept != null)
        {
            // On click
            accept.setOnClickListener(v ->
            {
                // Check flag
                if (changed)
                {
                    // Save text
                    save();
                    // Get text
                    loadMarkdown();
                    // Clear flag
                    changed = false;
                    // Set flag
                    entry = true;
                }

                // Animation
                animateAccept();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                shown = true;
            });

            // On long click
            accept.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                return true;
            });
        }

        if (edit != null)
        {
            // On click
            edit.setOnClickListener(v ->
            {
                // Animation
                animateEdit();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                shown = false;
            });

            // On long click
            edit.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                return true;
            });
        }

        if (textView != null)
        {
            // onFocusChange
            textView.setOnFocusChangeListener((v, hasFocus) ->
            {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
                if (!hasFocus)
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            });

            // On long click
            textView.setOnLongClickListener(v ->
            {
                // Reveal button
                accept.setVisibility(View.VISIBLE);
                return false;
            });
        }
    }

    // animateAccept
    public void animateAccept()
    {
        // Animation
        startAnimation(scrollView, R.anim.activity_close_exit, View.INVISIBLE);
        startAnimation(markdownView, R.anim.activity_open_enter, View.VISIBLE);

        startAnimation(accept, R.anim.flip_out, View.INVISIBLE);
        startAnimation(edit, R.anim.flip_in, View.VISIBLE);
    }

    // animateEdit
    private void animateEdit()
    {
        // Animation
        startAnimation(markdownView, R.anim.activity_close_exit, View.INVISIBLE);
        startAnimation(scrollView, R.anim.activity_open_enter, View.VISIBLE);

        startAnimation(edit, R.anim.flip_out, View.INVISIBLE);
        startAnimation(accept, R.anim.flip_in, View.VISIBLE);
    }

    // startAnimation
    private void startAnimation(View view, int anim, int visibility)
    {
        Animation animation = AnimationUtils.loadAnimation(this, anim);
        view.startAnimation(animation);
        view.setVisibility(visibility);
    }

    // getPreferences
    private void getPreferences()
    {
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        custom = preferences.getBoolean(PREF_CUSTOM, true);
        markdown = preferences.getBoolean(PREF_MARKDOWN, true);
        external = preferences.getBoolean(PREF_EXTERNAL, false);
        useIndex = preferences.getBoolean(PREF_USE_INDEX, false);
        copyMedia = preferences.getBoolean(PREF_COPY_MEDIA, false);
        darkTheme = preferences.getBoolean(PREF_DARK_THEME, false);

        // Index page
        long value = preferences.getLong(PREF_INDEX_PAGE,
                                         DatePickerPreference.DEFAULT_VALUE);
        indexPage = Calendar.getInstance();
        indexPage.setTimeInMillis(value);

        // Folder
        folder = preferences.getString(PREF_FOLDER, DIARY);
    }

    // mediaCheck
    private void mediaCheck(Intent intent)
    {
        // Check for sent media
        if (intent.getAction().equals(Intent.ACTION_SEND) ||
                intent.getAction().equals(Intent.ACTION_VIEW) ||
                intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
        {
            haveMedia = true;
            goToDate(currEntry);
        }
    }

    // eventCheck
    private String eventCheck(String text)
    {
        Pattern pattern = Pattern.compile(EVENT_PATTERN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        // Find matches
        while (matcher.find())
        {
            // Parse time
            Date date;
            try
            {
                date = dateFormat.parse(matcher.group(1));
            }

            // Ignore errors
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
        }

        return matcher.replaceAll(EVENT_TEMPLATE);
    }

    // loadMarkdown
    private void loadMarkdown()
    {
        String text = textView.getText().toString();
        loadMarkdown(text);
    }

    // loadMarkdown
    private void loadMarkdown(String text)
    {
        markdownView.loadMarkdown(getBaseUrl(), markdownCheck(text),
                                  getStyles(), getScript());
    }

    // markdownCheck
    private String markdownCheck(String text)
    {
        // Date check
        text = dateCheck(text);

        // Check for map
        text = mapCheck(text);

        // Check for media
        return mediaCheck(text);
    }

    // mediaCheck
    private String mediaCheck(String text)
    {
        StringBuffer buffer = new StringBuffer();

        Pattern pattern = Pattern.compile(MEDIA_PATTERN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        // Find matches
        while (matcher.find())
        {
            File file = new File(matcher.group(2));
            String type = FileUtils.getMimeType(file);

            if (type == null)
            {
                Pattern geoPattern = Pattern.compile(GEO_PATTERN);
                Matcher geoMatcher = geoPattern.matcher(matcher.group(2));

                if (geoMatcher.matches())
                {
                    NumberFormat parser =
                        NumberFormat.getInstance(Locale.ENGLISH);

                    double lat;
                    double lng;

                    try
                    {
                        lat = parser.parse(geoMatcher.group(1)).doubleValue();
                        lng = parser.parse(geoMatcher.group(2)).doubleValue();
                    }

                    // Ignore parse error
                    catch (Exception e)
                    {
                        continue;
                    }

                    // Create replacement iframe
                    String replace =
                        String.format(Locale.ENGLISH, MAP_TEMPLATE,
                                      lng - 0.005, lat - 0.005,
                                      lng + 0.005, lat + 0.005,
                                      lat, lng);

                    // Append replacement
                    matcher.appendReplacement(buffer, replace);
                }
                else
                {
                }
            }
            else if (type.startsWith(IMAGE))
            {
                // Do nothing, handled by markdown view
            }
            else if (type.startsWith(AUDIO))
            {
                // Create replacement
                String replace =
                    String.format(AUDIO_TEMPLATE, matcher.group(2));

                // Append replacement
                matcher.appendReplacement(buffer, replace);
            }
            else if (type.startsWith(VIDEO))
            {
                // Create replacement
                String replace =
                    String.format(VIDEO_TEMPLATE, matcher.group(2));

                // Append replacement
                matcher.appendReplacement(buffer, replace);
            }
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    // mapCheck
    private String mapCheck(String text)
    {
        StringBuffer buffer = new StringBuffer();

        Pattern pattern = Pattern.compile(MAP_PATTERN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        // Find matches
        while (matcher.find())
        {
            double lat;
            double lng;

            try
            {
                lat = Double.parseDouble(matcher.group(1));
                lng = Double.parseDouble(matcher.group(2));
            }

            // Ignore parse error
            catch (Exception e)
            {
                continue;
            }

            // Create replacement iframe
            String replace =
                String.format(Locale.ENGLISH, GEO_TEMPLATE,
                              lat, lng);

            // Substitute replacement
            matcher.appendReplacement(buffer, replace);
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    // dateCheck
    private String dateCheck(String text)
    {
        StringBuffer buffer = new StringBuffer();
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        Pattern pattern = Pattern.compile(DATE_PATTERN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        // Find matches
        while (matcher.find())
        {
            try
            {
                // Parse date
                Date date = dateFormat.parse(matcher.group(2));
                calendar.setTime(date);
            }

            // Ignore parse error
            catch (Exception e)
            {
                continue;
            }

            // Get file
            File file = getDay(calendar.get(Calendar.YEAR),
                               calendar.get(Calendar.MONTH),
                               calendar.get(Calendar.DATE));

            // Get uri
            Uri uri = Uri.fromFile(file);

            // Create replacement
            String replace =
                String.format(Locale.getDefault(), LINK_TEMPLATE,
                              matcher.group(1), uri.toString());
            // Substitute replacement
            matcher.appendReplacement(buffer, replace);
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    // addMedia
    private void addMedia(Intent intent)
    {
        String type = intent.getType();

        if (type == null)
        {
            // Get uri
            Uri uri = intent.getData();
            if (uri.getScheme().equalsIgnoreCase(GEO))
                addMap(uri);
        }
        else if (type.equalsIgnoreCase(TEXT_PLAIN))
        {
            // Get the text
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            // Check text
            if (text != null)
            {
                // Check if it's an URL
                Uri uri = Uri.parse(text);
                if ((uri != null) && (uri.getScheme() != null) &&
                        (uri.getScheme().equalsIgnoreCase(HTTP) ||
                         uri.getScheme().equalsIgnoreCase(HTTPS)))
                    addLink(uri, intent.getStringExtra(Intent.EXTRA_TITLE),
                            true);
                else
                {
                    textView.append(text);
                    loadMarkdown();
                }
            }

            // Get uri
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            // Check uri
            if (uri != null)
            {
                // Resolve content uri
                if (uri.getScheme().equalsIgnoreCase(CONTENT))
                    uri = resolveContent(uri);

                addLink(uri, intent.getStringExtra(Intent.EXTRA_TITLE), true);
            }
        }
        else if (type.startsWith(IMAGE) ||
                 type.startsWith(AUDIO) ||
                 type.startsWith(VIDEO))
        {
            if (intent.getAction().equals(Intent.ACTION_SEND))
            {
                // Get the media uri
                Uri media =
                    intent.getParcelableExtra(Intent.EXTRA_STREAM);

                // Resolve content uri
                if (media.getScheme().equalsIgnoreCase(CONTENT))
                    media = resolveContent(media);

                // Attempt to get web uri
                String path = intent.getStringExtra(Intent.EXTRA_TEXT);

                if (path != null)
                {
                    // Try to get the path as an uri
                    Uri uri = Uri.parse(path);
                    // Check if it's an URL
                    if ((uri != null) && (uri.getScheme() != null) &&
                            (uri.getScheme().equalsIgnoreCase(HTTP) ||
                             uri.getScheme().equalsIgnoreCase(HTTPS)))
                        media = uri;
                }

                addMedia(media, true);
            }
            else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
            {
                // Get the media
                ArrayList<Uri> media =
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (Uri uri : media)
                {
                    // Resolve content uri
                    if (uri.getScheme().equalsIgnoreCase(CONTENT))
                        uri = resolveContent(uri);

                    addMedia(uri, true);
                }
            }
        }

        // Reset the flag
        haveMedia = false;
    }

    // getBaseUrl
    private String getBaseUrl()
    {
        return Uri.fromFile(getCurrent()).toString() + File.separator;
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
        File cssFile = new File(getHome(), CSS_STYLES);

        if (cssFile.exists())
            return Uri.fromFile(cssFile).toString();

        return STYLES;
    }

    // getScript
    private String getScript()
    {
        File jsFile = new File(getHome(), JS_SCRIPT);

        if (jsFile.exists())
            return Uri.fromFile(jsFile).toString();

        return null;
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
        // Show the dialog
        dialog.show();

        // Get the decorators
        List<DayDecorator> decorators = new ArrayList<DayDecorator>();
        decorators.add(new EntryDecorator());

        // Get the calendar
        CustomCalendarView calendarView = dialog.getCalendarView();

        // Set the decorators
        calendarView.setDecorators(decorators);

        // Refresh the calendar
        calendarView.refreshCalendar(date);
    }

    // showDatePickerDialog
    private void showDatePickerDialog(Calendar date)
    {
        DatePickerDialog dialog = new
        DatePickerDialog(this, this,
                         date.get(Calendar.YEAR),
                         date.get(Calendar.MONTH),
                         date.get(Calendar.DATE));
        // Show the dialog
        dialog.show();
    }

    // findAll
    public void findAll()
    {
        // Get search string
        String search = searchView.getQuery().toString();

        // Execute find task
        FindTask findTask = new FindTask(this);
        findTask.execute(search);
    }

    // addTime
    public void addTime()
    {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        String time = format.format(new Date());
        Editable editable = textView.getEditableText();
        int position = textView.getSelectionStart();
        editable.insert(position, time);
        loadMarkdown();
    }

    // addMedia
    public void addMedia()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(WILD_WILD);
        startActivityForResult(Intent.createChooser(intent, null), ADD_MEDIA);
    }

    // editStyles
    public void editStyles()
    {
        File file = new File(getHome(), CSS_STYLES);
        Uri uri = Uri.fromFile(file);

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, "text/css");
        startActivity(intent);
    }

    // editScript
    public void editScript()
    {
        File file = new File(getHome(), JS_SCRIPT);
        Uri uri = Uri.fromFile(file);

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, "text/javascript");
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
        File file = new File(folder);
        if (file.isAbsolute() && file.isDirectory() && file.canWrite())
            return file;

        return new File(Environment.getExternalStorageDirectory(), folder);
    }

    // getYear
    private File getYear(int year)
    {
        return new File(getHome(), String.format(Locale.ENGLISH,
                        "%04d", year));
    }

    // getMonth
    private File getMonth(int year, int month)
    {
        return new File(getYear(year), String.format(Locale.ENGLISH,
                        "%02d", month + 1));
    }

    // getDay
    private File getDay(int year, int month, int day)
    {
        return new
               File(getMonth(year, month), String.format(Locale.ENGLISH,
                       "%02d.txt", day));
    }

    // getFile
    private File getFile()
    {
        return getDay(currEntry.get(Calendar.YEAR),
                      currEntry.get(Calendar.MONTH),
                      currEntry.get(Calendar.DATE));
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
        List<Calendar> list = new ArrayList<>();
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

    // diaryEntry
    private Calendar diaryEntry(String url)
    {
        // Get home folder
        String home = Uri.fromFile(getHome()).toString();

        // Check url
        if (!url.startsWith(home))
            return null;

        // Get uri
        Uri uri = Uri.parse(url);
        File file = new File(uri.getPath());

        // Check file
        if (!file.exists())
            return null;

        // Get segments
        List<String> segments = uri.getPathSegments();
        int size = segments.size();

        // Parse segments
        try
        {
            int day = Integer.parseInt(segments.get(--size).split("\\.")[0]);
            int month = Integer.parseInt(segments.get(--size)) - 1;
            int year = Integer.parseInt(segments.get(--size));

            return new GregorianCalendar(year, month, day);
        }
        catch (Exception e)
        {
        }

        return null;
    }

    // save
    private void save()
    {
        if (currEntry != null)
        {
            String text = textView.getText().toString();

            // Check for events
            text = eventCheck(text);

            // Check for maps
            text = mapCheck(text);

            // Save text
            save(text);
        }
    }

    // save
    private void save(String text)
    {
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
            file.getParentFile().mkdirs();
            try
            {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(text);
                fileWriter.close();
            }
            catch (Exception e)
            {
                alertDialog(R.string.appName, e.getMessage(),
                            android.R.string.ok);
            }
        }
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

    // readAssetFile
    private String readAssetFile(String file)
    {
        try
        {
            // Open file
            try (InputStream input = getAssets().open(file))
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

                changed = true;
                return content.toString();
            }
        }
        catch (Exception e)
        {
        }

        return null;
    }

    // load
    private void load()
    {
        String text = read(getFile());
        textView.setText(text);
        if (markdown)
        {
            loadMarkdown();
            changed = false;
        }
        textView.setSelection(textView.length());
    }

    // setDate
    private void setDate(Calendar date)
    {
        setTitleDate(date.getTime());

        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DATE);

        Calendar calendar = Calendar.getInstance();
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
        if (changed)
            save();

        setDate(date);
        load();

        entry = true;
    }

    // prevEntry
    private void prevEntry()
    {
        entryStack.push(currEntry);
        changeDate(prevEntry);
    }

    // nextEntry
    private void nextEntry()
    {
        entryStack.push(currEntry);
        changeDate(nextEntry);
    }

    // today
    private void today()
    {
        Calendar calendar = Calendar.getInstance();
        Calendar today = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                               calendar.get(Calendar.MONTH),
                                               calendar.get(Calendar.DATE));
        entryStack.clear();
        changeDate(today);
    }

    // index
    private void index()
    {
        entryStack.clear();
        changeDate(indexPage);
    }

    // addMedia
    private void addMedia(Uri media, boolean append)
    {
        String name = media.getLastPathSegment();
        // Copy media file to diary folder
        // TODO: as for now, only for images because video and audio
        // are too time-consuming to be copied on the main thread
        if (copyMedia)
        {
            // Get type
            String type = FileUtils.getMimeType(this, media);
            if (type.startsWith(IMAGE))
            {
                File newMedia = new
                File(getCurrent(), UUID.randomUUID().toString() +
                     FileUtils.getExtension(media.toString()));
                File oldMedia = FileUtils.getFile(this, media);
                try
                {
                    FileUtils.copyFile(oldMedia, newMedia);
                    String newName =
                        Uri.fromFile(newMedia).getLastPathSegment();
                    media = Uri.parse(newName);
                }
                catch (Exception e)
                {
                }
            }
        }

        String mediaText = String.format(MEDIA_TEMPLATE,
                                         name,
                                         media.toString());
        if (append)
            textView.append(mediaText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, mediaText);
        }

        loadMarkdown();
    }

    // addLink
    private void addLink(Uri uri, String title, boolean append)
    {
        if ((title == null) || (title.length() == 0))
            title = uri.getLastPathSegment();

        String url = uri.toString();
        String linkText = String.format(LINK_TEMPLATE, title, url);

        if (append)
            textView.append(linkText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, linkText);
        }

        loadMarkdown();
    }

    // addMap
    private void addMap(Uri uri)
    {
        String mapText = String.format(MEDIA_TEMPLATE,
                                       OSM,
                                       uri.toString());
        if (true)
            textView.append(mapText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, mapText);
        }

        loadMarkdown();
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

    // getNextCalendarMonth
    private Calendar getNextCalendarMonth()
    {
        Calendar nextMonth =
            new GregorianCalendar(currEntry.get(Calendar.YEAR),
                                  currEntry.get(Calendar.MONTH),
                                  currEntry.get(Calendar.DATE));
        nextMonth.add(Calendar.MONTH, 1);
        return nextMonth;
    }

    // getPrevCalendarMonth
    private Calendar getPrevCalendarMonth()
    {
        Calendar prevMonth =
            new GregorianCalendar(currEntry.get(Calendar.YEAR),
                                  currEntry.get(Calendar.MONTH),
                                  currEntry.get(Calendar.DATE));

        prevMonth.add(Calendar.MONTH, -1);
        return prevMonth;
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

    // onSwipeDown
    private void onSwipeDown()
    {
        if (!canSwipe && shown)
            return;

        Calendar prevMonth = getPrevCalendarMonth();
        changeDate(prevMonth);

        if (markdown && shown)
            animateSwipeRight();
    }

    // onSwipeUp
    private void onSwipeUp()
    {
        if (!canSwipe && shown)
            return;

        Calendar nextMonth = getNextCalendarMonth();
        changeDate(nextMonth);

        if (markdown && shown)
            animateSwipeLeft();
    }

    // onActionModeStarted
    @Override
    public void onActionModeStarted(ActionMode mode)
    {
        super.onActionModeStarted(mode);

        // Not on markdown view
        if (!shown)
        {
            // Get the start and end of the selection
            int start = textView.getSelectionStart();
            int end = textView.getSelectionEnd();
            // And the text
            String text = textView.getText().toString();

            // Get a pattern and a matcher for delimiter
            // characters
            Pattern pattern =
                Pattern.compile(PATTERN_CHARS);
            Matcher matcher =
                pattern.matcher(text);

            // Find the first match after the end of the selection
            if (matcher.find(end))
            {
                // Update the selection end
                end = matcher.start();

                // Get the matched char
                char c = text.charAt(end);

                // Check for opening brackets
                if (BRACKET_CHARS.indexOf(c) == -1)
                {
                    switch (c)
                    {
                        // Check for close brackets and look for
                        // the open brackets
                    case ')':
                        c = '(';
                        break;

                    case ']':
                        c = '[';
                        break;

                    case '}':
                        c = '{';
                        break;

                    case '>':
                        c = '<';
                        break;
                    }

                    // Do reverse search
                    start = text.lastIndexOf(c, start) + 1;

                    // Check for included newline
                    if (start > text.lastIndexOf('\n', end))
                        // Update selection
                        textView.setSelection(start, end);
                }
            }
        }
    }

    // FindTask
    @SuppressLint("StaticFieldLeak")
    private class FindTask
        extends AsyncTask<String, Void, List<String>>
    {
        private Context context;
        private String search;

        public FindTask(Context context)
        {
            this.context = context;
        }

        // doInBackground
        @Override
        protected List<String> doInBackground(String... params)
        {
            search = params[0];
            Pattern pattern = Pattern.compile(search,
                                              Pattern.CASE_INSENSITIVE |
                                              Pattern.LITERAL |
                                              Pattern.UNICODE_CASE);
            // Get entry list
            List<Calendar> entries = getEntries();

            // Create a list of matches
            List<String> matches = new ArrayList<>();

            // Check the entries
            for (Calendar entry : entries)
            {
                File file = getDay(entry.get(Calendar.YEAR),
                                   entry.get(Calendar.MONTH),
                                   entry.get(Calendar.DATE));

                Matcher matcher = pattern.matcher(read(file));
                if (matcher.find())
                    matches.add(DateFormat.getDateInstance(DateFormat.MEDIUM)
                                .format(entry.getTime()));
            }

            return matches;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(List<String> matches)
        {
            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.findAll);

            // If found populate dialog
            if (!matches.isEmpty())
            {
                final String[] choices = matches.toArray(new String[0]);
                builder.setItems(choices, (dialog, which) ->
                {
                    String choice = choices[which];
                    DateFormat format =
                        DateFormat.getDateInstance(DateFormat.MEDIUM);

                    // Get the entry chosen
                    try
                    {
                        Date date = format.parse(choice);
                        Calendar entry = Calendar.getInstance();
                        entry.setTime(date);
                        changeDate(entry);

                        // Put the search text back - why it
                        // disappears I have no idea or why I have to
                        // do it after a delay
                        // run
                        searchView.postDelayed(() -> searchView.setQuery(search, false), FIND_DELAY);
                    }
                    catch (Exception e)
                    {
                    }
                });
            }

            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    // QueryTextListener
    private class QueryTextListener
        implements SearchView.OnQueryTextListener
    {
        private BackgroundColorSpan span = new
        BackgroundColorSpan(Color.YELLOW);
        private Editable editable;
        private Pattern pattern;
        private Matcher matcher;
        private String text;
        private int index;
        private int height;

        // onQueryTextChange
        @Override
        @SuppressWarnings("deprecation")
        public boolean onQueryTextChange(String newText)
        {
            // Use web view functionality
            if (shown)
                markdownView.findAll(newText);

            // Use regex search and spannable for highlighting
            else
            {
                height = scrollView.getHeight();
                editable = textView.getEditableText();
                text = textView.getText().toString();

                // Reset the index and clear highlighting
                if (newText.length() == 0)
                {
                    index = 0;
                    editable.removeSpan(span);
                }

                // Get pattern
                pattern = Pattern.compile(newText,
                                          Pattern.CASE_INSENSITIVE |
                                          Pattern.LITERAL |
                                          Pattern.UNICODE_CASE);
                // Find text
                matcher = pattern.matcher(text);
                if (matcher.find(index))
                {
                    // Get index
                    index = matcher.start();

                    // Get text position
                    int line = textView.getLayout()
                               .getLineForOffset(index);
                    int pos = textView.getLayout()
                              .getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, pos - height / 2);

                    // Highlight it
                    editable
                    .setSpan(span, index, index +
                             newText.length(),
                             Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            return true;
        }

        // onQueryTextSubmit
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            // Use web view functionality
            if (shown)
                markdownView.findNext(true);

            // Use regex search and spannable for highlighting
            else
            {
                // Find next text
                if (matcher.find())
                {
                    // Get index
                    index = matcher.start();

                    // Get text position
                    int line = textView.getLayout()
                               .getLineForOffset(index);
                    int pos = textView.getLayout()
                              .getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, pos - height / 2);

                    // Highlight it
                    editable
                    .setSpan(span, index, index +
                             query.length(),
                             Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Reset matcher
                if (matcher.hitEnd())
                    matcher.reset();
            }

            return true;
        }
    }

    // GestureListener
    private class GestureListener
        extends GestureDetector.SimpleOnGestureListener
    {
        private static final int SWIPE_THRESHOLD = 256;
        private static final int SWIPE_VELOCITY_THRESHOLD = 256;

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
                else
                {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                            Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD &&
                            multi)
                    {
                        if (diffY > 0)
                        {
                            onSwipeDown();
                        }
                        else
                        {
                            onSwipeUp();
                        }
                    }

                    result = true;
                }

                multi = false;
            }
            catch (Exception e)
            {
            }

            return result;
        }
    }

    // EntryDecorator
    private class EntryDecorator
        implements DayDecorator
    {
        // EntryDecorator
        private EntryDecorator()
        {
        }

        // decorate
        @Override
        public void decorate(DayView dayView)
        {
            Calendar cellDate = dayView.getDate();
            File dayFile = getDay(cellDate.get(Calendar.YEAR),
                                  cellDate.get(Calendar.MONTH),
                                  cellDate.get(Calendar.DATE));

            if (dayFile.exists())
                    dayView.setBackgroundResource(R.drawable.diary_entry);
        }
    }
}
