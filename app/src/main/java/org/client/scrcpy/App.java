package org.client.scrcpy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.client.scrcpy.utils.ExecUtil;
import org.client.scrcpy.utils.PreUtils;
import org.client.scrcpy.utils.ThreadUtils;
import org.lsposed.lsparanoid.Obfuscate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

@Obfuscate
public class App extends Application implements Application.ActivityLifecycleCallbacks {

    @SuppressLint("StaticFieldLeak")
    public static Context mContext;

    private final static LinkedList<Activity> activityList = new LinkedList<Activity>();

    private static boolean startAdbRun = false;

    @Override
    public void onCreate() {
        super.onCreate();
        init();  // 初始化id 数据
        startAdbServer();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mContext = base;
        registerActivityLifecycleCallbacks(this);
    }

    private void init() {
        String userId = PreUtils.get(this, Constant.USER_ID, "");
        if (TextUtils.isEmpty(userId)) {
            PreUtils.put(this, Constant.USER_ID, UUID.randomUUID().toString());
        }
    }

    public static Activity getCurActivity() {
        // 获取最新的一个 activity
        try {
            return activityList.getFirst();
        } catch (Exception ignore) {
            return null;
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
        startAdbRun = true;
        ThreadUtils.execute(() -> {
            // 启动 adb 服务
            Log.i("Scrcpy", "start adb server ...");
            adbCmd("kill-server");
            adbCmd("start-server");
            // 启动完毕，重置为false，使其下次可以被重新调用
            startAdbRun = false;
        });
    }


    public static String adbCmd(String... cmd) {
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

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        activityList.addFirst(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        activityList.remove(activity);
    }
}
