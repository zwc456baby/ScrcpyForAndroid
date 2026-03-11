package org.client.scrcpy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.client.scrcpy.utils.AdbHelper;
import org.client.scrcpy.utils.ExecUtil;
import org.client.scrcpy.utils.PreUtils;
import org.client.scrcpy.utils.ThreadUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public class App extends Application implements Application.ActivityLifecycleCallbacks {

    @SuppressLint("StaticFieldLeak")
    public static Context mContext;

    private final static LinkedList<Activity> activityList = new LinkedList<Activity>();

    @Override
    public void onCreate() {
        super.onCreate();
        init();  // 初始化id 数据
        AdbHelper.startAdbServer();
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
