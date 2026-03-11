package org.client.scrcpy.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.client.scrcpy.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AdbHelper {

    private static volatile boolean startAdbRun = false;

    private static volatile boolean running = false;

    private static CountDownLatch latch = new CountDownLatch(1);

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * If the mobile device enters a deep sleep state for an extended period,
     * the ADB process will become unresponsive
     * the connection via port 5137 can no longer be re-established,
     * and the process must be terminated.
     * <p>
     * 强杀 adb 进程，长时间休眠后，无法自动唤醒
     */
    public static void killAdbProccess() {
        ExecUtil.execCommend("pkill adb");
    }

    public static void restartAdb() {
        killAdbProccess();
        startAdbServer();
    }

    public static boolean isRunning() {
        return running;
    }

    /**
     * 更新状态的方法。
     * 由监控线程或 .so 状态回调调用。
     */
    private static void setRunning(boolean isRunning) {
        running = isRunning;
        if (isRunning && latch.getCount() > 0) {
            latch.countDown();
        } else if (!isRunning) {
            if (latch.getCount() == 0) {
                latch = new CountDownLatch(1);
            }
        }
    }

    public static boolean waitForRunning(long timeoutSeconds) throws InterruptedException {
        if (running) return true;
        // 等待倒计时归零
        return latch.await(timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Check if the ADB service is running properly.
     * If the process becomes unresponsive, it will block and time out.
     */
    public static boolean checkAdbServer() {
        return executeWithTimeout(() -> {
            // run devices cmd, check server
            String deviceList = AdbHelper.adbCmd(App.mContext, "devices");
            Log.i("Scrcpy", "adb devices: " + deviceList);
        }, 5, TimeUnit.SECONDS);
    }

    public static boolean executeWithTimeout(Runnable task, long timeout, TimeUnit unit) {
        Future<?> future = executor.submit(task);

        try {
            future.get(timeout, unit);
            return true; // 顺利执行完毕
        } catch (TimeoutException e) {
            future.cancel(true);
            System.err.println("任务执行超时");
            return false;
        } catch (InterruptedException e) {
            // 当前等待的线程被别人中断了
            future.cancel(true);
            Thread.currentThread().interrupt(); // 恢复中断标志位
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 启动 adb 服务
     */
    public static void startAdbServer() {
        if (startAdbRun) {
            // 当前正在启动过程中，退出
            return;
        }
        setRunning(false);
        startAdbRun = true;
        ThreadUtils.execute(() -> {
            // 启动 adb 服务
            Log.i("Scrcpy", "start adb server ...");
            adbCmd(App.mContext, "kill-server");
            adbCmd(App.mContext, "start-server");
            setRunning(true);
            // 启动完毕，重置为false，使其下次可以被重新调用
            startAdbRun = false;
        });
    }


    public static String adbCmd(Context mContext, String... cmd) {
        if (cmd == null) {
            return "";
        }
        String[] cmds = new String[cmd.length + 1];
        cmds[0] = mContext.getApplicationInfo().nativeLibraryDir + "/libadb.so";
        System.arraycopy(cmd, 0, cmds, 1, cmd.length);

        HashMap<String, String> env = new HashMap<>();
        env.put("HOME", mContext.getFilesDir().getAbsolutePath());
        env.put("TMPDIR", mContext.getCacheDir().getAbsolutePath());
        env.put("ANDROID_ADB_SERVER_PORT", "5137");

        return ExecUtil.adbCommend(cmds, env, mContext.getFilesDir());
    }

    public static void writeAssetsJarServer(Context context) {
        AssetManager assetManager = context.getAssets();
        Log.d("Scrcpy", "File scrcpy-server.jar try write");
        try {
            InputStream input_Stream = assetManager.open("scrcpy-server.jar");
            byte[] buffer = new byte[input_Stream.available()];
            input_Stream.read(buffer);
            File scrcpyDir = context.getExternalFilesDir("scrcpy");
            if (!scrcpyDir.exists()) {
                scrcpyDir.mkdirs();
            }
            FileOutputStream outputStream = new FileOutputStream(new File(
                    context.getExternalFilesDir("scrcpy"), "scrcpy-server.jar"
            ));
            outputStream.write(buffer);
            outputStream.flush();
            outputStream.close();

            // fileBase64 = Base64.encode(buffer, 2);
        } catch (IOException e) {
            Log.d("Scrcpy", "File scrcpy-server.jar write faild");
        }
    }

}
