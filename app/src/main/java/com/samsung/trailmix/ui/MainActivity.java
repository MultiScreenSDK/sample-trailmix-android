

package com.samsung.trailmix.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.samsung.multiscreen.util.RunUtil;
import com.samsung.trailmix.R;
import com.samsung.trailmix.interceptor.AppCompatActivityMenuKeyInterceptor;
import com.samsung.trailmix.multiscreen.MultiscreenManager;
import com.samsung.trailmix.multiscreen.events.ConnectionChangedEvent;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.util.Util;

import de.greenrobot.event.EventBus;


/**
 * Initial activity for the application.
 */
public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;

    /**
     * The connectivity manager instance.
     */
    private MultiscreenManager mMultiscreenManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        //Initialize the interceptor
        AppCompatActivityMenuKeyInterceptor.intercept(this);

        //Add toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Register to receive events.
        EventBus.getDefault().register(this);

        //Get connectivity manager.
        mMultiscreenManager = MultiscreenManager.getInstance();

        //Load library in background.
        RunUtil.runInBackground(loadLibrary);

        if (Util.isWiFiConnected()) {
            mMultiscreenManager.startDiscovery();
        }
    }

    protected void onDestroy() {
        super.onDestroy();

        //Remove event monitor.
        EventBus.getDefault().unregister(this);

        //Stop discovery if it is running.
        if (mMultiscreenManager.isDiscovering()) {
            mMultiscreenManager.stopDiscovery();
        }

        //Disconnect from multiscreen app.
        mMultiscreenManager.disconnect();

        //Release multiscreen manager
        mMultiscreenManager.release();
        mMultiscreenManager = null;
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(this.getClass().getName(), "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            case R.id.action_connect:
                //When the S icon is clicked, opens the service list dialog.
                showServiceListDialog();

                // TODO: Replace with real logic
                //startActivity(new Intent(this, VideoActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    // This method will be called when a MessageEvent is posted
    public void onEvent(ConnectionChangedEvent event) {
        if (mMultiscreenManager.isTVConnected()) {

            //Cancel the toast before launch playlist
            //cancelToast();

            //When TV is connected, go to playlist screen.
            // startActivity(new Intent(this, PlaylistActivity.class));
        } else if (event.errorMessage != null) {
            //Error happens.
            Util.e(event.errorMessage);

            //Show the error message to user.
            //displayErrorMessage(event.errorMessage);
        }
    }

    /**
     * The background thread to load playlist from server.
     */
    private Runnable loadLibrary = new Runnable() {
        @Override
        public void run() {
            MetaData[] mds = null;
            try {
                mds = Util.readJsonFromUrl(getString(R.string.playlist_url), MetaData[].class);
            } catch (Exception e) {
                Util.e("Error when loading library:" + e.toString());
            }
        }
    };

}
