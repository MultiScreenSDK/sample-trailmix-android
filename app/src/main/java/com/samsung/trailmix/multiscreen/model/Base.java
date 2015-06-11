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

package com.samsung.trailmix.multiscreen.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samsung.trailmix.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The base model.
 */
public class Base {

    /**
     * Convert object into json string.
     * @return the json string.
     */
    public String toJsonString() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        return gson.toJson(this);
    }

    /**
     * Convert object into json object.
     * @return
     */
    public JSONObject getJsonObject() {
        JSONObject obj = null;
        try {
            obj = new JSONObject(toJsonString());
        } catch (JSONException e) {
            Util.e("Error during parse string to JSONObject:" + e.toString());
        }

        return obj;
    }

    /**
     * Parse the given json string into target instance.
     * @param data the json string.
     * @return the target instance.
     */
    public static <T> T parse(String data, Class<T> clazz) {
        if (data == null) {
            return null;
        }

        Gson gson = new Gson();
        return gson.fromJson(data, clazz);
    }
}
