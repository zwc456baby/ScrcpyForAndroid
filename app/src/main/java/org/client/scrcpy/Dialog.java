package org.client.scrcpy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

public class Dialog implements Runnable {
    private final String title;
    private final String message;
    private final String hint;
    private final String help;
    private final Activity activity;
    private final Runnable runOnDismiss;
    private final Runnable calcelDismiss;
    private final Runnable helpDismiss;

    private boolean isEdit = false;

    private AlertDialog alert;
    private EditCallback callback;

    private static final ArrayList<Dialog> rundownDialogs = new ArrayList<>();

    private Dialog(Activity activity, String title, String message, Runnable runOnDismiss, Runnable calcelDismiss) {
        this(activity, title, message, "", runOnDismiss, calcelDismiss);
    }

    private Dialog(Activity activity, String title, String message, String hint, Runnable runOnDismiss, Runnable calcelDismiss) {
        this(activity, title, message, "", "", runOnDismiss, calcelDismiss, null);
    }

    private Dialog(Activity activity, String title, String message, String hint, String help,
                   Runnable runOnDismiss, Runnable calcelDismiss, Runnable helpDismiss) {
        this.activity = activity;
        this.title = title;
        this.message = message;
        this.hint = hint;
        this.help = help;
        this.runOnDismiss = runOnDismiss;
        this.calcelDismiss = calcelDismiss;
        this.helpDismiss = helpDismiss;
    }

    private Dialog(Activity activity, String title, String message, Runnable runOnDismiss, Runnable calcelDismiss,
                   EditCallback callback) {
        this(activity, title, message, "", runOnDismiss, calcelDismiss, callback);
    }

    private Dialog(Activity activity, String title, String message, String hint, Runnable runOnDismiss, Runnable calcelDismiss,
                   EditCallback callback) {
        this(activity, title, message, hint, "", runOnDismiss, calcelDismiss, null, callback);
    }

    private Dialog(Activity activity, String title, String message, String hint, String help,
                   Runnable runOnDismiss, Runnable calcelDismiss,
                   Runnable helpDismiss,
                   EditCallback callback) {
        isEdit = true;
        this.activity = activity;
        this.title = title;
        this.message = message;
        this.hint = hint;
        this.help = help;
        this.runOnDismiss = runOnDismiss;
        this.calcelDismiss = calcelDismiss;
        this.helpDismiss = helpDismiss;
        this.callback = callback;
    }

    public static int curDialogCount() {
        return rundownDialogs.size();
    }

    public static void closeDialogs() {
        synchronized (rundownDialogs) {
            for (Dialog d : rundownDialogs) {
                if (d.alert.isShowing()) {
                    d.alert.dismiss();
                }
            }

            rundownDialogs.clear();
        }
    }

    public static void displayEdit(final Activity activity, String title, String message, String hint, EditCallback callback) {
        activity.runOnUiThread(new Dialog(activity, title, message, hint, null, null, callback));
    }

    public static void displayEdit(final Activity activity, String title, String message, String hint, String help,
                                   Runnable helpDismiss,
                                   EditCallback callback) {
        activity.runOnUiThread(new Dialog(activity, title, message, hint, help,
                null, null, helpDismiss,
                callback));
    }

    public static void displayEdit(Activity activity, String title, String message, String hint, Runnable runOnDismiss, Runnable calcelDismiss,
                                   EditCallback callback) {
        activity.runOnUiThread(new Dialog(activity, title, message, hint, runOnDismiss, calcelDismiss, callback));
    }

    public static void displayDialog(final Activity activity, String title, String message, final boolean endAfterDismiss) {
        activity.runOnUiThread(new Dialog(activity, title, message, new Runnable() {
            @Override
            public void run() {
                if (endAfterDismiss) {
                    activity.finish();
                }
            }
        }, () -> {
        }));
    }

    public static void displayDialog(Activity activity, String title, String message, Runnable runOnDismiss, boolean allowCancel) {
        activity.runOnUiThread(new Dialog(activity, title, message, runOnDismiss, allowCancel ? (Runnable) () -> {
        } : null));
    }

    public static void displayDialog(Activity activity, String title, String message, Runnable runOnDismiss) {
        displayDialog(activity, title, message, runOnDismiss, true);
    }

    public static void displayDialog(Activity activity, String title, String message, Runnable runOnDismiss, Runnable calcelDismiss) {
        activity.runOnUiThread(new Dialog(activity, title, message, runOnDismiss, calcelDismiss));
    }

    /**
     * 弹出带 帮助 按钮的
     */
    public static void displayDialog(Activity activity, String title, String message,
                                     String helpBtn,
                                     Runnable runOnDismiss, Runnable calcelDismiss, Runnable helpDismiss) {
        activity.runOnUiThread(new Dialog(activity, title, message, "", helpBtn,
                runOnDismiss, calcelDismiss, helpDismiss));
    }

    public abstract static class EditCallback {
        public abstract void editClose(boolean confirm, EditText editText);

        public void editShow(EditText editText) {
        }
    }

    @Override
    public void run() {
        // If we're dying, don't bother creating a dialog
        if (activity.isFinishing())
            return;

        alert = new AlertDialog.Builder(activity).create();

        alert.setTitle(title);
        alert.setCancelable(false);
        alert.setCanceledOnTouchOutside(false);

        EditText et = new EditText(activity);
        if (isEdit) {
            et.setHint(hint);  // 设置旧的key数据
            et.setText(message);  // 设置旧的key数据
            et.setGravity(Gravity.CENTER);
            alert.setView(et); //给对话框添加一个EditText输入文本框
        } else {
            alert.setMessage(message);
        }

        alert.setButton(AlertDialog.BUTTON_POSITIVE, activity.getResources().getText(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                synchronized (rundownDialogs) {
                    rundownDialogs.remove(Dialog.this);
                    alert.dismiss();
                }

                if (runOnDismiss != null) {
                    runOnDismiss.run();
                }
                if (isEdit && callback != null) {
                    callback.editClose(true, et);
                }
            }
        });
        if (calcelDismiss != null || isEdit) {
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getResources().getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (rundownDialogs) {
                        rundownDialogs.remove(Dialog.this);
                        alert.dismiss();
                    }

                    if (calcelDismiss != null) {
                        calcelDismiss.run();
                    }
                    if (isEdit && callback != null) {
                        callback.editClose(false, et);
                    }
                }
            });
        }
        if (!TextUtils.isEmpty(help)) {
            alert.setButton(AlertDialog.BUTTON_NEUTRAL, help, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (rundownDialogs) {
                        rundownDialogs.remove(Dialog.this);
                        alert.dismiss();
                    }

                    if (helpDismiss != null) {
                        helpDismiss.run();
                    }

                    if (isEdit && callback != null) {
                        callback.editClose(false, et);
                    }
                }
            });
        }
//        alert.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getResources().getText(R.string.help), new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                synchronized (rundownDialogs) {
//                    rundownDialogs.remove(Dialog.this);
//                    alert.dismiss();
//                }
//
//                runOnDismiss.run();
//
//                HelpLauncher.launchTroubleshooting(activity);
//            }
//        });
        alert.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                // Set focus to the OK button by default
                Button button = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setFocusable(true);
                button.setFocusableInTouchMode(false);
                button.requestFocus();
                if (isEdit && callback != null) {
                    callback.editShow(et);
                }
            }
        });

        synchronized (rundownDialogs) {
            rundownDialogs.add(this);
            alert.show();
        }
    }

}
