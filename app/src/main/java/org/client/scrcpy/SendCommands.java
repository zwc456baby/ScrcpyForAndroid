package org.client.scrcpy;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.client.scrcpy.utils.ThreadUtils;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class SendCommands {

    private Context context;
    private int status;


    public SendCommands() {

    }

    public int SendAdbCommands(Context context, final String ip, int port, int forwardport, String localip, int bitrate, int size) {
        return this.SendAdbCommands(context, null, ip, port, forwardport, localip, bitrate, size);
    }

    public int SendAdbCommands(Context context, final byte[] fileBase64, final String ip, int port, int forwardport, String localip, int bitrate, int size) {
        this.context = context;
        status = 1;
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
                // 新版的复制方式
                newAdbServerStart(context, ip, localip, port, forwardport, commands);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        int count = 0;
        while (status == 1 && count < 50) {
            Log.e("ADB", "Connecting...");
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (count >= 50) {
            status = 2;
            return status;
        }
        if (status == 0) {
            count = 0;
            //  检测程序是否已经启动，如果启动了，该文件会被删除
            while (status == 0 && count < 10) {
                String adbTextCmd = App.adbCmd("-s", ip + ":" + port, "shell", "ls", "-alh", "/data/local/tmp/scrcpy-server.jar");
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
        return status;
    }


    private void newAdbServerStart(Context context, String ip, String localip, int port, int serverport, String[] command) {
        App.adbCmd("connect", ip + ":" + port);

        Log.i("Scrcpy", "adb devices: " + App.adbCmd("devices"));
        // 复制server端到可执行目录
        String pushRet = App.adbCmd("-s", ip + ":" + port, "push", new File(
                context.getExternalFilesDir("scrcpy"), "scrcpy-server.jar"
        ).getAbsolutePath(), "/data/local/tmp/scrcpy-server.jar");

        Log.i("Scrcpy", "pushRet: " + pushRet);

        String adbTextCmd = App.adbCmd("-s", ip + ":" + port, "shell", "ls", "-alh", "/data/local/tmp/scrcpy-server.jar");
        if (TextUtils.isEmpty(adbTextCmd)) {
            status = 2;
            return;
        }
        // 开启本地端口 forward 转发
        Log.i("Scrcpy", "开启本地端口转发");
        App.adbCmd("-s", ip + ":" + port, "forward", "tcp:" + serverport, "tcp:" + 7007);

        status = 0;
        // 执行启动命令
        App.adbCmd(command);
    }

}
