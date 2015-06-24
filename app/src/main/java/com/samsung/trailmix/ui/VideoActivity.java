

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
import android.widget.LinearLayout;
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

import java.util.Map;


/**
 * Activity for view videos
 */
public class VideoActivity extends BaseActivity implements SurfaceHolder.Callback, View.OnClickListener,
        DemoPlayer.Listener, DemoPlayer.TextListener, DemoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener {

    private static final float CAPTION_LINE_HEIGHT_RATIO = 0.0533f;


    private EventLogger eventLogger;
//    private MediaController mediaController;


    /**
     * The shutter view when we pause the video
     */
    private View shutterView;

    /**
     * The caption view
     */
    private SubtitleView subtitleView;


    //The view to render debug info.
    private TextView debugTextView = null;


    private DemoPlayer player;
    private boolean playerNeedsPrepare;

    private boolean enableBackgroundAudio;

    //The current playing state.
    private CurrentStatus currentStatus;


    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;

    //the video to display video.
    private VideoSurfaceView surfaceView;

    private PlayControlImageView playControlImageView;

    /**
     * The root of the control panel
     */
    private LinearLayout controls_root;

    private SeekBar seekBar;

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

        //Read the video information
        readPlaybackInfo();

        //play default movie when no data is passed.
        setDefaultValueIfNull();


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
                    return seekBar.dispatchKeyEvent(event);
                }
                return false;
            }
        });

        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getApplicationContext(), this);

        shutterView = findViewById(R.id.shutter);
        controls_root = (LinearLayout) findViewById(R.id.controls_root);
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
                    player.seekTo(seekBar.getProgress());
                }
            }
        });

        surfaceView = (VideoSurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
        subtitleView = (SubtitleView) findViewById(R.id.subtitles);

//        mediaController = new MediaController(this);
//        mediaController.setAnchorView(root);

        playControlImageView = (PlayControlImageView) findViewById(R.id.playControl);
        playControlImageView.setOnClickListener(this);

        DemoUtil.setDefaultCookieManager();

        updateUI();
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
        }catch (Exception e){}

        shutterView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        releasePlayer();
    }

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        com.samsung.trailmix.util.Util.d("onClick");
        if (view == playControlImageView) {
            PlayControlImageView.State state = playControlImageView.getState();
            if (state == PlayControlImageView.State.retry) {

                // Replay from beginning.
                currentStatus.setTime(0);
                //seekTo(0);
                releasePlayer();
                preparePlayer();
//                playControlImageView.setState(PlayControlImageView.State.play);
            } else if (state == PlayControlImageView.State.pause) {
                player.getPlayerControl().start();
                playControlImageView.setState(PlayControlImageView.State.play);
            } else {
                player.getPlayerControl().pause();
                playControlImageView.setState(PlayControlImageView.State.pause);
            }

            resetAutoHideTimer();
        }
    }

    // AudioCapabilitiesReceiver.Listener methods

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        com.samsung.trailmix.util.Util.d("onAudioCapabilitiesChanged=" + audioCapabilities.toString());

        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;

            releasePlayer();
            preparePlayer();
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }


    // This method will be called when a app state event is received.
    public void onEvent(AppStateEvent event) {
        com.samsung.trailmix.util.Util.d("VideoActivity  AppStateEvent: " + event.status);

        currentStatus = event.status;
        String currentPlayingId = currentStatus.getId();

        if (currentPlayingId != null) {

            // Display join/overwrite dialog.
            String name = com.samsung.trailmix.util.Util.getFriendlyTvName(mMultiscreenManager.getConnectedService().getName());
            showJoinOverwritetDialog(name, currentStatus.getTitle(), metaData.toJsonString());
        } else {

            // Nothing is played, play current video.
            mMultiscreenManager.play(metaData);

            // Exit the local player.
            finish();
        }
    }


    // Internal methods

    private Runnable updateMediaPosition = new Runnable() {

        @Override
        public void run() {
            if (player != null) {

                long position = player.getCurrentPosition();
                seekBar.setProgress((int)position);

                //Update the media position every second.
                handler.postDelayed(updateMediaPosition, 1000);
            }
        }
    };

    /**
     * Read the playback information from intent.
     */
    private void readPlaybackInfo() {
        Intent intent = getIntent();
        if (intent != null) {
            String jstr = intent.getStringExtra("meta");
            if (jstr != null) {
                try {
                    //metaData = MetaData.parse(jstr, MetaData.class);


                    //TODO: add temp as there is type to read.
                    //metaData.setType(DemoUtil.TYPE_MP4);

                    //com.samsung.trailmix.util.Util.d("VideoActivity Read meta data : " + metaData);
                } catch (Exception e) {
                }
            }

            String status = intent.getStringExtra("status");
            if (status != null) {
                try {
                    currentStatus = CurrentStatus.parse(status, CurrentStatus.class);


                    //TODO: add temp as there is type to read.
                    //metaData.setType(DemoUtil.TYPE_MP4);

                    //com.samsung.trailmix.util.Util.d("VideoActivity Read meta data : " + metaData);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Use default value when no video information is passed.
     */
    private void setDefaultValueIfNull() {
        //play default movie when no data is passed.
        if (metaData == null) {
            metaData = new MetaData();
            metaData.setId("1");
            metaData.setTitle("Big Buck Bunny (MP4 Video)");
            metaData.setDuration(596000);
            metaData.setType(DemoUtil.TYPE_MP4);
            metaData.setFile("http://redirector.c.youtube.com/videoplayback?id=604ed5ce52eda7ee&itag=22&source=youtube&"
                    + "sparams=ip,ipbits,expire,source,id&ip=0.0.0.0&ipbits=0&expire=19000000000&signature="
                    + "513F28C7FDCBEC60A66C86C9A393556C99DC47FB.04C88036EEE12565A1ED864A875A58F15D8B5300"
                    + "&key=ik0");

//            metaData.setType(DemoUtil.TYPE_DASH);
//            metaData.setFile("http://www.youtube.com/api/manifest/dash/id/3aa39fa2cc27967f/source/youtube?"
//                    + "as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&"
//                    + "ipbits=0&expire=19000000000&signature=A2716F75795F5D2AF0E88962FFCD10DB79384F29."
//                    + "84308FF04844498CE6FBCE4731507882B8307798&key=ik0");


            //metaData.setType(DemoUtil.TYPE_HLS);
            //metaData.setFile("https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8");
        }

        if (currentStatus == null) {
            currentStatus = new CurrentStatus();
            currentStatus.setId(metaData.getId());
            currentStatus.setState(CurrentStatus.STATE_PLAYING);
            currentStatus.setTime(0);
        }
    }

    private void updateUI() {
        appText.setText(metaData.getTitle());
    }

    private DemoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this, "TrailMix");

        Uri contentUri = Uri.parse(metaData.getFile());
        int contentType = metaData.getType();

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

    private void preparePlayer() {
        if (player == null) {
            player = new DemoPlayer(getRendererBuilder());
            player.addListener(this);
            player.setTextListener(this);
            player.setMetadataListener(this);

            com.samsung.trailmix.util.Util.d("preparePlayer, seek to:" + metaData.getDuration());
            com.samsung.trailmix.util.Util.d("preparePlayer, seek to:" + currentStatus.getTime());


            seekBar.setMax(metaData.getDuration());
            seekTo(currentStatus.getTime());

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
            updateButtonVisibilities();
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(true);

        // update the play control button
        playControlImageView.setState(PlayControlImageView.State.play);
    }

    private void releasePlayer() {
        //Cancel media position update.
        handler.removeCallbacks(updateMediaPosition);

        if (player != null) {
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    private void seekTo(float time) {
        player.seekTo((long) time);
        seekBar.setProgress((int) time);
    }

    // User controls

    private void updateButtonVisibilities() {
//    retryButton.setVisibility(playerNeedsPrepare ? View.VISIBLE : View.GONE);
//    videoButton.setVisibility(haveTracks(DemoPlayer.TYPE_VIDEO) ? View.VISIBLE : View.GONE);
//    audioButton.setVisibility(haveTracks(DemoPlayer.TYPE_AUDIO) ? View.VISIBLE : View.GONE);
//    textButton.setVisibility(haveTracks(DemoPlayer.TYPE_TEXT) ? View.VISIBLE : View.GONE);
    }


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


    private void togglePanels() {
        showPanels(toolbar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void showPanels(int visible) {
//        if (visible == 0) {
//
//            mediaController.show(visible);
//        } else {
//            mediaController.hide();
//        }

        controls_root.setVisibility(visible);
        toolbar.setVisibility(visible);
        playControlImageView.setVisibility(visible);

//        resetAutoHideTimer();
    }

    private void resetAutoHideTimer() {
        //Cancel the previous auto hide task.
        handler.removeCallbacks(hidePanelThread);

        //When panels are displaying, we will auto hide them after certain seconds.
        if (toolbar.getVisibility() == View.VISIBLE) {
            handler.postDelayed(hidePanelThread, 3000);
        }

    }

    Runnable hidePanelThread = new Runnable() {

        @Override
        public void run() {
            showPanels(View.GONE);
        }
    };

    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            //showControls();

            //show retry button
            //
            //
        }
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";

                // Display retry button
                playControlImageView.setState(PlayControlImageView.State.retry);
                showPanels(View.VISIBLE);

                //Cancel media position update.
                handler.removeCallbacks(updateMediaPosition);

                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";


                //Show the panels when video is started.
                showPanels(View.VISIBLE);

                //Update seekbar.
                handler.postDelayed(updateMediaPosition, 1000);

                break;
            default:
                text += "unknown";
                break;
        }

        com.samsung.trailmix.util.Util.d("onStateChanged: " + text);

//        playerStateTextView.setText(text);
        updateButtonVisibilities();
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
        updateButtonVisibilities();
        //showControls();



        //Cancel media position update.
        handler.removeCallbacks(updateMediaPosition);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
        shutterView.setVisibility(View.GONE);
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

    // SurfaceHolder.Callback implementation

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
            player.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (player != null) {
            player.blockingClearSurface();
        }
    }


    //==============================================================================================
    //      Multiscreen events and functions
    //==============================================================================================

//    // This method will be called when a app state event is recieved.
//    public void onEvent(AppStateEvent event) {
//        com.samsung.trailmix.util.Util.d("VideoActivity  AppStateEvent: " + event.status);
//        //updateUI();
//
//        if (metaData != null) {
//            mMultiscreenManager.play(metaData);
//        }
//    }


}
