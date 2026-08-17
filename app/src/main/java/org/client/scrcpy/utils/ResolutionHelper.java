package org.client.scrcpy.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.client.scrcpy.App;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResolutionHelper {

    private static final String TAG = "Scrcpy";
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)");

    public enum LimitSource {
        LOCAL,
        REMOTE
    }

    public static final class AutoResolution {
        public final int maxSize;
        public final int displayWidth;
        public final int displayHeight;
        public final int localMax;
        public final int remoteWidth;
        public final int remoteHeight;
        public final LimitSource limitSource;

        AutoResolution(int maxSize, int displayWidth, int displayHeight,
                       int localMax, int remoteWidth, int remoteHeight, LimitSource limitSource) {
            this.maxSize = maxSize;
            this.displayWidth = displayWidth;
            this.displayHeight = displayHeight;
            this.localMax = localMax;
            this.remoteWidth = remoteWidth;
            this.remoteHeight = remoteHeight;
            this.limitSource = limitSource;
        }
    }

    public static int getLocalMaxDimension(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return 0;
        }
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return alignTo8(Math.max(metrics.widthPixels, metrics.heightPixels));
    }

    public static int[] getLocalDisplaySize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return new int[]{0, 0};
        }
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return new int[]{metrics.widthPixels, metrics.heightPixels};
    }

    public static int[] queryRemoteDisplaySize(Context context, String serverAdr) {
        String[] serverInfo = Util.getServerHostAndPort(serverAdr);
        String device = serverInfo[0] + ":" + serverInfo[1];
        AdbHelper.adbCmd(App.mContext, "connect", device);
        String wmSize = AdbHelper.adbCmd(App.mContext, "-s", device, "shell", "wm", "size");
        Log.i(TAG, "Remote wm size: " + wmSize);
        return parseWmSizeOutput(wmSize);
    }

    public static AutoResolution computeAuto(Context context, String serverAdr) {
        int localMax = getLocalMaxDimension(context);
        int[] remoteSize = queryRemoteDisplaySize(context, serverAdr);
        int remoteWidth = alignTo8(remoteSize[0]);
        int remoteHeight = alignTo8(remoteSize[1]);

        if (remoteWidth <= 0 || remoteHeight <= 0) {
            Log.w(TAG, "Remote resolution unavailable, using local display only");
            int[] localSize = getLocalDisplaySize(context);
            int width = alignTo8(localSize[0]);
            int height = alignTo8(localSize[1]);
            int maxSize = alignTo8(Math.max(width, height));
            return new AutoResolution(maxSize, width, height, localMax, width, height, LimitSource.LOCAL);
        }

        int remoteMax = Math.max(remoteWidth, remoteHeight);
        LimitSource limitSource = localMax <= remoteMax ? LimitSource.LOCAL : LimitSource.REMOTE;
        int maxSize = alignTo8(Math.min(localMax, remoteMax));
        int[] scaled = scaleToMaxSize(remoteWidth, remoteHeight, maxSize);
        return new AutoResolution(maxSize, scaled[0], scaled[1], localMax, remoteWidth, remoteHeight, limitSource);
    }

    /**
     * Same scaling rules as the scrcpy server ({@code Device.computeScreenInfo}).
     */
    public static int[] scaleToMaxSize(int width, int height, int maxSize) {
        int w = alignTo8(width);
        int h = alignTo8(height);
        if (maxSize <= 0) {
            return new int[]{w, h};
        }
        boolean portrait = h > w;
        int major = portrait ? h : w;
        int minor = portrait ? w : h;
        if (major > maxSize) {
            int minorExact = minor * maxSize / major;
            minor = alignTo8(minorExact + 4);
            major = maxSize;
        }
        w = portrait ? minor : major;
        h = portrait ? major : minor;
        return new int[]{w, h};
    }

    private static int[] parseWmSizeOutput(String output) {
        if (output == null) {
            return new int[]{0, 0};
        }
        int[] overrideSize = null;
        int[] physicalSize = null;
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Override size:")) {
                overrideSize = parseSizeLine(trimmed);
            } else if (trimmed.startsWith("Physical size:")) {
                physicalSize = parseSizeLine(trimmed);
            } else if (trimmed.contains("x") && physicalSize == null) {
                int[] parsed = parseSizeLine(trimmed);
                if (parsed[0] > 0 && parsed[1] > 0) {
                    physicalSize = parsed;
                }
            }
        }
        if (overrideSize != null && overrideSize[0] > 0 && overrideSize[1] > 0) {
            return overrideSize;
        }
        if (physicalSize != null && physicalSize[0] > 0 && physicalSize[1] > 0) {
            return physicalSize;
        }
        return new int[]{0, 0};
    }

    private static int[] parseSizeLine(String line) {
        Matcher matcher = SIZE_PATTERN.matcher(line);
        if (matcher.find()) {
            return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
        }
        return new int[]{0, 0};
    }

    private static int alignTo8(int value) {
        return value & ~7;
    }
}
