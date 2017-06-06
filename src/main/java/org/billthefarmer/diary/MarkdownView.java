/*
 * Copyright 2011 Feras Alnatsheh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.billthefarmer.diary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebView;

import org.markdownj.MarkdownProcessor;

/**
 * @author Feras Alnatsheh
 */
// MarkdownView
public class MarkdownView extends WebView
{
    private static final String TAG = "MarkdownView";

    private static final String CSS =
        "<link rel='stylesheet' type='text/css' href='%s' />\n%s";

    // MarkdownView
    public MarkdownView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    // MarkdownView
    public MarkdownView(Context context)
    {
        super(context);
    }

    /**
     * Loads the given Markdown text to the view as rich formatted
     * HTML. The HTML output will be styled based on the given CSS
     * file.
     *
     * @param baseUrl
     *            - the URL to use as the page's base URL
     * @param text
     *            - input in markdown format
     * @param cssFileUrl
     *            - a URL to css File. If the file located in the project assets
     *            folder then the URL should start with "file:///android_asset/"
     */
    public void loadMarkdown(String baseUrl, String text, String cssFileUrl)
    {
        loadMarkdownToView(baseUrl, text, cssFileUrl);
    }

    /**
     * Loads the given Markdown text to the view as rich formatted
     * HTML. The HTML output will be styled based on the given CSS
     * file.
     *
     * @param text
     *            - input in markdown format
     * @param cssFileUrl
     *            - a URL to css File. If the file located in the project assets
     *            folder then the URL should start with "file:///android_asset/"
     */
    public void loadMarkdown(String text, String cssFileUrl)
    {
        loadMarkdown(null, text, cssFileUrl);
    }

    /**
     * Loads the given Markdown text to the view as rich formatted HTML.
     *
     * @param text
     *            - input in Markdown format
     */
    public void loadMarkdown(String text)
    {
        loadMarkdown(text, null);
    }

    /**
     * Loads the given Markdown file to the view as rich formatted
     * HTML. The HTML output will be styled based on the given CSS
     * file.
     *
     * @param baseUrl
     *            - the URL to use as the page's base URL
     * @param url
     *            - a URL to the Markdown file. If the file located in the
     *            project assets folder then the URL should start with
     *            "file:///android_asset/"
     * @param cssFileUrl
     *            - a URL to css File. If the file located in the project assets
     *            folder then the URL should start with "file:///android_asset/"
     */
    public void loadMarkdownFile(String baseUrl, String url, String cssFileUrl)
    {
        new LoadMarkdownUrlTask().execute(baseUrl, url, cssFileUrl);
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
        loadMarkdownFile(null, url, cssFileUrl);
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
     */
    public void loadMarkdownFile(String url)
    {
        loadMarkdownFile(url, null);
    }

    // readFileFromAsset
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

    // LoadMarkdownUrlTask
    private class LoadMarkdownUrlTask
        extends AsyncTask<String, Integer, String>
    {
        private String baseUrl;
        private String cssFileUrl;

        // doInBackground
        @Override
        protected String doInBackground(String... params)
        {
            try
            {
                String markdown = "";
                baseUrl = params[0];
                String url = params[1];
                cssFileUrl = params[2];
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

        // onProgressUpdate
        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            // no-op
        }

        // onPostExecute
        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                loadMarkdownToView(baseUrl, result, cssFileUrl);
            }

            else
            {
                loadUrl("about:blank");
            }
        }
    }

    // loadMarkdownToView
    private void loadMarkdownToView(String baseUrl, String text,
                                    String cssFileUrl)
    {
        MarkdownProcessor mark = new MarkdownProcessor();
        String html = mark.markdown(text);
        if (cssFileUrl != null)
        {
            html = String.format(Locale.getDefault(), CSS, cssFileUrl, html);
        }

        loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

}
