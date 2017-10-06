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

import android.content.Context;
import android.util.AttributeSet;

import org.billthefarmer.markdown.MarkdownView;

// DiaryView
public class DiaryView extends MarkdownView
{
    private OnScrollChangedListener listener;

    // DiaryView
    public DiaryView(Context context)
    {
        super(context);
    }

    // DiaryView
    public DiaryView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    // onScrollChanged
    @Override
    protected void onScrollChanged (int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged (l, t, oldl, oldt);

        if (listener != null)
            listener.onScrollChanged (l, t, oldl, oldt);
    }

    // setOnScrollChangedListener
    public void setOnScrollChangedListener(OnScrollChangedListener l)
    {
        listener = l;
    }

    public interface OnScrollChangedListener
    {
        // onScrollChanged
        void onScrollChanged (int l, int t, int oldl, int oldt);
    }
}
