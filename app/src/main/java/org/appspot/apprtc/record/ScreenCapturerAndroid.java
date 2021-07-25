package org.appspot.apprtc.record;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjection.Callback;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Surface;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import kr.wayde.webrtc.gpupack.GpuImageEffector;

@TargetApi(21)
public class ScreenCapturerAndroid implements VideoCapturer, VideoSink {
    private static final int DISPLAY_FLAGS = 3;
    private static final int VIRTUAL_DISPLAY_DPI = 400;
    private final Intent mediaProjectionPermissionResultData;
    private final Callback mediaProjectionCallback;
    private final Context mContext;
    private int width;
    private int height;
    @Nullable
    private VirtualDisplay virtualDisplay;
    @Nullable
    private SurfaceTextureHelper surfaceTextureHelper;
    @Nullable
    private CapturerObserver capturerObserver;
    private long numCapturedFrames;
    @Nullable
    private MediaProjection mediaProjection;
    private boolean isDisposed;
    @Nullable
    private MediaProjectionManager mediaProjectionManager;

    public ScreenCapturerAndroid(Intent mediaProjectionPermissionResultData, Callback mediaProjectionCallback, Context context) {
        this.mediaProjectionPermissionResultData = mediaProjectionPermissionResultData;
        this.mediaProjectionCallback = mediaProjectionCallback;
        this.mContext = context;
    }

    private void checkNotDisposed() {
        if (this.isDisposed) {
            throw new RuntimeException("capturer is disposed.");
        }
    }

    @Nullable
    public MediaProjection getMediaProjection() {
        return this.mediaProjection;
    }

    @SuppressLint("WrongConstant")
    public synchronized void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.checkNotDisposed();
        if (capturerObserver == null) {
            throw new RuntimeException("capturerObserver not set.");
        } else {
            this.capturerObserver = capturerObserver;
            if (surfaceTextureHelper == null) {
                throw new RuntimeException("surfaceTextureHelper not set.");
            } else {
                this.surfaceTextureHelper = surfaceTextureHelper;
                this.mediaProjectionManager = (MediaProjectionManager) applicationContext.getSystemService("media_projection");
            }
        }
    }

    public synchronized void startCapture(int width, int height, int ignoredFramerate) {
        this.checkNotDisposed();
        this.width = width;
        this.height = height;
        this.mediaProjection = this.mediaProjectionManager.getMediaProjection(-1, this.mediaProjectionPermissionResultData);
        this.mediaProjection.registerCallback(this.mediaProjectionCallback, this.surfaceTextureHelper.getHandler());
        this.createVirtualDisplay();
        this.capturerObserver.onCapturerStarted(true);
        this.surfaceTextureHelper.startListening(this);
    }

    public synchronized void stopCapture() {
        this.checkNotDisposed();
        ThreadUtils.invokeAtFrontUninterruptibly(this.surfaceTextureHelper.getHandler(), new Runnable() {
            public void run() {

                ScreenCapturerAndroid.this.surfaceTextureHelper.stopListening();
                ScreenCapturerAndroid.this.capturerObserver.onCapturerStopped();

                if (ScreenCapturerAndroid.this.mGpuImageEffector != null) {
                    ScreenCapturerAndroid.this.mGpuImageEffector.stopPreview();
                    ScreenCapturerAndroid.this.mGpuImageEffector.destroyProcessor();
                    ScreenCapturerAndroid.this.mGpuImageEffector = null;
                }

                if (ScreenCapturerAndroid.this.virtualDisplay != null) {
                    ScreenCapturerAndroid.this.virtualDisplay.release();
                    ScreenCapturerAndroid.this.virtualDisplay = null;
                }

                if (ScreenCapturerAndroid.this.mediaProjection != null) {
                    ScreenCapturerAndroid.this.mediaProjection.unregisterCallback(ScreenCapturerAndroid.this.mediaProjectionCallback);
                    ScreenCapturerAndroid.this.mediaProjection.stop();
                    ScreenCapturerAndroid.this.mediaProjection = null;
                }

            }
        });
    }

    public synchronized void dispose() {
        this.isDisposed = true;
    }

    public synchronized void changeCaptureFormat(int width, int height, int ignoredFramerate) {
        this.checkNotDisposed();
        this.width = width;
        this.height = height;
        if (this.virtualDisplay != null) {
            ThreadUtils.invokeAtFrontUninterruptibly(this.surfaceTextureHelper.getHandler(), new Runnable() {
                public void run() {
                    ScreenCapturerAndroid.this.virtualDisplay.release();
                    ScreenCapturerAndroid.this.createVirtualDisplay();
                }
            });
        }
    }

    private GpuImageEffector mGpuImageEffector = null;

    private void createVirtualDisplay() {
        this.surfaceTextureHelper.setTextureSize(this.width, this.height);


        Surface surface = new Surface(this.surfaceTextureHelper.getSurfaceTexture());
        mGpuImageEffector = new GpuImageEffector(mContext);
        mGpuImageEffector.setThreadHandler(this.surfaceTextureHelper.getHandler());
        mGpuImageEffector.createProcessor();
        mGpuImageEffector.initProcessor(this.width, this.height, 30);
        mGpuImageEffector.createSession(surface, (surface1, texture) -> {
            this.virtualDisplay = this.mediaProjection.createVirtualDisplay("WebRTC_ScreenCapture", this.width, this.height, 500, 3,
                    surface1, (android.hardware.display.VirtualDisplay.Callback) null, (Handler) null);

            mGpuImageEffector.startPreview();
        });


    }

    public void onFrame(VideoFrame frame) {
        ++this.numCapturedFrames;
        this.capturerObserver.onFrameCaptured(frame);
    }

    public boolean isScreencast() {
        return true;
    }

    public long getNumCapturedFrames() {
        return this.numCapturedFrames;
    }
}
