package org.client.scrcpy;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.client.scrcpy.utils.AdbHelper;
import org.client.scrcpy.utils.ExecUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdbTerminalDialog extends Dialog {

    public interface OnIpSelectedListener {
        void onIpSelected(String ipWithPort);
    }

    private final Activity activity;
    private final String defaultTargetIp;
    private final OnIpSelectedListener ipSelectedListener;

    private TextView consoleOutput;
    private ScrollView consoleScroll;
    private ProgressBar consoleProgress;
    private EditText editCommand;
    private Button btnRun;
    private Button btnClear;
    private LinearLayout layoutApplyIp;
    private TextView tvDetectedIp;
    private Button btnApplyIp;

    private final SpannableStringBuilder consoleBuffer = new SpannableStringBuilder();

    private static final int COLOR_CMD = Color.parseColor("#00E5FF");     // Cyan
    private static final int COLOR_STDOUT = Color.parseColor("#ECEFF1");  // Light gray
    private static final int COLOR_STDERR = Color.parseColor("#FF5252");  // Soft red
    private static final int COLOR_INFO = Color.parseColor("#FFD54F");    // Soft amber

    private static final Pattern IP_PORT_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):?(\\d{1,5})?");

    public AdbTerminalDialog(Activity activity, String defaultTargetIp, OnIpSelectedListener ipSelectedListener) {
        super(activity);
        this.activity = activity;
        this.defaultTargetIp = defaultTargetIp != null ? defaultTargetIp.trim() : "";
        this.ipSelectedListener = ipSelectedListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_adb_terminal);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int width = (int) (metrics.widthPixels * 0.94);
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        consoleOutput = findViewById(R.id.console_output);
        consoleScroll = findViewById(R.id.console_scroll);
        consoleProgress = findViewById(R.id.console_progress);
        editCommand = findViewById(R.id.edit_command);
        btnRun = findViewById(R.id.btn_run);
        btnClear = findViewById(R.id.btn_clear);
        layoutApplyIp = findViewById(R.id.layout_apply_ip);
        tvDetectedIp = findViewById(R.id.tv_detected_ip);
        btnApplyIp = findViewById(R.id.btn_apply_ip);

        ImageView closeBtn = findViewById(R.id.dialog_close_btn);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dismiss());
        }

        btnRun.setOnClickListener(v -> runCurrentCommand());
        btnClear.setOnClickListener(v -> clearConsole());

        editCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                runCurrentCommand();
                return true;
            }
            return false;
        });

        setupChips();

        appendLine("ADB Ready. Type commands (e.g., pair, connect, devices)\n", COLOR_INFO);
        // Automatically run 'devices' on open so user sees current state
        executeCommand("devices");
    }

    private void setupChips() {
        Button chipPair = findViewById(R.id.chip_pair);
        Button chipConnect = findViewById(R.id.chip_connect);
        Button chipDevices = findViewById(R.id.chip_devices);
        Button chipTcpip = findViewById(R.id.chip_tcpip);
        Button chipKill = findViewById(R.id.chip_kill);

        String baseIp = extractBaseIp(defaultTargetIp);

        if (chipPair != null) {
            chipPair.setOnClickListener(v -> {
                String template = !TextUtils.isEmpty(baseIp) ? "pair " + baseIp + ": " : "pair <ip>:<port> <code>";
                editCommand.setText(template);
                editCommand.setSelection(editCommand.getText().length());
                editCommand.requestFocus();
            });
        }

        if (chipConnect != null) {
            chipConnect.setOnClickListener(v -> {
                String template = !TextUtils.isEmpty(defaultTargetIp) ? "connect " + defaultTargetIp : "connect <ip>:5555";
                editCommand.setText(template);
                editCommand.setSelection(editCommand.getText().length());
                editCommand.requestFocus();
            });
        }

        if (chipDevices != null) {
            chipDevices.setOnClickListener(v -> {
                editCommand.setText("devices -l");
                runCurrentCommand();
            });
        }

        if (chipTcpip != null) {
            chipTcpip.setOnClickListener(v -> {
                editCommand.setText("tcpip 5555");
                runCurrentCommand();
            });
        }

        if (chipKill != null) {
            chipKill.setOnClickListener(v -> {
                editCommand.setText("kill-server");
                runCurrentCommand();
            });
        }
    }

    private String extractBaseIp(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }
        int colon = raw.indexOf(':');
        return colon > 0 ? raw.substring(0, colon) : raw;
    }

    private void runCurrentCommand() {
        String cmd = editCommand.getText().toString().trim();
        if (TextUtils.isEmpty(cmd)) {
            return;
        }
        executeCommand(cmd);
    }

    private void executeCommand(String cmd) {
        setRunningState(true);
        appendLine("$ adb " + cmd, COLOR_CMD);

        AdbHelper.runCustomAdbCommand(activity, cmd, new ExecUtil.AdbOutputListener() {
            @Override
            public void onLine(String line, boolean isError) {
                activity.runOnUiThread(() -> {
                    appendLine(line, isError ? COLOR_STDERR : COLOR_STDOUT);
                    checkForDetectedIp(line);
                });
            }

            @Override
            public void onComplete(int exitCode) {
                activity.runOnUiThread(() -> {
                    setRunningState(false);
                    if (exitCode != 0) {
                        appendLine("[Process exited with code " + exitCode + "]", COLOR_INFO);
                    }
                });
            }
        });
    }

    private void checkForDetectedIp(String line) {
        if (TextUtils.isEmpty(line)) {
            return;
        }
        String lower = line.toLowerCase();
        if (lower.contains("connected to") || lower.contains("paired to") || lower.contains("success")) {
            Matcher matcher = IP_PORT_PATTERN.matcher(line);
            if (matcher.find()) {
                String ip = matcher.group(1);
                String port = matcher.group(2);
                String fullTarget = (port != null && !port.isEmpty()) ? (ip + ":" + port) : (ip + ":5555");
                showDetectedIpBar(fullTarget);
            }
        }
    }

    private void showDetectedIpBar(String target) {
        if (layoutApplyIp != null && tvDetectedIp != null && btnApplyIp != null) {
            layoutApplyIp.setVisibility(View.VISIBLE);
            tvDetectedIp.setText("Target: " + target);
            btnApplyIp.setOnClickListener(v -> {
                if (ipSelectedListener != null) {
                    ipSelectedListener.onIpSelected(target);
                    Toast.makeText(activity, "Set target to " + target, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
    }

    private void setRunningState(boolean running) {
        if (consoleProgress != null) {
            consoleProgress.setVisibility(running ? View.VISIBLE : View.GONE);
        }
        if (btnRun != null) {
            btnRun.setEnabled(!running);
            btnRun.setText(running ? "..." : activity.getString(R.string.dialog_adb_terminal_run));
        }
    }

    private void clearConsole() {
        consoleBuffer.clear();
        consoleOutput.setText("");
    }

    private void appendLine(String line, int color) {
        int start = consoleBuffer.length();
        consoleBuffer.append(line).append("\n");
        consoleBuffer.setSpan(new ForegroundColorSpan(color), start, consoleBuffer.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        consoleOutput.setText(consoleBuffer);
        consoleScroll.post(() -> consoleScroll.fullScroll(View.FOCUS_DOWN));
    }
}

