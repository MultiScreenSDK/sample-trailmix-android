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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
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
import com.samsung.trailmix.util.Util;
import com.squareup.picasso.Picasso;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.app.Notification.FLAG_NO_CLEAR;


/**
 * Initial activity for the application.
 */
public class MainActivity extends BaseActivity {
    public static final int NOTIFICATION_ID = 100;

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

    // The media playback position.
    private TextView postionTextView;

    // The video duration.
    private TextView durationTextView;

    // Create a fixed thread pool containing one thread
    ExecutorService loadLibExecutor = Executors.newFixedThreadPool(1);

    // Current playing state
    CurrentStatus currentStatus;

    // The flag shows it is seeking to a new position. The video status event will be ignored during seeking.
    boolean isSeeking = false;

    //When is seeking, we will not update the seek bar until this value is reached.
    int expectedSeekbarValue;

    // Switching video when it is true.
    boolean isSwitchingVideo = false;

    // The broadcast listener to receive play action from notification.
    PlayActionBroadcastReceiver broadcastReceiver;

    //==============================================================================================
    //      Activity methods
    //==============================================================================================

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

                    if (currentStatus != null && currentStatus.getId() != null &&
                            !currentStatus.getId().equals(md.getId())) {
                        // something different is playing on TV.
                        String name = com.samsung.trailmix.util.Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName());
                        showJoinOverwritetDialog(name, currentStatus.getTitle(), md.toJsonString());
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

        clearNotification();

        super.onDestroy();
    }

    public void onResume() {
        super.onResume();

        Util.d("onResume,  isDiscovering=" + mMultiscreenManager.isDiscovering());

        // Dismiss any dialog.
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

    //==============================================================================================
    //      Multiscreen events and methods
    //==============================================================================================


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

        // Show notification when join the TV. Make sure it is called after updateUI().
        showNotification();
    }

    // This method will be called when a app state event is received.
    public void onEvent(VideoStatusEvent event) {
        Util.d("MainActivity  VideoStatusEvent: " + event.status);

        if (!event.status.getId().equals(metaData.getId())) {
            Util.d("MainActivity  status of video is different from target video, ignore.");

            return;
        }

        if (isSwitchingVideo) {
            if ((int) event.status.getTime() == 0) {
                isSwitchingVideo = false;
            } else {
                Util.d("MainActivity  switching to new video, ignore the wrong data.");
                return;
            }
        }

        if (isSeeking) {
            if ((int) event.status.getTime() == expectedSeekbarValue) {
                isSeeking = false;
            } else {
                Util.d("MainActivity  in the seeking mode, ignore the status update.");
                return;
            }
        }


        // Only handle the event when it is not seeking.
        currentStatus = event.status;

        //Retrieve the metadata if it is null.
        if (metaData == null) {
            metaData = findMetaDataById(currentStatus.getId());
        }
        updateUI();
    }

    // This method will be called when a app state event is received.
    public void onEvent(PlaybackEvent event) {
        Util.d("MainActivity  PlaybackEvent: " + event.isStart());

        // Set play control button according to playback state.
        if (event.isStart()) {
            playControl.setState(PlayControlImageView.State.play);

            // Show notification
            showNotification();
        } else {

            //Show the retry button only when it is not switching video.
            if (!isSwitchingVideo) {
                playControl.setState(PlayControlImageView.State.retry);

                //Make sure the seek bar reaches the end.
                updateMediaPosition(seekBar.getMax());

                showNotification();
            }
        }
    }


    // This method will be called when a MessageEvent is posted
    public void onEvent(ConnectionChangedEvent event) {
        super.onEvent(event);

        Util.d("MainActivity ConnectionChangedEvent: " + event.toString());

        // We only handle disconnect event.
        if (event.errorMessage == null) {
            if (!mMultiscreenManager.isTVConnected()) {
                handleDisconnect();
            } else {
                //Send app state request when TV is connected.
                mMultiscreenManager.requestAppState();
            }
        }
    }


    /**
     * Play a new video and overwrite the currently video.
     * The video will be started at given position.
     */
    public void overwritePlaying(String metatdata) {
        metaData = MetaData.parse(metatdata, MetaData.class);

        if (currentStatus != null) {
            currentStatus.setTime(0);
        }
        play();
    }


    //==============================================================================================
    //      Internal methods.
    //==============================================================================================


    /**
     * Handle the service disconnection event.
     */
    private void handleDisconnect() {
        Util.d("TV is disconnected.");

        // Remove notification.
        clearNotification();

        // Continue to play the video with local player if the video is not ended.
        if (playControl.getState() != PlayControlImageView.State.retry) {
            openLocalVideoPlayer();
        }

        // Hide the playback control panel.
        currentStatus = null;
        updateUI();
    }


    /**
     * Find the metadata by given id.
     *
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
            if (id.equals(md.getId())) {
                Util.d("    found meta data = " + md);
                return md;
            }
        }

        return null;
    }

    /**
     * Set up the playback panel.
     */
    private void setupPlaybackPanel() {
        // Playback control panel.
        playControlLayout = (LinearLayout) findViewById(R.id.playControlLayout);
        playControlLayout.setVisibility(View.GONE);

        playText = (TextView) findViewById(R.id.playText);

        // Play/Pause button.
        playControl = (PlayControlImageView) findViewById(R.id.playControl);
        playControl.setState(PlayControlImageView.State.play);
        playControl.setUseSmallIcon(true);
        playControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOnPlayControlButtonClick();
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
                expectedSeekbarValue = seekBar.getProgress();
                postionTextView.setText(Util.formatTimeString(seekBar.getProgress() * 1000));
                mMultiscreenManager.seek(seekBar.getProgress());
            }
        });

        postionTextView = (TextView) findViewById(R.id.positionTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
    }

    private void handleOnPlayControlButtonClick() {
        PlayControlImageView.State state = playControl.getState();
        if (state == PlayControlImageView.State.retry) {
            replay();
        } else if (state == PlayControlImageView.State.pause) {
            resume();
        } else {
            pause();
        }

        showNotification();
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
            currentStatus.setDuration(metaData.getDuration());
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
            if (metaData != null && currentStatus != null && currentStatus.getId() != null) {
                playText.setText(metaData.getTitle());
                playControl.setSelected(currentStatus.isPlaying());

                int duration = (int) currentStatus.getDuration();
                if (duration <= 0) {
                    //Duration in the meta data is seconds.
                    Util.d("metaData=" + metaData);
                    duration = metaData.getDuration();
                }
                seekBar.setMax(duration);
                durationTextView.setText(Util.formatTimeString(duration * 1000));

                updateMediaPosition((int) currentStatus.getTime());


                if (playControl.getState() != PlayControlImageView.State.retry) {
                    playControl.setState(currentStatus.isPlaying() ? PlayControlImageView.State.play : PlayControlImageView.State.pause);
                }

                // Make sure the playback control panel is below toolbar.
                params.addRule(RelativeLayout.BELOW, R.id.toolbar);
            }
        } else {

            // Restore the toolbar position (floating).
            params.removeRule(RelativeLayout.BELOW);
        }

        // Update the connected service name or app name.
        appText.setText(isPlayingOnTV ? Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName()) : getString(R.string.app_name));
        iconImageView.setVisibility(isPlayingOnTV ? View.VISIBLE : View.GONE);

        // Update toolbar background color
        toolbar.setBackgroundColor(isPlayingOnTV ? getResources().getColor(R.color.black) : getResources().getColor(R.color.toolbar_background_color));

        // Update now playing item.
        libraryAdapter.setNowPlaying(isPlayingOnTV ? currentStatus.getId() : null);
    }

    private void updateMediaPosition(int position) {
        // Ignore the wrong position when error happens.
        if (position > seekBar.getMax()) {
            Util.d("updateMediaPosition ignore wrong position at: " + position);
            Util.d("updateMediaPosition seekbar max: " + seekBar.getMax());
            return;
        }

        seekBar.setProgress(position);
        postionTextView.setText(Util.formatTimeString(position * 1000));
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
     * Play a video on TV.
     */
    private void play() {
        Util.d("play() is called, metadata=" + metaData);
        if (metaData != null) {
            mMultiscreenManager.play(metaData);
            playControl.setState(PlayControlImageView.State.play);
        }
    }


    /**
     * Resume video from current position.
     */
    private void resume() {
        mMultiscreenManager.resume();
        playControl.setState(PlayControlImageView.State.play);
    }

    /**
     * Pause video.
     */
    private void pause() {
        mMultiscreenManager.pause();
        playControl.setState(PlayControlImageView.State.pause);
    }

    /**
     * Play a new video on TV from beginning.
     */
    private void replay() {
        Util.d("replay() is called, metadata=" + metaData);

        isSeeking = false;
        if (metaData != null) {
            mMultiscreenManager.play(metaData, 0);
            playControl.setState(PlayControlImageView.State.play);
        }
    }

    /**
     * Show notification.
     */
    private void showNotification() {
        Notification notification = getNotification(metaData);
        if (notification != null) {
            // Clear previous notification.
            clearNotification();

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }

        registerBroadcastListener();
    }

    /**
     * Remove notification.
     */
    private void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        unregisterBroadcastListener();
    }

    /**
     * Create notification and set its state.
     *
     * @param metaData
     * @return
     */
    private Notification getNotification(MetaData metaData) {
        if (metaData == null) {
            return null;
        }

        // Using RemoteViews to bind custom layouts into Notification
        RemoteViews notificationView = new RemoteViews(
                getApplicationContext().getPackageName(), R.layout.notification
        );

        // Locate and set the Text into customnotificationtext.xml TextViews
        notificationView.setTextViewText(R.id.content, metaData.getTitle());


        // Create pending intent and notification instance.
        PendingIntent pIntent = PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(getApplicationContext())
                // Set Icon
                .setSmallIcon(R.drawable.appicon_trailmix)
                .setAutoCancel(false)
                .setContentTitle("Boarding pass")
                .setContentText("Click for more info")
                        // Set PendingIntent into Notification
                .setContentIntent(pIntent).build();

        // Do not clear the notification
        n.flags |= FLAG_NO_CLEAR;

        //Load the cover image.
        Picasso.with(this).load(Uri.parse(metaData.getCover())).
                into(notificationView, R.id.coverImage, NOTIFICATION_ID, n);

        // This is the intent that is supposed to be called when the playback button is clicked
        Intent switchIntent = null;
        if (playControl.getState() == PlayControlImageView.State.play) {
            switchIntent = new Intent(PlayActionBroadcastReceiver.ACTION_PAUSE);
            notificationView.setImageViewResource(R.id.playControl, R.drawable.ic_pause_dark_sm);
        } else if (playControl.getState() == PlayControlImageView.State.pause) {
            switchIntent = new Intent(PlayActionBroadcastReceiver.ACTION_PLAY);
            notificationView.setImageViewResource(R.id.playControl, R.drawable.ic_play_dark_sm);
        } else {
            switchIntent = new Intent(PlayActionBroadcastReceiver.ACTION_REPLAY);
            notificationView.setImageViewResource(R.id.playControl, R.drawable.ic_replay_dark_sm);
        }

        PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(this, 0, switchIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.playControl, pendingSwitchIntent);

        // Use customized layout.
        n.contentView = notificationView;

        return n;
    }

    /**
     * Register broadcast receiver to receive play/pause/replay action from notification.
     */
    private void registerBroadcastListener() {
        // Remove previous receiver if it is not null.
        if (broadcastReceiver != null) {
            unregisterBroadcastListener();
        }

        // Create intent filter.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        // set the custom action
        intentFilter.addAction(PlayActionBroadcastReceiver.ACTION_PLAY);
        intentFilter.addAction(PlayActionBroadcastReceiver.ACTION_PAUSE);
        intentFilter.addAction(PlayActionBroadcastReceiver.ACTION_REPLAY);

        // register the receiver
        broadcastReceiver = new PlayActionBroadcastReceiver();
        registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * Unregister broadcast receiver.
     */
    private void unregisterBroadcastListener() {
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
            }
        }

        broadcastReceiver = null;
    }

    /**
     * The broadcast receiver to receive the play button actions from notification.
     */
    public class PlayActionBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION_PLAY = "com.samsung.trailmix.ACTION_PLAY";
        public static final String ACTION_PAUSE = "com.samsung.trailmix.ACTION_PAUSE";
        public static final String ACTION_REPLAY = "com.samsung.trailmix.ACTION_REPLAY";

        @Override
        public void onReceive(Context context, Intent intent) {
            handleOnPlayControlButtonClick();
        }
    }
}
