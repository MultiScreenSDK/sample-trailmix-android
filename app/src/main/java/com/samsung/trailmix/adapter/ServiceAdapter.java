/**
 * ****************************************************************************
 * Copyright (c) 2015 Samsung Electronics
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * *****************************************************************************
 */

package com.samsung.trailmix.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.samsung.multiscreen.Service;
import com.samsung.trailmix.R;
import com.samsung.trailmix.multiscreen.MultiscreenManager;
import com.samsung.trailmix.multiscreen.events.ServiceChangedEvent;
import com.samsung.trailmix.util.Util;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class ServiceAdapter extends BaseAdapter {

    // The layout resource id.
    private static final int layoutResourceId = R.layout.service_list_item;
    private static LayoutInflater inflater = null;
    private Context context;

    // The service list.
    ArrayList<Service> serviceList = null;

    public ServiceAdapter(Context context, ArrayList<Service> serviceList) {
        this.context = context;
        inflater = LayoutInflater.from(this.context);

        // Use the service list given in the parameters.
        this.serviceList = serviceList;

        // Register to receive events.
        EventBus.getDefault().register(this);
    }

    public ServiceAdapter(Context context) {
        this(context, MultiscreenManager.getInstance().getServiceList());
    }

    /**
     * Release the adapter and clean up resources.
     */
    public void release() {
        serviceList = null;

        // Unregister the events.
        EventBus.getDefault().unregister(this);
    }


    /**
     * Return the items count.
     * @return
     */
    @Override
    public int getCount() {
        return serviceList.size();
    }

    @Override
    public Service getItem(int position) {
        return serviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * This method will be service is changed (add/remove/update).
     */
    public void onEvent(ServiceChangedEvent event) {
        // Run it at UI thread.
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            row = inflater.inflate(layoutResourceId, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.deviceName = (TextView) row.findViewById(R.id.serviceText);
            holder.serviceIcon = (ImageView)row.findViewById(R.id.serviceIcon);
            row.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) row.getTag();

        final Service service = getItem(position);

        // Set the service name.
        holder.deviceName.setText(Util.getFriendlyTvName(service.getName()));

        // Set the service icon according to the service type.
        if (MultiscreenManager.getInstance().getServiceType(service) == MultiscreenManager.ServiceType.Speaker) {
            holder.serviceIcon.setImageResource(R.drawable.ic_speaker_gray);
        } else {
            holder.serviceIcon.setImageResource(R.drawable.ic_tv_gray);
        }

        return row;
    }


    private static class ViewHolder {
        public TextView deviceName;
        public ImageView serviceIcon;
    }


}
