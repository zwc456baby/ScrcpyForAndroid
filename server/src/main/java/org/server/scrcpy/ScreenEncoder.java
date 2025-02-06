package org.server.scrcpy;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import org.server.scrcpy.audio.AudioCaptureException;
import org.server.scrcpy.model.MediaPacket;
import org.server.scrcpy.model.VideoPacket;
import org.server.scrcpy.wrappers.DisplayManager;
import org.server.scrcpy.wrappers.ServiceManager;
import org.server.scrcpy.wrappers.SurfaceControl;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

@Obfuscate
public class ScreenEncoder implements Device.RotationListener {

    private static final int DEFAULT_FRAME_RATE = 60; // fps
    private static final int DEFAULT_I_FRAME_INTERVAL = 10; // seconds

    private static final int REPEAT_FRAME_DELAY = 6; // repeat after 6 frames

    private static final int MICROSECONDS_IN_ONE_SECOND = 1_000_000;

    private final AtomicBoolean rotationChanged = new AtomicBoolean();

    private int bitRate;
    private int frameRate;
    private int iFrameInterval;

    public ScreenEncoder(int bitRate, int frameRate, int iFrameInterval) {
        this.bitRate = bitRate;
        this.frameRate = frameRate;
        this.iFrameInterval = iFrameInterval;
    }

    public ScreenEncoder(int bitRate) {
        this(bitRate, DEFAULT_FRAME_RATE, DEFAULT_I_FRAME_INTERVAL);
    }

    private static MediaCodec createCodec() throws IOException {
        return MediaCodec.createEncoderByType("video/avc");
    }

    private static MediaFormat createFormat(int bitRate, int frameRate, int iFrameInterval) throws IOException {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "video/avc");
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);

        // display the very first frame, and recover from bad quality when no new frames
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, MICROSECONDS_IN_ONE_SECOND * REPEAT_FRAME_DELAY / frameRate); // µs
        return format;
    }

    private static IBinder createDisplay() {
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

    public void streamScreen(Device device, OutputStream outputStream) throws IOException {
        // Log.d("ScreenCapture", buildDisplayListMessage());
        int[] buf = new int[]{device.getScreenInfo().getDeviceSize().getWidth(), device.getScreenInfo().getDeviceSize().getHeight()};
        final byte[] array = new byte[buf.length * 4];   // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        outputStream.write(array, 0, array.length);   // Sending device resolution

        startAudioCapture(outputStream);  // start audio capture

        MediaFormat format = createFormat(bitRate, frameRate, iFrameInterval);
        device.setRotationListener(this);
        boolean alive;
        int errorCount = 0;
        ScreenCapture capture = new ScreenCapture(device);
        try {
            do {
                MediaCodec codec = createCodec();
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
                    codec.stop();
                    // destroyDisplay(display);
                    codec.release();
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
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!consumeRotationChange() && !eof) {
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, -1);
            eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            try {
                if (consumeRotationChange()) {
                    // must restart encoding with new size
                    break;
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
