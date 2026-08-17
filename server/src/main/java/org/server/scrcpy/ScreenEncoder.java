package org.server.scrcpy;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import org.server.scrcpy.audio.AudioCaptureException;
import org.server.scrcpy.model.MediaPacket;
import org.server.scrcpy.model.VideoPacket;
import org.server.scrcpy.wrappers.DisplayManager;
import org.server.scrcpy.wrappers.ServiceManager;
import org.server.scrcpy.wrappers.SurfaceControl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenEncoder implements Device.RotationListener {

    private static final int DEFAULT_FRAME_RATE = 60; // fps
    private static final int DEFAULT_I_FRAME_INTERVAL = 10; // seconds

    private static final int REPEAT_FRAME_DELAY = 6; // repeat after 6 frames

    private static final int MICROSECONDS_IN_ONE_SECOND = 1_000_000;

    private final AtomicBoolean rotationChanged = new AtomicBoolean();

    private final AtomicBoolean requestKeyFrame = new AtomicBoolean(false);

    private int bitRate;
    private int frameRate;
    private int iFrameInterval;
    private String mimeType = "video/avc";
    private String encoderName = "";

    public ScreenEncoder(int bitRate, int frameRate, int iFrameInterval) {
        this(bitRate, frameRate, iFrameInterval, "video/avc", "");
    }

    public ScreenEncoder(int bitRate, int frameRate, int iFrameInterval, String mimeType, String encoderName) {
        this.bitRate = bitRate;
        this.frameRate = frameRate > 0 ? frameRate : DEFAULT_FRAME_RATE;
        this.iFrameInterval = iFrameInterval;
        this.mimeType = mimeType == null || mimeType.isEmpty() ? "video/avc" : mimeType;
        this.encoderName = normalizeEncoderName(encoderName);
    }

    public ScreenEncoder(int bitRate) {
        this(bitRate, DEFAULT_FRAME_RATE, DEFAULT_I_FRAME_INTERVAL);
    }

    public ScreenEncoder(Options options) {
        this(options.getBitRate(), options.getFrameRate(), DEFAULT_I_FRAME_INTERVAL,
                options.getVideoMimeType(),
                options.hasCustomEncoder() ? options.getVideoEncoder() : "");
    }

    private static String normalizeEncoderName(String encoderName) {
        if (encoderName == null) {
            return "";
        }
        String name = encoderName.trim();
        if (name.isEmpty() || Options.DEFAULT_ENCODER.equals(name)) {
            return "";
        }
        return name;
    }

    private MediaCodec createCodec(int attempt) throws IOException {
        Exception last = null;
        boolean hevc = "video/hevc".equals(mimeType);
        String[] fallbacks;
        if (attempt == 0) {
            fallbacks = hevc
                    ? new String[]{encoderName, null, "c2.android.hevc.encoder", "OMX.google.hevc.encoder"}
                    : new String[]{encoderName, null, "c2.android.avc.encoder", "OMX.google.h264.encoder"};
        } else {
            fallbacks = hevc
                    ? new String[]{"c2.android.hevc.encoder", "OMX.google.hevc.encoder", null}
                    : new String[]{"c2.android.avc.encoder", "OMX.google.h264.encoder", null};
        }
        for (String name : fallbacks) {
            if (name != null && name.isEmpty()) {
                continue;
            }
            try {
                MediaCodec codec;
                if (name == null) {
                    Ln.i("Using video codec mime: " + mimeType);
                    codec = MediaCodec.createEncoderByType(mimeType);
                } else {
                    Ln.i("Trying video encoder: " + name);
                    codec = MediaCodec.createByCodecName(name);
                }
                Ln.i("Video encoder ready: " + codec.getName());
                return codec;
            } catch (Exception e) {
                Ln.e("Encoder candidate failed: " + name, e);
                last = e;
            }
        }
        if (last instanceof IOException) {
            throw (IOException) last;
        }
        throw new IOException("No video encoder available for " + mimeType, last);
    }

    private MediaFormat createFormat(int bitRate, int frameRate, int iFrameInterval) {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, mimeType);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        format.setInteger("max-bframes", 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            format.setInteger(MediaFormat.KEY_PRIORITY, 0);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            format.setInteger(MediaFormat.KEY_LATENCY, 1);
        }
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000);
        return format;
    }

    private static IBinder createDisplay() throws Exception {
        // Since Android 12 (preview), secure displays could not be created with shell permissions anymore.
        // On Android 12 preview, SDK_INT is still R (not S), but CODENAME is "S".
        boolean secure = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S".equals(
                Build.VERSION.CODENAME));
        return SurfaceControl.createDisplay("scrcpy", secure);
    }

    private static void configure(MediaCodec codec, MediaFormat format) {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private static void setSize(MediaFormat format, int width, int height) {
        format.setInteger(MediaFormat.KEY_WIDTH, width);
        format.setInteger(MediaFormat.KEY_HEIGHT, height);
    }

    private static void setDisplaySurface(IBinder display, Surface surface, Rect deviceRect, Rect displayRect) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, 0, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, 0);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    private static void destroyDisplay(IBinder display) {
        SurfaceControl.destroyDisplay(display);
    }

    @Override
    public void onRotationChanged(int rotation) {
        rotationChanged.set(true);
    }

    public boolean consumeRotationChange() {
        return rotationChanged.getAndSet(false);
    }

    public void asyncRequestKeyFrame() {
        requestKeyFrame.set(true);
    }

    /**
     * 开启音频流转发
     *
     * @param outputStream
     */
    private void startAudioCapture(OutputStream outputStream) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioEncoder audioEncoder = new AudioEncoder(128000);
                try {
                    audioEncoder.streamScreen(outputStream);
                } catch (IOException e) {
                    Ln.e("audio capture IOException", e);
                } catch (AudioCaptureException e) {
                    Ln.e("audio capture AudioCaptureException", e);
                } catch (Exception e) {
                    Ln.e("audio capture Exception", e);
                }
            }
        }).start();
    }

    public void streamScreen(Options options, Device device, OutputStream outputStream) throws IOException {
        // Log.d("ScreenCapture", buildDisplayListMessage());
        Size deviceSize = device.getScreenInfo().getDeviceSize();
        Size videoSize = device.getScreenInfo().getVideoSize();
        int[] buf = new int[]{
                deviceSize.getWidth(), deviceSize.getHeight(),
                videoSize.getWidth(), videoSize.getHeight()
        };
        final byte[] array = new byte[buf.length * 4];
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        outputStream.write(array, 0, array.length);
        outputStream.flush();
        Ln.i("Sent device resolution " + buf[0] + "x" + buf[1]
                + " video=" + buf[2] + "x" + buf[3]
                + " mime=" + mimeType + " encoder=" + (encoderName.isEmpty() ? "default" : encoderName));

        if(options.isEnableAudioForward()){
            startAudioCapture(outputStream);  // start audio capture
        }

        MediaFormat format = createFormat(bitRate, frameRate, iFrameInterval);
        device.setRotationListener(this);
        boolean alive;
        int errorCount = 0;
        ScreenCapture capture = new ScreenCapture(device);
        try {
            do {
                MediaCodec codec = createCodec(errorCount);
//                IBinder display = createDisplay();
//                Rect deviceRect = device.getScreenInfo().getDeviceSize().toRect();
                Rect videoRect = device.getScreenInfo().getVideoSize().toRect();
                setSize(format, videoRect.width(), videoRect.height());
                configure(codec, format);
                Surface surface = null;

                // setDisplaySurface(display, surface, deviceRect, videoRect);

                try {
                    surface = codec.createInputSurface();
                    capture.start(surface);
                    codec.start();

                    alive = encode(codec, outputStream);
                    errorCount = 0;
                } catch (IllegalStateException | IllegalArgumentException e) {
                    Ln.e("Encoding error: " + e.getClass().getName(), e);
                    if (errorCount > 3) {
                        throw e;
                    } else {
                        errorCount++;
                    }
                    Ln.i("Retrying...");
                    alive = true;
                } finally {
                    Log.d("ScreenCapture", "帧处理 finally 退出了");
                    try {
                        codec.stop();
                    } catch (Exception ignored) {
                    }
                    try {
                        codec.release();
                    } catch (Exception ignored) {
                    }
                    if (surface != null) {
                        surface.release();
                    }
                }
            } while (alive);
        } finally {
            Log.d("ScreenCapture", "streamScreen 退出了");
            capture.release();
            // device.setRotationListener(null);
        }
    }

    public static String buildDisplayListMessage() {
        StringBuilder builder = new StringBuilder("List of displays:");
        DisplayManager displayManager = ServiceManager.getDisplayManager();
        int[] displayIds = displayManager.getDisplayIds();
        if (displayIds == null || displayIds.length == 0) {
            builder.append("\n    (none)");
        } else {
            for (int id : displayIds) {
                builder.append("\n    --display-id=").append(id).append("    (");
                DisplayInfo displayInfo = displayManager.getDisplayInfo(id);
                if (displayInfo != null) {
                    Size size = displayInfo.getSize();
                    builder.append(size.getWidth()).append("x").append(size.getHeight());
                } else {
                    builder.append("size unknown");
                }
                builder.append(")");
            }
        }
        return builder.toString();
    }

    @SuppressLint("NewApi")
    private boolean encode(MediaCodec codec, OutputStream outputStream) throws IOException {
        @SuppressWarnings("checkstyle:MagicNumber")
//        byte[] buf = new byte[bitRate / 8]; // may contain up to 1 second of video
        boolean eof = false;
        int noFrameCount = 0;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!consumeRotationChange() && !eof) {
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 1_000_000);
            if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                noFrameCount++;
                if (noFrameCount > 8) {
                    throw new IllegalStateException("Encoder produced no frames");
                }
                continue;
            }
            noFrameCount = 0;
            eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            try {
                if (consumeRotationChange()) {
                    // must restart encoding with new size
                    break;
                }
                if (requestKeyFrame.getAndSet(false)) {
                    Bundle b = new Bundle();
                    b.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                    codec.setParameters(b);
                    Log.i("Scrcpy", "request new key frame");
                }
                if (outputBufferId >= 0) {
                    ByteBuffer outputBuffer;

                    outputBuffer = codec.getOutputBuffer(outputBufferId);

                    if (bufferInfo.size > 0 && outputBuffer != null) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] b = new byte[outputBuffer.remaining()];
                        outputBuffer.get(b);

                        MediaPacket.Type type = MediaPacket.Type.VIDEO;
                        VideoPacket.Flag flag = VideoPacket.Flag.CONFIG;

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            flag = VideoPacket.Flag.END;
                        } else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                            flag = VideoPacket.Flag.KEY_FRAME;
                        } else if (bufferInfo.flags == 0) {
                            flag = VideoPacket.Flag.FRAME;
                        }
                        VideoPacket packet = new VideoPacket(type, flag, bufferInfo.presentationTimeUs, b);
                        outputStream.write(packet.toByteArray());
                    }

                }
            } finally {
                if (outputBufferId >= 0) {
                    codec.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }

        return !eof;
    }
}
