
package org.billthefarmer.diary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.markdownj.MarkdownProcessor;

/**
 * @author Feras Alnatsheh
 */
public class MarkdownView extends WebView
{
    private static final String TAG = "MarkdownView";

    public MarkdownView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MarkdownView(Context context)
    {
        this(context, null);
    }

    /**
     * Loads the given Markdown text to the view as rich formatted
     * HTML. The HTML output will be styled based on the given CSS
     * file.
     *
     * @param baseUrl
     *            - the URL to use as the page's base URL
     * @param txt
     *            - input in markdown format
     * @param cssFileUrl
     *            - a URL to css File. If the file located in the project assets
     *            folder then the URL should start with "file:///android_asset/"
     */
    public void loadMarkdown(String baseUrl, String txt, String cssFileUrl)
    {
        loadMarkdownToView(baseUrl, txt, cssFileUrl);
    }

    /**
     * Loads the given Markdown text to the view as rich formatted
     * HTML. The HTML output will be styled based on the given CSS
     * file.
     *
     * @param txt
     *            - input in markdown format
     * @param cssFileUrl
     *            - a URL to css File. If the file located in the project assets
     *            folder then the URL should start with "file:///android_asset/"
     */
    public void loadMarkdown(String txt, String cssFileUrl)
    {
        loadMarkdown(null, txt, cssFileUrl);
    }

    /**
     * Loads the given Markdown text to the view as rich formatted HTML.
     *
     * @param txt
     *            - input in Markdown format
     */
    public void loadMarkdown(String txt)
    {
        loadMarkdown(txt, null);
    }

    /**
     * Loads the given Markdown file to the view as rich formatted
     * HTML. The HTML output will be styled based on the given CSS
     * file.
     *
     * @param url
     *            - a URL to the Markdown file. If the file located in the
     *            project assets folder then the URL should start with
     *            "file:///android_asset/"
     * @param cssFileUrl
     *            - a URL to css File. If the file located in the project assets
     *            folder then the URL should start with "file:///android_asset/"
     */
    public void loadMarkdownFile(String url, String cssFileUrl)
    {
        new LoadMarkdownUrlTask().execute(url, cssFileUrl);
    }

    public void loadMarkdownFile(String url)
    {
        loadMarkdownFile(url, null);
    }

    public void setZoom(boolean zoom)
    {
        WebSettings settings = getSettings();
        settings.setBuiltInZoomControls(zoom);
    }

    private String readFileFromAsset(String fileName)
    {
        try
        {
            InputStream input =  getContext().getAssets().open(fileName);
            try
            {
                BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(input));
                StringBuilder content = new StringBuilder(input.available());
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

    private class LoadMarkdownUrlTask
        extends AsyncTask<String, Integer, String>
    {
        private String cssFileUrl;

        protected String doInBackground(String... params)
        {
            try
            {
                String markdown = "";
                String url = params[0];
                this.cssFileUrl = params[1];
                if(URLUtil.isNetworkUrl(url))
                {
                    markdown = HttpHelper.get(url).getResponseMessage();
                }

                else if (URLUtil.isAssetUrl(url))
                {
                    markdown =
                        readFileFromAsset(url
                                          .substring("file:///android_asset/"
                                                     .length(),
                                                     url.length()));
                }

                else
                {
                    throw
                        new IllegalArgumentException("The URL string provided is not a network or asset URL.");
                }

                return markdown;
            }

            catch (Exception e)
            {
                Log.d(TAG, "Error Loading Markdown File.", e);
                return null;
            }
        }

        protected void onProgressUpdate(Integer... progress)
        {
            // no-op
        }

        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                loadMarkdownToView(null, result, cssFileUrl);
            }

            else
            {
                loadUrl("about:blank");
            }
        }
    }

    private void loadMarkdownToView(String baseUrl, String txt,
                                    String cssFileUrl)
    {
        MarkdownProcessor mark = new MarkdownProcessor();
        String html = mark.markdown(txt);
        if (cssFileUrl != null)
        {
            html = "<link rel='stylesheet' type='text/css' href='" +
                cssFileUrl + "' />\n" + html;
        }

        loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

}
