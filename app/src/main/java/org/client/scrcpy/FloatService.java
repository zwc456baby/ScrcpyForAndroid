package org.client.scrcpy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatService extends Service {
    private static final String TAG = "FloatService";

    DisplayWindow displayWindow;
    WindowManager windowManager;
    WindowManager.LayoutParams lp;

    ScrcpyHost scrcpyHost;

    int w;
    int h;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        String ip = intent.getStringExtra("ip");
        w = intent.getIntExtra("w", 1080);
        h = intent.getIntExtra("h", 1920);
        int b = intent.getIntExtra("b", 1024000);

        Log.d(TAG, "onStartCommand: " + w + "," + h + "|" + b + " ->" + ip);
        displayWindow.setRemote(w, h);

        new Thread(new Runnable() {
            @Override
            public void run() {
                startCopy(ip, w, h, b);
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupDisplay();
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.verticalMargin = 0;
        lp.horizontalMargin = 0;
        windowManager.addView(displayWindow, lp);

//        startCopy();

    }

    private void startCopy(String ip, int width, int height, int bitrate) {
        scrcpyHost = new ScrcpyHost();
        scrcpyHost.setConnectCallBack(new ScrcpyHost.ConnectCallBack() {
            @Override
            public void onConnect(float w, float h) {
                displayWindow.setRemote((int) w, (int) h);
                displayWindow.hideHintTip();
            }
        });
        scrcpyHost.connect(getApplicationContext(), ip, width, height, bitrate, displayWindow.getDisplaySurface());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: ");
        displayWindow.setRemote(w, h);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scrcpyHost.destroy();
        System.exit(0);
    }

    private void setupDisplay() {
        displayWindow = new DisplayWindow(getApplicationContext());
        displayWindow.setCloseListener(v -> {
            Log.d(TAG, "close");
            windowManager.removeView(displayWindow);
            stopSelf();
        });
        displayWindow.setMoveCallback((x, y) -> {
            lp.x += x;
            lp.y += y;
            windowManager.updateViewLayout(displayWindow, lp);
        });
        displayWindow.setActionCallback(new DisplayWindow.OnActionCallback() {
            @Override
            public void onAction(int actionType) {
                switch (actionType) {
                    case 0:
                        scrcpyHost.keyEvent(4);
                        break;
                    case 1:
                        scrcpyHost.keyEvent(3);
                        break;
                    case 2:
                        scrcpyHost.keyEvent(187);
                        break;
                }
            }
        });
        displayWindow.setOnDisplayTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return scrcpyHost.touch(motionEvent, displayWindow.getSurfaceWidth(), displayWindow.getSurfaceHeight());
            }
        });
    }
}
