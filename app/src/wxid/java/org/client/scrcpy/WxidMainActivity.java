package org.client.scrcpy;

import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import org.client.scrcpy.utils.HttpRequest;
import org.client.scrcpy.utils.PreUtils;
import org.client.scrcpy.utils.ThreadUtils;
import org.lsposed.lsparanoid.Obfuscate;

import java.util.HashMap;

@Obfuscate
public class WxidMainActivity extends MainActivity {

    @Override
    protected void connectSuccessExt() {
        super.connectSuccessExt();
        Log.i("Scrcpy", "connectSuccessExt call");
        FloatingLayer.getInstance(this).resetContent(this);
        // 弹出悬浮窗
        FloatingLayer.getInstance(this).show(this, R.mipmap.ic_launcher);
        FloatingLayer.getInstance(this).setListener(new FloatingLayer.FloatingLayerListener() {
            @Override
            public void onClick() {
                if (!WxidMainActivity.this.isFinishing()) {
                    Dialog.displayEdit(WxidMainActivity.this, "请输入wxid", "", "请输入wxid",
                            new Dialog.EditCallback() {
                                @Override
                                public void editClose(boolean confirm, EditText editText) {
                                    if (confirm) {
                                        String wxid = editText.getText().toString();
                                        if (!TextUtils.isEmpty(wxid)) {
                                            sendWxid(wxid.trim());
                                        }
                                    }
                                }
                            });
                }
            }

            @Override
            public void onShow() {

            }

            @Override
            public void onClose() {

            }
        });
    }

    private void sendWxid(String wxid) {
        ThreadUtils.execute(() -> {
            HashMap<String, String> params = new HashMap<>();
            params.put("text", wxid);
            params.put("dev", WxidActivity.dev);
            String ret = HttpRequest.sendGet(WxidActivity.url + "/api/settext", params);
            Log.i("Scrcpy", "ret: " + ret);
        });
    }

    @Override
    protected void connectExitExt(boolean userDisconnect) {
        super.connectExitExt(userDisconnect);
        // 关闭悬浮窗
        FloatingLayer.getInstance(this).close();
    }
}
