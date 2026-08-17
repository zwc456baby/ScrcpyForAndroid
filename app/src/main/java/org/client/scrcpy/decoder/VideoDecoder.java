package org.client.scrcpy.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoDecoder {
    private static final String TAG = "Scrcpy";
    private MediaCodec mCodec;
    private Worker mWorker;
    private AtomicBoolean mIsConfigured = new AtomicBoolean(false);

    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        if (mWorker != null) {
            mWorker.decodeSample(data, offset, size, presentationTimeUs, flags);
        }
    }

    public void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1) {
        configure(surface, width, height, csd0, csd1, "video/avc");
    }

    public void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1, String mimeType) {
        if (mWorker != null) {
            mWorker.configure(surface, width, height, csd0, csd1, mimeType);
        }
    }


    public void start() {
        if (mWorker == null) {
            mWorker = new Worker();
            mWorker.setRunning(true);
            mWorker.start();
        }
    }

    public void stop() {
        if (mWorker != null) {
            mWorker.setRunning(false);
            mWorker = null;
            mIsConfigured.set(false);
            if (mCodec != null) {
                mCodec.stop();
            }
        }
    }

    private class Worker extends Thread {

        private AtomicBoolean mIsRunning = new AtomicBoolean(false);

        Worker() {
        }

        private void setRunning(boolean isRunning) {
            mIsRunning.set(isRunning);
        }

        private void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1, String mimeType) {
            if (surface == null) {
                Log.e(TAG, "VideoDecoder configure skipped: surface is null");
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !surface.isValid()) {
                Log.e(TAG, "VideoDecoder configure skipped: surface is invalid");
                return;
            }
            if (mIsConfigured.get()) {
                mIsConfigured.set(false);
                if (mCodec != null) {
                    try {
                        mCodec.stop();
                    } catch (IllegalStateException ignore) {
                    }
                    try {
                        mCodec.release();
                    } catch (IllegalStateException ignore) {
                    }
                    mCodec = null;
                }

            }
            String mime = TextUtils.isEmpty(mimeType) ? "video/avc" : mimeType;
            int safeW = Math.max(width, 16);
            int safeH = Math.max(height, 16);
            MediaFormat format = MediaFormat.createVideoFormat(mime, safeW, safeH);
            if (csd0 != null) {
                format.setByteBuffer("csd-0", csd0);
            }
            // H.264 uses SPS/PPS; HEVC prefers a single csd-0 with VPS/SPS/PPS
            if (csd1 != null && "video/avc".equals(mime)) {
                format.setByteBuffer("csd-1", csd1);
            }
            try {
                mCodec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create codec for " + mime, e);
            }
            mCodec.configure(format, surface, null, 0);
            mCodec.start();
            mIsConfigured.set(true);
        }


        @SuppressWarnings("deprecation")
        public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
            if (mIsConfigured.get() && mIsRunning.get()) {
                int index = mCodec.dequeueInputBuffer(-1);
                if (index >= 0) {
                    ByteBuffer buffer;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        buffer = mCodec.getInputBuffers()[index];
                        buffer.clear();
                    } else {
                        buffer = mCodec.getInputBuffer(index);
                    }
                    if (buffer != null) {
                        buffer.put(data, offset, size);
                        mCodec.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (mIsRunning.get()) {
                    if (mIsConfigured.get()) {
                        int index = mCodec.dequeueOutputBuffer(info, 0);
                        if (index >= 0) {
                            // setting true is telling system to render frame onto Surface
                            mCodec.releaseOutputBuffer(index, true);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    } else {
                        // just waiting to be configured, then decode and render
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "VideoDecoder output loop failed: " + e.getMessage());
            }

        }
    }
}
