package org.client.scrcpy;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.client.scrcpy.utils.AdbHelper;
import org.client.scrcpy.utils.ThreadUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class SendCommands {

    private final static int WAIT_TIME = 20000;

    public enum CmdStatus {
        SUCCESS,
        RUNNING,
        ERROR
    }

    public SendCommands() {

    }

    public CmdStatus SendAdbCommands(Context context, final String ip, int port, int forwardport, String localip, int bitrate, int size) {
        return this.SendAdbCommands(context, null, ip, port, forwardport, localip, bitrate, size);
    }

    public CmdStatus SendAdbCommands(Context context, final byte[] fileBase64, final String ip, int port, int forwardport, String localip, int bitrate, int size) {
        AtomicReference<CmdStatus> status = new AtomicReference<>(CmdStatus.RUNNING);
        String[] commands = new String[]{
                "-s", ip + ":" + port,
                "shell",
                " CLASSPATH=/data/local/tmp/scrcpy-server.jar",
                "app_process",
                "/",
                "org.server.scrcpy.Server",
                "/" + localip,
                Long.toString(size),
                Long.toString(bitrate) + ";"
        };
        ThreadUtils.execute(() -> {
            try {
                if (AdbHelper.isRunning()) {
                    boolean serverIsRunning = AdbHelper.checkAdbServer();
                    Log.i("Scrcpy", "serverIsRunning: " + serverIsRunning);
                    if (!serverIsRunning) {
                        AdbHelper.restartAdb();
                        AdbHelper.waitForRunning(5);
                    }
                    CmdStatus curStatus = startPortForward(context, ip, port, forwardport);
                    status.set(curStatus);
                    if(curStatus == CmdStatus.SUCCESS){
                        newAdbServerStart(commands);
                    }
                } else {
                    status.set(CmdStatus.ERROR);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        int count = 0;
        while (status.get() == CmdStatus.RUNNING && count < (WAIT_TIME / 100)) {
            Log.e("ADB", "Connecting...");
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (count >= 50) {
            status.set(CmdStatus.ERROR);
            return status.get();
        }
        if (status.get() == CmdStatus.SUCCESS) {
            count = 0;
            //  检测程序是否已经启动，如果启动了，该文件会被删除
            while (status.get() == CmdStatus.SUCCESS && count < 10) {
                String adbTextCmd = AdbHelper.adbCmd(App.mContext,
                        "-s", ip + ":" + port, "shell", "ls", "-alh", "/data/local/tmp/scrcpy-server.jar");
                if (TextUtils.isEmpty(adbTextCmd)) {
                    break;
                } else {
                    try {
                        Thread.sleep(100);
                        count++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return status.get();
    }

    private CmdStatus startPortForward(Context context, String ip, int port, int serverport) {
        Log.i("Scrcpy", "try connect to ip: " + ip);
        AdbHelper.adbCmd(App.mContext, "connect", ip + ":" + port);
        // 复制server端到可执行目录
        String pushRet = AdbHelper.adbCmd(App.mContext, "-s", ip + ":" + port, "push", new File(
                context.getExternalFilesDir("scrcpy"), "scrcpy-server.jar"
        ).getAbsolutePath(), "/data/local/tmp/scrcpy-server.jar");

        Log.i("Scrcpy", "pushRet: " + pushRet);

        String adbTextCmd = AdbHelper.adbCmd(App.mContext, "-s", ip + ":" + port, "shell", "ls", "-alh", "/data/local/tmp/scrcpy-server.jar");
        if (TextUtils.isEmpty(adbTextCmd)) {
            return CmdStatus.ERROR;
        }
        // 开启本地端口 forward 转发
        Log.i("Scrcpy", "开启本地端口转发");
        AdbHelper.adbCmd(App.mContext, "-s", ip + ":" + port, "forward", "tcp:" + serverport, "tcp:" + 7007);
        return CmdStatus.SUCCESS;
    }

    private void newAdbServerStart(String[] command) {
        // 执行启动命令
        AdbHelper.adbCmd(App.mContext, command);
    }

}
