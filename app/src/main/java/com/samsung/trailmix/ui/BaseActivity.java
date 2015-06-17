package com.samsung.trailmix.ui;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.interceptor.AppCompatActivityMenuKeyInterceptor;
import com.samsung.trailmix.multiscreen.MultiscreenManager;
import com.samsung.trailmix.multiscreen.events.ConnectionChangedEvent;
import com.samsung.trailmix.multiscreen.events.ServiceChangedEvent;
import com.samsung.trailmix.util.Util;

import de.greenrobot.event.EventBus;

/**
 * Created by bliu on 6/17/2015.
 */
public class BaseActivity extends AppCompatActivity {
    protected Toolbar toolbar;

    //Handler
    protected Handler handler = new Handler();

    //The connect icon.
    protected MenuItem miConnect;


    /**
     * The connectivity manager instance.
     */
    protected MultiscreenManager mMultiscreenManager;

    //Show connecting message
    AlertDialog alertDialog;

    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Register to receive events.
        EventBus.getDefault().register(this);

        //Get connectivity manager.
        mMultiscreenManager = MultiscreenManager.getInstance();
    }


    protected void onDestroy() {
        super.onDestroy();

        //Remove event monitor.
        EventBus.getDefault().unregister(this);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(this.getClass().getName(), "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu, menu);

        // Get the connect menu item.
        miConnect = menu.findItem(R.id.action_connect);

        // Update toolbar status.
        updateToolbar();

        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(this.getClass().getName(), "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            case R.id.action_connect:
                //When the S icon is clicked, opens the service list dialog.
                showServiceListDialog();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onResume() {
        super.onResume();

        //Update the toolbar.
        updateToolbar();
    }

    public void onStart() {
        super.onStart();

        //If it is already discovering. Fetch the result directly.
        updateToolbar();
    }

    // This method will be called when a service is changed.
    public void onEvent(ServiceChangedEvent event) {
        updateToolbar();
    }


    // This method will be called when a MessageEvent is posted
    public void onEvent(ConnectionChangedEvent event) {
        Util.d("BaseActivity ConnectionChangedEvent: " + event.toString());

        if (mMultiscreenManager.isTVConnected()) {

            //Cancel the toast before launch playlist
            cancelToast();

            //When service is connected, we stop discovery.
            mMultiscreenManager.stopDiscovery();

            //Send app state request when TV is connected.
            mMultiscreenManager.requestAppState();

            //If it is video screen, exit to main screen.
            if (this instanceof VideoActivity) {
                finish();
            }
        } else if (event.errorMessage != null) {
            //Error happens.
            Util.e(event.errorMessage);

            //When error happens, restart the discovery
            if (!mMultiscreenManager.isDiscovering()) {
                mMultiscreenManager.startDiscovery();
            }

            //Show the error message to user.
            displayErrorMessage(event.errorMessage);
        } else {
            // normal disconnect.
            handleDisconnect();
        }

        updateToolbar();
    }

    protected  void handleDisconnect() {
    }

    /**
     * Update the toolbar according to the service count and network condition.
     */
    private void updateToolbar() {
        //Do nothing if connectivity manager is null.
        if (mMultiscreenManager == null || miConnect == null) {
            return;
        }

        //Check if the WIFI is connected.
        if (Util.isWiFiConnected()) {
            int count = mMultiscreenManager.getServiceList().size();

            miConnect.setVisible(count > 0);

            if (mMultiscreenManager.isTVConnected()) {
                //show the connected icon.
                miConnect.setIcon(R.drawable.ic_connected_white);
            } else {
                //show discovered icon.
                miConnect.setIcon(R.drawable.ic_discovered_white);
            }
        } else {
            //hide the s icon.
            miConnect.setVisible(false);
        }
    }

    void setupToolbar() {
        //Initialize the interceptor
        AppCompatActivityMenuKeyInterceptor.intercept(this);

        //Add toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Show service list dialog.
     */
    private void showServiceListDialog() {

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog, only shows the connect to panel.
        DialogFragment newFragment = ServiceListFragment.newInstance(0);
        newFragment.show(ft, "dialog");
    }

    public void displayConnectingMessage(String tvName) {
        //final String message = String.format(getString(R.string.connect_to_message), Util.getFriendlyTvName(tvName));
        final String message = Util.getFriendlyTvName(tvName) + "\u2026";
        handler.post(new Runnable() {
            @Override
            public void run() {
                View toastLayout = getLayoutInflater().inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toastLayout));
                TextView serviceText = (TextView) toastLayout.findViewById(R.id.serviceText);
                serviceText.setText(message);

                //Dismiss the dialog if it is showing.
                if (alertDialog != null && alertDialog.isShowing()) {
                    cancelToast();
                }

                //Display alert dialog with customized layout.
                alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.CustomTheme_Dialog).create();
                alertDialog.setView(toastLayout);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
        });
    }

    /**
     * Display an error message.
     * @param errorMsg
     */
    public void displayErrorMessage(final String errorMsg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                View toastLayout = getLayoutInflater().inflate(R.layout.multiline_toast, (ViewGroup) findViewById(R.id.toastLayout));
                final TextView mesgText = (TextView) toastLayout.findViewById(R.id.mesgText);
                mesgText.setText(errorMsg);

                // If the error message width is greater than the view width, then change the
                // content gravity.
                mesgText.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        mesgText.removeOnLayoutChangeListener(this);
                        final TextPaint textPaint = mesgText.getPaint();
                        float width = textPaint.measureText(errorMsg);
                        if ((int) width > mesgText.getWidth()) {
                            mesgText.setGravity(Gravity.CENTER_VERTICAL);
                        }
                    }
                });

                //Dismiss the dialog if it is showing.
                if (alertDialog != null && alertDialog.isShowing()) {
                    cancelToast();
                }

                //Display alert dialog with customized layout.
                alertDialog = new AlertDialog.Builder(BaseActivity.this, R.style.CustomTheme_Dialog).create();
                alertDialog.setView(toastLayout);
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
            }
        });
    }

    /**
     * Dismiss the connecting message dialog.
     */
    public void cancelToast() {
        if (alertDialog != null) {
            alertDialog.cancel();
        }

        alertDialog = null;
    }
}