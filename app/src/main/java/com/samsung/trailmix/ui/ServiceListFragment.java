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

package com.samsung.trailmix.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.adapter.ServiceAdapter;
import com.samsung.trailmix.multiscreen.MultiscreenManager;
import com.samsung.trailmix.util.Util;

public class ServiceListFragment extends DialogFragment {
    //The user color.
    int mColor;


    private ServiceAdapter serviceAdapter;


    /**
     * Create a new instance of MyDialogFragment, providing dialog type
     * as an argument.
     */
    static ServiceListFragment newInstance(int color) {
        ServiceListFragment f = new ServiceListFragment();

        // Supply type input as an argument.
        Bundle args = new Bundle();
        args.putInt("color", color);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Read user color.
        mColor = getArguments().getInt("color");


        //Create service adapter.
        serviceAdapter = new ServiceAdapter(getActivity());
    }

    public void onDestroyView (){
        super.onDestroyView();

        Util.d("ServiceListFragment.onDestroyView");
        serviceAdapter.release();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_connect_service, null);


        if (view != null) {
            ListView listView = (ListView)view.findViewById(R.id.deviceListView);
            listView.setAdapter(serviceAdapter);
            listView.setOnItemClickListener(new ListView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//                    //When item is clicked, get the service clicked first.
//                    ServiceAdapter adapter = ConnectivityManager.getInstance().getServiceAdapter();
//                    Service service = adapter.getItem(position);
//
//                    Activity activity = getActivity();
//                    if (activity instanceof ConnectActivity) {
//
//                        //Display connecting message if it is in connection screen.
//                        ConnectActivity ca = (ConnectActivity) getActivity();
//                        ca.displayConnectingMessage(service.getName());
//                    } else if (activity instanceof PlaylistActivity) {
//
//                        //Switching service if it is in playlist screen.
//                        PlaylistActivity pa = (PlaylistActivity) activity;
//                        pa.isSwitchingService = true;
//                        ConnectivityManager.getInstance().disconnect();
//                    }
//
//                    ConnectivityManager.getInstance().setService(service);
//                    ServiceListFragment.this.getDialog().dismiss();
                }
            });

            LinearLayout llConnectTo = (LinearLayout)view.findViewById(R.id.selectedServiceLayout);
//            TextView connectedToText = (TextView)view.findViewById(R.id.connectedToText);
//            ImageView connectedToIcon = (ImageView)view.findViewById(R.id.connectedToIcon);
//
//            LinearLayout switchToLayout = (LinearLayout)view.findViewById(R.id.switchToLayout);

            if (MultiscreenManager.getInstance().isTVConnected()) {

                //Display connected device and disconnect button.
//                llConnectTo.setVisibility(View.VISIBLE);
//
//                //Update the dialog title
//                connectedToText.setText(getString(R.string.connected_to));

                //Update the dialog icon
//                connectedToIcon.setImageResource(R.drawable.ic_connected_white);

                //Remove the connected service from service list.
//                ConnectivityManager.getInstance().removeConnectedServiceFromList();

//                //Update the service icon according to service type.
//                ImageView selectedServiceIcon = (ImageView)view.findViewById(R.id.selectedServiceIcon);
//                if (ConnectivityManager.getInstance().getConnectedServiceType() == ConnectivityManager.ServiceType.Speaker) {
//                    //The speaker is connected
//                    selectedServiceIcon.setImageResource(R.drawable.ic_speaker_gray);
//                } else if (ConnectivityManager.getInstance().getConnectedServiceType() == ConnectivityManager.ServiceType.TV) {
//                    //The TV or TV simulator is connected.
//                    selectedServiceIcon.setImageResource(R.drawable.ic_tv_white);
//                }

                //Display the connected service name
//                TextView selectedServiceText = (TextView)view.findViewById(R.id.selectedServiceText);
//                selectedServiceText.setText(Util.getFriendlyTvName(ConnectivityManager.getInstance().getService().getName()));
//
//                //Update the disconnect button with user color.
//                Button btnDisconnect = (Button) view.findViewById(R.id.disconnectButton);
//                btnDisconnect.setTextColor(mColor);
//
//                //When disconnect button is clicked, close the activity and returns to connection screen.
//                btnDisconnect.setOnClickListener(new View.OnClickListener() {
//
//                    @Override
//                    public void onClick(View v) {
//                        ConnectivityManager.getInstance().addConnectedServerToList();
//                        getActivity().finish();
//                    }
//                });
//
//                switchToLayout.setVisibility(ConnectivityManager.getInstance().getServiceAdapter().getCount()==0?View.GONE:View.VISIBLE);
            } else {

                //Hide connected device and disconnect button.
                llConnectTo.setVisibility(View.GONE);

                //Update the dialog title.
//                connectedToText.setText(getString(R.string.connect_to));

                //Update the dialog icon.
//                connectedToIcon.setImageResource(R.drawable.ic_discovered_white);
            }
        }

        //Create a alert dialog with customized style.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.CustomTheme_Dialog);
        builder.setView(view);

        // Allow dismiss by clicking outside the dialog
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        // Set window width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.gravity = Gravity.TOP;
        lp.width = getResources().getDimensionPixelSize(R.dimen.connect_width);

        // Set window height
        int displayHeight = Util.getDisplayHeight(getActivity());
        int maxHeight = Math.round((float)displayHeight * 75 / 100);
        lp.y = Math.round((float)(displayHeight - maxHeight) / 2);
        lp.height = maxHeight;

        dialog.getWindow().setAttributes(lp);
        return dialog;
    }
}
