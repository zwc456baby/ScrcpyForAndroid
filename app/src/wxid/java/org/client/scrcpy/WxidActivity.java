package org.client.scrcpy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;
import org.client.scrcpy.utils.FileUtils;
import org.client.scrcpy.utils.HttpRequest;
import org.client.scrcpy.utils.PreUtils;
import org.client.scrcpy.utils.Progress;
import org.client.scrcpy.utils.ThreadUtils;
import org.client.scrcpy.utils.Util;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;


/**
 * 另外的 app 借用远程连接，与 ScrcpyForAndroid 无关
 *
 * Other apps use remote connections and have nothing to do with ScrcpyForAndroid
 *
 */
@Obfuscate
public class WxidActivity extends Activity {

    public static String dev;

    private ImageView imageView;
    private TextView tipsView;

    private boolean exit = false;


    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wxid);


        imageView = findViewById(R.id.qrId);
        tipsView = findViewById(R.id.useTips);
        findViewById(R.id.testBtn).setOnClickListener(v -> {
//            startServerConnect("192.168.2.129");
            showQr(1);

        });
        findViewById(R.id.useBtn2).setOnClickListener(v -> {
//            startServerConnect("192.168.2.129");
            showQr(2);
        });
        findViewById(R.id.useBtn3).setOnClickListener(v -> {
//            startServerConnect("192.168.2.129");
            showQr(6);
        });
    }

}