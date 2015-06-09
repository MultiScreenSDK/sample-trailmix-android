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

package com.samsung.trailmix.interceptor;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.WindowCallbackWrapper;
import android.view.KeyEvent;
import android.view.Window;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * A hardware key interceptor to intercept the menu key which has been disabled from handling by
 * default in the latest appcompat library in Android.
 */
public class AppCompatActivityMenuKeyInterceptor {

    private static final String FIELD_NAME_DELEGATE = "mDelegate";
    private static final String FIELD_NAME_WINDOW = "mWindow";

    public static void intercept(AppCompatActivity appCompatActivity) {
        new AppCompatActivityMenuKeyInterceptor(appCompatActivity);
    }

    private AppCompatActivityMenuKeyInterceptor(AppCompatActivity activity) {
        try {
            Field mDelegateField = AppCompatActivity.class.getDeclaredField(FIELD_NAME_DELEGATE);
            mDelegateField.setAccessible(true);
            Object mDelegate = mDelegateField.get(activity);

            Field mWindowField = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mWindowField = mDelegate.getClass().getSuperclass().getSuperclass().getDeclaredField(FIELD_NAME_WINDOW);
            } else {
                mWindowField = mDelegate.getClass().getSuperclass().getDeclaredField(FIELD_NAME_WINDOW);
            }

            mWindowField.setAccessible(true);
            Window mWindow = (Window) mWindowField.get(mDelegate);

            Window.Callback mOriginalWindowCallback = mWindow.getCallback();
            mWindow.setCallback(new AppCompatWindowCallbackCustom(mOriginalWindowCallback, activity));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private class AppCompatWindowCallbackCustom extends WindowCallbackWrapper {

        private WeakReference<AppCompatActivity> mActivityWeak;

        public AppCompatWindowCallbackCustom(Window.Callback wrapped, AppCompatActivity appCompatActivity) {
            super(wrapped);

            mActivityWeak = new WeakReference<AppCompatActivity>(appCompatActivity);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            final int keyCode = event.getKeyCode();

            AppCompatActivity appCompatActivity = mActivityWeak.get();

            if (appCompatActivity != null && keyCode == KeyEvent.KEYCODE_MENU) {
                if (appCompatActivity.dispatchKeyEvent(event))
                    return true;
            }

            return super.dispatchKeyEvent(event);
        }
    }
}
