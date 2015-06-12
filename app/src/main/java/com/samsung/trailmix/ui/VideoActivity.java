

package com.samsung.trailmix.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.MediaController;
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
import com.google.android.exoplayer.util.Util;
import com.samsung.trailmix.R;
import com.samsung.trailmix.interceptor.AppCompatActivityMenuKeyInterceptor;
import com.samsung.trailmix.multiscreen.model.MetaData;
import com.samsung.trailmix.player.DemoUtil;
import com.samsung.trailmix.player.EventLogger;
import com.samsung.trailmix.player.SmoothStreamingTestMediaDrmCallback;
import com.samsung.trailmix.player.WidevineTestMediaDrmCallback;

import java.util.Map;


/**
 * Activity for view videos
 */
public class VideoActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener,
        DemoPlayer.Listener, DemoPlayer.TextListener, DemoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener {
    private Toolbar toolbar;

    private EventLogger eventLogger;
    private MediaController mediaController;
    private VideoSurfaceView surfaceView;

    private DemoPlayer player;
    private boolean playerNeedsPrepare;

    private long playerPosition;
    private boolean enableBackgroundAudio;

    //The metadata to be played.
    private MetaData metaData;

    private Uri contentUri;
    private int contentType;
    private String contentId;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;

    //The view to render debug info.
    private TextView debugTextView = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity);

        //Initialize the interceptor
        AppCompatActivityMenuKeyInterceptor.intercept(this);

        //Add toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        toolbar.setBackgroundColor(Color.GRAY);
        TextView textView = (TextView)toolbar.findViewById(R.id.appText);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent != null) {
            String jstr = intent.getStringExtra("json");
            if (jstr != null) {
                metaData = MetaData.parse(jstr, MetaData.class);
            }
        }

        //play default movide when no data is passed.
        if (metaData == null) {
            metaData = new MetaData();
            metaData.setTitle("Jurassic World");
            metaData.setDuration(133);
            metaData.setFile("https://s3.amazonaws.com/dev-multiscreen-video-library/trailers/Avengers_2_trailer_3_51-1080p-HDTN.mp4");
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                //When the S icon is clicked, opens the service list dialog.
//                showServiceListDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        //if (view == retryButton) {
        //    preparePlayer();
        //}
    }

    // AudioCapabilitiesReceiver.Listener methods

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;
            releasePlayer();
            preparePlayer();
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }

    // Internal methods


    private DemoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        switch (contentType) {
            case DemoUtil.TYPE_SS:
                return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
                        new SmoothStreamingTestMediaDrmCallback(), debugTextView);
            case DemoUtil.TYPE_DASH:
                return new DashRendererBuilder(this, userAgent, contentUri.toString(),
                        new WidevineTestMediaDrmCallback(contentId), debugTextView, audioCapabilities);
            case DemoUtil.TYPE_HLS:
                return new HlsRendererBuilder(this, userAgent, contentUri.toString(), debugTextView,
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
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            mediaController.setMediaPlayer(player.getPlayerControl());
            mediaController.setEnabled(true);
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
//            updateButtonVisibilities();
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }


    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
//            showControls();
        }
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                break;
            default:
                text += "unknown";
                break;
        }
//        playerStateTextView.setText(text);
//        updateButtonVisibilities();
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
//        updateButtonVisibilities();
//        showControls();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
//        shutterView.setVisibility(View.GONE);
        surfaceView.setVideoWidthHeightRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }

    // DemoPlayer.TextListener implementation

    @Override
    public void onText(String text) {
        com.samsung.trailmix.util.Util.d("onText: " + text);

        if (TextUtils.isEmpty(text)) {
            //subtitleView.setVisibility(View.INVISIBLE);
        } else {
            ///subtitleView.setVisibility(View.VISIBLE);
            //subtitleView.setText(text);
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
}
