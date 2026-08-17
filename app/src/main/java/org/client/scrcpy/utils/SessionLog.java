package org.client.scrcpy.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.client.scrcpy.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Session log: reset at every app start, then capture our process logcat.
 */
public final class SessionLog {

    private static final String TAG = "Scrcpy";
    private static final Object LOCK = new Object();
    private static File logFile;
    private static final AtomicBoolean started = new AtomicBoolean(false);

    private static final int MAX_LOG_BYTES = 256 * 1024;

    private SessionLog() {
    }

    public static void resetAndStart(Context context) {
        synchronized (LOCK) {
            File dir = new File(context.getExternalFilesDir(null), "logs");
            if (!dir.exists() && !dir.mkdirs()) {
                dir = context.getFilesDir();
            }
            logFile = new File(dir, "session.log");
            try (FileOutputStream out = new FileOutputStream(logFile, false)) {
                String header = "=== Scrcpy session "
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                        + " ===\n"
                        + "app=" + BuildConfig.VERSION_NAME + " r" + BuildConfig.VERSION_CODE + "\n"
                        + "clientSdk=" + Build.VERSION.SDK_INT
                        + " device=" + Build.MANUFACTURER + " " + Build.MODEL + "\n\n";
                out.write(header.getBytes("UTF-8"));
            } catch (Exception e) {
                Log.e(TAG, "Could not reset session log", e);
            }
        }
        if (started.compareAndSet(false, true)) {
            startLogcatCollector();
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                e("Uncaught on " + t.getName(), e);
            });
        }
        i("Session log reset");
    }

    public static File getFile() {
        synchronized (LOCK) {
            return logFile;
        }
    }

    public static String readAll() {
        File file = getFile();
        if (file == null || !file.exists()) {
            return "";
        }
        try {
            byte[] data = new byte[(int) Math.min(file.length(), 512 * 1024)];
            java.io.FileInputStream in = new java.io.FileInputStream(file);
            int n = in.read(data);
            in.close();
            return n <= 0 ? "" : new String(data, 0, n, "UTF-8");
        } catch (Exception e) {
            return "Could not read log: " + e.getMessage();
        }
    }

    /** Riassunto corto per WhatsApp (~400 caratteri). Il log intero va solo in allegato. */
    public static String readPreview() {
        return buildWhatsAppSummary(readAll());
    }

    static String buildWhatsAppSummary(String all) {
        if (all == null || all.isEmpty()) {
            return "Log vuoto. Allegato scrcpy-session.log";
        }
        String app = findAfter(all, "app=", "\n");
        String device = findAfter(all, "clientSdk=", "\n");
        String start = findLineContaining(all, "Start clicked");
        if (start != null) {
            int idx = start.indexOf("Start clicked");
            if (idx >= 0) {
                start = start.substring(idx);
            }
        }
        String errors = collectErrorHints(all);
        StringBuilder sb = new StringBuilder();
        if (app != null && !app.isEmpty()) {
            sb.append(app.trim());
        }
        if (device != null && !device.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(device.trim());
        }
        if (start != null) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(start.trim());
        }
        if (!errors.isEmpty()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(errors);
        }
        if (sb.length() == 0) {
            sb.append("Sessione senza errori evidenti.");
        }
        sb.append("\nLog completo: allegato scrcpy-session.log");
        String text = sb.toString();
        if (text.length() <= 400) {
            return text;
        }
        return text.substring(0, 397) + "...";
    }

    private static String collectErrorHints(String all) {
        String[][] patterns = {
                {"can't read socket Resolution", "no resolution"},
                {"createDisplay", "createDisplay missing"},
                {"CodecException", "MediaCodec fail"},
                {"Pending dequeue output buffer", "encoder dequeue cancelled"},
                {"Could not create display", "no virtual display"},
                {"Display: using DisplayManager", "DisplayManager OK"},
                {"Display: using SurfaceControl", "SurfaceControl OK"},
                {"Display: using AUTO_MIRROR", "AUTO_MIRROR OK"},
                {"Sent device resolution", "resolution sent"},
                {"Connection Timed out", "timeout"},
                {"Network OR ADB connection failed", "ADB fail"},
                {"Uncaught", "crash"},
        };
        StringBuilder hints = new StringBuilder();
        for (String[] p : patterns) {
            if (all.contains(p[0])) {
                if (hints.length() > 0) {
                    hints.append("; ");
                }
                hints.append(p[1]);
            }
        }
        return hints.length() == 0 ? "" : "Esito: " + hints;
    }

    private static String findAfter(String all, String prefix, String end) {
        int i = all.indexOf(prefix);
        if (i < 0) {
            return null;
        }
        int from = i + prefix.length();
        int to = all.indexOf(end, from);
        if (to < 0) {
            to = Math.min(all.length(), from + 80);
        }
        return prefix + all.substring(from, to);
    }

    private static String findLineContaining(String all, String token) {
        int i = all.indexOf(token);
        if (i < 0) {
            return null;
        }
        int start = all.lastIndexOf('\n', i);
        int end = all.indexOf('\n', i);
        if (start < 0) {
            start = 0;
        } else {
            start++;
        }
        if (end < 0) {
            end = all.length();
        }
        return all.substring(start, end);
    }

    public static void i(String message) {
        Log.i(TAG, message);
        append("I", message, null);
    }

    public static void e(String message) {
        Log.e(TAG, message);
        append("E", message, null);
    }

    public static void e(String message, Throwable t) {
        Log.e(TAG, message, t);
        append("E", message, t);
    }

    private static void append(String level, String message, Throwable t) {
        synchronized (LOCK) {
            if (logFile == null) {
                return;
            }
            try (FileOutputStream out = new FileOutputStream(logFile, true)) {
                String time = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
                StringBuilder sb = new StringBuilder();
                sb.append(time).append(' ').append(level).append("/Scrcpy: ").append(message).append('\n');
                if (t != null) {
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    sb.append(sw);
                }
                out.write(sb.toString().getBytes("UTF-8"));
            } catch (Exception ignored) {
            }
        }
    }

    private static void startLogcatCollector() {
        Thread thread = new Thread(() -> {
            java.lang.Process logcat = null;
            try {
                logcat = new ProcessBuilder("logcat", "-v", "time", "--pid=" + android.os.Process.myPid())
                        .redirectErrorStream(true)
                        .start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(logcat.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!isUseful(line)) {
                        continue;
                    }
                    synchronized (LOCK) {
                        if (logFile == null) {
                            continue;
                        }
                        if (logFile.length() > MAX_LOG_BYTES) {
                            continue;
                        }
                        try (FileOutputStream out = new FileOutputStream(logFile, true)) {
                            out.write((line + "\n").getBytes("UTF-8"));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                append("W", "logcat collector failed: " + e.getMessage(), null);
            } finally {
                if (logcat != null) {
                    logcat.destroy();
                }
            }
        }, "session-logcat");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean isUseful(String line) {
        if (line == null || line.length() > 2000) {
            return false;
        }
        String upper = line.toUpperCase(Locale.US);
        return line.contains("Scrcpy")
                || line.contains("scrcpy")
                || line.contains("/ADB")
                || line.contains("MediaCodec")
                || line.contains("AndroidRuntime")
                || line.contains("ScreenCapture")
                || line.contains("CCodec")
                || line.contains("VirtualDisplay")
                || line.contains("DisplayManager")
                || line.contains("SurfaceControl")
                || line.contains("Encoding")
                || line.contains("FATAL")
                || upper.contains(" EXCEPTION")
                || line.contains("Start clicked")
                || line.contains("org.server.scrcpy")
                || line.contains("ERROR:")
                || line.contains("INFO:")
                || line.contains("WARN:")
                || line.contains("Sent device resolution")
                || line.contains("Could not create");
    }
}
