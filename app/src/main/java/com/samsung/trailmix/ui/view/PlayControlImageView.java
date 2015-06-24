package com.samsung.trailmix.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.samsung.trailmix.R;

/**
 * Created by bliu on 6/19/2015.
 */
public class PlayControlImageView extends ImageView {
    public enum State {
        play, pause, retry
    }

    private State state = null;

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
                setImageResource(R.drawable.ic_pause_dark);
                break;
            case pause:
                setImageResource(R.drawable.ic_play_dark);
                break;
            case retry:
                setImageResource(R.drawable.ic_replay_dark);
                break;
        }
    }

}
