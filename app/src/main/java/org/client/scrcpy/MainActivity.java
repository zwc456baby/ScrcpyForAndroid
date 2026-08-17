package org.client.scrcpy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.client.scrcpy.utils.AdbHelper;
import org.client.scrcpy.utils.PreUtils;
import org.client.scrcpy.utils.Progress;
import org.client.scrcpy.utils.ResolutionHelper;
import org.client.scrcpy.utils.ThreadUtils;
import org.client.scrcpy.utils.Util;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity implements Scrcpy.ServiceCallbacks, SensorEventListener {

    // 是否直接连接远程
    public final static String START_REMOTE = "start_remote_headless";

    private boolean headlessMode = false;  // 是否为无头模式，不显示操作选项等
    private int screenWidth;
    private int screenHeight;
    private boolean landscape = false;
    private boolean first_time = true;
    private boolean result_of_Rotation = false;
    private boolean serviceBound = false;
    // 如果 pause 切换到后台，断开后，自动重连
    // 该状态禁止保存恢复
    private boolean resumeScrcpy = false;
    SensorManager sensorManager;
    private SendCommands sendCommands;
    private int videoBitrate;
    private int delayControl;
    private Context context;
    private String serverAdr = null;
    private SurfaceView surfaceView;
    private Surface surface;
    private SurfaceHolder.Callback surfaceCallback;
    private Scrcpy scrcpy;
    private long timestamp = 0;

    private LinearLayout linearLayout;

    private boolean autoResolutionEnabled = false;
    private ResolutionHelper.LimitSource resolutionLimitSource = null;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            scrcpy = ((Scrcpy.MyServiceBinder) iBinder).getService();
            scrcpy.setServiceCallbacks(MainActivity.this);
            serviceBound = true;
            if (first_time) {
                if (!Progress.isShowing()) {
                    Progress.showDialog(MainActivity.this, getString(R.string.please_wait));
                }
                scrcpy.start(surface, Scrcpy.LOCAL_IP + ":" + Scrcpy.LOCAL_FORWART_PORT,
                        screenHeight, screenWidth, delayControl,
                        PreUtils.get(MainActivity.this, Constant.AUDIO_FORWARD, true));
                ThreadUtils.workPost(() -> {
                    boolean success = AdbHelper.executeWithTimeout(() -> {
                        while (!scrcpy.check_socket_connection()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }, SendCommands.WAIT_TIME, TimeUnit.MILLISECONDS);
                    ThreadUtils.post(() -> {
                        Progress.closeDialog();
                        if (!success) {
                            if (serviceBound) {
                                showMainView();
                            }
                            Toast.makeText(context, "Connection Timed out 2", Toast.LENGTH_SHORT).show();
                        } else {
                            first_time = false;
                            // 连接成功后，再把按钮显示出来
                            set_display_nd_touch();
                            connectSuccessExt();
                        }
                    });
                });
            } else {
                scrcpy.setParms(surface, screenWidth, screenHeight);
                set_display_nd_touch();
                connectSuccessExt();
            }
            // set_display_nd_touch();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    private void showMainView() {
        showMainView(false);
    }

    // userDisconnect ：是否为用户手动断开连接
    private void showMainView(boolean userDisconnect) {
        if (scrcpy != null) {
            scrcpy.StopService();
        }
        try {
            // 可能会导致重复解绑，所以捕获异常
            unbindService(serviceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (surfaceView != null && surfaceCallback != null) {
            surfaceView.getHolder().removeCallback(surfaceCallback);
            surfaceView = null;
        }
        surfaceCallback = null;
        if (surface != null) {
            surface = null;
        }
        serviceBound = false;
        scrcpy_main();

        if (scrcpy != null) {
            scrcpy = null;
        }
        // 退出连接，需要处理额外的事件
        connectExitExt(userDisconnect);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setTitle(getString(R.string.app_name));
        if (savedInstanceState != null) {
            first_time = savedInstanceState.getBoolean("first_time");
            landscape = savedInstanceState.getBoolean("landscape");
            headlessMode = savedInstanceState.getBoolean("headlessMode");
            resumeScrcpy = savedInstanceState.getBoolean("resumeScrcpy");
            screenHeight = savedInstanceState.getInt("screenHeight");
            screenWidth = savedInstanceState.getInt("screenWidth");
        }
        if (first_time) {
            scrcpy_main();
        } else {
            landscape = getResources().getConfiguration().orientation
                    != Configuration.ORIENTATION_PORTRAIT;
            Log.e("Scrcpy: ", "from onCreate");
            start_screen_copy_magic();
        }
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        Sensor proximity;
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);

        if (savedInstanceState != null) {
            Log.i("Scrcpy", "outState: " + savedInstanceState.getBoolean("from_save_instance"));
        }
        // 从销毁状态恢复
        if (savedInstanceState == null || !savedInstanceState.getBoolean("from_save_instance", false)) {
            // 初次进入 app
            if (getIntent() != null && getIntent().getExtras() != null) {
                headlessMode = getIntent().getExtras().getBoolean(START_REMOTE, headlessMode);
            }
        }
        if (headlessMode && first_time) {
            getAttributes();
            connectScrcpyServer(PreUtils.get(this, Constant.CONTROL_REMOTE_ADDR, ""));
        }
        if (headlessMode) {
            View scrollView = findViewById(R.id.main_scroll_view);
            if (scrollView != null) {
                scrollView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i("Scrcpy", "enter onSaveInstanceState");
        outState.putBoolean("from_save_instance", true);
        outState.putBoolean("first_time", first_time);
        outState.putBoolean("landscape", landscape);
        outState.putBoolean("headlessMode", headlessMode);  // 第二次进入时，intent会被重置，需要保存状态
        // 小窗模式、半屏模式切换避免恢复横竖屏，会导致黑屏（因为scrcpy恢复的resume只允许一次连接）
        // outState.putBoolean("resumeScrcpy", resumeScrcpy);
        outState.putInt("screenHeight", screenHeight);
        outState.putInt("screenWidth", screenWidth);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void scrcpy_main() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(getColor(R.color.status_bar));
        } else {
            this.getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar));
        }
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        landscape = false;  // 将模式重新置为 竖屏，模式不正确将导致连接黑屏
        setContentView(R.layout.activity_main);

        // find view by id
        ScrollView scrollView = findViewById(R.id.main_scroll_view);
        Button startButton = findViewById(R.id.button_start);
        TextView btnMoreSettings = findViewById(R.id.btn_more_settings);
        LinearLayout layoutMoreSettings = findViewById(R.id.layout_more_settings);

        sendCommands = new SendCommands();

        startButton.setOnClickListener(v -> {
            getAttributes();
            connectScrcpyServer(serverAdr);
        });
        btnMoreSettings.setOnClickListener(v -> {
            AutoTransition autoTransition = new AutoTransition();
            TransitionManager.beginDelayedTransition(scrollView, autoTransition);

            if (layoutMoreSettings.getVisibility() == View.GONE) {
                layoutMoreSettings.setVisibility(View.VISIBLE);
                btnMoreSettings.setText(R.string.collapse_settings);
            } else {
                layoutMoreSettings.setVisibility(View.GONE);
                btnMoreSettings.setText(R.string.more_settings);
            }
        });

        get_saved_preferences();

        EditText editText = findViewById(R.id.editText_server_host);

        findViewById(R.id.history_list).setOnClickListener(v -> {
            Log.i("Scrcpy", "focus true");
            editText.clearFocus();
            showListPopulWindow(editText);
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateResolutionPreview();
            }
        });

        // 无头模式，实际上要隐藏掉所有控件，否则会被显示出 ip 地址
        if (headlessMode) {
            if (scrollView != null) {
                scrollView.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showListPopulWindow(EditText mEditText) {
        String[] list = getHistoryList();//要填充的数据
        if (list.length == 0) {  // 如果list为空，则使用本机填充一个
            list = new String[]{"127.0.0.1"};
        }
        final ListPopupWindow listPopupWindow;
        listPopupWindow = new ListPopupWindow(this);
        listPopupWindow.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list));//用android内置布局，或设计自己的样式
        listPopupWindow.setAnchorView(mEditText);//以哪个控件为基准，在该处以mEditText为基准
        listPopupWindow.setModal(true);
        listPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        String[] finalList = list;
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {//设置项点击监听
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mEditText.setText(finalList[i]);
                listPopupWindow.dismiss();
            }
        });
        listPopupWindow.show();
    }


//    private void showDisplayWindow() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                //启动Activity让用户授权
//                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                startActivity(intent);
//                return;
//            }
//        }
//        Intent it = new Intent(this, FloatService.class);
//        it.putExtra("ip", serverAdr);
//        it.putExtra("w", screenWidth);
//        it.putExtra("h", screenHeight);
//        it.putExtra("b", videoBitrate);
//        startService(it);
//        finish();
//    }


    public void get_saved_preferences() {
        EditText editTextServerHost = findViewById(R.id.editText_server_host);
        Switch aSwitch0 = findViewById(R.id.switch0);
        Switch aSwitch1 = findViewById(R.id.switch1);
        Switch audioForwardSwitch = findViewById(R.id.switch_audio_enable);
        String historySpServerAdr = PreUtils.get(context, Constant.CONTROL_REMOTE_ADDR, "");
        if (TextUtils.isEmpty(historySpServerAdr)) {
            String[] historyList = getHistoryList();
            if (historyList.length > 0) {
                editTextServerHost.setText(historyList[0]);
            }
        } else {
            editTextServerHost.setText(historySpServerAdr);
        }
        aSwitch0.setChecked(PreUtils.get(context, Constant.CONTROL_NO, false));
        aSwitch1.setChecked(PreUtils.get(context, Constant.CONTROL_NAV, false));
        audioForwardSwitch.setChecked(PreUtils.get(context, Constant.AUDIO_FORWARD, true));

        setSpinner(R.array.options_bitrate_keys, R.id.spinner_video_bitrate, Constant.PREFERENCE_SPINNER_BITRATE);
        setSpinner(R.array.options_delay_keys, R.id.delay_control_spinner, Constant.PREFERENCE_SPINNER_DELAY);
        setupResolutionSpinner();
        if (aSwitch0.isChecked()) {
            aSwitch1.setClickable(false);
            aSwitch1.setTextColor(Color.GRAY);
            // aSwitch1.setVisibility(View.GONE);
        }

        aSwitch0.setOnClickListener(v -> {
            if (aSwitch0.isChecked()) {
                aSwitch1.setClickable(false);
                aSwitch1.setTextColor(Color.GRAY);
                // aSwitch1.setVisibility(View.GONE);
            } else {
                aSwitch1.setClickable(true);
                aSwitch1.setTextColor(Color.WHITE);
                // aSwitch1.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void set_display_nd_touch() {
        DisplayMetrics metrics = new DisplayMetrics();
        if (ViewConfiguration.get(context).hasPermanentMenuKey()) {
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        } else {
            final Display display = getWindowManager().getDefaultDisplay();
            display.getRealMetrics(metrics);
        }
//        float this_dev_height = metrics.heightPixels;
//        float this_dev_width = metrics.widthPixels;

        float this_dev_height = linearLayout.getHeight();
        float this_dev_width = linearLayout.getWidth();
        if (PreUtils.get(context, Constant.CONTROL_NAV, false) &&
                !PreUtils.get(context, Constant.CONTROL_NO, false)) {
            if (landscape) {
                this_dev_width = this_dev_width - 96;
            } else {                                                 //100 is the height of nav bar but need multiples of 8.
                this_dev_height = this_dev_height - 96;
            }
        }
        int[] rem_res = scrcpy.get_remote_device_resolution();
        int remote_device_height = rem_res[1];
        int remote_device_width = rem_res[0];
        float remote_device_aspect_ratio = (float) remote_device_height / remote_device_width;

        if (!landscape) {                                                            //Portrait
            float this_device_aspect_ratio = this_dev_height / this_dev_width;
//            Log.d("fuck", "set_display_nd_touch: "+this_device_aspect_ratio);
            if (remote_device_aspect_ratio > this_device_aspect_ratio) {
                //TODO
                float wantWidth = this_dev_height / remote_device_aspect_ratio;
                int padding = (int) (this_dev_width - wantWidth) / 2;
                linearLayout.setPadding(padding, 0, padding, 0);
            } else if (remote_device_aspect_ratio < this_device_aspect_ratio) {
                linearLayout.setPadding(0, (int) (((this_device_aspect_ratio - remote_device_aspect_ratio) * this_dev_width)), 0, 0);
            }

        } else {                                                                        //Landscape
            float this_device_aspect_ratio = this_dev_width / this_dev_height;
//            Log.d("fuck", "set_display_nd_touch_land: "+this_device_aspect_ratio);
            if (remote_device_aspect_ratio > this_device_aspect_ratio) {
                float wantHeight = this_dev_width / remote_device_aspect_ratio;
                int padding = (int) (this_dev_height - wantHeight) / 2;
                linearLayout.setPadding(0, padding, 0, padding);
            } else if (remote_device_aspect_ratio < this_device_aspect_ratio) {
                linearLayout.setPadding(((int) (((this_device_aspect_ratio - remote_device_aspect_ratio) * this_dev_height)) / 2), 0, ((int) (((this_device_aspect_ratio - remote_device_aspect_ratio) * this_dev_height)) / 2), 0);
            }

        }
        if (!PreUtils.get(context, Constant.CONTROL_NO, false)) {
            // Log.i("Screen", "setOnTouchListener: " + surfaceView.getWidth() + "x" + surfaceView.getHeight());
            surfaceView.setOnTouchListener((view, event) -> scrcpy.touchevent(event, landscape, surfaceView.getWidth(), surfaceView.getHeight()));
        }

        if (PreUtils.get(context, Constant.CONTROL_NAV, false) &&
                !PreUtils.get(context, Constant.CONTROL_NO, false)) {
            final View backButton = findViewById(R.id.back_button);
            final View homeButton = findViewById(R.id.home_button);
            final View appswitchButton = findViewById(R.id.appswitch_button);

            if (backButton != null) {
                backButton.setOnClickListener(v -> scrcpy.sendKeyevent(KeyEvent.KEYCODE_BACK));
            }
            if (homeButton != null) {
                homeButton.setOnClickListener(v -> scrcpy.sendKeyevent(KeyEvent.KEYCODE_HOME));
            }
            if (appswitchButton != null) {
                appswitchButton.setOnClickListener(v -> scrcpy.sendKeyevent(KeyEvent.KEYCODE_APP_SWITCH));
            }
        }
    }

    private void setSpinner(final int textArrayOptionResId, final int textViewResId, final String preferenceId) {

        final Spinner spinner = findViewById(textViewResId);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, textArrayOptionResId, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreUtils.put(context, preferenceId, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                PreUtils.put(context, preferenceId, 0);
            }
        });
        int selection = PreUtils.get(context, preferenceId, 0);
        if (selection < arrayAdapter.getCount()) {
            spinner.setSelection(selection);
        } else {
            spinner.setSelection(0);
        }
    }

    private void setupResolutionSpinner() {
        final Spinner spinner = findViewById(R.id.spinner_video_resolution);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreUtils.put(context, Constant.PREFERENCE_SPINNER_RESOLUTION, position);
                updateResolutionPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                PreUtils.put(context, Constant.PREFERENCE_SPINNER_RESOLUTION, 0);
            }
        });
        int selection = PreUtils.get(context, Constant.PREFERENCE_SPINNER_RESOLUTION, 0);
        if (selection < spinner.getCount()) {
            spinner.setSelection(selection);
        } else {
            spinner.setSelection(0);
        }
        updateResolutionPreview();
    }

    private void updateResolutionPreview() {
        TextView info = findViewById(R.id.text_resolution_info);
        Spinner spinner = findViewById(R.id.spinner_video_resolution);
        if (info == null || spinner == null) {
            return;
        }
        if (spinner.getSelectedItemPosition() != Constant.RESOLUTION_AUTO_INDEX) {
            info.setVisibility(View.GONE);
            return;
        }
        EditText editText = findViewById(R.id.editText_server_host);
        String remoteAdr = editText != null ? editText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(remoteAdr)) {
            info.setText(R.string.resolution_auto_need_ip);
            info.setVisibility(View.VISIBLE);
            return;
        }
        info.setText(R.string.resolution_auto_detecting);
        info.setVisibility(View.VISIBLE);
        ThreadUtils.workPost(() -> {
            try {
                ResolutionHelper.AutoResolution autoResolution =
                        ResolutionHelper.computeAuto(context, remoteAdr);
                final String message = formatResolutionMessage(autoResolution, false);
                ThreadUtils.post(() -> {
                    if (!isFinishing()) {
                        updateResolutionInfoText(message);
                    }
                });
            } catch (Exception e) {
                Log.e("Scrcpy", "Resolution preview failed: " + e.getMessage());
                ThreadUtils.post(() -> {
                    if (!isFinishing()) {
                        info.setText(R.string.resolution_auto_failed);
                        info.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void updateResolutionInfoText(String message) {
        TextView info = findViewById(R.id.text_resolution_info);
        if (info == null) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            info.setVisibility(View.GONE);
            return;
        }
        info.setText(message);
        info.setVisibility(View.VISIBLE);
    }

    private String formatResolutionMessage(ResolutionHelper.AutoResolution autoResolution, boolean activeStream) {
        if (autoResolution.limitSource == ResolutionHelper.LimitSource.LOCAL) {
            return getString(activeStream ? R.string.resolution_auto_stream_local : R.string.resolution_auto_local,
                    autoResolution.displayWidth, autoResolution.displayHeight);
        }
        return getString(activeStream ? R.string.resolution_auto_stream_remote : R.string.resolution_auto_remote,
                autoResolution.displayWidth, autoResolution.displayHeight);
    }

    private String formatActiveStreamResolutionMessage() {
        return formatResolutionMessage(
                new ResolutionHelper.AutoResolution(
                        Math.max(screenWidth, screenHeight),
                        screenWidth,
                        screenHeight,
                        ResolutionHelper.getLocalMaxDimension(context),
                        screenWidth,
                        screenHeight,
                        resolutionLimitSource),
                true);
    }

    private void getAttributes() {

        final EditText editTextServerHost = findViewById(R.id.editText_server_host);
        serverAdr = editTextServerHost.getText().toString();
        if (!TextUtils.isEmpty(serverAdr)) {
            serverAdr = serverAdr.trim();
        }
        if (!TextUtils.isEmpty(serverAdr)) {
            PreUtils.put(context, Constant.CONTROL_REMOTE_ADDR, serverAdr);
        }
        final Spinner videoResolutionSpinner = findViewById(R.id.spinner_video_resolution);
        final Spinner videoBitrateSpinner = findViewById(R.id.spinner_video_bitrate);
        final Spinner delayControlSpinner = findViewById(R.id.delay_control_spinner);

        Switch a_Switch0 = findViewById(R.id.switch0);
        Switch a_Switch1 = findViewById(R.id.switch1);
        Switch audioEnableSwitch = findViewById(R.id.switch_audio_enable);
        PreUtils.put(context, Constant.CONTROL_NO, a_Switch0.isChecked());
        PreUtils.put(context, Constant.CONTROL_NAV, a_Switch1.isChecked());
        PreUtils.put(context, Constant.AUDIO_FORWARD, audioEnableSwitch.isChecked());

        final String[] videoResolutions = getResources().getStringArray(R.array.options_resolution_values)[videoResolutionSpinner.getSelectedItemPosition()].split("x");
        autoResolutionEnabled = videoResolutionSpinner.getSelectedItemPosition() == Constant.RESOLUTION_AUTO_INDEX;
        resolutionLimitSource = null;
        if (!autoResolutionEnabled) {
            screenHeight = Integer.parseInt(videoResolutions[0]);
            screenWidth = Integer.parseInt(videoResolutions[1]);
        }
        videoBitrate = getResources().getIntArray(R.array.options_bitrate_values)[videoBitrateSpinner.getSelectedItemPosition()];
        delayControl = getResources().getIntArray(R.array.options_delay_values)[delayControlSpinner.getSelectedItemPosition()];
    }

    private String[] getHistoryList() {
        String historyList = PreUtils.get(context, Constant.HISTORY_LIST_KEY, "");
        if (TextUtils.isEmpty(historyList)) {
            return new String[]{};
        }
        try {
            JSONArray historyJson = new JSONArray(historyList);
            String[] retList = new String[historyJson.length()];
            for (int i = 0; i < historyJson.length(); i++) {
                retList[i] = historyJson.get(i).toString();
            }
            return retList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[]{};
    }

    /**
     * 保存设备历史连接记录
     */
    private boolean saveHistory(String device) {
        if (headlessMode) {
            // 无头模式不保存记录
            return false;
        }
        JSONArray historyJson = new JSONArray();
        String[] historyList = getHistoryList();
        if (historyList.length == 0) {
            historyJson.put(device);
        } else {
            try {
                historyJson.put(0, device);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // 最多记录 30 个
            int count = Math.min(historyList.length, 30);
            for (int i = 0; i < count; i++) {
                if (!historyList[i].equals(device)) {
                    historyJson.put(historyList[i]);
                }
            }
        }
        try {
            return PreUtils.put(context, Constant.HISTORY_LIST_KEY, historyJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void swapDimensions() {
        int temp = screenHeight;
        screenHeight = screenWidth;
        screenWidth = temp;
    }

    private void syncLandscapeFromConfiguration() {
        landscape = getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_PORTRAIT;
    }

    private void applyMirrorImmersiveMode() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void refreshMirrorLayout() {
        if (linearLayout == null || scrcpy == null) {
            return;
        }
        linearLayout.setPadding(0, 0, 0, 0);
        linearLayout.post(() -> {
            if (serviceBound && scrcpy != null) {
                set_display_nd_touch();
                if (surfaceView != null) {
                    Surface currentSurface = surfaceView.getHolder().getSurface();
                    if (isSurfaceReady(currentSurface)) {
                        surface = currentSurface;
                        scrcpy.setParms(surface, screenWidth, screenHeight);
                    }
                }
            }
        });
    }

    private void setMirrorContentView() {
        Configuration overrideConfig = new Configuration(getResources().getConfiguration());
        overrideConfig.orientation = landscape
                ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT;
        View root = LayoutInflater.from(createConfigurationContext(overrideConfig))
                .inflate(R.layout.surface, null);
        setContentView(root);
    }

    private void setupNavBar() {
        final LinearLayout nav_bar = findViewById(R.id.nav_button_bar);
        if (nav_bar == null) {
            return;
        }
        if (PreUtils.get(context, Constant.CONTROL_NAV, false) &&
                !PreUtils.get(context, Constant.CONTROL_NO, false)) {
            nav_bar.setVisibility(LinearLayout.VISIBLE);
        } else {
            nav_bar.setVisibility(LinearLayout.GONE);
        }
    }

    private void bindMirrorSurfaceHolder() {
        surfaceView = findViewById(R.id.decoder_surface);
        if (surfaceView == null) {
            return;
        }
        if (surfaceCallback != null) {
            surfaceView.getHolder().removeCallback(surfaceCallback);
        }
        surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surface = holder.getSurface();
                if (!serviceBound) {
                    start_Scrcpy_service();
                } else if (scrcpy != null && isSurfaceReady(surface)) {
                    scrcpy.setParms(surface, screenWidth, screenHeight);
                    refreshMirrorLayout();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                surface = holder.getSurface();
                if (serviceBound && scrcpy != null && isSurfaceReady(surface)) {
                    scrcpy.setParms(surface, screenWidth, screenHeight);
                    refreshMirrorLayout();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surface = null;
            }
        };
        surfaceView.getHolder().addCallback(surfaceCallback);
    }

    private void setupMirrorContentView() {
        syncLandscapeFromConfiguration();
        setMirrorContentView();
        applyMirrorImmersiveMode();
        bindMirrorSurfaceHolder();
        setupNavBar();
        linearLayout = findViewById(R.id.container1);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!first_time && serviceBound) {
            result_of_Rotation = true;
            setupMirrorContentView();
        }
    }

    private boolean isSurfaceReady(Surface displaySurface) {
        if (displaySurface == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return displaySurface.isValid();
        }
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void start_screen_copy_magic() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        setupMirrorContentView();
    }

    private void start_Scrcpy_service() {
        Intent intent = new Intent(this, Scrcpy.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void loadNewRotation() {
        ThreadUtils.post(() -> {
            if (first_time) {
                first_time = false;
            }
            result_of_Rotation = true;
            landscape = !landscape;
            swapDimensions();
            if (landscape) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        });
    }

    @Override
    public void errorDisconnect() {
        // 必须退出
        // 退出重连
        Dialog.displayDialog(this, getString(R.string.disconnect),
                getString(R.string.disconnect_ask), () -> {
                    if (serviceBound) {
                        showMainView();
                        first_time = true;
                    } else {
                        MainActivity.this.finish();
                    }
                }, false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (resumeScrcpy && !isChangingConfigurations()) {
            // 返回到主页面，属于用户主动断开场景（旋转屏幕时不断开）
            showMainView(true);
            first_time = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Scrcpy", "onStart: " + serviceBound);
        if (resumeScrcpy) {
            if (!serviceBound) {
                resumeScrcpy = false;
                connectScrcpyServer(PreUtils.get(context, Constant.CONTROL_REMOTE_ADDR, ""));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Scrcpy", "onPause: " + serviceBound);
        if (serviceBound && scrcpy != null) {
            scrcpy.pause();
            resumeScrcpy = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!first_time && !result_of_Rotation) {
            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            if (serviceBound) {
                if (surfaceView != null) {
                    Surface currentSurface = surfaceView.getHolder().getSurface();
                    if (isSurfaceReady(currentSurface)) {
                        surface = currentSurface;
                        scrcpy.setParms(surface, screenWidth, screenHeight);
                    }
                }
                scrcpy.resume();
            }
        }
        if (resumeScrcpy && !result_of_Rotation && scrcpy != null) {
            scrcpy.resume();
        }
        resumeScrcpy = false;  // 两处都要resumeScrcpy设置为false
        result_of_Rotation = false;
    }

    @Override
    public void onBackPressed() {
        if (timestamp == 0) {
            if (serviceBound) {
                timestamp = SystemClock.uptimeMillis();
                Toast.makeText(context, "Press again to exit", Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        } else {
            long now = SystemClock.uptimeMillis();
            if (now < timestamp + 1000) {
                timestamp = 0;
                if (serviceBound) {
                    showMainView(true);
                    first_time = true;
                } else {
                    finish();
                }
            }
            timestamp = 0;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (sensorEvent.values[0] == 0) {
                if (serviceBound) {
                    // 该事件会使远程手机 按下电源键，触发方式：按住距离传感器，然后点击屏幕即可锁屏
                    // 发送横竖屏会导致抬起事件无效
                    // scrcpy.sendKeyevent(28);
                }
            } else {
                if (serviceBound) {
                    // 发送横竖屏会导致抬起事件无效
                    // scrcpy.sendKeyevent(29);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void connectScrcpyServer(String serverAdr) {
        if (!TextUtils.isEmpty(serverAdr)) {
            saveHistory(serverAdr);  // 保存到历史记录
            String[] serverInfo = Util.getServerHostAndPort(serverAdr);
            String serverHost = serverInfo[0];
            int serverPort = Integer.parseInt(serverInfo[1]);
            int localForwardPort = Scrcpy.LOCAL_FORWART_PORT;

            Progress.showDialog(MainActivity.this, getString(R.string.please_wait));
            ThreadUtils.workPost(() -> {
                AdbHelper.writeAssetsJarServer(App.mContext);
                if (autoResolutionEnabled) {
                    ResolutionHelper.AutoResolution autoResolution =
                            ResolutionHelper.computeAuto(context, serverAdr);
                    screenWidth = autoResolution.displayWidth;
                    screenHeight = autoResolution.displayHeight;
                    resolutionLimitSource = autoResolution.limitSource;
                    final String resolutionMessage = formatResolutionMessage(autoResolution, false);
                    ThreadUtils.post(() -> updateResolutionInfoText(resolutionMessage));
                }
                SendCommands.CmdStatus sendStatus = sendCommands.SendAdbCommands(context, serverHost,
                        serverPort,
                        localForwardPort,
                        Scrcpy.LOCAL_IP,
                        videoBitrate, Math.max(screenHeight, screenWidth),
                        PreUtils.get(context, Constant.AUDIO_FORWARD, true));
                if (sendStatus == SendCommands.CmdStatus.SUCCESS) {
                    ThreadUtils.post(() -> {
                        if (!MainActivity.this.isFinishing()) {
                            // 进入主线程
                            Log.e("Scrcpy: ", "from startButton");
                            start_screen_copy_magic();
                        }
                    });
                } else {
                    ThreadUtils.post(Progress::closeDialog);
                    Toast.makeText(context, "Network OR ADB connection failed", Toast.LENGTH_SHORT).show();
                    connectExitExt();
                }
            });
        } else {
            Toast.makeText(context, "Server Address Empty", Toast.LENGTH_SHORT).show();
            connectExitExt();
        }
    }

    /**
     * 连接成功了，而且成功的显示了画面出来
     */
    protected void connectSuccessExt() {
        Dialog.closeDialogs();
        if (autoResolutionEnabled && resolutionLimitSource != null) {
            Toast.makeText(context, formatActiveStreamResolutionMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void connectExitExt() {
        this.connectExitExt(false);
    }

    /**
     * 连接失败的额外处理
     */
    protected void connectExitExt(boolean userDisconnect) {
        if (!userDisconnect) {  // userDisconnect : 用户主动断开连接
            // 如果自动断开了端口连接，在系统恢复时，重启adb，避免
            // 警告！！！ 重启将会导致 adb 配对过程失效，从而无法连接新设备，需要更智能的重启机制
            // AdbHelper.restartAdb();
        }
        if (headlessMode && !resumeScrcpy && !result_of_Rotation) {
            if (!userDisconnect) {
                Dialog.displayDialog(this, getString(R.string.connect_faild),
                        getString(R.string.connect_faild_ask), () -> {
                            // 重试连接
                            connectScrcpyServer(PreUtils.get(context, Constant.CONTROL_REMOTE_ADDR, ""));
                        }, () -> {
                            // 取消重试
                            finishAndRemoveTask();
                        });
            } else {
                finishAndRemoveTask();
            }
        }
//        Log.i("Scrcpy", "headlessMode： " + headlessMode +
//                " ,resumeScrcpy: " + resumeScrcpy + " ,result_of_Rotation: " + result_of_Rotation);
    }

}
