

package com.samsung.trailmix.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.VideoSurfaceView;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.demo.player.DashRendererBuilder;
import com.google.android.exoplayer.demo.player.DemoPlayer;
import com.google.android.exoplayer.demo.player.ExtractorRendererBuilder;
import com.google.android.exoplayer.demo.player.HlsRendererBuilder;
import com.google.android.exoplayer.demo.player.SmoothStreamingRendererBuilder;
import com.google.android.exoplayer.demo.player.UnsupportedDrmException;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer.extractor.ts.TsExtractor;
import com.google.android.exoplayer.extractor.webm.WebmExtractor;
import com.google.android.exoplayer.metadata.GeobMetadata;
import com.google.android.exoplayer.metadata.PrivMetadata;
import com.google.android.exoplayer.metadata.TxxxMetadata;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.SubtitleView;
import com.google.android.exoplayer.util.Util;
import com.samsung.trailmix.R;
import com.samsung.trailmix.multiscreen.events.AppStateEvent;
import com.samsung.trailmix.multiscreen.model.CurrentStatus;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.player.DemoUtil;
import com.samsung.trailmix.player.EventLogger;
import com.samsung.trailmix.player.SmoothStreamingTestMediaDrmCallback;
import com.samsung.trailmix.player.WidevineTestMediaDrmCallback;
import com.samsung.trailmix.ui.view.PlayControlImageView;
import com.squareup.picasso.Picasso;

import java.util.Map;


/**
 * Activity for view videos
 */
public class VideoActivity extends BaseActivity implements SurfaceHolder.Callback, View.OnClickListener,
        DemoPlayer.Listener, DemoPlayer.TextListener, DemoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener {

    private static final float CAPTION_LINE_HEIGHT_RATIO = 0.0533f;


    private EventLogger eventLogger;

    // The shutter view when we pause the video
    private View shutterView;

    // The caption view
    private SubtitleView subtitleView;


    //The view to render debug info.
    private TextView debugTextView = null;

    // The exo player
    private DemoPlayer player;

    // Flag to call prepare() method.
    private boolean playerNeedsPrepare;

    // The flag whether or not to use background audio, by default it is false.
    private boolean enableBackgroundAudio = false;

    //The current playing state.
    private CurrentStatus currentStatus;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;

    // The video to display video.
    private VideoSurfaceView surfaceView;

    private PlayControlImageView playControlImageView;

    // The root of the control panel
    private RelativeLayout controls_root;

    // The seek bar for video playback control.
    private SeekBar seekBar;

    // The media playback position.
    private TextView postionTextView;

    // The video duration.
    private TextView durationTextView;

    // The cover screen of the movie displayed when the video is finished.
    private ImageView cover;

    //==============================================================================================
    //      Activity methods
    //==============================================================================================

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        // Set up toolbar.
        setupToolbar();

        // Custom toolbar color
        toolbar.setBackgroundColor(getResources().getColor(R.color.video_toolbar_background));
        appText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        appText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        // Add back button in toolbar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Read the video information
        readPlaybackInfo();

        // Play default movie when no data is passed.
        setDefaultValueIfNull();

        // create AudioCapabilitiesReceiver instance.
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getApplicationContext(), this);

        // set up views.
        View root = findViewById(R.id.root);
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    togglePanels();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    com.samsung.trailmix.util.Util.d("onTouch, view=" + view.toString());
                    view.performClick();
                }
                return true;
            }
        });
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    //return mediaController.dispatchKeyEvent(event);
                    return playControlImageView.dispatchKeyEvent(event);
                }
                return false;
            }
        });
        shutterView = findViewById(R.id.shutter);
        cover = (ImageView)findViewById(R.id.cover);
        Picasso.with(this).load(com.samsung.trailmix.util.Util.getUriFromUrl(metaData.getCover())).into(cover);
        controls_root = (RelativeLayout) findViewById(R.id.controls_root);
        seekBar = (SeekBar) findViewById(R.id.videoSeekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {

                    seekTo(seekBar.getProgress());

                    // When in retry mode and seekbar is pressed, automatically play from the position.
                    if (playControlImageView.getState() == PlayControlImageView.State.retry) {
                        cover.setVisibility(View.GONE);
                        play();
                    }
                }
            }
        });
        postionTextView = (TextView) findViewById(R.id.positionTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        surfaceView = (VideoSurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
        subtitleView = (SubtitleView) findViewById(R.id.subtitles);

        // The play/pause/retry button at the center of the screen.
        playControlImageView = (PlayControlImageView) findViewById(R.id.playControl);
        playControlImageView.setOnClickListener(this);

        // Update the movie title.
        appText.setText(metaData.getTitle());

        // Hide the TV icon.
        iconImageView.setVisibility(View.GONE);

        DemoUtil.setDefaultCookieManager();
    }


    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onResume() {
        super.onResume();
        configureSubtitleView();

        // The player will be prepared on receiving audio capabilities.
        audioCapabilitiesReceiver.register();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (!enableBackgroundAudio) {
            currentStatus.setTime(player.getCurrentPosition());
            releasePlayer();
        } else {
            player.setBackgrounded(true);
        }

        try {
            audioCapabilitiesReceiver.unregister();
        } catch (Exception e) {
        }

        // Hide the shutter view.
        shutterView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Release player and resource before exit.
        releasePlayer();
    }

    //==============================================================================================
    //      Internal methods
    //==============================================================================================

    /**
     * Update the media playback position every second.
     */
    private Runnable updateMediaPosition = new Runnable() {

        @Override
        public void run() {
            if (player != null) {
                updateMediaPosition((int)player.getCurrentPosition());

                // Start to update the media position at next second.
                handler.postDelayed(updateMediaPosition, 1000);
            }
        }
    };

    private void updateMediaPosition(int position) {
        // Ignore the wrong position when error happens.
        if (position>seekBar.getMax()) {
            com.samsung.trailmix.util.Util.d("updateMediaPosition ignore wrong position at: " + position);
            return;
        }

        seekBar.setProgress(position);
        postionTextView.setText(com.samsung.trailmix.util.Util.formatTimeString(position));
        currentStatus.setTime(position/1000);
    }

    /**
     * Read the playback information from intent.
     */
    private void readPlaybackInfo() {
        Intent intent = getIntent();
        if (intent != null) {
            String jstr = intent.getStringExtra("meta");
            if (jstr != null) {
                try {
                    metaData = MetaData.parse(jstr, MetaData.class);
                    com.samsung.trailmix.util.Util.d("VideoActivity Read meta data : " + metaData);
                } catch (Exception e) {
                }
            }

            String status = intent.getStringExtra("status");
            if (status != null) {
                try {
                    currentStatus = CurrentStatus.parse(status, CurrentStatus.class);
                    com.samsung.trailmix.util.Util.d("VideoActivity Read currentStatus : " + currentStatus);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Use default value when no video information is passed.
     */
    private void setDefaultValueIfNull() {
        // Play default movie when no data is passed.
        if (metaData == null) {
            metaData = new MetaData();
            metaData.setId("big-buck-bunny");
            metaData.setTitle("Big Buck Bunny");
            metaData.setDuration(33000);
            metaData.setCover("http://s3-us-west-1.amazonaws.com/dev-multiscreen-examples/examples/trailmix/trailers/big-buck-bunny.png");
            metaData.setType("mp4");
            metaData.setFile("http://s3-us-west-1.amazonaws.com/dev-multiscreen-examples/examples/trailmix/trailers/big-buck-bunny.mp4");
        }

        if (currentStatus == null) {
            currentStatus = new CurrentStatus();
            currentStatus.setId(metaData.getId());
            currentStatus.setState(CurrentStatus.STATE_PLAYING);
            currentStatus.setTime(0);
        }
    }


    /**
     * Get the renderer builder according to the type of media.
     * @return
     */
    private DemoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this, "TrailMix");

        Uri contentUri = Uri.parse(metaData.getFile());
        int contentType = DemoUtil.parseMediaType(metaData.getType());

        switch (contentType) {
            case DemoUtil.TYPE_SS:
                return new SmoothStreamingRendererBuilder(this, userAgent, metaData.getFile(),
                        new SmoothStreamingTestMediaDrmCallback(), debugTextView);
            case DemoUtil.TYPE_DASH:
                return new DashRendererBuilder(this, userAgent, metaData.getFile(),
                        new WidevineTestMediaDrmCallback(metaData.getTitle()), debugTextView, audioCapabilities);
            case DemoUtil.TYPE_HLS:
                return new HlsRendererBuilder(this, userAgent, metaData.getFile(), debugTextView,
                        audioCapabilities);
            case DemoUtil.TYPE_M4A: // There are no file format differences between M4A and MP4.
            case DemoUtil.TYPE_MP4:
                return new ExtractorRendererBuilder(this, userAgent, contentUri, debugTextView,
                        new Mp4Extractor());
            case DemoUtil.TYPE_MP3:
                return new ExtractorRendererBuilder(this, userAgent, contentUri, debugTextView,
                        new Mp3Extractor());
            case DemoUtil.TYPE_TS:
                return new ExtractorRendererBuilder(this, userAgent, contentUri, debugTextView,
                        new TsExtractor(0, audioCapabilities));
            case DemoUtil.TYPE_AAC:
                return new ExtractorRendererBuilder(this, userAgent, contentUri, debugTextView,
                        new AdtsExtractor());
            case DemoUtil.TYPE_WEBM:
                return new ExtractorRendererBuilder(this, userAgent, contentUri, debugTextView,
                        new WebmExtractor());
            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    /**
     * Prepare a new player and resume previous position.
     */
    private void preparePlayer() {
        if (player == null) {
            player = new DemoPlayer(getRendererBuilder());
            player.addListener(this);
            player.setTextListener(this);
            player.setMetadataListener(this);

            updateDuration();

            // The time is in seconds.
            com.samsung.trailmix.util.Util.d("preparePlayer, seek to:" + currentStatus.getTime()*1000);
            seekTo(currentStatus.getTime()*1000);

            playerNeedsPrepare = true;
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);

        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(surfaceView.getHolder().getSurface());

        if (currentStatus != null && !currentStatus.isPlaying()) {

            // Movie is paused.
            playControlImageView.setState(PlayControlImageView.State.pause);
        } else {

            // Play video when there is no current statua or it is playing state.
            player.setPlayWhenReady(true);

            // update the play control button
            playControlImageView.setState(PlayControlImageView.State.play);
        }
    }


    /**
     * Repleae the player and related resource.
     */
    private void releasePlayer() {
        // Cancel media position update.
        handler.removeCallbacks(updateMediaPosition);

        // Release the player and end the event logger.
        if (player != null) {
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    /**
     * Seek the video to give position.
     * @param time the new position to seek to.
     */
    private void seekTo(float time) {
        player.seekTo((long) time);
        updateMediaPosition((int)time);
    }

    /**
     * Play the video and update play control button state.
     */
    private void play() {
        player.getPlayerControl().start();
        playControlImageView.setState(PlayControlImageView.State.play);
    }

    /**
     * Pause the video and update play control button state.
     */
    private void pause() {
        player.getPlayerControl().pause();
        playControlImageView.setState(PlayControlImageView.State.pause);
    }

    /**
     * Update the video duration on UI.
     */
    private void updateDuration() {
        long duration = 0;

        if (player != null) {
            duration = player.getDuration();

            // If we could not get the duration from player, use the duration from metadata.
            if (duration < 0) {
                // The duration in meta data is seconds.
                duration = metaData.getDuration()*1000;
            }
        } else {
            duration = metaData.getDuration()*1000;
        }

        // Update the seekbar and duration textview.
        com.samsung.trailmix.util.Util.d("updateDuration, max value:" + duration);
        seekBar.setMax((int) duration);
        durationTextView.setText(com.samsung.trailmix.util.Util.formatTimeString(duration));
    }

    // Caption related functions.

    private void configureSubtitleView() {
        CaptionStyleCompat captionStyle;
        float captionTextSize = getCaptionFontSize();
        if (Util.SDK_INT >= 19) {
            captionStyle = getUserCaptionStyleV19();
            captionTextSize *= getUserCaptionFontScaleV19();
        } else {
            captionStyle = CaptionStyleCompat.DEFAULT;
        }
        subtitleView.setStyle(captionStyle);
        subtitleView.setTextSize(captionTextSize);
    }

    private float getCaptionFontSize() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        return Math.max(getResources().getDimension(R.dimen.subtitle_minimum_font_size),
                CAPTION_LINE_HEIGHT_RATIO * Math.min(displaySize.x, displaySize.y));
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }


    /**
     * Toggle the visibility of panels.
     */
    private void togglePanels() {
        showPanels(toolbar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    /**
     * Show or hide the toolbar and control panel.
     * @param visible the visibility of the panels.
     */
    private void showPanels(int visible) {
        controls_root.setVisibility(visible);
        toolbar.setVisibility(visible);
        playControlImageView.setVisibility(visible);

        // Hide panels after certain seconds.
        resetAutoHideTimer();
    }

    /**
     * Reset the auto-hide timer.
     */
    private void resetAutoHideTimer() {
        // Cancel the previous auto hide task.
        handler.removeCallbacks(hidePanelThread);

        // When panels are displaying and it is in play state, we will auto hide them after certain seconds.
        if (toolbar.getVisibility() == View.VISIBLE && playControlImageView.getState()== PlayControlImageView.State.play) {
            handler.postDelayed(hidePanelThread, 3000);
        }
    }


    private Runnable hidePanelThread = new Runnable() {

        @Override
        public void run() {
            showPanels(View.GONE);
        }
    };


    //==============================================================================================
    //      Listener implementations
    //==============================================================================================

    // OnClickListener methods
    @Override
    public void onClick(View view) {
        com.samsung.trailmix.util.Util.d("onClick");
        if (view == playControlImageView) {
            PlayControlImageView.State state = playControlImageView.getState();
            if (state == PlayControlImageView.State.retry) {

                // Hide the cover image.
                cover.setVisibility(View.GONE);

                // Replay from beginning.
                currentStatus.setTime(0);

                // Start to play automatically.
                currentStatus.setState(CurrentStatus.STATE_PLAYING);

                releasePlayer();
                preparePlayer();
            } else if (state == PlayControlImageView.State.pause) {
                play();
            } else {
                pause();
            }

            resetAutoHideTimer();
        }
    }

    // AudioCapabilitiesReceiver.Listener methods

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        com.samsung.trailmix.util.Util.d("onAudioCapabilitiesChanged=" + audioCapabilities.toString());
        com.samsung.trailmix.util.Util.d("current audioCapabilities=" + this.audioCapabilities);

        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        com.samsung.trailmix.util.Util.d("audioCapabilitiesChanged=" + audioCapabilitiesChanged);
        com.samsung.trailmix.util.Util.d("player=" + player);

        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;

            releasePlayer();
            preparePlayer();
        } else if (player != null) {

            com.samsung.trailmix.util.Util.d("Move the player to foreground.");
            player.setBackgrounded(false);
        }
    }


    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";

                // Display cover view when movie is ended.
                cover.setVisibility(View.VISIBLE);

                // Display retry button
                playControlImageView.setState(PlayControlImageView.State.retry);
                showPanels(View.VISIBLE);

                //Make sure the seek bar reaches the end.
                updateMediaPosition(seekBar.getMax());

                //Cancel media position update.
                handler.removeCallbacks(updateMediaPosition);

                //Cancel auto hide task.
                handler.removeCallbacks(hidePanelThread);

                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";

                updateDuration();

                // Show the panels when video is started.
                showPanels(View.VISIBLE);

                // Update seek bar.
                handler.postDelayed(updateMediaPosition, 1000);

                break;
            default:
                text += "unknown";
                break;
        }

        com.samsung.trailmix.util.Util.d("onStateChanged: " + text);
    }

    @Override
    public void onError(Exception e) {
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            int stringId = unsupportedDrmException.reason == UnsupportedDrmException.REASON_NO_DRM
                    ? R.string.drm_error_not_supported
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? R.string.drm_error_unsupported_scheme
                    : R.string.drm_error_unknown;
            Toast.makeText(getApplicationContext(), stringId, Toast.LENGTH_LONG).show();
        }
        playerNeedsPrepare = true;

        // Cancel media position update.
        handler.removeCallbacks(updateMediaPosition);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
        shutterView.setVisibility(View.GONE);
        cover.setVisibility(View.GONE);
        surfaceView.setVideoWidthHeightRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }

    // DemoPlayer.TextListener implementation

    @Override
    public void onText(String text) {
        com.samsung.trailmix.util.Util.d("onText: " + text);

        if (TextUtils.isEmpty(text)) {
            subtitleView.setVisibility(View.INVISIBLE);
        } else {
            subtitleView.setVisibility(View.VISIBLE);
            subtitleView.setText(text);
        }
    }

    // DemoPlayer.MetadataListener implementation

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (TxxxMetadata.TYPE.equals(entry.getKey())) {
                TxxxMetadata txxxMetadata = (TxxxMetadata) entry.getValue();
                com.samsung.trailmix.util.Util.d(String.format("ID3 TimedMetadata %s: description=%s, value=%s",
                        TxxxMetadata.TYPE, txxxMetadata.description, txxxMetadata.value));
            } else if (PrivMetadata.TYPE.equals(entry.getKey())) {
                PrivMetadata privMetadata = (PrivMetadata) entry.getValue();
                com.samsung.trailmix.util.Util.d(String.format("ID3 TimedMetadata %s: owner=%s",
                        PrivMetadata.TYPE, privMetadata.owner));
            } else if (GeobMetadata.TYPE.equals(entry.getKey())) {
                GeobMetadata geobMetadata = (GeobMetadata) entry.getValue();
                com.samsung.trailmix.util.Util.d(String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        GeobMetadata.TYPE, geobMetadata.mimeType, geobMetadata.filename,
                        geobMetadata.description));
            } else {
                com.samsung.trailmix.util.Util.d(String.format("ID3 TimedMetadata %s", entry.getKey()));
            }
        }
    }

    //==============================================================================================
    //      SurfaceHolder.Callback implementation
    //==============================================================================================

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
            player.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (player != null) {
            player.blockingClearSurface();
        }
    }


    //==============================================================================================
    //      Multiscreen events and functions
    //==============================================================================================

    /**
     * This method will be called when a app state event is received.
      * @param event
     */
    public void onEvent(AppStateEvent event) {
        com.samsung.trailmix.util.Util.d("VideoActivity  AppStateEvent: " + event.status);
        com.samsung.trailmix.util.Util.d("VideoActivity  currentStatus: " + currentStatus);


        String currentPlayingId = event.status.getId();



        if (currentPlayingId != null) {

            // Check if the same video is playing.
            if (!currentPlayingId.equals(currentStatus.getId())) {

                // Display join/overwrite dialog.
                String name = com.samsung.trailmix.util.Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName());
                showJoinOverwritetDialog(name, currentStatus.getTitle(), metaData.toJsonString());
            } else {

                //Same video, check if played at same position.
                if (event.status.getTime() == currentStatus.getTime()) {
                    //Played at same position, just join the video automatically.
                    finish();
                } else {

                    //Played at different position.
                    // Display join/overwrite dialog.
                    String name = com.samsung.trailmix.util.Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName());
                    showJoinOverwritetDialog(name, currentStatus.getTitle(), metaData.toJsonString());
                }

            }
        } else {

            // Nothing is played, play current video.
            mMultiscreenManager.play(metaData);

            // Exit the local player.
            finish();
        }

        currentStatus = event.status;
    }

    /**
     * Play a new video and overwrite the currently video.
     * The video will be started at given position.
     */
    public void overwritePlaying() {
        mMultiscreenManager.play(metaData, (int)currentStatus.getTime());
    }

}
