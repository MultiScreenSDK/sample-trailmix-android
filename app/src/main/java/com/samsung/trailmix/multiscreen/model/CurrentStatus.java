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

import lombok.Data;

@Data
public class CurrentStatus extends Base {
    /**
     * The playback state - playing.
     */
    public static final String STATE_PLAYING = "playing";

    /**
     * The playback state - paused.
     */
    public static final String STATE_PAUSED = "paused";

    //video id
    private String id;

    //The playback position
    private float time;

    //The current playback state either STATE_PLAYING or STATE_PAUSED
    private String state;

    //The duration of the video.
    private long duration;

    //The video title.
    private String title;

    //The volume value.
    private int volume;


    /**
     * Check if the track is playing.
     * @return true if tracking is playing, otherwise false.
     */
    public boolean isPlaying() {
        return (state != null && state.equals(STATE_PLAYING));
    }
}
