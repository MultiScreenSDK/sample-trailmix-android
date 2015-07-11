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

package com.samsung.trailmix.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.multiscreen.MultiscreenManager;

public class JoinOverwriteFragment extends DialogFragment {
    // The TV name.
    private String tvName;

    //The title of dialog.
    private String title;

    //The meta data of the video.
    private String metadata;

    /**
     * Create a new instance of JoinOverwriteFragment, providing dialog type
     * as an argument.
     */
    static JoinOverwriteFragment newInstance(String tvName, String title, String metadata) {
        JoinOverwriteFragment f = new JoinOverwriteFragment();

        // Supply type input as an argument.
        Bundle args = new Bundle();
        if (tvName.length()>10) {
            args.putString("name", tvName + "\nis playing");
        }else {
            args.putString("name", tvName + " is playing");
        }
        args.putString("title", title);
        args.putString("metadata", metadata);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read TV name
        tvName = getArguments().getString("name");

        // Read movie title.
        title = getArguments().getString("title");

        // Read metadata
        metadata = getArguments().getString("metadata");

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_join_overwrite, null);


        if (view != null) {
            TextView join = (TextView) view.findViewById(R.id.join);
            if (getActivity() instanceof MainActivity) {
                // Main screen should show a cancel button
                join.setText(getActivity().getString(R.string.cancel));
            }

            join.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();

                    if (getActivity() instanceof VideoActivity) {
                        getActivity().finish();
                    }
                }
            });

            TextView overwrite = (TextView) view.findViewById(R.id.orverwrite);
            overwrite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();

                    if (getActivity() instanceof MainActivity) {

                        MainActivity ma = (MainActivity) getActivity();

                        // Indicate that it is switching video now.
                        ma.isSwitchingVideo = true;

                        //Overwrite playing
                        ma.overwritePlaying(metadata);

                        //Update UI.
                        ma.updateUI();
                    } else {
                        VideoActivity va = (VideoActivity) getActivity();
                        va.overwritePlaying();
                        getActivity().finish();
                    }
                }
            });

            TextView connectToText = (TextView) view.findViewById(R.id.connectToText);
            connectToText.setText(tvName);

            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(title);
        }

        // Create a alert dialog with customized style.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.CustomTheme_Dialog);

        // Apply the view.
        builder.setView(view);

        // Create alert dialog
        AlertDialog dialog = builder.create();

        // Ignore the back key. User has to make a choice.
        dialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode,
                                 KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialogInterface.dismiss();
                    MultiscreenManager.getInstance().disconnect();
                    return true;
                }
                return false;
            }
        });

        // Note allow dismiss by clicking outside the dialog
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // Set window width.
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = getResources().getDimensionPixelSize(R.dimen.connect_width);

        dialog.getWindow().setAttributes(lp);
        return dialog;
    }
}
