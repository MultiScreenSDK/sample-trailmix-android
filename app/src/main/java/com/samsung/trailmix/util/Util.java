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

package com.samsung.trailmix.util;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.samsung.trailmix.App;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Util {
    //The debug tag.
    private static final String TAG = "SamsungTrailmix";

    //The flag to output debug message or not.
    public static final boolean DEBUG = true;


    /**
     * Generate the Log.d log message.
     * @param message The message to be output.
     */
    public static void d(String message) {
        if (DEBUG) Log.d(TAG, message);
    }

    /**
     * Generate the Log.e log message.
     * @param message The message to be output.
     */
    public static void e(String message) {
        if (DEBUG) Log.e(TAG, message);
    }


    /**
     * Get the connected WiFi network name.
     * @return network name or null is WiFi is not connected.
     */
    public static String getWifiName() {
        String ssid = null;
        if (isWiFiConnected()) {
            WifiManager wifiManager = (WifiManager) App.getInstance().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            ssid = wifiInfo.getSSID();
        }

        return ssid;
    }


    /**
     * Return whether or not WiFi is connected.
     *
     * @return true if WiFi is connected, otherwise false.
     */
    public static boolean isWiFiConnected() {
        //Get connectivity manager.
        ConnectivityManager connManager = (ConnectivityManager) App.getInstance().
                getSystemService(Context.CONNECTIVITY_SERVICE);

        //get network info object.
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();
    }


    /**
     * Read Json content from Url.
     * @param urlString the url string.
     * @param clazz the class of the target object.
     * @return the Json object.
     * @throws Exception
     */
    public static <T> T readJsonFromUrl(String urlString, Class<T> clazz) throws Exception {
        T result = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();

            //Disable android cache.
            urlConnection.setUseCaches(false);

            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            Gson gson = new Gson();
            result = gson.fromJson(reader, clazz);
        } finally {
            if (reader != null)
                reader.close();
        }

        return result;
    }

    /**
     * Stripe out the leading characters and returns a friendly name.
     * @param name The TV name.
     * @return the friendly name.
     */
    public static String getFriendlyTvName(String name) {
        if (name == null) {
            return null;
        }

        if (name.startsWith("[TV]")) {
            return name.substring(4).trim();
        }

        return name;
    }

    /**
     * Get Uri object from url string.
     * @param thisUrl the url string.
     * @return Uri object.
     */
    public static Uri getUriFromUrl(String thisUrl) {
        Uri uri = Uri.parse(thisUrl);
        Uri.Builder builder = uri.buildUpon();
        builder.scheme(uri.getScheme())
                .authority(uri.getAuthority())
                .path(uri.getPath())
                .query(uri.getQuery())
                .fragment(uri.getFragment());

        return builder.build();
    }

    /**
     * Get the dimensions of the display.
     *
     * @param context   the context
     * @return  the point holding the display dimensions
     */
    public static Point getDisplayDimensions(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        return point;
    }

    /**
     * Get the width of the display.
     *
     * @param context   the context
     * @return  the width of the display
     */
    public static int getDisplayWidth(Context context) {
        Point point = getDisplayDimensions(context);
        return point.x;
    }

    /**
     * Get the height of the display.
     *
     * @param context   the context
     * @return  the height of the display
     */
    public static int getDisplayHeight(Context context) {
        Point point = getDisplayDimensions(context);
        return point.y;
    }
}
