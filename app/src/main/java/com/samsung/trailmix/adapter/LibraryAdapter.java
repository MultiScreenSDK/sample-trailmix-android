/*******************************************************************************
 * Copyright (c) 2015 Samsung Electronics
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

package com.samsung.trailmix.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.util.Util;
import com.squareup.picasso.Picasso;

public class LibraryAdapter extends ArrayAdapter<MetaData> {

    private int layoutResourceId;
    private static LayoutInflater inflater = null;
    private Context context;

    // now playing id
    private String nowPlayingId;

    public LibraryAdapter(Context context, int resourceId) {
        super(context, resourceId);
        this.context = context;
        this.layoutResourceId = resourceId;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public boolean contains(MetaData metaData) {
        return (getPosition(metaData) >= 0);
    }

    /**
     * Replace the existing service with new service.
     *
     * @param metaData the metadata.
     */
    public void replace(MetaData metaData) {

        // Get the service position.
        int position = getPosition(metaData);

        // Check if position is valid.
        if (position >= 0) {

            // Remove the existing service.
            remove(metaData);

            // Insert the new service at the same position.
            insert(metaData, position);
        }
    }

    public void setNowPlaying(String id) {
        if (nowPlayingId != id) {
            nowPlayingId = id;
            notifyDataSetChanged();
        }
    }


    static class ViewHolder {
        public TextView trailerText;
        public ImageView trailerArt;
        public ImageView nowPlayingIndicator;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            row = inflater.inflate(layoutResourceId, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.trailerText = (TextView) row.findViewById(R.id.trailerText);
            holder.trailerArt = (ImageView) row.findViewById(R.id.trailerArt);
            holder.nowPlayingIndicator = (ImageView) row.findViewById(R.id.nowPlayingIndicator);
            row.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) row.getTag();

        // Set the video name.
        final MetaData md = getItem(position);
        holder.trailerText.setText(md.getTitle());

        // Load cover image.
        String cover = md.getCover();
        if (cover != null) {
            Picasso.with(context).load(Util.getUriFromUrl(cover)).into(holder.trailerArt);
        }

        // Update the now playing indicator.
        if (nowPlayingId == null) {
            holder.nowPlayingIndicator.setVisibility(View.GONE);
        } else {
            if (nowPlayingId.equals(md.getId())) {
                holder.nowPlayingIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.nowPlayingIndicator.setVisibility(View.GONE);
            }
        }

        return row;
    }

}
