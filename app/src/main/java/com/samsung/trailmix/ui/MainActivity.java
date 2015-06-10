

package com.samsung.trailmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.samsung.multiscreen.util.RunUtil;
import com.samsung.trailmix.R;
import com.samsung.trailmix.adapter.ServiceAdapter;
import com.samsung.trailmix.interceptor.AppCompatActivityMenuKeyInterceptor;
import com.samsung.trailmix.multiscreen.MultiscreenManager;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.util.Util;

import de.greenrobot.event.EventBus;


/**
 * Initial activity for the application.
 */
public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private ServiceAdapter serviceAdapter;

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

        //Create service adapter.
        serviceAdapter = new ServiceAdapter(this);

        //Load library in background.
        RunUtil.runInBackground(loadLibrary);
    }

    protected void onDestroy() {
        super.onDestroy();

        //Remove event monitor.
        EventBus.getDefault().unregister(this);

        //Release adapter.
        serviceAdapter.release();

        //Disconnect from multiscreen app.
        MultiscreenManager.getInstance().disconnect();

        //Release multiscreen manager
        MultiscreenManager.getInstance().release();
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
//                showServiceListDialog();

                // TODO: Replace with real logic
                startActivity(new Intent(this, VideoActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * The background thread to load playlist from server.
     */
    private Runnable loadLibrary = new Runnable() {
        @Override
        public void run() {
            String data = null;
            try {
                data = Util.readUrl(getString(R.string.playlist_url));
            } catch (Exception e) {
                Util.e("Error when loading library:" + e.toString());
            }

            //Parse data into objects.
            if (data != null) {
                Gson gson = new Gson();
                //Parse string to meta array, then add into library list.
                MetaData[] mds = gson.fromJson(data, MetaData[].class);
                if (mds != null) {
                    //addMetadataIntoLibrary(gson.fromJson(data, MetaData[].class));
                }
            }
        }
    };

}
