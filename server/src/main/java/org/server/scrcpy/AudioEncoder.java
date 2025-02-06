package org.server.scrcpy;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import org.lsposed.lsparanoid.Obfuscate;
import org.server.scrcpy.audio.AudioCapture;
import org.server.scrcpy.audio.AudioCaptureException;
import org.server.scrcpy.audio.AudioDirectCapture;
import org.server.scrcpy.audio.AudioSource;
import org.server.scrcpy.model.AudioPacket;
import org.server.scrcpy.model.MediaPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@Obfuscate
public class AudioEncoder {
    public static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";

    private int bitRate;

    private HandlerThread mediaCodecThread;

    private final AudioCapture capture = new AudioDirectCapture(AudioSource.OUTPUT);

    private volatile int runStatus = 1;

    private final Object lock = new Object();

    public AudioEncoder(int bitRate) {
        this.bitRate = bitRate;
    }

    private static MediaCodec createCodec() throws IOException {
        return MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
    }

    private static MediaFormat createFormat(int bitRate) throws IOException {

        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, MIMETYPE_AUDIO_AAC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);  // 通道数固定
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);  // 采样率固定

        // display the very first frame, and recover from bad quality when no new frames
//        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, MICROSECONDS_IN_ONE_SECOND * REPEAT_FRAME_DELAY / frameRate); // µs
        return format;
    }

    private static void configure(MediaCodec codec, MediaFormat format) {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void streamScreen(OutputStream outputStream) throws IOException, AudioCaptureException {
        Log.d("ScreenCapture", "audio stream screen");

        MediaFormat format = createFormat(bitRate);

        boolean alive;
        int errorCount = 0;
//        ScreenCapture capture = new ScreenCapture(device);
        try {
            mediaCodecThread = new HandlerThread("media-codec");
            mediaCodecThread.start();

            capture.start();
            do {
                MediaCodec codec = createCodec();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    codec.setCallback(new EncoderCallback(codec, outputStream), new Handler(mediaCodecThread.getLooper()));
                }
                configure(codec, format);

                try {
                    codec.start();
                    alive = waitEnd();
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
                }
            } while (alive);
        } finally {
            Log.d("ScreenCapture", "Audio streamScreen 退出了");
            if (mediaCodecThread != null) {
                Looper looper = mediaCodecThread.getLooper();
                if (looper != null) {
                    looper.quitSafely();
                }
            }

            if (mediaCodecThread != null) {
                try {
                    mediaCodecThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            capture.stop();
        }
    }

    @SuppressLint("NewApi")
    private boolean waitEnd() throws IOException {

        runStatus = 1;

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        switch (runStatus) {
            case 1:
                return true;
            case 2:
                return false;
            case 3:
                throw new IOException("running error");
            case 4:
                throw new IllegalStateException("running error");
            default:
                throw new IOException("running none error");
        }
    }

    private void end() {
        end(2);
    }

    private void end(int status) {
        runStatus = status;
        synchronized (lock) {
            lock.notify();
        }
    }


    private final class EncoderCallback extends MediaCodec.Callback {

        private MediaCodec mediaCodec;

        private OutputStream outputStream;

        private EncoderCallback(MediaCodec mediaCodec, OutputStream outputStream) {
            this.mediaCodec = mediaCodec;
            this.outputStream = outputStream;
        }

        final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();


        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            try {
                ByteBuffer buffer = mediaCodec.getInputBuffer(index);
                int r = capture.read(buffer, bufferInfo);
                if (r <= 0) {
                    Ln.w("Could not read audio: " + r);
                    end();
                    return;
                }
                mediaCodec.queueInputBuffer(index, bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
            } catch (IllegalStateException e) {
                end(4);
            }
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo bufferInfo) {
//            try {
//                outputTasks.put(new OutputTask(index, bufferInfo));
//            } catch (InterruptedException e) {
//                end();
//            }
            try {
                try {
                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                    if (outputBuffer != null) {
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] b = new byte[outputBuffer.remaining()];
                        outputBuffer.get(b);

                        AudioPacket.Flag flag = AudioPacket.Flag.CONFIG;

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            flag = AudioPacket.Flag.END;
                        } else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                            flag = AudioPacket.Flag.KEY_FRAME;
                        } else if (bufferInfo.flags == 0) {
                            flag = AudioPacket.Flag.FRAME;
                        }
                        AudioPacket packet = new AudioPacket(MediaPacket.Type.AUDIO, flag, bufferInfo.presentationTimeUs, b);
                        try {
                            outputStream.write(packet.toByteArray());
                        } catch (IOException e) {
                            Ln.e("output stream write faild");
                            end();
                        }
                    }

                } finally {
                    mediaCodec.releaseOutputBuffer(index, false);
                }
            } catch (IllegalStateException e) {
                end(4);
            }
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Ln.e("MediaCodec error", e);
            end();
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            // ignore
        }
    }
}
