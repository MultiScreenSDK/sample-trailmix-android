package com.samsung.trailmix.player;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;

import com.samsung.trailmix.R;

/**
 * Created by bliu on 6/23/2015.
 */
public class SimpleMediaController extends MediaController {

    ImageButton mCCBtn;
    Context mContext;
//    AlertDialog mLangDialog;

    public SimpleMediaController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void setAnchorView(View view) {
        super.setAnchorView(view);

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        frameParams.gravity = Gravity.RIGHT|Gravity.TOP;

        View v = makeCCView();
        addView(v, frameParams);

    }

    private View makeCCView() {
        mCCBtn = new ImageButton(mContext);
        mCCBtn.setImageResource(R.drawable.ic_play_dark);

        mCCBtn.setOnClickListener(new OnClickListener() {


            public void onClick(View v) {
            }
        });

        return mCCBtn;
    }

}
