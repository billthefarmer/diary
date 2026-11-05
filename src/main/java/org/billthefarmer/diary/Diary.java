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

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.text.Editable;
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
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import android.support.v4.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import androidx.annotation.NonNull;

import org.billthefarmer.markdown.MarkdownView;
import org.billthefarmer.view.CustomCalendarDialog;
import org.billthefarmer.view.CustomCalendarView;
import org.billthefarmer.view.DayDecorator;
import org.billthefarmer.view.DayView;

// Diary
@SuppressWarnings("deprecation")
public class Diary extends Activity
    implements DatePickerDialog.OnDateSetListener,
    CustomCalendarDialog.OnDateSetListener {
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

    public static final int LIGHT = 0;
    public static final int DARK = 1;
    public static final int SYSTEM = 2;

    public static final int VERSION_CODE_S_V2 = 32;

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
    public final static Pattern CHECK_PATTERN = Pattern.compile
            ("^\\s*(?:[-+*]|\\d+\\.)\\s+\\[(X|x|\\s)\\]\\s+(?=\\p{Graph}+)",
                    Pattern.MULTILINE);

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
    private boolean extended = true;
    private boolean external = false;
    private boolean useIndex = false;
    private boolean useTemplate = false;
    private boolean copyMedia = false;
    private boolean haveMedia = false;

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

    private int theme = 0;

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

        private final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501;
        private static final int REQUEST_STORAGE_PERMISSIONS = 123;
        private static final int REQUEST_MEDIA_PERMISSIONS = 456;

        private void checkPermissions() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                //As the device is Android 13 and above so I want the permission of accessing Audio, Images, Videos
                //You can ask permission according to your requirements what you want to access.
                String audioPermission = Manifest.permission.READ_MEDIA_AUDIO;
                String imagesPermission = Manifest.permission.READ_MEDIA_IMAGES;
                String videoPermission = Manifest.permission.READ_MEDIA_VIDEO;
                // Check for permissions and request them if needed
                if (ContextCompat.checkSelfPermission(Diary.this, audioPermission) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(Diary.this, imagesPermission) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(Diary.this, videoPermission) == PackageManager.PERMISSION_GRANTED) {
                    // You have the permissions, you can proceed with your media file operations.
                    //Showing dialog when Show Dialog button is clicked.
                    // dialog.show();
                } else {
                    // You don't have the permissions. Request them.
                    ActivityCompat.requestPermissions(Diary.this, new String[]{audioPermission, imagesPermission, videoPermission}, REQUEST_MEDIA_PERMISSIONS);
                }
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            switch (requestCode)
            {
            case REQUEST_STORAGE_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissions were granted. You can proceed with your file operations.
                    //Showing dialog when Show Dialog button is clicked.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        //Android version is 11 and above so to access all types of files we have to give
                        //special permission so show user a dialog..
                        accessAllFilesPermissionDialog();
                    } else {
                        //Android version is 10 and below so need of special permission...
                        // dialog.show();
                    }

                }
                break;

            case REQUEST_MEDIA_PERMISSIONS:   //This conditions only works on Android 13 and above versions
                if (grantResults.length > 0 && areAllPermissionsGranted(grantResults)) {
                    // Permissions were granted. You can proceed with your media file operations.
                    //Showing dialog when Show Dialog button is clicked.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        //Android version is 11 and above so to access all types of files we have to give
                        //special permission so show user a dialog..
                        accessAllFilesPermissionDialog();
                    }
                }
                break;

            case REQUEST_WRITE:
                for (int i = 0; i < grantResults.length; i++)
                        save();
                break;

            case REQUEST_READ:
                for (int i = 0; i < grantResults.length; i++)
                        load();
                break;

            case REQUEST_TEMPLATE:
                for (int i = 0; i < grantResults.length; i++)
                        template();
                break;
            }
        }


        private boolean areAllPermissionsGranted(int[] grantResults) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }



        // @RequiresApi(api = Build.VERSION_CODES.R)
        private void accessAllFilesPermissionDialog() {

            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("This app needs all files access permissions to view files from your storage. Clicking on OK will redirect you to new window were you have to enable the option.")
                    .setPositiveButton("OK", (myDialog, which) -> {
                        // Request permissions when the user clicks OK.
                        Intent intent = new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                        startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", (myDialog, which) -> {
                        myDialog.dismiss();
                        // Handle the case where the user cancels the permission request.
                        // dialog.show();
                    })
                    .show();
        }




    // sortFiles
    public static DocumentFile[] sortFiles(DocumentFile[] files)
    {
        if (files == null)
            return new DocumentFile[0];

        // compare
        Arrays.sort(files, (file1, file2) ->
                file2.getName().compareTo(file1.getName()));

        return files;
    }

    // listYears
    public static DocumentFile[] listYears(DocumentFile home)
    {
        if (home == null || !home.isDirectory())
            return new DocumentFile[0];
        return sortFiles(home.listFiles(), YEAR_DIR);
    }

    // listMonths
    public static DocumentFile[] listMonths(DocumentFile yearDir)
    {
        if (yearDir == null || !yearDir.isDirectory())
            return new DocumentFile[0];
        return sortFiles(yearDir.listFiles(), MONTH_DIR);
    }

    // listDays
    public static DocumentFile[] listDays(DocumentFile monthDir)
    {
        if (monthDir == null || !monthDir.isDirectory())
            return new DocumentFile[0];

        return sortFiles(monthDir.listFiles(), DAY_FILE);
    }

    // sortFiles
    private static DocumentFile[] sortFiles(
            DocumentFile[] files, String pattern)
    {
        return Arrays.stream(files)
                .filter(file -> file.getName() != null
                        && file.getName().matches(pattern))
                .sorted(Comparator.comparing(DocumentFile::getName))
                .toArray(DocumentFile[]::new);
    }

    // yearValue
    public static int yearValue(DocumentFile yearDir) {
        return Integer.parseInt(yearDir.getName());
    }

    // monthValue
    public static int monthValue(DocumentFile monthDir) {
        return Integer.parseInt(monthDir.getName()) - 1;
    }

    // dayValue
    public static int dayValue(DocumentFile dayFile) {
        return Integer.parseInt(dayFile.getName().split("\\.")[0]);
    }



    // parseTime
    public static long parseTime(DocumentFile file)
    {
        try
        {
            // Get directory names
            DocumentFile monthDir = file.getParentFile();
            DocumentFile yearDir = monthDir.getParentFile();

            int year = Integer.parseInt(yearDir.getName());
            int month = Integer.parseInt(monthDir.getName()) - 1;

            // Get day from filename
            String name = file.getName();
            int dot = name.lastIndexOf('.');

            if (dot >= 0)
                name = name.substring(0, dot);

            int day = Integer.parseInt(name);

            return new GregorianCalendar(
                    year, month, day).getTimeInMillis();
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    // listEntries
    public static void listEntries(DocumentFile directory,
                                   List<DocumentFile> fileList)
    {
        if (directory == null || !directory.isDirectory())
            return;

        // Get all entries from a directory
        DocumentFile[] files = directory.listFiles();

        // Sort files, reverse order
        Arrays.sort(files, (a, b) ->
                b.getName().compareTo(a.getName()));

        for (DocumentFile file : files)
        {
            String name = file.getName();

            if (file.isFile() && name != null && name.matches(DAY_FILE))
                fileList.add(file);

            else if (file.isDirectory())
                listEntries(file, fileList);
        }
    }

    // listFiles
    public static void listFiles(DocumentFile directory,
                                 List<DocumentFile> fileList)
    {
        if (directory == null || !directory.isDirectory())
            return;

        // Get all entries from a directory
        DocumentFile[] files = directory.listFiles();

        for (DocumentFile file : files)
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get preferences
        getPreferences();

        Configuration config = getResources().getConfiguration();
        int night = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        android.util.Log.d("LIFECYCLE",
                "theme=" + theme
                        + ", uiMode=" + config.uiMode
                        + ", night=" + night);

        switch (theme) {
            case LIGHT:
                setTheme(R.style.AppTheme);
                break;

            case DARK:
                setTheme(R.style.AppDarkTheme);
                break;

            case SYSTEM:
                switch (night) {
                    case Configuration.UI_MODE_NIGHT_NO:
                        setTheme(R.style.AppTheme);
                        break;

                    case Configuration.UI_MODE_NIGHT_YES:
                        setTheme(R.style.AppDarkTheme);
                        break;
                }
                break;
        }
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

        if (folder == null || folder.isEmpty() || folder.equals(DIARY)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(i, 9999);
            }
        }


        // Check startup
        if (savedInstanceState == null) {
            Intent intent = getIntent();

            // Check index and start from launcher
            if (useIndex && Intent.ACTION_MAIN.equals(intent.getAction()))
                index();

                // Check start from widget
            else if (intent.hasExtra(DiaryWidgetProvider.ENTRY))
                changeDate(intent.getLongExtra(DiaryWidgetProvider.ENTRY,
                        new Date().getTime()));
                // Set the date
            else
                today();

            // Check for sent media
            mediaCheck(intent);
        }
    }


    @Override
    protected void onDestroy() {
        android.util.Log.e(
                "LIFECYCLE",
                "onDestroy: isFinishing=" + isFinishing()
                        + ", isChangingConfigurations=" + isChangingConfigurations()
        );

        super.onDestroy();
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
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
    protected void onResume() {
        super.onResume();

        int last = theme;

        // Get preferences
        getPreferences();

        // Recreate
        if (last != theme)
            recreate();

        // Set date
        setDate(currEntry);

        // Reload if modified
        DocumentFile file = getFile();

        if (file != null && file.exists() && file.lastModified() > saved)
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        markdownView.saveState(outState);

        if (currEntry != null) {
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
    public void onPause() {
        super.onPause();

        if (changed) {
            save();
            // Clear flag
            changed = false;
        }

        DocumentFile file = getFile();
        saved = (file != null && file.exists()) ? file.lastModified() : 0;
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

        // Set up search view
        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();

        // Set up search view options and listener
        if (searchView != null) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.prev) {
            onBackPressedDispatcher();
            return true;
        } else if (id == R.id.cancel) {
            onBackPressedDispatcher();
            return true;
        } else if (id == R.id.prevEntry) {
            prevEntry();
            return true;
        } else if (id == R.id.nextEntry) {
            nextEntry();
            return true;
        } else if (id == R.id.today) {
            today();
            return true;
        } else if (id == R.id.goToDate) {
            goToDate(currEntry);
            return true;
        } else if (id == R.id.index) {
            index();
            return true;
        } else if (id == R.id.link) {
            addIndexLink();
            return true;
        } else if (id == R.id.findAll) {
            findAll();
            return true;
        } else if (id == R.id.print) {
            print();
            return true;
        } else if (id == R.id.share) {
            share();
            return true;
        } else if (id == R.id.addTime) {
            addTime();
            return true;
        } else if (id == R.id.addEvents) {
            addEvents();
            return true;
        } else if (id == R.id.addMedia) {
            addMedia();
            return true;
        } else if (id == R.id.editStyles) {
            editStyles();
            return true;
        } else if (id == R.id.editScript) {
            editScript();
            return true;
        } else if (id == R.id.backup) {
            backup();
            return true;
        } else if (id == R.id.settings) {
            settings();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }


            // Close text search
            //if (searchItem.isActionViewExpanded() && item.getItemId() != R.id.findAll)
            //    searchItem.collapseActionView();
            //return true;
        }


    // onBackPressed
    // @Override
    public void onBackPressedDispatcher()    {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
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

        case 9999:
            // Save selected path
            if (data != null) {
                SharedPreferences pref =
                        PreferenceManager.getDefaultSharedPreferences(this);

                Uri treeUri = data.getData();

                if (treeUri != null) {
                    int flags = data.getFlags();

                    boolean canRead =
                            (flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0;

                    boolean canWrite =
                            (flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0;

                    if (canRead && canWrite) {
                        getContentResolver().takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                    else if (canRead) {
                        getContentResolver().takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    else if (canWrite) {
                        getContentResolver().takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }

                    pref.edit()
                            .putString(Settings.PREF_FOLDER, treeUri.toString())
                            .apply();
                }
            }

            Log.d("DiaryFolder", "Gespeicherter Ordner: " +
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .getString(Settings.PREF_FOLDER, DIARY));

            break;

        case APP_STORAGE_ACCESS_REQUEST_CODE:
            // Permission granted. Now resume your workflow.
            // dialog.show();
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

            // On click
            textView.setOnClickListener(v ->
            {
                // Get selection
                int selection = textView.getSelectionStart();

                // Get text position
                int line = textView.getLayout().getLineForOffset(selection);
                int start = textView.getLayout().getLineStart(line);
                int end = textView.getLayout().getLineEnd(line);

                // Match checkbox pattern
                Matcher matcher = CHECK_PATTERN.matcher(textView.getText());
                if (matcher.region(start, end).find() &&
                    matcher.end() >= selection)
                {
                    Editable editable = textView.getEditableText();
                    switch (editable.charAt(matcher.start(1)))
                    {
                    case 'x':
                    case 'X':
                        editable.replace(matcher.start(1), matcher.end(1), " ");
                        break;

                    case ' ':
                        editable.replace(matcher.start(1), matcher.end(1), "x");
                        break;
                    }
                }
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

        theme = Integer.parseInt(preferences.getString(Settings.PREF_THEME,
                                                       "0"));

        copyMedia = preferences.getBoolean(Settings.PREF_COPY_MEDIA, false);
        custom = preferences.getBoolean(Settings.PREF_CUSTOM, true);
        extended = preferences.getBoolean(Settings.PREF_EXTENDED, true);
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
        String baseUrl = getBaseUrl();

        android.util.Log.d("DIARY_TEST", "Base URL: " + baseUrl);
        android.util.Log.d("DIARY_TEST", "Text length: " +
                (text == null ? -1 : text.length()));
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
            String name = matcher.group(2);
            String extension = MimeTypeMap.getFileExtensionFromUrl(name);
            String type = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));

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
            DocumentFile file = getDay(calendar.get(Calendar.YEAR),
                               calendar.get(Calendar.MONTH),
                               calendar.get(Calendar.DATE));

            // Get uri
            Uri uri = file.getUri();

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
                {
                    String title = intent.hasExtra(Intent.EXTRA_TITLE)?
                        intent.getStringExtra(Intent.EXTRA_TITLE):
                        intent.getStringExtra(Intent.EXTRA_SUBJECT);
                    addLink(uri, title, true);
                }

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

                String title = intent.hasExtra(Intent.EXTRA_TITLE)?
                    intent.getStringExtra(Intent.EXTRA_TITLE):
                    intent.getStringExtra(Intent.EXTRA_SUBJECT);
                addLink(uri, title, true);
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

    private String getBaseUrl() {
        DocumentFile current = getCurrent();
        if (current == null)
            return "";

        return current.getUri().toString() + "/";
    }

    // getCurrent
    private DocumentFile getCurrent()
    {
        return findMonth(currEntry.get(Calendar.YEAR),
                        currEntry.get(Calendar.MONTH));
    }

    // getStyles
    private String getStyles()
    {
        DocumentFile home = getHome();

        if (home != null)
        {
            DocumentFile cssFolder = home.findFile("css");

            if (cssFolder != null)
            {
                DocumentFile cssFile = cssFolder.findFile("styles.css");

                if (cssFile != null && cssFile.exists())
                    return cssFile.getUri().toString();
            }
        }

        return STYLES;
    }

    // getScript
    private String getScript()
    {
        DocumentFile home = getHome();

        if (home != null)
        {
            DocumentFile jsFolder = home.findFile("js");

            if (jsFolder != null)
            {
                DocumentFile jsFile = jsFolder.findFile("script.js");

                if (jsFile != null && jsFile.exists())
                    return jsFile.getUri().toString();
            }
        }

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
    // share
    private void share()
    {
        Intent intent = new Intent(Intent.ACTION_SEND);

        String title = String.format("%s: %s",
                getString(R.string.appName), getTitle());

        intent.putExtra(Intent.EXTRA_TITLE, title);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);

        if (shown)
        {
            // Share rendered image
            intent.setType(IMAGE_PNG);

            markdownView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(markdownView.getDrawingCache());
            markdownView.setDrawingCacheEnabled(false);

            File image = new File(getCacheDir(), DIARY_IMAGE);

            try (FileOutputStream out = new FileOutputStream(image))
            {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }

            Uri imageUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER, image);

            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.putExtra(Intent.EXTRA_TEXT,
                    textView.getText().toString());

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newUri(
                    getContentResolver(), "Diary image", imageUri));
        }
        else
        {
            // Share diary entry
            intent.setType(TEXT_PLAIN);

            DocumentFile file = getFile();

            if (file != null && file.exists())
            {
                Uri fileUri = file.getUri();

                intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(ClipData.newUri(
                        getContentResolver(), "Diary entry", fileUri));
            }

            intent.putExtra(Intent.EXTRA_TEXT,
                    textView.getText().toString());
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
        DocumentFile home = getHome();

        if (home == null)
            return;

        DocumentFile cssFolder = home.findFile("css");

        if (cssFolder == null)
            cssFolder = home.createDirectory("css");

        if (cssFolder == null)
            return;

        DocumentFile file = cssFolder.findFile("styles.css");

        if (file == null)
            file = cssFolder.createFile("text/css", "styles.css");

        if (file == null)
            return;

        Uri uri = file.getUri();

        Intent intent = new Intent(Intent.ACTION_EDIT, uri, this, Editor.class);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        startActivityForResult(intent, EDIT_STYLES);
    }
    /// editScript
    private void editScript()
    {
        DocumentFile home = getHome();

        if (home == null)
            return;

        DocumentFile jsFolder = home.findFile("js");

        if (jsFolder == null)
            jsFolder = home.createDirectory("js");

        if (jsFolder == null)
            return;

        DocumentFile file = jsFolder.findFile("script.js");

        if (file == null)
            file = jsFolder.createFile("text/javascript", "script.js");

        if (file == null)
            return;

        Uri uri = file.getUri();

        Intent intent = new Intent(Intent.ACTION_EDIT, uri, this, Editor.class);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        startActivityForResult(intent, EDIT_STYLES);
    }

    // backup
    private void backup()
    {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(APPLICATION_ZIP);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, ZIP);

        startActivityForResult(intent, CREATE_BACKUP);
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
    private DocumentFile getHome()
    {
        if (folder == null || folder.isEmpty() || folder.equals(DIARY))
            return null;

        try
        {
            Uri uri = Uri.parse(folder);

            if (!"content".equals(uri.getScheme()))
                return null;

            return DocumentFile.fromTreeUri(this, uri);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

   // findYear
    private DocumentFile findYear(int year)
    {
        DocumentFile home = getHome();

        if (home == null)
            return null;

        String name = String.format(Locale.ENGLISH, YEAR_FORMAT, year);

        return home.findFile(name);
    }

    // getOrCreateYear
    private DocumentFile getOrCreateYear(int year)
    {
        DocumentFile home = getHome();

        if (home == null)
            return null;

        String name = String.format(Locale.ENGLISH, YEAR_FORMAT, year);

        DocumentFile dir = home.findFile(name);

        if (dir == null)
            dir = home.createDirectory(name);

        return dir;
    }

    // findMonth
    private DocumentFile findMonth(int year, int month)
    {
        DocumentFile yearFolder = findYear(year);

        if (yearFolder == null)
            return null;

        String name = String.format(Locale.ENGLISH,
                MONTH_FORMAT, month + 1);

        return yearFolder.findFile(name);
    }

    // getOrCreateMonth
    private DocumentFile getOrCreateMonth(int year, int month)
    {
        DocumentFile yearFolder = getOrCreateYear(year);

        if (yearFolder == null)
            return null;

        String name = String.format(Locale.ENGLISH,
                MONTH_FORMAT, month + 1);

        DocumentFile dir = yearFolder.findFile(name);

        if (dir == null)
            dir = yearFolder.createDirectory(name);

        return dir;
    }

    // getDay
    private DocumentFile getDay(int year, int month, int day)
    {
        DocumentFile monthFolder = findMonth(year, month);

        if (monthFolder == null)
            return null;

        String name = String.format(Locale.ENGLISH, DAY_FORMAT, day);
        DocumentFile file = monthFolder.findFile(name);

        if (file != null)
            return file;

        if (markdown)
        {
            name = String.format(Locale.ENGLISH, MD_FORMAT, day);
            return monthFolder.findFile(name);
        }

        return null;
    }

    // getFile
    private DocumentFile getFile()
    {
        return getFile(currEntry);
    }

    private DocumentFile getFile(Calendar entry)
    {
        return getDay(
                entry.get(Calendar.YEAR),
                entry.get(Calendar.MONTH),
                entry.get(Calendar.DATE));
    }

    // read
    public StringBuilder read(DocumentFile file) {
        return read(this, file);
    }

    public static StringBuilder read(
            android.content.Context context,
            DocumentFile file)
    {
        StringBuilder text = new StringBuilder();

        if (file == null || !file.exists() || !file.isFile())
            return text;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        context.getContentResolver()
                                .openInputStream(file.getUri()),
                        StandardCharsets.UTF_8)))
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
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return text;
    }

    // prevYear
    private int prevYear(int year)
    {
        int prev = -1;
        for (DocumentFile yearDir : listYears(getHome()))
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
        for (DocumentFile monthDir : listMonths(findYear(year)))
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
        for (DocumentFile dayFile : listDays(findMonth(year, month)))
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
        for (DocumentFile yearDir : listYears(getHome()))
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
        for (DocumentFile monthDir : listMonths(findYear(year)))
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
        for (DocumentFile dayFile : listDays(findMonth(year, month)))
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
        DocumentFile home = getHome();
        if (home == null)
            return null;

        String homeUri = home.getUri().toString();

        // Check url
        if (url == null || !url.startsWith(homeUri))
            return null;

        // Get document
        Uri uri = Uri.parse(url);
        DocumentFile file = DocumentFile.fromSingleUri(this, uri);

        // Check file
        if (file == null || !file.exists() || !file.isFile())
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

    // save
    private void save()
    {
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
        DocumentFile file = getFile();

        DocumentFile parent = getOrCreateMonth(
                currEntry.get(Calendar.YEAR),
                currEntry.get(Calendar.MONTH));

        if (parent == null)
        {
            alertDialog(R.string.appName,
                    "Der Tagebuchordner konnte nicht geöffnet werden.",
                    android.R.string.ok);
            return;
        }

        if (text.length() == 0)
        {
            // Delete file
            if (file != null && file.exists())
                file.delete();

            // Delete empty month folder
            if (parent.exists() && parent.listFiles().length == 0)
            {
                parent.delete();

                // Delete empty year folder
                DocumentFile yearFolder = findYear(
                        currEntry.get(Calendar.YEAR));

                if (yearFolder != null
                        && yearFolder.exists()
                        && yearFolder.listFiles().length == 0)
                    yearFolder.delete();
            }
        }
        else
        {
            // Create file if it does not exist
            if (file == null || !file.exists())
            {
                String name = String.format(Locale.ENGLISH,
                        markdown ? MD_FORMAT : DAY_FORMAT,
                        currEntry.get(Calendar.DATE));

                String mimeType = markdown ? "text/markdown" : "text/plain";

                file = parent.createFile(mimeType, name);
            }

            if (file == null)
                return;

            try (OutputStream output = getContentResolver()
                    .openOutputStream(file.getUri(), "wt");
                 OutputStreamWriter writer = new OutputStreamWriter(
                         output, StandardCharsets.UTF_8))
            {
                writer.append(text);
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
    private void updateWidgets(CharSequence text)
    {
        // Get manager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName
            (this, DiaryWidgetProvider.class);

        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(provider);

        Intent broadcast = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        broadcast.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(broadcast);
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

    private void load()
    {
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

    // changeDate
    private void changeDate(long entry)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(entry);
        Calendar date = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                              calendar.get(Calendar.MONTH),
                                              calendar.get(Calendar.DATE));
        changeDate(date);
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
                return;
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
    // addMedia
    private void addMedia(Uri media, boolean append)
    {
        String name = getMediaName(media);

        // Copy media file to diary folder
        // TODO: as for now, only for images because video and audio
        // are too time-consuming to be copied on the main thread
        if (copyMedia)
        {
            // Get type
            String type = FileUtils.getMimeType(this, media);

            if (type != null && type.startsWith(IMAGE))
            {
                DocumentFile current = getCurrent();

                if (current != null)
                {
                    // Find unused filename
                    String newName = name;
                    int i = 1;

                    while (current.findFile(newName) != null)
                    {
                        newName = String.format(
                                Locale.getDefault(),
                                "%s_%d%s",
                                FileUtils.getPathWithoutExtension(name),
                                i++,
                                FileUtils.getExtension(name));
                    }

                    // Create destination file
                    DocumentFile newMedia =
                            current.createFile(type, newName);

                    if (newMedia != null)
                    {
                        try (InputStream input = getContentResolver()
                                .openInputStream(media);
                             OutputStream output = getContentResolver()
                                     .openOutputStream(newMedia.getUri(), "w"))
                        {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int size;

                            while ((size = input.read(buffer)) != -1)
                                output.write(buffer, 0, size);

                            // Keep relative reference as before
                            name = newName;
                            media = Uri.parse(newName);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        String mediaText = String.format(
                MEDIA_TEMPLATE, name, media.toString());

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

    // getMediaName
    private String getMediaName(Uri uri)
    {
        String name = null;

        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[] { OpenableColumns.DISPLAY_NAME },
                null, null, null))
        {
            if (cursor != null && cursor.moveToFirst())
                name = cursor.getString(0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (name == null)
            name = uri.getLastPathSegment();

        return name;
    }

    // addLink
    private void addLink(Uri uri, String title, boolean append)
    {
        if ((title == null) || (title.length() == 0))
            title = uri.getLastPathSegment();

        String url = uri.toString();
        addLinkDialog(url, title, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                EditText text = ((Dialog) dialog).findViewById(R.id.pathText);
                String string = text.getText().toString();

                // Ignore empty string
                if (string.isEmpty())
                    return;

                String linkText = String.format(LINK_TEMPLATE, string, url);

                if (append)
                    textView.append(linkText);

                else
                {
                    Editable editable = textView.getEditableText();
                    int position = textView.getSelectionStart();
                    editable.insert(position, linkText);
                }

                loadMarkdown();
                break;
            }
        });
    }

    // addLinkDialog
    private void addLinkDialog(String url, String title,
                               DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.addLink);
        builder.setMessage(url);

        // Add the buttons
        builder.setPositiveButton(R.string.addLink, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);

        // Create edit text
        LayoutInflater inflater = (LayoutInflater) builder.getContext()
            .getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.save_path, null);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.show();
        TextView text = dialog.findViewById(R.id.pathText);
        text.setHint("");
        text.setText(title);
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
        if (view != null && Build.VERSION.SDK_INT > VERSION_CODE_S_V2)
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
        if (!shown && extended)
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
        private boolean success = false;

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

            if (diary == null || uris.length == 0)
                return null;

            DocumentFile home = diary.getHome();

            if (home == null || !home.exists())
                return null;

            try (ZipOutputStream output = new ZipOutputStream(
                    diary.getContentResolver().openOutputStream(uris[0], "w")))
            {
                byte[] buffer = new byte[BUFFER_SIZE];

                // Add all files and directories recursively
                zipDirectory(diary, home, "", output, buffer);
            }
            catch (Exception e)
            {
                diary.runOnUiThread(() ->
                        diary.alertDialog(R.string.appName, e.getMessage(),
                                android.R.string.ok));

                e.printStackTrace();
            }

            return null;
        }

        // zipDirectory
        private void zipDirectory(Diary diary,
                                  DocumentFile directory,
                                  String path,
                                  ZipOutputStream output,
                                  byte[] buffer) throws IOException
        {
            for (DocumentFile file : directory.listFiles())
            {
                String name = path + file.getName();

                if (file.isDirectory())
                {
                    // Add directory entry
                    ZipEntry entry = new ZipEntry(name + "/");

                    if (file.lastModified() > 0)
                        entry.setTime(file.lastModified());

                    output.putNextEntry(entry);
                    output.closeEntry();

                    // Process directory contents
                    zipDirectory(diary, file, name + "/", output, buffer);
                }
                else if (file.isFile())
                {
                    // Add file entry
                    ZipEntry entry = new ZipEntry(name);

                    if (file.lastModified() > 0)
                        entry.setTime(file.lastModified());

                    output.putNextEntry(entry);

                    try (BufferedInputStream input = new BufferedInputStream(
                            diary.getContentResolver()
                                    .openInputStream(file.getUri())))
                    {
                        int size;

                        while ((size = input.read(buffer)) != -1)
                            output.write(buffer, 0, size);
                    }

                    output.closeEntry();
                }
            }
        }


        @Override
        protected void onPostExecute(Void noresult)
        {
            final Diary diary = diaryWeakReference.get();

            if (diary != null)
                diary.showToast(success ? R.string.complete : R.string.error);
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
            List<DocumentFile> entries = new ArrayList<>();
            listEntries(diary.getHome(), entries);

            DateFormat dateFormat =
                    DateFormat.getDateInstance(DateFormat.MEDIUM);

            // Check the entries
            for (DocumentFile file : entries)
            {
                StringBuilder content = diary.read(file);
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
            else if (matcher != null)
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
            DocumentFile dayFile = getDay(
                    cellDate.get(Calendar.YEAR),
                    cellDate.get(Calendar.MONTH),
                    cellDate.get(Calendar.DATE));

            if (dayFile != null && dayFile.exists())
                dayView.setBackgroundResource(R.drawable.diary_entry);
        }
    }
}
