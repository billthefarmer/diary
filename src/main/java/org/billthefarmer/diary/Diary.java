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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import android.support.v4.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.billthefarmer.markdown.MarkdownView;
import org.billthefarmer.view.CustomCalendarDialog;
import org.billthefarmer.view.CustomCalendarView;
import org.billthefarmer.view.DayDecorator;
import org.billthefarmer.view.DayView;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

// Diary
public class Diary extends Activity
    implements DatePickerDialog.OnDateSetListener,
    CustomCalendarDialog.OnDateSetListener
{
    public final static int ADD_MEDIA = 1;
    public final static int EDIT_STYLES = 2;
    public final static int CREATE_BACKUP = 3;

    public final static int REQUEST_READ = 1;
    public final static int REQUEST_WRITE = 2;
    public final static int REQUEST_TEMPLATE = 3;

    public final static int POSITION_DELAY = 128;
    public final static int VISIBLE_DELAY = 2048;
    public final static int LARGE_SIZE = 262144;
    public final static int BUFFER_SIZE = 4096;
    public final static int SCALE_RATIO = 128;
    public final static int FIND_DELAY = 256;
    public final static int FIND_SIZE = 64;

    // Indices for the ViewSwitchers
    public static final int EDIT_TEXT = 0;
    public static final int MARKDOWN = 1;
    public static final int ACCEPT = 0;
    public static final int EDIT = 1;

    public final static String DIARY = "Diary";
    public final static String TAG = DIARY;

    public final static String YEAR = "year";
    public final static String MONTH = "month";
    public final static String DAY = "day";

    public final static String SAVED = "saved";
    public final static String SHOWN = "shown";
    public final static String ENTRY = "entry";

    // Patterns
    public final static Pattern PATTERN_CHARS =
        Pattern.compile("[\\(\\)\\[\\]\\{\\}\\<\\>\"'`]");
    public final static Pattern MEDIA_PATTERN =
        Pattern.compile("!\\[(.*?)\\]\\((.+?)\\)", Pattern.MULTILINE);
    public final static Pattern EVENT_PATTERN =
        Pattern.compile("^@ *(\\d{1,2}:\\d{2}) +(.+)$", Pattern.MULTILINE);
    public final static Pattern MAP_PATTERN =
        Pattern.compile("\\[(?:osm:)?(-?\\d+[,.]\\d+)[,;] ?(-?\\d+[,.]\\d+)\\]",
                        Pattern.MULTILINE);
    public final static Pattern GEO_PATTERN =
        Pattern.compile("geo:(-?\\d+[.]\\d+), ?(-?\\d+[.]\\d+).*");
    public final static Pattern DATE_PATTERN =
        Pattern.compile("(?<!`)\\[(.+?)\\]\\(date: ?(\\d+.\\d+.\\d+)\\)(?!`)",
                        Pattern.MULTILINE);
    public final static Pattern POSN_PATTERN =
        Pattern.compile("^ ?\\[([<#>])\\]: ?#(?: ?\\((\\d+)\\))? *$",
                        Pattern.MULTILINE);
    public final static Pattern FILE_PATTERN =
        Pattern.compile("([0-9]{4}).([0-9]{2}).([0-9]{2}).(txt|md)$");

    public final static Pattern TEMP_PATTERN =
        Pattern.compile("<<date *(.*)>>", Pattern.MULTILINE);

    public final static String DATE_FORMAT = "EEEE d MMMM yyyy HH:mm";
    public final static String TEMP_FORMAT = "EEEE d MMMM yyyy";
    public final static String LINK_FORMAT = "date:%s";

    public final static String YEAR_DIR = "^[0-9]{4}$";
    public final static String MONTH_DIR = "^[0-9]{2}$";
    public final static String DAY_FILE = "^[0-9]{2}.(txt|md)$";

    public final static String YEAR_FORMAT = "%04d";
    public final static String MONTH_FORMAT = "%02d";
    public final static String DAY_FORMAT = "%02d.txt";
    public final static String MD_FORMAT = "%02d.md";

    public final static String ZIP = ".zip";
    public final static String HELP = "help.md";
    public final static String STYLES = "file:///android_asset/styles.css";
    public final static String SCRIPT = "file:///android_asset/script.js";
    public final static String CSS_STYLES = "css/styles.css";
    public final static String TEXT_CSS = "text/css";
    public final static String JS_SCRIPT = "js/script.js";
    public final static String TEXT_JAVASCRIPT = "text/javascript";
    public final static String APPLICATION_ZIP = "application/zip";
    public final static String FILE_PROVIDER =
        "org.billthefarmer.diary.fileprovider";

    public final static String MEDIA_TEMPLATE = "![%s](%s)";
    public final static String LINK_TEMPLATE = "[%s](%s)";
    public final static String INDEX_TEMPLATE = "<<date>>";
    public final static String AUDIO_TEMPLATE =
        "<audio controls src=\"%s\"></audio>";
    public final static String VIDEO_TEMPLATE =
        "<video controls src=\"%s\"></video>";
    public final static String EVENT_TEMPLATE = "@:$1 $2";
    public final static String MAP_TEMPLATE =
        "<iframe width=\"560\" height=\"420\" " +
        "src=\"https://www.openstreetmap.org/export/embed.html?" +
        "bbox=%f,%f,%f,%f&amp;layer=mapnik\">" +
        "</iframe><br/><small>" +
        "<a href=\"https://www.openstreetmap.org/#map=16/%f/%f\">" +
        "View Larger Map</a></small>";
    public final static String GEO_TEMPLATE = "![osm](geo:%f,%f)";
    public final static String POSN_TEMPLATE = "[#]: # (%d)";
    public final static String EVENTS_TEMPLATE = "@:%s %s\n";

    public final static String BRACKET_CHARS = "([{<";
    public final static String DIARY_IMAGE = "Diary.png";

    public final static String GEO = "geo";
    public final static String OSM = "osm";
    public final static String HTTP = "http";
    public final static String TEXT = "text";
    public final static String HTTPS = "https";
    public final static String MAILTO = "mailto";
    public final static String CONTENT = "content";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String IMAGE_PNG = "image/png";
    public final static String WILD_WILD = "*/*";
    public final static String IMAGE = "image";
    public final static String AUDIO = "audio";
    public final static String VIDEO = "video";
    public final static String ELLIPSIS = "…";

    private boolean custom = true;
    private boolean markdown = true;
    private boolean external = false;
    private boolean useIndex = false;
    private boolean useTemplate = false;
    private boolean copyMedia = false;
    private boolean haveMedia = false;
    private boolean darkTheme = false;

    private boolean changed = false;
    private boolean shown = true;

    private boolean scrollUp = false;
    private boolean scrollDn = false;

    private boolean multi = false;
    private boolean entry = false;

    private long saved = 0;

    private float minScale = 1000;


    private long indexPage;
    private long templatePage;

    private String folder = DIARY;
    private String template = INDEX_TEMPLATE;

    private Calendar prevEntry;
    private Calendar currEntry;
    private Calendar nextEntry;

    private EditText textView;
    private ScrollView scrollView;

    private MarkdownView markdownView;
    private ViewSwitcher layoutSwitcher;
    private ViewSwitcher buttonSwitcher;

    private SearchView searchView;
    private MenuItem searchItem;

    private Runnable showEdit;
    private Runnable showAccept;

    private GestureDetector gestureDetector;

    private Deque<Calendar> entryStack;

    private Toast toast;
    private View accept;
    private View edit;

    // sortFiles
    public static File[] sortFiles(File[] files)
    {
        if (files == null)
            return new File[0];
        // compare
        Arrays.sort(files, (file1, file2) ->
                    file2.getName().compareTo(file1.getName()));
        return files;
    }

    // listYears
    public static File[] listYears(File home)
    {
        // accept
        return sortFiles(home.listFiles((dir, filename) ->
                                        filename.matches(YEAR_DIR)));
    }

    // listMonths
    public static File[] listMonths(File yearDir)
    {
        // accept
        return sortFiles(yearDir.listFiles((dir, filename) ->
                                           filename.matches(MONTH_DIR)));
    }

    // listDays
    public static File[] listDays(File monthDir)
    {
        // accept
        return sortFiles(monthDir.listFiles((dir, filename) ->
                                            filename.matches(DAY_FILE)));
    }

    // yearValue
    public static int yearValue(File yearDir)
    {
        return Integer.parseInt(yearDir.getName());
    }

    // monthValue
    public static int monthValue(File monthDir)
    {
        return Integer.parseInt(monthDir.getName()) - 1;
    }

    // dayValue
    public static int dayValue(File dayFile)
    {
        return Integer.parseInt(dayFile.getName().split("\\.")[0]);
    }

    // read
    public static StringBuilder read(File file)
    {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                text.append(line);
                text.append(System.getProperty("line.separator"));
                if (text.length() >= LARGE_SIZE)
                    break;
            }
        }

        catch (Exception e) {}

        return text;
    }

    // parseTime
    public static long parseTime(File file)
    {
        Matcher matcher = FILE_PATTERN.matcher(file.getPath());
        if (matcher.find())
        {
            try
            {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2)) - 1;
                int dayOfMonth = Integer.parseInt(matcher.group(3));

                return new GregorianCalendar
                    (year, month, dayOfMonth).getTimeInMillis();
            }

            catch (NumberFormatException e)
            {
                return -1;
            }
        }

        return -1;
    }

    // listEntries
    public static void listEntries(File directory, List<File> fileList)
    {
        // Get all entry files from a directory.
        File[] files = directory.listFiles();
        if (files != null)
        {
            // Sort files, reverse order
            Arrays.sort(files, Collections.reverseOrder());
            for (File file : files)
            {
                if (file.isFile() && file.getName().matches(DAY_FILE))
                    fileList.add(file);

                else if (file.isDirectory())
                    listEntries(file, fileList);
            }
        }
    }

    // listFiles
    public static void listFiles(File directory, List<File> fileList)
    {
        // Get all entry files from a directory.
        File[] files = directory.listFiles();
        if (files != null)
            for (File file : files)
            {
                if (file.isFile())
                    fileList.add(file);

                else if (file.isDirectory())
                {
                    fileList.add(file);
                    listFiles(file, fileList);
                }
            }
    }

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        getPreferences();

        if (!darkTheme)
            setTheme(R.style.AppTheme);

        setContentView(R.layout.main);

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.scroll);
        markdownView = findViewById(R.id.markdown);

        accept = findViewById(R.id.accept);
        edit = findViewById(R.id.edit);

        layoutSwitcher = findViewById(R.id.layout_switcher);
        buttonSwitcher = findViewById(R.id.button_switcher);

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
            if (useIndex && Intent.ACTION_MAIN.equals(intent.getAction()))
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
        if (dark != darkTheme && Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();

        // Set date
        setDate(currEntry);

        // Reload if modified
        if (getFile().lastModified() > saved)
            load();

        // Clear cache
        markdownView.clearCache(true);

        // Copy help text to today's page if no entries
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
             == PackageManager.PERMISSION_GRANTED) &&
            prevEntry == null && nextEntry == null && textView.length() == 0)
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
        {
            save();
            // Clear flag
            changed = false;
        }

        saved = getFile().lastModified();
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
        menu.findItem(R.id.cancel).setVisible(changed);
        menu.findItem(R.id.index).setVisible(useIndex);
        menu.findItem(R.id.link).setVisible(useIndex);
        menu.findItem(R.id.print)
            .setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

        // Set up search view
        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();

        // Set up search view options and listener
        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener());
        }

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
        case R.id.cancel:
            cancel();
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
        case R.id.link:
            addIndexLink();
            break;
        case R.id.findAll:
            findAll();
            break;
        case R.id.print:
            print();
            break;
        case R.id.share:
            share();
            break;
        case R.id.addTime:
            addTime();
            break;
        case R.id.addEvents:
            addEvents();
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
        case R.id.backup:
            backup();
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
            if (uri == null)
                return;

            // Resolve content uri
            if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                uri = resolveContent(uri);

            String title = (CONTENT.equalsIgnoreCase(uri.getScheme()))?
                FileUtils.getDisplayName(this, uri, null, null):
                uri.getLastPathSegment();

            String type = (CONTENT.equalsIgnoreCase(uri.getScheme()))?
                getContentResolver().getType(uri):
                FileUtils.getMimeType(this, uri);

            if (type == null)
                addLink(uri, title, false);

            else if (type.startsWith(IMAGE) ||
                     type.startsWith(AUDIO) ||
                     type.startsWith(VIDEO))
                addMedia(uri, false);

            else
                addLink(uri, title, false);
            break;

        case EDIT_STYLES:
            markdownView.reload();
            break;

        case CREATE_BACKUP:
            // Check data
            if (data == null || data.getData() == null)
                return;

            uri = data.getData();
            ZipTask zipTask = new ZipTask(this);
            zipTask.execute(uri);
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

    // setListeners
    private void setListeners()
    {
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

                    if (URLUtil.isFileUrl(url) || URLUtil.isAssetUrl(url))
                    {
                        entry = false;
                        return false;
                    }

                    Uri uri = Uri.parse(url);
                    // Email url or use external browser
                    if (external || MAILTO.equalsIgnoreCase(uri.getScheme()))
                    {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        if (intent.resolveActivity(getPackageManager()) != null)
                            startActivity(intent);
                        return true;
                    }

                    entry = false;
                    return false;
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                showEdit = () ->
                {
                    startAnimation(edit, R.anim.fade_in, View.VISIBLE);
                    scrollUp = false;
                    scrollDn = false;
                };

                // onScrollChange
                markdownView.setOnScrollChangeListener((v, x, y, oldX, oldY) ->
                {
                    // Scroll up
                    if (y > oldY)
                    {
                        if (!scrollUp)
                        {
                            // Hide button
                            // edit.setVisibility(View.INVISIBLE);
                            startAnimation(edit, R.anim.fade_out,
                                           View.INVISIBLE);

                            // Set flags
                            scrollUp = true;
                            scrollDn = false;
                        }

                        // Show button delayed
                        markdownView.removeCallbacks(showEdit);
                        markdownView.postDelayed(showEdit, VISIBLE_DELAY);
                    }

                    else if (!scrollDn)
                    {
                        // Set flags
                        scrollUp = false;
                        scrollDn = true;

                        // Show button
                        if (edit.getVisibility() != View.VISIBLE)
                        {
                            // edit.setVisibility(View.VISIBLE);
                            startAnimation(edit, R.anim.fade_in, View.VISIBLE);
                            markdownView.removeCallbacks(showEdit);
                        }
                   }
                });
            }

            // On long click
            markdownView.setOnLongClickListener(v ->
            {
                // Show button
                if (edit.getVisibility() != View.VISIBLE)
                    startAnimation(edit, R.anim.fade_in, View.VISIBLE);
                scrollUp = false;
                scrollDn = false;
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
                    // Update menu
                    invalidateOptionsMenu();
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
                scrollUp = true;
                scrollDn = false;
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

                // Check template
                if (useTemplate)
                    template();

                // Scroll after delay
                edit.postDelayed(() ->
                {
                    // Get selection
                    int selection = textView.getSelectionStart();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(selection);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    int height = scrollView.getHeight();
                    scrollView.smoothScrollTo(0, position - height / 2);
                }, POSITION_DELAY);

                shown = false;
            });

            // On long click
            edit.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                scrollUp = true;
                scrollDn = false;
                return true;
            });
        }

        if (textView != null)
        {
            textView.addTextChangedListener(new TextWatcher()
            {
                // afterTextChanged
                @Override
                public void afterTextChanged(Editable s)
                {
                    // Text changed
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
                // Show button
                if (accept.getVisibility() != View.VISIBLE)
                    startAnimation(accept, R.anim.fade_in, View.VISIBLE);
                scrollUp = false;
                scrollDn = false;
                return false;
            });
        }

        if (scrollView != null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                showAccept = () ->
                {
                    startAnimation(accept, R.anim.fade_in, View.VISIBLE);
                    scrollUp = false;
                };

                // onScrollChange
                scrollView.setOnScrollChangeListener((v, x, y, oldX, oldY) ->
                {
                    // Scroll up
                    if (y > oldY)
                    {
                        if (!scrollUp)
                        {
                            // Hide button
                            // accept.setVisibility(View.INVISIBLE);
                            startAnimation(accept, R.anim.fade_out,
                                           View.INVISIBLE);

                            // Set flags
                            scrollUp = true;
                            scrollDn = false;
                        }

                        // Show button delayed
                        scrollView.removeCallbacks(showAccept);
                        scrollView.postDelayed(showAccept, VISIBLE_DELAY);
                    }

                    else if (!scrollDn)
                    {
                        // Set flags
                        scrollUp = false;
                        scrollDn = true;

                        // Show button
                        if (accept.getVisibility() != View.VISIBLE)
                        {
                            // accept.setVisibility(View.VISIBLE);
                            startAnimation(accept, R.anim.fade_in,
                                           View.VISIBLE);
                            scrollView.removeCallbacks(showAccept);
                        }
                    }
                });
            }
    }

    // cancel
    private void cancel()
    {
        if (changed)
            alertDialog(R.string.appName, R.string.changes,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                save();
                load();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                load();
                break;
            }
        });

        else
            load();
    }

    // animateAccept
    private void animateAccept()
    {
        // Animation
        layoutSwitcher.setDisplayedChild(MARKDOWN);
        buttonSwitcher.setDisplayedChild(EDIT);
    }

    // animateEdit
    private void animateEdit()
    {
        // Animation
        layoutSwitcher.setDisplayedChild(EDIT_TEXT);
        buttonSwitcher.setDisplayedChild(ACCEPT);
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

        copyMedia = preferences.getBoolean(Settings.PREF_COPY_MEDIA, false);
        custom = preferences.getBoolean(Settings.PREF_CUSTOM, true);
        darkTheme = preferences.getBoolean(Settings.PREF_DARK_THEME, false);
        external = preferences.getBoolean(Settings.PREF_EXTERNAL, false);
        markdown = preferences.getBoolean(Settings.PREF_MARKDOWN, true);
        useIndex = preferences.getBoolean(Settings.PREF_USE_INDEX, false);
        useTemplate = preferences.getBoolean(Settings.PREF_USE_TEMPLATE, false);

        // Index page
        indexPage = preferences.getLong(Settings.PREF_INDEX_PAGE,
                                        DatePickerPreference.DEFAULT_VALUE);
        // Template page
        templatePage = preferences.getLong(Settings.PREF_TEMPLATE_PAGE,
                                           DatePickerPreference.DEFAULT_VALUE);
        // Folder
        folder = preferences.getString(Settings.PREF_FOLDER, DIARY);

        // Link template
        template = preferences.getString(Settings.PREF_INDEX_TEMPLATE,
                                         INDEX_TEMPLATE);
    }

    // mediaCheck
    private void mediaCheck(Intent intent)
    {
        // Check for sent media
        if (Intent.ACTION_SEND.equals(intent.getAction()) ||
            Intent.ACTION_VIEW.equals(intent.getAction()) ||
            Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
        {
            haveMedia = true;
            goToDate(currEntry);
        }
    }

    // eventCheck
    private String eventCheck(CharSequence text)
    {
        Matcher matcher = EVENT_PATTERN.matcher(text);

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
        CharSequence text = textView.getText();
        loadMarkdown(text);
    }

    // loadMarkdown
    private void loadMarkdown(CharSequence text)
    {
        markdownView.loadMarkdown(getBaseUrl(), markdownCheck(text),
                                  getStyles(), getScript());
    }

    // markdownCheck
    private String markdownCheck(CharSequence text)
    {
        // Date check
        text = dateCheck(text);

        // Check for map
        text = mapCheck(text);

        // Check for media
        return mediaCheck(text).toString();
    }

    // mediaCheck
    private CharSequence mediaCheck(CharSequence text)
    {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = MEDIA_PATTERN.matcher(text);

        // Find matches
        while (matcher.find())
        {
            File file = new File(matcher.group(2));
            String type = FileUtils.getMimeType(file);

            if (type == null)
            {
                Matcher geoMatcher = GEO_PATTERN.matcher(matcher.group(2));

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

        return buffer;
    }

    // mapCheck
    private CharSequence mapCheck(CharSequence text)
    {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = MAP_PATTERN.matcher(text);

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

        return buffer;
    }

    // dateCheck
    private CharSequence dateCheck(CharSequence text)
    {
        StringBuffer buffer = new StringBuffer();
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        Matcher matcher = DATE_PATTERN.matcher(text);

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
                String.format(Locale.ROOT, LINK_TEMPLATE,
                              matcher.group(1), uri.toString());
            // Substitute replacement
            matcher.appendReplacement(buffer, replace);
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer;
    }

    // addMedia
    private void addMedia(Intent intent)
    {
        // Get type
        String type = intent.getType();

        if (type == null)
        {
            // Get uri
            Uri uri = intent.getData();
            if (GEO.equalsIgnoreCase(uri.getScheme()))
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
                if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                    uri = resolveContent(uri);

                addLink(uri, intent.getStringExtra(Intent.EXTRA_TITLE), true);
            }
        }
        else if (type.startsWith(IMAGE) ||
                 type.startsWith(AUDIO) ||
                 type.startsWith(VIDEO))
        {
            if (Intent.ACTION_SEND.equals(intent.getAction()))
            {
                // Get the media uri
                Uri media =
                    intent.getParcelableExtra(Intent.EXTRA_STREAM);

                // Resolve content uri
                if (CONTENT.equalsIgnoreCase(media.getScheme()))
                    media = resolveContent(media);

                if (media != null)
                    addMedia(media, true);

                else
                {
                    // Attempt to get web uri
                    String path = intent.getStringExtra(Intent.EXTRA_TEXT);

                    if (path != null)
                    {
                        // Try to get the path as an uri
                        media = Uri.parse(path);
                        // Check if it's an URL
                        if ((media != null) &&
                            (HTTP.equalsIgnoreCase(media.getScheme()) ||
                             HTTPS.equalsIgnoreCase(media.getScheme())))
                            addMedia(media, true);
                    }
                }
            }
            else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
            {
                // Get the media
                ArrayList<Uri> media =
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (Uri uri : media)
                {
                    // Resolve content uri
                    if (CONTENT.equalsIgnoreCase(uri.getScheme()))
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
            buttonSwitcher.setVisibility(View.VISIBLE);
            // Check if shown
            if (shown)
            {
                layoutSwitcher.setDisplayedChild(MARKDOWN);
                buttonSwitcher.setDisplayedChild(EDIT);
            }

            else
            {
                layoutSwitcher.setDisplayedChild(EDIT_TEXT);
                buttonSwitcher.setDisplayedChild(ACCEPT);
            }
        }

        else
        {
            layoutSwitcher.setDisplayedChild(EDIT_TEXT);
            buttonSwitcher.setVisibility(View.GONE);
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

        // Get the calendar view
        CustomCalendarView calendarView = dialog.getCalendarView();

        // Set the decorators
        calendarView.setDecorators(decorators);

        // New calendar for calendar view
        GregorianCalendar calendar = new
            GregorianCalendar(date.get(Calendar.YEAR),
                              date.get(Calendar.MONTH),
                              date.get(Calendar.DATE));
        // Refresh the calendar
        calendarView.refreshCalendar(calendar);
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
    private void findAll()
    {
        // Get search string
        String search = searchView.getQuery().toString();

        // Execute find task
        FindTask findTask = new FindTask(this);
        findTask.execute(search);
    }

    // print
    private void print()
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;

        // Get a PrintManager instance
        PrintManager printManager = (PrintManager)
            getSystemService(PRINT_SERVICE);

        String jobName = getString(R.string.appName) + " Document";

        // Get a print adapter instance
        PrintDocumentAdapter printAdapter =
            markdownView.createPrintDocumentAdapter(jobName);

        // Create a print job with name and adapter instance
        printManager.print(jobName, printAdapter,
                           new PrintAttributes.Builder()
                           .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                           .build());
    }

    // share
    @SuppressWarnings("deprecation")
    private void share()
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        String title =
            String.format("%s: %s", getString(R.string.appName), getTitle());
        intent.putExtra(Intent.EXTRA_TITLE, title);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        if (shown)
        {
            intent.setType(IMAGE_PNG);

            markdownView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(markdownView.getDrawingCache());
            markdownView.setDrawingCacheEnabled(false);

            File image = new File(getCacheDir(), DIARY_IMAGE);
            try (FileOutputStream out = new FileOutputStream(image))
            {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            }

            catch (Exception e) {}

            Uri imageUri = FileProvider
                .getUriForFile(this, FILE_PROVIDER, image);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
        }

        else
        {
            intent.setType(TEXT_PLAIN);
            Uri fileUri = FileProvider
                .getUriForFile(this, FILE_PROVIDER, getFile());
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
        }

        startActivity(Intent.createChooser(intent, null));
    }

    // addTime
    private void addTime()
    {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        String time = format.format(new Date());
        Editable editable = textView.getEditableText();
        int position = textView.getSelectionStart();
        editable.insert(position, time);
        loadMarkdown();
    }

    // addEvents
    private void addEvents()
    {
        GregorianCalendar endTime = new
            GregorianCalendar(currEntry.get(Calendar.YEAR),
                              currEntry.get(Calendar.MONTH),
                              currEntry.get(Calendar.DATE));
        endTime.add(Calendar.DATE, 1);
        QueryHandler.queryEvents(this, currEntry.getTimeInMillis(),
                                 endTime.getTimeInMillis(),
                                 (startTime, title) ->
        {
            DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
            String time = format.format(startTime);
            String event = String.format(EVENTS_TEMPLATE, time, title);
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, event);
            loadMarkdown();
        });
    }

    // addMedia
    private void addMedia()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(WILD_WILD);
        startActivityForResult(Intent.createChooser(intent, null), ADD_MEDIA);
    }

    // editStyles
    private void editStyles()
    {
        File file = new File(getHome(), CSS_STYLES);
        Uri uri = Uri.fromFile(file);
        startActivityForResult(new Intent(Intent.ACTION_EDIT, uri,
                                          this, Editor.class), EDIT_STYLES);
    }

    // editScript
    private void editScript()
    {
        File file = new File(getHome(), JS_SCRIPT);
        Uri uri = Uri.fromFile(file);
        startActivityForResult(new Intent(Intent.ACTION_EDIT, uri,
                                          this, Editor.class), EDIT_STYLES);
    }

    // backup
    private void backup()
    {
        // Remove path prefix
        String name = (getHome().getPath() + ZIP)
            .replaceFirst(Environment.getExternalStorageDirectory()
                          .getPath() + File.separator, "");

        saveAsDialog(name, R.string.backup, R.string.choose, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                EditText text = ((Dialog) dialog).findViewById(R.id.pathText);
                String string = text.getText().toString();

                // Ignore empty string
                if (string.isEmpty())
                    return;

                File file = new File(string);

                // Check absolute file
                if (!file.isAbsolute())
                    file = new
                        File(Environment.getExternalStorageDirectory(), string);

                // Start zip task
                Uri uri = Uri.fromFile(file);
                ZipTask zipTask = new ZipTask(this);
                zipTask.execute(uri);
                break;

            case DialogInterface.BUTTON_NEUTRAL:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.setType(APPLICATION_ZIP);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_TITLE, name);
                    startActivityForResult(intent, CREATE_BACKUP);
                }
                break;
            }
        });
    }

    // saveAsDialog
    private void saveAsDialog(String path, int title, int message,
                              DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(title).replace("…", ""));
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(R.string.save, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            builder.setNeutralButton(R.string.storage, listener);

        // Create edit text
        LayoutInflater inflater = (LayoutInflater) builder.getContext()
            .getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.save_path, null);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.show();
        TextView text = dialog.findViewById(R.id.pathText);
        text.setText(path);
    }

    // settings
    private void settings()
    {
        startActivity(new Intent(this, Settings.class));
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
                                                 YEAR_FORMAT, year));
    }

    // getMonth
    private File getMonth(int year, int month)
    {
        return new File(getYear(year), String.format(Locale.ENGLISH,
                                                     MONTH_FORMAT, month + 1));
    }

    // getDay
    private File getDay(int year, int month, int day)
    {
        File folder = getMonth(year, month);
        File file = new File(folder, String.format(Locale.ENGLISH,
                                                   DAY_FORMAT, day));
        if (file.exists())
            return file;

        else if (markdown)
            return new File(folder, String.format(Locale.ENGLISH,
                                                  MD_FORMAT, day));
        else
            return file;
    }

    // getFile
    private File getFile()
    {
        return getFile(currEntry);
    }

    // getFile
    private File getFile(Calendar entry)
    {
        return getDay(entry.get(Calendar.YEAR),
                      entry.get(Calendar.MONTH),
                      entry.get(Calendar.DATE));
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
        Calendar entry = getNextEntry(1970, Calendar.JANUARY, 1);
        return getEntries(entry);
    }

    // getEntries
    private List<Calendar> getEntries(Calendar entry)
    {
        List<Calendar> list = new ArrayList<>();
        while (entry != null && currEntry.compareTo(entry) <= 0)
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

        catch (Exception e) {}

        return null;
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
                    // Granted, save
                    save();
            break;

        case REQUEST_READ:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, load
                    load();
            break;

        case REQUEST_TEMPLATE:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, template
                    template();
            break;
        }
    }

    // save
    private void save()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.WRITE_CALENDAR,
                     Manifest.permission.READ_CALENDAR}, REQUEST_WRITE);

                return;
            }
        }

        if (currEntry != null)
        {
            CharSequence text = textView.getText();

            // Check for events
            text = eventCheck(text);

            // Check for maps
            text = mapCheck(text);

            // Check for cursor position
            text = positionCheck(text);

            // Save text
            save(text);

            // Update app widgets
            updateWidgets(text);
        }
    }

    // save
    private void save(CharSequence text)
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
            try (FileWriter fileWriter = new FileWriter(file))
            {
                fileWriter.append(text);
            }

            catch (Exception e)
            {
                alertDialog(R.string.appName, e.getMessage(),
                            android.R.string.ok);

                e.printStackTrace();
           }
        }
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

    // updateWidgets
    @SuppressWarnings("deprecation")
    @SuppressLint("InlinedApi")
    private void updateWidgets(CharSequence text)
    {
        Calendar today = Calendar.getInstance();
        if (currEntry != null &&
            currEntry.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            currEntry.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
            currEntry.get(Calendar.DATE) == today.get(Calendar.DATE))
        {
            // Get date
            DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
            String date = format.format(today.getTime());

            if (markdown)
            {
                // Use commonmark
                Parser parser = Parser.builder().build();
                Node document = parser.parse(text.toString());
                HtmlRenderer renderer = HtmlRenderer.builder().build();

                String html = renderer.render(document);
                text = Html.fromHtml(html);
            }

            // Create an Intent to launch Diary
            Intent intent = new Intent(this, Diary.class);
            PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent,
                                          PendingIntent.FLAG_UPDATE_CURRENT |
                                          PendingIntent.FLAG_IMMUTABLE);

            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            ComponentName provider = new
                ComponentName(this, DiaryWidgetProvider.class);
            RemoteViews views = new
                RemoteViews(getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);
            views.setTextViewText(R.id.header, date);
            views.setTextViewText(R.id.entry, text);
            manager.updateAppWidget(provider, views);
        }
    }

    // readAssetFile
    private CharSequence readAssetFile(String file)
    {
        try
        {
            // Open file
            try (BufferedReader reader = new BufferedReader
                 (new InputStreamReader(getAssets().open(file))))
            {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    content.append(line);
                    content.append(System.getProperty("line.separator"));
                }

                changed = true;
                return content;
            }
        }

        catch (Exception e) {}

        return null;
    }

    // load
    private void load()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.WRITE_CALENDAR,
                     Manifest.permission.READ_CALENDAR}, REQUEST_READ);

                return;
            }
        }

        CharSequence text = read(getFile());
        textView.setText(text);
        changed = false;
        invalidateOptionsMenu();
        if (markdown)
            loadMarkdown();

        if (text != null)
            checkPosition(text);
    }

    // checkPosition
    private void checkPosition(CharSequence text)
    {
        // Get a pattern and a matcher for position pattern
        Matcher matcher = POSN_PATTERN.matcher(text);
        // Check pattern
        if (matcher.find())
        {
            switch (matcher.group(1))
            {
                // Start
            case "<":
                textView.setSelection(0);
                break;

                // End
            case ">":
                textView.setSelection(textView.length());
                break;

                // Saved position
            case "#":
                try
                {
                    textView.setSelection(Integer.parseInt(matcher.group(2)));
                }

                catch (Exception e)
                {
                    textView.setSelection(textView.length());
                }
                break;
            }
        }

        else
            textView.setSelection(textView.length());

        // Scroll after delay
        textView.postDelayed(() ->
        {
            // Get selection
            int selection = textView.getSelectionStart();

            // Get text position
            int line = textView.getLayout().getLineForOffset(selection);
            int position = textView.getLayout().getLineBaseline(line);

            // Scroll to it
            int height = scrollView.getHeight();
            scrollView.smoothScrollTo(0, position - height / 2);
        }, POSITION_DELAY);
    }

    // positionCheck
    private CharSequence positionCheck(CharSequence text)
    {
        // Get a pattern and a matcher for position pattern
        Matcher matcher = POSN_PATTERN.matcher(text);
        // Check pattern
        if (matcher.find())
        {
            // Save position
            if ("#".equals(matcher.group(1)))
            {
                // Create replacement
                String replace =
                        String.format(Locale.ROOT, POSN_TEMPLATE,
                                textView.getSelectionStart());
                return matcher.replaceFirst(replace);
            }
        }

        return text;
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
        {
            save();
            // Clear flag
            changed = false;
        }

        setDate(date);
        load();

        if (markdown && !shown)
        {
            animateAccept();
            shown = true;
        }

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

        // Check template
        if (useTemplate && !markdown)
            template();
    }

    // index
    private void index()
    {
        entryStack.clear();
        Calendar index = Calendar.getInstance();
        index.setTimeInMillis(indexPage);
        changeDate(index);
    }

    // addIndexLink
    private void addIndexLink()
    {
        if (!useIndex)
            return;

        Date linkDate = currEntry.getTime();
        index();

        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
        String date = format.format(linkDate);
        String title = dateCheck(template, TEMP_FORMAT, linkDate).toString();
        String dateUrl = String.format(Locale.getDefault(), LINK_FORMAT, date);
        String linkText = String.format(Locale.getDefault(), LINK_TEMPLATE,
                                        title, dateUrl);
        textView.append(linkText);

        if (markdown)
            loadMarkdown();
    }

    // dateCheck
    private CharSequence dateCheck(CharSequence text, String pattern, Date when)
    {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = TEMP_PATTERN.matcher(text);

        // Find matches
        while (matcher.find())
        {
            try
            {
                DateFormat format = new
                    SimpleDateFormat(matcher.group(1).isEmpty()?
                                     pattern: matcher.group(1),
                                     Locale.getDefault());
                // Create date
                String date = format.format(when);
                matcher.appendReplacement(buffer, date);
            }

            catch (Exception e)
            {
                alertDialog(R.string.appName, e.getMessage(),
                            android.R.string.ok);
                e.printStackTrace();
            }
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer;
    }

    // template
    private void template()
    {
        // No template if not empty
        if (textView.length() > 0)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE,
                     Manifest.permission.WRITE_CALENDAR,
                     Manifest.permission.READ_CALENDAR}, REQUEST_TEMPLATE);

                return;
            }
        }

        Calendar template = Calendar.getInstance();
        template.setTimeInMillis(templatePage);
        CharSequence text = read(getFile(template));
        text = dateCheck(text, DATE_FORMAT);
        textView.setText(text);

        if (markdown)
            loadMarkdown();
    }

    // dateCheck
    private CharSequence dateCheck(CharSequence text, String pattern)
    {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = TEMP_PATTERN.matcher(text);

        // Find matches
        while (matcher.find())
        {
            try
            {
                DateFormat format = new
                SimpleDateFormat(matcher.group(1).isEmpty()?
                                 pattern: matcher.group(1),
                                 Locale.getDefault());
                // Create date
                String date = format.format(new Date());
                matcher.appendReplacement(buffer, date);
            }

            catch (Exception e)
            {
                alertDialog(R.string.appName, e.getMessage(),
                            android.R.string.ok);
                e.printStackTrace();
            }
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer;
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
            if (type != null && type.startsWith(IMAGE))
            {
                File oldMedia = FileUtils.getFile(this, media);
                File newMedia = new File(getCurrent(), name);
                int i = 1;
                while (newMedia.exists())
                {
                    String newName = String.format
                        (Locale.getDefault(),
                         "%s_%d%s", FileUtils.getPathWithoutExtension(name),
                         i++, FileUtils.getExtension(name));
                    newMedia = new File(getCurrent(), newName);
                }

                try
                {
                    FileUtils.copyFile(oldMedia, newMedia);
                    String newName =
                        Uri.fromFile(newMedia).getLastPathSegment();
                    name = newName;
                    media = Uri.parse(newName);
                }

                catch (Exception e) {}
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

    // showToast
    void showToast(int id)
    {
        String text = getString(id);
        showToast(text);
    }

    // showToast
    void showToast(String text)
    {
        // Cancel the last one
        if (toast != null)
            toast.cancel();

        // Make a new one
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        // Fix for android 13
        View view = toast.getView();
        if (view != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
            view.setBackgroundResource(R.drawable.toast_frame);
        toast.show();
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
        Calendar nextDay = getNextCalendarDay();
        entryStack.push(currEntry);
        changeDate(nextDay);

        if (markdown && shown)
            animateSwipeLeft();
    }

    // onSwipeRight
    private void onSwipeRight()
    {
        Calendar prevDay = getPrevCalendarDay();
        entryStack.push(currEntry);
        changeDate(prevDay);

        if (markdown && shown)
            animateSwipeRight();
    }

    // onSwipeDown
    private void onSwipeDown()
    {
        Calendar prevMonth = getPrevCalendarMonth();
        entryStack.push(currEntry);
        changeDate(prevMonth);

        if (markdown && shown)
            animateSwipeRight();
    }

    // onSwipeUp
    private void onSwipeUp()
    {
        Calendar nextMonth = getNextCalendarMonth();
        entryStack.push(currEntry);
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
            CharSequence text = textView.getText();

            // Get a pattern and a matcher for delimiter characters
            Matcher matcher = PATTERN_CHARS.matcher(text);

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
                        // Check for close brackets and look for the
                        // open brackets
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

                    String string = text.toString();
                    // Do reverse search
                    start = string.lastIndexOf(c, start) + 1;

                    // Check for included newline
                    if (start > string.lastIndexOf('\n', end))
                        // Update selection
                        textView.setSelection(start, end);
                }
            }
        }
    }

    // ZipTask
    private static class ZipTask
            extends AsyncTask<Uri, Void, Void>
    {
        private WeakReference<Diary> diaryWeakReference;
        private String search;

        // ZipTask
        public ZipTask(Diary diary)
        {
            diaryWeakReference = new WeakReference<>(diary);
        }

        // onPreExecute
        @Override
        protected void onPreExecute()
        {
            final Diary diary = diaryWeakReference.get();
            if (diary != null)
                diary.showToast(R.string.start);
        }

        // doInBackground
        @Override
        protected Void doInBackground(Uri... uris)
        {
            final Diary diary = diaryWeakReference.get();
            if (diary == null)
                return null;

            File home = diary.getHome();

            // Create output stream
            try (ZipOutputStream output = new ZipOutputStream
                 (diary.getContentResolver().openOutputStream(uris[0])))
            {
                byte[] buffer = new byte[BUFFER_SIZE];

                // Get entry list
                List<File> files = new ArrayList<>();
                listFiles(home, files);

                for (File file: files)
                {
                    // Get path
                    String path = file.getPath();
                    path = path.substring(home.getPath().length() + 1);

                    if (file.isDirectory())
                    {
                        ZipEntry entry = new ZipEntry(path + File.separator);
                        entry.setMethod(ZipEntry.STORED);
                        entry.setTime(file.lastModified());
                        entry.setSize(0);
                        entry.setCompressedSize(0);
                        entry.setCrc(0);
                        output.putNextEntry(entry);
                    }

                    else if (file.isFile())
                    {
                        ZipEntry entry = new ZipEntry(path);
                        entry.setMethod(ZipEntry.DEFLATED);
                        entry.setTime(file.lastModified());
                        output.putNextEntry(entry);

                        try (BufferedInputStream input = new
                             BufferedInputStream(new FileInputStream(file)))
                        {
                            while (input.available() > 0)
                            {
                                int size = input.read(buffer);
                                output.write(buffer, 0, size);
                            }
                        }
                    }
                }

                // Close last entry
                output.closeEntry();
            }

            catch (Exception e)
            {
                diary.runOnUiThread (() ->
                    diary.alertDialog(R.string.appName, e.getMessage(),
                                      android.R.string.ok));
                e.printStackTrace();
            }

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Void noresult)
        {
            final Diary diary = diaryWeakReference.get();
            if (diary != null)
                diary.showToast(R.string.complete);
        }
    }

    // FindTask
    private static class FindTask
            extends AsyncTask<String, Void, List<String>>
    {
        private WeakReference<Diary> diaryWeakReference;
        private String search;

        // FindTask
        public FindTask(Diary diary)
        {
            diaryWeakReference = new WeakReference<>(diary);
        }

        // doInBackground
        @Override
        protected List<String> doInBackground(String... params)
        {
            // Create a list of matches
            List<String> matches = new ArrayList<>();
            final Diary diary = diaryWeakReference.get();
            if (diary == null)
                return matches;

            search = params[0];
            Pattern pattern =
                Pattern.compile(search, Pattern.CASE_INSENSITIVE |
                                Pattern.LITERAL | Pattern.UNICODE_CASE);
            // Get entry list
            List<File> entries = new ArrayList<>();
            listEntries(diary.getHome(), entries);

            DateFormat dateFormat =
                DateFormat.getDateInstance(DateFormat.MEDIUM);

            // Check the entries
            for (File file : entries)
            {
                StringBuilder content = read(file);
                Matcher matcher = pattern.matcher(content);
                if (matcher.find())
                {
                    int start = content.lastIndexOf("\n", matcher.start()) + 1;
                    int end = matcher.end();
                    boolean truncEnd = false;
                    if (start < end - FIND_SIZE)
                        start = end - FIND_SIZE;
                    if (start < 0)
                        start = 0;
                    end = content.indexOf("\n", end);
                    if (end - start > FIND_SIZE)
                    {
                        end = start + FIND_SIZE;
                        truncEnd = true;
                    }
                    if (end > content.length())
                        end = content.length();
                    String headline = content.substring(start, end);
                    if (truncEnd)
                        headline += "…";
                    matches.add (dateFormat.format(parseTime(file)) +
                                 "  " + headline);
                }
            }

            return matches;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(List<String> matches)
        {
            final Diary diary = diaryWeakReference.get();
            if (diary == null)
                return;

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(diary);
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
                        diary.changeDate(entry);

                        // Put the search text back - why it
                        // disappears I have no idea or why I have to
                        // do it after a delay
                        diary.searchView.postDelayed(() ->
                                diary.searchView.setQuery(search, false),
                                                     FIND_DELAY);
                    }

                    catch (Exception e) {}
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
                matcher = pattern.matcher(editable);
                if (matcher.find(index))
                {
                    // Get index
                    index = matcher.start();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(index);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, position - height / 2);

                    // Highlight it
                    editable.setSpan(span, matcher.start(), matcher.end(),
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
                    int line = textView.getLayout().getLineForOffset(index);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, position - height / 2);

                    // Highlight it
                    editable.setSpan(span, matcher.start(), matcher.end(),
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

            if (!multi)
                return result;

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
                        Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD)
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
            }

            catch (Exception e) {}

            multi = false;
            return result;
        }

        // onDoubleTap
        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            if (shown)
            {
                int[] l = new int[2];
                markdownView.getLocationOnScreen(l);

                // Get tap position
                float y = e.getY();
                y -= l[1];

                int scrollY = markdownView.getScrollY();
                int contentHeight = markdownView.getContentHeight();
                float density = getResources().getDisplayMetrics().density;

                // Get markdown position
                final float p = (y + scrollY) / (contentHeight * density);

                // Remove callbacks
                if (showEdit != null)
                    markdownView.removeCallbacks(showEdit);

                // Animation
                animateEdit();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                // Scroll after delay
                textView.postDelayed(() ->
                {
                    int h = textView.getLayout().getHeight();
                    int v = Math.round(h * p);

                    // Get line
                    int line = textView.getLayout().getLineForVertical(v);
                    int offset = textView.getLayout()
                        .getOffsetForHorizontal(line, 0);
                    textView.setSelection(offset);

                    // get text position
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    int height = scrollView.getHeight();
                    scrollView.smoothScrollTo(0, position - height / 2);
                }, POSITION_DELAY);

                shown = false;

                return true;
            }

            return false;
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
