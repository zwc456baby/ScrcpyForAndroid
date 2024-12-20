package org.client.scrcpy;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.client.scrcpy.utils.ExecUtil;
import org.client.scrcpy.utils.ThreadUtils;
import org.lsposed.lsparanoid.Obfuscate;

import java.util.HashMap;

@Obfuscate
public class Test extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        startAdbServer();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mContext = base;
    }

    /**
     * 启动 adb 服务
     */
    public static void startAdbServer() {
        ThreadUtils.execute(() -> {
            // 启动 adb 服务
            Log.i("Scrcpy", "start adb server ...");
            adbCmd("kill-server");
            adbCmd("start-server");
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

        return ExecUtil.adbCommend(cmds, env, mContext.getFilesDir());
    }
}
