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

package com.samsung.trailmix.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.samsung.trailmix.R;

public class PlayControlImageView extends ImageView {
    public enum State {
        play, pause, retry
    }

    private State state = null;
    private boolean useSmallIcon = false;

    public PlayControlImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setState(State.play);
    }


    public void setState(State state) {
        if (this.state != state) {
            this.state = state;
            updateImageRes();
        }
    }

    public State getState() {
        return state;
    }

    public void updateImageRes() {
        switch (state) {
            case play:
                setImageResource(useSmallIcon?R.drawable.ic_pause_dark_sm:R.drawable.ic_pause_dark);
                break;
            case pause:
                setImageResource(useSmallIcon?R.drawable.ic_play_dark_sm:R.drawable.ic_play_dark);
                break;
            case retry:
                setImageResource(useSmallIcon?R.drawable.ic_replay_dark_sm:R.drawable.ic_replay_dark);
                break;
        }
    }

    public void setUseSmallIcon(boolean useSmallIcon) {
        this.useSmallIcon = useSmallIcon;
        updateImageRes();
    }
}
