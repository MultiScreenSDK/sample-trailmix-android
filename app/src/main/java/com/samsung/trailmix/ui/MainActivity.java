

package com.samsung.trailmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.adapter.LibraryAdapter;
import com.samsung.trailmix.multiscreen.events.AppStateEvent;
import com.samsung.trailmix.multiscreen.events.PlaybackEvent;
import com.samsung.trailmix.multiscreen.events.VideoStatusEvent;
import com.samsung.trailmix.multiscreen.model.CurrentStatus;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.util.Util;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Initial activity for the application.
 */
public class MainActivity extends BaseActivity {


    // The adapter to display tracks from library.
    LibraryAdapter libraryAdapter;

    // The list view to display playlist.
    ListView libraryListView;

    // The playback control panel.
    LinearLayout playControlLayout;

    // The video information in playback control panel.
    TextView playText;

    //The play/pause control in playback control panel.
    ImageView playControl;

    //The seek bar in playback control.
    SeekBar seekBar;

    // Create a fixed thread pool containing one thread
    ExecutorService loadLibExecutor = Executors.newFixedThreadPool(1);

    // Current playing metadata
    MetaData metaData;

    // Current playing state
    CurrentStatus currentStatus;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Setup UIs.
        setupToolbar();
        setupPlaybackPanel();


        // If the discovery is still running,restart it.
        if (mMultiscreenManager.isStoppingDiscovery() || mMultiscreenManager.isDiscovering()) {
            //start discovery after some delay.
            mMultiscreenManager.restartDiscovery();
        }

        // Create library adapter.
        libraryAdapter = new LibraryAdapter(this, R.layout.library_list_item);
        libraryListView = (ListView) findViewById(R.id.libraryListView);
        libraryListView.setAdapter(libraryAdapter);
        libraryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Util.d("libraryListView onItemClick at position: " + position);

                // Get the clicked meta data
                metaData = libraryAdapter.getItem(position);

                if (mMultiscreenManager.isTVConnected()) {
                    // Play on the TV directly.
                    play();

                    // Update the UI.
                    updatePlaybackPanel();
                } else {
                    openLocalVideoPlayer();
                }
            }
        });

        // Load library in background.
        loadLibExecutor.submit(loadLibrary);
    }

    protected void onDestroy() {
        //Stop any working thread.
        loadLibExecutor.shutdownNow();

        //Stop discovery if it is running.
        if (mMultiscreenManager.isDiscovering()) {
            mMultiscreenManager.stopDiscovery();
        }

        //Disconnect from multiscreen app.
        mMultiscreenManager.disconnect();

        //Release multiscreen manager
        mMultiscreenManager.release();
        mMultiscreenManager = null;

        super.onDestroy();
    }

    public void onResume() {
        super.onResume();

        //Update the UI.
        updatePlaybackPanel();
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
        updatePlaybackPanel();
    }


    // This method will be called when a app state event is received.
    public void onEvent(AppStateEvent event) {
        Util.d("MainActivity  AppStateEvent: " + event.status);
        //updatePlaybackPanel();

        if (metaData != null) {
            mMultiscreenManager.play(metaData);
        }
    }

    // This method will be called when a app state event is received.
    public void onEvent(VideoStatusEvent event) {
        Util.d("MainActivity  VideoStatusEvent: " + event.status);
        //updatePlaybackPanel();

        currentStatus = event.status;
        updatePlaybackPanel();
    }

    // This method will be called when a app state event is received.
    public void onEvent(PlaybackEvent event) {
        Util.d("MainActivity  PlaybackEvent: " + event.toString());
        //updatePlaybackPanel();

        //Video is finished, hide the playback panel.
        if (!event.isStart()){
            currentStatus = null;
            updatePlaybackPanel();
        }
    }

//
//    // This method will be called when a MessageEvent is posted
//    public void onEvent(ConnectionChangedEvent event) {
//        Util.d("MainActivity ConnectionChangedEvent: " + event.toString());
//
//        // We only handle disconnect event.
//        if (!mMultiscreenManager.isTVConnected() && event.errorMessage == null) {
//            Util.d("TV is disconnected.");
//
//            //Continue to play the video with local player.
//            openLocalVideoPlayer();
//
//            // Hide the playback control panel.
//            currentStatus = null;
//            updatePlaybackPanel();
//        }
//    }

    protected void handleDisconnect() {
        super.handleDisconnect();

        Util.d("TV is disconnected.");

        //Continue to play the video with local player.
        openLocalVideoPlayer();

        // Hide the playback control panel.
        currentStatus = null;
        updatePlaybackPanel();
    }


    //Set up the playback panel.
    private void setupPlaybackPanel() {
        // Playback control panel.
        playControlLayout = (LinearLayout) findViewById(R.id.playControlLayout);
        playControlLayout.setVisibility(View.GONE);

        playText = (TextView)findViewById(R.id.playText);
        playControl = (ImageView)findViewById(R.id.playControl);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
    }

    private void openLocalVideoPlayer() {

        // Launch local video player.
        if (currentStatus == null) {
            currentStatus = new CurrentStatus();
            currentStatus.setId(metaData.getId());
            currentStatus.setState(CurrentStatus.STATE_PLAYING);
            currentStatus.setTime(0);
        }

        Intent intent = new Intent(MainActivity.this, VideoActivity.class);
        intent.putExtra("meta", metaData.toJsonString());
        intent.putExtra("status", currentStatus.toJsonString());
        startActivity(intent);

//                Intent mpdIntent = new Intent(MainActivity.this, PlayerActivity.class)
//                        .setData(Uri.parse("http://redirector.c.youtube.com/videoplayback?id=604ed5ce52eda7ee&itag=22&source=youtube&"
//                                + "sparams=ip,ipbits,expire,source,id&ip=0.0.0.0&ipbits=0&expire=19000000000&signature="
//                                + "513F28C7FDCBEC60A66C86C9A393556C99DC47FB.04C88036EEE12565A1ED864A875A58F15D8B5300"
//                                + "&key=ik0"))
//                        .putExtra(PlayerActivity.CONTENT_ID_EXTRA, md.getTitle())
//                        .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, DemoUtil.TYPE_MP4);
//                startActivity(mpdIntent);
    }

    /**
     * Update the playback panel according to the service count and network condition.
     */
    private void updatePlaybackPanel() {
        //Do nothing if connectivity manager is null.
        if (mMultiscreenManager == null) {
            return;
        }

        //Check if the WIFI is connected.
        boolean isPlayingOnTV = Util.isWiFiConnected() && currentStatus != null;

        playControlLayout.setVisibility(isPlayingOnTV?View.VISIBLE:View.GONE);
        if (isPlayingOnTV) {
            playText.setText(metaData.getTitle());
            playControl.setImageResource(currentStatus.isPlaying()?R.drawable.ic_pause_dark:R.drawable.ic_play_dark);

            seekBar.setProgress((int)(currentStatus.getTime()*100/currentStatus.getDuration()));
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

                // Download video lib.
                mds = Util.readJsonFromUrl(getString(R.string.playlist_url), MetaData[].class);

                // Assign ID for each video.
                for (MetaData md : mds) {
                    md.setId(UUID.randomUUID().toString());
                }

                // Add videos to UI.
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
                }
            });
        }
    }

    private void play() {
        if (metaData != null) {
            mMultiscreenManager.play(metaData);
        }
    }

}
