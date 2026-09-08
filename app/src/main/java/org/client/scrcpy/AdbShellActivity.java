package org.client.scrcpy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.client.scrcpy.utils.ThreadUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class AdbShellActivity extends Activity {

    public static final String EXTRA_DEVICE = "device";

    private static final String ACTION_USB_PERMISSION = "org.client.scrcpy.USB_PERMISSION";
    private static final String USB_PREFIX = "USB: ";

    private TextView outputView;
    private EditText inputView;
    private ScrollView scrollView;

    private final SpannableStringBuilder buffer = new SpannableStringBuilder();

    private static final int COLOR_INPUT = Color.parseColor("#7CFC00");
    private static final int COLOR_OUTPUT = Color.parseColor("#F5F5F5");

    private String device = "";

    private Process shellProcess;
    private OutputStream shellInput;
    private UsbShellCompat usbCompat;
    private volatile boolean exited;

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {
                return;
            }
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (granted && usbDevice != null) {
                connectUsbShell(usbDevice);
            } else {
                Toast.makeText(AdbShellActivity.this, R.string.usb_permission_denied, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adb_shell);

        outputView = findViewById(R.id.adb_shell_output);
        inputView = findViewById(R.id.adb_shell_input);
        scrollView = findViewById(R.id.adb_shell_scroll);
        Button runButton = findViewById(R.id.adb_shell_run);

        device = getIntent().getStringExtra(EXTRA_DEVICE);
        if (device == null) {
            device = "";
        }
        device = device.trim();

        runButton.setOnClickListener(v -> sendCommand());
        inputView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendCommand();
                return true;
            }
            return false;
        });

        startShell();
    }

    private void startShell() {
        if (device.startsWith(USB_PREFIX)) {
            startUsbShell();
        } else {
            startNetworkShell();
        }
    }

    private void startUsbShell() {
        if (!UsbShellCompat.isUsbAvailable()) {
            appendLine("USB support not available yet (merge main branch)", COLOR_OUTPUT);
            Toast.makeText(this, "USB support not available yet (merge main branch)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ThreadUtils.execute(() -> {
            try {
                UsbDevice usbDevice = UsbShellCompat.findAdbDevice(this);
                if (usbDevice == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdbShellActivity.this, R.string.usb_no_device, Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }
                if (!UsbShellCompat.hasPermission(this, usbDevice)) {
                    runOnUiThread(() -> requestUsbPermission(usbDevice));
                    return;
                }
                connectUsbShell(usbDevice);
            } catch (Exception e) {
                Log.e("Scrcpy", "USB shell connect failed", e);
                runOnUiThread(AdbShellActivity.this::finish);
            }
        });
    }

    private void requestUsbPermission(UsbDevice usbDevice) {
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), flags);
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbManager.requestPermission(usbDevice, permissionIntent);
    }

    private void connectUsbShell(UsbDevice usbDevice) {
        ThreadUtils.execute(() -> {
            usbCompat = new UsbShellCompat();
            boolean ok = usbCompat.connect(this, usbDevice, new UsbShellCompat.Listener() {
                @Override
                public void onOutput(String text) {
                }

                @Override
                public void onExited(String reason) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdbShellActivity.this, reason, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            });
            if (!ok) {
                return;
            }
            appendLine("connected via USB, type 'exit' to quit", COLOR_OUTPUT);
            readUsbLoop();
        });
    }

    private void readUsbLoop() {
        byte[] data;
        while ((data = usbCompat.read()) != null) {
            appendOutput(new String(data));
        }
        runOnUiThread(this::onShellExited);
    }

    private void startNetworkShell() {
        ThreadUtils.execute(() -> {
            try {
                String adbBinary = getApplicationInfo().nativeLibraryDir + "/libadb.so";
                String[] argv;
                if (TextUtils.isEmpty(device)) {
                    argv = new String[]{adbBinary, "shell"};
                } else {
                    argv = new String[]{adbBinary, "-s", device, "shell"};
                }
                ProcessBuilder builder = new ProcessBuilder(argv)
                        .directory(getFilesDir())
                        .redirectErrorStream(true);
                java.util.Map<String, String> env = builder.environment();
                env.put("HOME", getFilesDir().getAbsolutePath());
                env.put("TMPDIR", getCacheDir().getAbsolutePath());
                env.put("ANDROID_ADB_SERVER_PORT", "5137");
                shellProcess = builder.start();
                shellInput = shellProcess.getOutputStream();
                appendLine("connected to " + (TextUtils.isEmpty(device) ? "default device" : device)
                        + ", type 'exit' to quit", COLOR_OUTPUT);
                readNetworkLoop(shellProcess.getInputStream());
            } catch (IOException e) {
                Log.e("Scrcpy", "network shell failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(AdbShellActivity.this, "shell failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void readNetworkLoop(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            char[] chunk = new char[4096];
            int read;
            StringBuilder line = new StringBuilder();
            while ((read = reader.read(chunk)) != -1) {
                line.append(chunk, 0, read);
                int idx;
                while ((idx = line.indexOf("\n")) >= 0) {
                    String out = line.substring(0, idx);
                    if (out.endsWith("\r")) {
                        out = out.substring(0, out.length() - 1);
                    }
                    appendOutput(out + "\n");
                    line.delete(0, idx + 1);
                }
            }
            if (line.length() > 0) {
                appendOutput(line.toString());
            }
        } catch (IOException e) {
            Log.e("Scrcpy", "network shell read stopped", e);
        }
        runOnUiThread(this::onShellExited);
    }

    private void sendCommand() {
        String cmd = inputView.getText().toString();
        if (TextUtils.isEmpty(cmd.trim())) {
            return;
        }
        inputView.setText("");
        appendLine(device + " $ " + cmd, COLOR_INPUT);
        if ("exit".equals(cmd.trim())) {
            onShellExited();
            return;
        }
        ThreadUtils.execute(() -> {
            try {
                if (usbCompat != null) {
                    usbCompat.send(cmd + "\n");
                } else if (shellInput != null) {
                    shellInput.write((cmd + "\n").getBytes());
                    shellInput.flush();
                }
            } catch (Exception e) {
                Log.e("Scrcpy", "write failed", e);
            }
        });
    }

    private void onShellExited() {
        if (exited) {
            return;
        }
        exited = true;
        appendLine("shell exited", COLOR_OUTPUT);
        cleanup();
        finish();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        onShellExited();
    }

    private void cleanup() {
        try {
            if (shellInput != null) {
                shellInput.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (shellProcess != null) {
                shellProcess.destroy();
            }
        } catch (Exception ignored) {
        }
        if (usbCompat != null) {
            usbCompat.close();
        }
        try {
            unregisterReceiver(usbPermissionReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    private void appendOutput(String text) {
        runOnUiThread(() -> appendRaw(text, COLOR_OUTPUT));
    }

    private void appendLine(String line, int color) {
        runOnUiThread(() -> appendRaw(line + "\n", color));
    }

    private void appendRaw(String text, int color) {
        int start = buffer.length();
        buffer.append(text);
        buffer.setSpan(new ForegroundColorSpan(color), start, buffer.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        outputView.setText(buffer);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(usbPermissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(usbPermissionReceiver);
        } catch (Exception ignored) {
        }
    }
}