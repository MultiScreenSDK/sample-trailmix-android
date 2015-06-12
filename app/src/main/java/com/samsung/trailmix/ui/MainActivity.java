

package com.samsung.trailmix.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.adapter.LibraryAdapter;
import com.samsung.trailmix.interceptor.AppCompatActivityMenuKeyInterceptor;
import com.samsung.trailmix.multiscreen.MultiscreenManager;
import com.samsung.trailmix.multiscreen.events.ConnectionChangedEvent;
import com.samsung.trailmix.multiscreen.events.ServiceChangedEvent;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.util.Util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;


/**
 * Initial activity for the application.
 */
public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;

    //Handler
    Handler handler = new Handler();

    //The connect icon.
    MenuItem miConnect;

    /**
     * The connectivity manager instance.
     */
    private MultiscreenManager mMultiscreenManager;

    //The adapter to display tracks from library.
    LibraryAdapter libraryAdapter;

    //The list view to display playlist.
    ListView libraryListView;

    // Create a fixed thread pool containing one thread
    ExecutorService loadLibExecutor = Executors.newFixedThreadPool(1);

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

        //If the discovery is still running,restart it.
        if (mMultiscreenManager.isStoppingDiscovery() || mMultiscreenManager.isDiscovering()) {
            //start discovery after some delay.
            mMultiscreenManager.restartDiscovery();
        }

        //Create library adapter.
        libraryAdapter = new LibraryAdapter(this, R.layout.library_list_item);
        libraryListView = (ListView) findViewById(R.id.libraryListView);
        libraryListView.setAdapter(libraryAdapter);
        libraryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Util.d("libraryListView onItemClick at position: " + position);

                MetaData md = libraryAdapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                intent.putExtra("json", md.toJsonString());
                startActivity(intent);
            }
        });

        //Load library in background.
        loadLibExecutor.submit(loadLibrary);
    }

    protected void onDestroy() {
        super.onDestroy();

        //Stop any working thread.
        loadLibExecutor.shutdownNow();

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

    public void onResume() {
        super.onResume();

        //Update the UI.
        updateUI();
    }

    public void onStop() {
        super.onStop();

        Util.d("onStop,  isDiscovering=" + mMultiscreenManager.isDiscovering());

        //Stop discovery when the app goes to background.
        mMultiscreenManager.stopDiscovery();
    }

    public void onStart() {
        super.onStart();

        Util.d("onStart,  isDiscovering=" + mMultiscreenManager.isDiscovering());

        //Start the service discovery if TV is not connected.
        if (!mMultiscreenManager.isTVConnected()) {

            //start discovery if it is not started yet.
            mMultiscreenManager.startDiscovery();
        }

        //If it is already discovering. Fetch the result directly.
        updateUI();
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

        //Get the connect menu item.
        miConnect = menu.findItem(R.id.action_connect);
        //hide the icon by default.
        miConnect.setVisible(false);

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


            //When service is connected, we stop discovery.
            mMultiscreenManager.stopDiscovery();
        } else if (event.errorMessage != null) {
            //Error happens.
            Util.e(event.errorMessage);

            //When error happens, restart the discovery
            if (!mMultiscreenManager.isDiscovering()) {
                mMultiscreenManager.startDiscovery();
            }

            //Show the error message to user.
            //displayErrorMessage(event.errorMessage);
        }

        updateUI();
    }

    // This method will be called when a service is changed.
    public void onEvent(ServiceChangedEvent event) {
        updateUI();
    }

    public void displayConnectingMessage(String tvName) {
        //final String message = String.format(getString(R.string.connect_to_message), Util.getFriendlyTvName(tvName));
        final String message = Util.getFriendlyTvName(tvName) + "\u2026";
        handler.post(new Runnable() {
            @Override
            public void run() {
//                View toastLayout = getLayoutInflater().inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toastLayout));
//                TextView serviceText = (TextView) toastLayout.findViewById(R.id.serviceText);
//                serviceText.setText(message);
//
//                //Dismiss the dialog if it is showing.
//                if (alertDialog != null && alertDialog.isShowing()) {
//                    cancelToast();
//                }
//
//                //Display alert dialog with customized layout.
//                alertDialog = new AlertDialog.Builder(ConnectActivity.this, R.style.CustomTheme_Dialog).create();
//                alertDialog.setView(toastLayout);
//                alertDialog.setCanceledOnTouchOutside(false);
//                alertDialog.show();
            }
        });
    }

    /**
     * Update the UI according to the service count and network condition.
     */
    private void updateUI() {
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
                miConnect.setIcon(R.drawable.ic_discovered_gray);
            } else {
                //show discovered icon.
                miConnect.setIcon(R.drawable.ic_discovered_white);
            }
        } else {
            //hide the s icon.
            miConnect.setVisible(false);
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
                addMetaDataIntoLibrary(mds);
            } catch (Exception e) {
                Util.e("Error when loading library:" + e.toString());
            }
        }
    };

    /**
     * Add metatda into library.
     *
     * @param mds
     */
    private void addMetaDataIntoLibrary(final MetaData[] mds) {
        if (mds != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    libraryAdapter.clear();
                    libraryAdapter.addAll(mds);
                    libraryAdapter.notifyDataSetChanged();
                }
            });
        }
    }

}
