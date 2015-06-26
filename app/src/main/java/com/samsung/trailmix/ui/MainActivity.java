

package com.samsung.trailmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.samsung.trailmix.R;
import com.samsung.trailmix.adapter.LibraryAdapter;
import com.samsung.trailmix.multiscreen.events.AppStateEvent;
import com.samsung.trailmix.multiscreen.events.ConnectionChangedEvent;
import com.samsung.trailmix.multiscreen.events.PlaybackEvent;
import com.samsung.trailmix.multiscreen.events.VideoStatusEvent;
import com.samsung.trailmix.multiscreen.model.CurrentStatus;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.ui.view.PlayControlImageView;
import com.samsung.trailmix.util.Settings;
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

    // The play/pause control in playback control panel.
    PlayControlImageView playControl;

    // The seek bar in playback control.
    SeekBar seekBar;

    // Create a fixed thread pool containing one thread
    ExecutorService loadLibExecutor = Executors.newFixedThreadPool(1);

    // Current playing state
    CurrentStatus currentStatus;

    // The flag shows it is seeking to a new position. The video status event will be ignored during seeking.
    boolean isSeeking = false;

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
                MetaData md = libraryAdapter.getItem(position);

                if (mMultiscreenManager.isTVConnected()) {

                    if (currentStatus != null && !currentStatus.getId().equals(md.getId())) {
                        // something different is playing on TV.
                        String name = com.samsung.trailmix.util.Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName());
                        showJoinOverwritetDialog(name, currentStatus.getTitle(), md.toJsonString() );
                    } else {
                        metaData = md;

                        // Play on the TV directly.
                        play();

                        // Update the UI.
                        updateUI();
                    }
                } else {
                    metaData = md;
                    openLocalVideoPlayer();
                }
            }
        });

        // Load library in background.
        loadLibExecutor.submit(loadLibrary);
    }

    protected void onDestroy() {
        // Stop any working thread.
        loadLibExecutor.shutdownNow();

        // Stop discovery if it is running.
        if (mMultiscreenManager.isDiscovering()) {
            mMultiscreenManager.stopDiscovery();
        }

        // Disconnect from multiscreen app.
        mMultiscreenManager.disconnect();

        // Release multiscreen manager
        mMultiscreenManager.release();
        mMultiscreenManager = null;

        super.onDestroy();
    }

    public void onResume() {
        super.onResume();

        Util.d("onResume,  isDiscovering=" + mMultiscreenManager.isDiscovering());

        //dismiss any dialog.
        cancelToast();

        // Update the UI.
        updateUI();

        // Check if video is playing.
        mMultiscreenManager.requestAppState();
    }

    public void onStop() {
        super.onStop();

        Util.d("onStop,  isDiscovering=" + mMultiscreenManager.isDiscovering());

        // Stop discovery when the app goes to background.
        mMultiscreenManager.stopDiscovery();

        // Check if video is playing.
        mMultiscreenManager.requestAppState();
    }

    public void onStart() {
        super.onStart();

        Util.d("onStart,  isDiscovering=" + mMultiscreenManager.isDiscovering());

        // Start the service discovery if TV is not connected.
        if (!mMultiscreenManager.isTVConnected()) {

            // Start discovery if it is not started yet.
            mMultiscreenManager.startDiscovery();
        }

        // If it is already discovering. Fetch the result directly.
        updateUI();
    }


    // This method will be called when a app state event is received.
    public void onEvent(AppStateEvent event) {
        Util.d("MainActivity  AppStateEvent: " + event.status);

        currentStatus = event.status;

        if (currentStatus.getId() != null) {
            // The video is playing/paused.
            metaData = findMetaDataById(currentStatus.getId());
        } else {
            // Nothing is playing.
            currentStatus = null;
        }

        updateUI();
    }

    // This method will be called when a app state event is received.
    public void onEvent(VideoStatusEvent event) {
        Util.d("MainActivity  VideoStatusEvent: " + event.status);

        // Only handle the event when it is not seeking.
        if (!isSeeking) {
            currentStatus = event.status;

            // Save the position
            Settings.instance.writePlaybackPosition(currentStatus.getId(), currentStatus.getTime());

            //Retrieve the metadata if it is null.
            if (metaData == null) {
                metaData = findMetaDataById(currentStatus.getId());
            }
            updateUI();
        }
    }

    // This method will be called when a app state event is received.
    public void onEvent(PlaybackEvent event) {
        Util.d("MainActivity  PlaybackEvent: " + event.toString());

        // When video playback is finished, show retry button.
        if (!event.isStart()) {
//            currentStatus = null;
//            updateUI();
            playControl.setState(PlayControlImageView.State.retry);
        }
    }


    // This method will be called when a MessageEvent is posted
    public void onEvent(ConnectionChangedEvent event) {
        super.onEvent(event);

        Util.d("MainActivity ConnectionChangedEvent: " + event.toString());

        // We only handle disconnect event.
        if (event.errorMessage == null) {

            // Update the connected service name or app name.
            appText.setText(mMultiscreenManager.isTVConnected() ? Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName()) : getString(R.string.app_name));

            if (!mMultiscreenManager.isTVConnected()) {
                handleDisconnect();
            }
        }
    }

    private void handleDisconnect() {
        Util.d("TV is disconnected.");

        //Continue to play the video with local player.
        openLocalVideoPlayer();

        // Hide the playback control panel.
        currentStatus = null;
        updateUI();
    }


    /**
     * Find the metadata by given id.
     * @param id
     * @return
     */
    private MetaData findMetaDataById(String id) {
        if (id == null) {
            return null;
        }

        Util.d("findMetaDataById = " + id);
        for (int i = 0; i < libraryAdapter.getCount(); i++) {
            MetaData md = libraryAdapter.getItem(i);
            Util.d("metadata id=" + md.getId());
            if (id.equals(md.getId())) {
                return md;
            }
        }

        return null;
    }

    // Set up the playback panel.
    private void setupPlaybackPanel() {
        // Playback control panel.
        playControlLayout = (LinearLayout) findViewById(R.id.playControlLayout);
        playControlLayout.setVisibility(View.GONE);

        playText = (TextView) findViewById(R.id.playText);

        // Play/Pause button.
        playControl = (PlayControlImageView) findViewById(R.id.playControl);
        playControl.setState(PlayControlImageView.State.play);
        playControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayControlImageView.State state = playControl.getState();
                if (state == PlayControlImageView.State.retry) {

                    // Replay from beginning.
                    play();
                    playControl.setState(PlayControlImageView.State.play);
                } else if (state == PlayControlImageView.State.pause) {
                    play();
                    playControl.setState(PlayControlImageView.State.play);
                } else {
                    mMultiscreenManager.pause();
                    playControl.setState(PlayControlImageView.State.pause);
                }
            }
        });
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                // Seek bar is changed.
                isSeeking = true;
                mMultiscreenManager.seek(seekBar.getProgress());

                // Give some time to located to new position.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Seeking is done now.
                        isSeeking = false;
                    }
                }, 2000);
            }
        });
    }

    /**
     * Launch local video player screen.
     */
    private void openLocalVideoPlayer() {
        // Return if no video is playing on TV.
        if (metaData == null) {
            return;
        }

        // Launch local video player.
        if (currentStatus == null) {
            String id = metaData.getId();
            currentStatus = new CurrentStatus();
            currentStatus.setId(id);
            currentStatus.setState(CurrentStatus.STATE_PLAYING);
            currentStatus.setTime(Settings.instance.readPlaybackPosition(id));
        }

        Intent intent = new Intent(MainActivity.this, VideoActivity.class);
        intent.putExtra("meta", metaData.toJsonString());
        intent.putExtra("status", currentStatus.toJsonString());
        startActivity(intent);
    }

    /**
     * Update the playback panel according to the service count and network condition.
     */
    void updateUI() {
        // Do nothing if connectivity manager is null.
        if (mMultiscreenManager == null) {
            return;
        }

        // Check if the WIFI is connected.
        boolean isPlayingOnTV = mMultiscreenManager.isTVConnected() && currentStatus != null;

        // Display playback control panel when playing on TV.
        playControlLayout.setVisibility(isPlayingOnTV ? View.VISIBLE : View.GONE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playControlLayout.getLayoutParams();
        if (isPlayingOnTV) {

            // Update video information when playing on TV.
            if (metaData != null && currentStatus != null && currentStatus.getId()!=null) {
                playText.setText(metaData.getTitle());
                playControl.setSelected(currentStatus.isPlaying());
                seekBar.setMax((int) currentStatus.getDuration());
                seekBar.setProgress((int) currentStatus.getTime());

                // Make sure the playback control panel is below toolbar.
                params.addRule(RelativeLayout.BELOW, R.id.toolbar);
            }
        } else {

            // Restore the toolbar position (floating).
            params.removeRule(RelativeLayout.BELOW);
        }

        // Update the connected service name or app name.
        appText.setText(mMultiscreenManager.isTVConnected() ? Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName()) : getString(R.string.app_name));

        // Update toolbar background color
        toolbar.setBackgroundColor(isPlayingOnTV ? getResources().getColor(R.color.black) : getResources().getColor(R.color.toolbar_background_color));

        // Update now playing item.
        libraryAdapter.setNowPlaying(isPlayingOnTV ? currentStatus.getId() : null);
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


    /**
     * Play a new video on TV.
     */
    private void play() {
        Util.d("play() is called, metadata=" + metaData);
        if (metaData != null) {

            float position = Settings.instance.readPlaybackPosition(metaData.getId());
            mMultiscreenManager.play(metaData);
        }
    }


}
