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

package jpf.android.diary;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

public class Diary extends Activity {
    private final static int DATE_DIALOG = 0;

    private Calendar prevEntry;
    private Calendar currEntry;
    private Calendar nextEntry;
    private EditText text;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        text = (EditText) findViewById(R.id.text);
        if (savedInstanceState == null)
            today();
        else
            setDate(new GregorianCalendar(
                (Integer) savedInstanceState.get("year"),
                (Integer) savedInstanceState.get("month"),
                (Integer) savedInstanceState.get("day")));
    }

    protected void onSaveInstanceState(Bundle outState) {
        if (currEntry != null) {
            outState.putInt("year", currEntry.get(Calendar.YEAR));
            outState.putInt("month", currEntry.get(Calendar.MONTH));
            outState.putInt("day", currEntry.get(Calendar.DATE));
        }
        super.onSaveInstanceState(outState);
    }

    public void onPause() {
        super.onPause();
        save();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        Calendar today = GregorianCalendar.getInstance();
        menu.findItem(R.id.today).setEnabled(
            currEntry == null
            || currEntry.get(Calendar.YEAR) != today.get(Calendar.YEAR)
            || currEntry.get(Calendar.MONTH) != today.get(Calendar.MONTH)
            || currEntry.get(Calendar.DATE) != today.get(Calendar.DATE));
        menu.findItem(R.id.nextEntry).setEnabled(nextEntry != null);
        menu.findItem(R.id.prevEntry).setEnabled(prevEntry != null);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.today:
            today();
            return true;
        case R.id.prevEntry:
            changeDate(prevEntry);
            return true;
        case R.id.nextEntry:
            changeDate(nextEntry);
            return true;
        case R.id.goToDate:
            showDialog(DATE_DIALOG);
            return true;
        case R.id.copyleft:
            showText(R.string.copyright);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DATE_DIALOG:
            return new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view,
                                          int year, 
                                          int month,
                                          int day) {
                        changeDate(new GregorianCalendar(
                                       year, month, day));
                    }
                },
                currEntry.get(Calendar.YEAR),
                currEntry.get(Calendar.MONTH),
                currEntry.get(Calendar.DATE));
        }
        return null;
    }

    private void showText(int string) {
        Intent intent = new Intent(this, Text.class);
        Bundle bundle = new Bundle();
        bundle.putInt("string", string);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private File getHome() {
        return new File(
            Environment.getExternalStorageDirectory().getPath()
            + File.separator + "Diary");
    }

    private File getYear(int year) {
        return new File(
            getHome().getPath() + File.separator + year);
    }

    private File getMonth(int year, int month) {
        return new File(
            getYear(year).getPath() + File.separator
            + String.format("%02d", month + 1));
    }

    private File getDay(int year, int month, int day) {
        return new File(
            getMonth(year, month).getPath() + File.separator
            + String.format("%02d.txt", day));
    }

    private File getFile() {
        return getDay(currEntry.get(Calendar.YEAR),
                      currEntry.get(Calendar.MONTH),
                      currEntry.get(Calendar.DATE));
    }

    private static File[] sortFiles(File[] files) {
        if (files == null)
            return new File[0];
        Arrays.sort(files, new Comparator<File> () {
            public int compare(File file1, File file2) {
                return file2.getName().compareTo(file1.getName());
            }
        });
        return files;
    }

    private static File[] listYears(File home) {
        return sortFiles(home.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return Pattern.matches("^[0-9]{4}$", filename);
                }
            }));
    }

    private static File[] listMonths(File yearDir) {
        return sortFiles(yearDir.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return Pattern.matches("^[0-9]{2}$", filename);
                }
            }));
    }

    private static File[] listDays(File monthDir) {
        return sortFiles(monthDir.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return Pattern.matches(
                        "^[0-9]{2}.txt$", filename);
                }
            }));
    }

    private static int yearValue(File yearDir) {
        return Integer.parseInt(yearDir.getName());
    }

    private static int monthValue(File monthDir) {
        return Integer.parseInt(monthDir.getName()) - 1;
    }

    private static int dayValue(File dayFile) {
        return Integer.parseInt(dayFile.getName().split("\\.")[0]);
    }

    private int prevYear(int year) {
        int prev = -1;
        for (File yearDir : listYears(getHome())) {
            int n = yearValue(yearDir);
            if (n < year && n > prev)
                prev = n;
        }
        return prev;
    }

    private int prevMonth(int year, int month) {
        int prev = -1;
        for (File monthDir : listMonths(getYear(year))) {
            int n = monthValue(monthDir);
            if (n < month && n > prev)
                prev = n;
        }
        return prev;
    }

    private int prevDay(int year, int month, int day) {
        int prev = -1;
        for (File dayFile : listDays(getMonth(year, month))) {
            int n = dayValue(dayFile);
            if (n < day && n > prev)
                prev = n;
        }
        return prev;
    }

    private Calendar getPrevEntry(int year, int month, int day) {
        int prev;
        if ((prev = prevDay(year, month, day)) == -1) {
            if ((prev = prevMonth(year, month)) == -1) {
                if ((prev = prevYear(year)) == -1)
                    return null;
                return getPrevEntry(prev, Calendar.DECEMBER, 32);
            }
            return getPrevEntry(year, prev, 32);
        }
        return new GregorianCalendar(year, month, prev);
    }

    private int nextYear(int year) {
        int next = -1;
        for (File yearDir : listYears(getHome())) {
            int n = yearValue(yearDir);
            if (n > year && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    private int nextMonth(int year, int month) {
        int next = -1;
        for (File monthDir : listMonths(getYear(year))) {
            int n = monthValue(monthDir);
            if (n > month && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    private int nextDay(int year, int month, int day) {
        int next = -1;
        for (File dayFile : listDays(getMonth(year, month))) {
            int n = dayValue(dayFile);
            if (n > day && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    private Calendar getNextEntry(int year, int month, int day) {
        int next;
        if ((next = nextDay(year, month, day)) == -1) {
            if ((next = nextMonth(year, month)) == -1) {
                if ((next = nextYear(year)) == -1)
                    return null;
                return getNextEntry(next, Calendar.JANUARY, -1);
            }
            return getNextEntry(year, next, -1);
        }
        return new GregorianCalendar(year, month, next);
    }

    private void save() {
        if (currEntry != null) {
            String string = text.getText().toString();
            File file = getFile();
            if (string.length() == 0) {
                if (file.exists())
                    file.delete();
                File parent = file.getParentFile();
                if (parent.exists() && parent.list().length == 0) {
                    parent.delete();
                    File grandParent = parent.getParentFile();
                    if (grandParent.exists()
                        && grandParent.list().length == 0)
                        grandParent.delete();
                }
            } else {
                file.getParentFile().mkdirs();
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(string);
                    fileWriter.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static String read(File file) {
        StringBuilder text = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(file);
            char buf[] = new char[1024];
            int n;
            while ((n = fileReader.read(buf)) != -1)
                text.append(String.valueOf(buf, 0, n));
            fileReader.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return text.toString();
    }

    private void load() {
        text.setText(read(getFile()));
        text.setSelection(0);
    }

    private void setDate(Calendar date) {
        setTitle(DateFormat.getDateInstance(DateFormat.FULL)
                           .format(new Date(date.getTimeInMillis())));
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DATE);
        prevEntry = getPrevEntry(year, month, day);
        currEntry = date;
        nextEntry = getNextEntry(year, month, day);
    }

    private void changeDate(Calendar date) {
        save();
        setDate(date);
        load();
    }

    private void today() {
        changeDate(GregorianCalendar.getInstance());
    }
}