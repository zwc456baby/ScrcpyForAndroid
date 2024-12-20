package org.client.scrcpy;


import static android.org.apache.commons.codec.binary.Base64.encodeBase64String;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import org.client.scrcpy.utils.ThreadUtils;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@Obfuscate
public class SendCommands {

    private Context context;
    private int status;


    public SendCommands() {

    }

    public static AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return encodeBase64String(arg0);
            }
        };
    }

    private AdbCrypto setupCrypto()
            throws NoSuchAlgorithmException, IOException {

        AdbCrypto c = null;
        try {
            c = AdbCrypto.loadAdbKeyPair(getBase64Impl(), context.getFileStreamPath("priv.key"), context.getFileStreamPath("pub.key"));
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException |
                 NullPointerException e) {
            // Failed to read from file
            c = null;
        }


        if (c == null) {
            // We couldn't load a key, so let's generate a new one
            c = AdbCrypto.generateAdbKeyPair(getBase64Impl());
            // Save it
            c.saveAdbKeyPair(context.getFileStreamPath("priv.key"), context.getFileStreamPath("pub.key"));
            //Generated new keypair
        } else {
            //Loaded existing keypair
        }

        return c;
    }

    public int SendAdbCommands(Context context, final String ip, int port, int forwardport, String localip, int bitrate, int size) {
        return this.SendAdbCommands(context, null, ip, port, forwardport, localip, bitrate, size);
    }

    public int SendAdbCommands(Context context, final byte[] fileBase64, final String ip, int port, int forwardport, String localip, int bitrate, int size) {
        this.context = context;
        status = 1;
//        final StringBuilder command = new StringBuilder();
//        command.append(" CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / org.server.scrcpy.Server ");
//        // command.append(" CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / org.server.scrcpy.Server ");
//        command.append(" /" + localip + " " + Long.toString(size) + " " + Long.toString(bitrate) + ";");

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
                // 旧版的 adb 复制方式
                // adbWrite(ip, 5555, fileBase64, command.toString());
                // 写入 转发
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

    private void adbWrite(String ip, int port, byte[] fileBase64, String command) throws IOException {

        AdbConnection adb = null;
        Socket sock = null;
        AdbCrypto crypto;
        AdbStream stream = null;

        try {
            crypto = setupCrypto();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Couldn't read/write keys");
        }

        try {
            Log.i("scrcpy", "write cmd to : " + ip);
            sock = new Socket(ip, port);
            Log.e("scrcpy", " ADB socket connection successful");
        } catch (UnknownHostException e) {
            status = 2;
            throw new UnknownHostException(ip + " is no valid ip address");
        } catch (ConnectException e) {
            status = 2;
            throw new ConnectException("Device at " + ip + ":" + port + " has no adb enabled or connection is refused");
        } catch (NoRouteToHostException e) {
            status = 2;
            throw new NoRouteToHostException("Couldn't find adb device at " + ip + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
            status = 2;
        }

        if (sock != null && status == 1) {
            try {
                adb = AdbConnection.create(sock, crypto);
                adb.connect();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        if (adb != null && status == 1) {

            try {
                stream = adb.open("shell:");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                status = 2;
                return;
            }
        }

        if (stream != null && status == 1) {
            try {
                stream.write(" " + '\n');
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }


        String responses = "";
        boolean done = false;
        while (!done && stream != null && status == 1) {
            try {
                byte[] responseBytes = stream.read();
                String response = new String(responseBytes, StandardCharsets.US_ASCII);
                if (response.substring(response.length() - 2).equals("$ ") ||
                        response.substring(response.length() - 2).equals("# ")) {
                    done = true;
//                    Log.e("ADB_Shell","Prompt ready");
                    responses += response;
                    break;
                } else {
                    responses += response;
                }
            } catch (InterruptedException | IOException e) {
                status = 2;
                e.printStackTrace();
            }
        }

        if (stream != null && status == 1) {
            int len = fileBase64.length;
            byte[] filePart = new byte[4056];
            int sourceOffset = 0;
            try {
                stream.write(" cd /data/local/tmp " + '\n');
                while (sourceOffset < len) {
                    if (len - sourceOffset >= 4056) {
                        System.arraycopy(fileBase64, sourceOffset, filePart, 0, 4056);  //Writing in 4KB pieces. 4096-40  ---> 40 Bytes for actual command text.
                        sourceOffset = sourceOffset + 4056;
                        String ServerBase64part = new String(filePart, StandardCharsets.US_ASCII);
                        stream.write(" echo " + ServerBase64part + " >> serverBase64" + '\n');
                        done = false;
                        while (!done) {
                            byte[] responseBytes = stream.read();
                            String response = new String(responseBytes, StandardCharsets.US_ASCII);
                            if (response.endsWith("$ ") || response.endsWith("# ")) {
                                done = true;
                            }
                        }
                    } else {
                        int rem = len - sourceOffset;
                        byte[] remPart = new byte[rem];
                        System.arraycopy(fileBase64, sourceOffset, remPart, 0, rem);
                        sourceOffset = sourceOffset + rem;
                        String ServerBase64part = new String(remPart, StandardCharsets.US_ASCII);
                        stream.write(" echo " + ServerBase64part + " >> serverBase64" + '\n');
                        done = false;
                        while (!done) {
                            byte[] responseBytes = stream.read();
                            String response = new String(responseBytes, StandardCharsets.US_ASCII);
                            if (response.endsWith("$ ") || response.endsWith("# ")) {
                                done = true;
                            }
                        }
                    }
                }
                stream.write(" base64 -d < serverBase64 > scrcpy-server.jar && rm serverBase64" + '\n');
                Thread.sleep(100);
                stream.write(command + '\n');
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                status = 2;
                return;
            }
        }
        if (status == 1) ;
        status = 0;

    }

}
