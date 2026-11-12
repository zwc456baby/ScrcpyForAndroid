package org.client.scrcpy.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import org.client.scrcpy.Scrcpy;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Map;

public class Util {

    public static String[] getServerHostAndPort(String serverAdr) {
        if (TextUtils.isEmpty(serverAdr)) {
            return new String[]{"127.0.0.1", String.valueOf(Scrcpy.DEFAULT_ADB_PORT)};
        }
        String serverHost = serverAdr;
        String serverPort = String.valueOf(Scrcpy.DEFAULT_ADB_PORT);
        if (serverAdr.contains(":")) {
            int lastIndex = serverAdr.lastIndexOf(":");

            try {
                serverHost = serverAdr.substring(0, lastIndex);
                serverPort = String.valueOf(Integer.parseInt(
                        serverAdr.substring(lastIndex + 1)
                ));
                // ipv6 不能去除前后的 []
//                if (serverHost.startsWith("[") && serverHost.endsWith("]")) {
//                    // 截取掉 ipv6 的 []
//                    serverHost = serverHost.substring(1, serverHost.length() - 1);
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new String[]{serverHost, serverPort};
    }

    public static int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics()
        );
    }

    /**
     * 把状态栏高度垫在标题栏上方：蓝色背景延伸到状态栏后面，标题仍在原来 80dp 区域居中。
     * API 35+ 强制 edge-to-edge 且 windowOptOutEdgeToEdgeEnforcement 在 targetSdk 36 下失效，
     * 所以只能通过 insets 处理；低版本 insets 为 0，表现和原来一致。
     *
     * @param header       标题栏
     * @param baseHeightDp 标题栏原始高度（dp）
     * @param contentBelow 紧跟在标题栏下方、用 marginTop 定位的视图，可为 null
     */
    public static void applyStatusBarInset(View header, int baseHeightDp, View contentBelow) {
        if (header == null) {
            return;
        }
        final int baseHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, baseHeightDp, header.getResources().getDisplayMetrics()
        );
        header.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                top = insets.getInsets(WindowInsets.Type.statusBars()).top;
            } else {
                top = insets.getSystemWindowInsetTop();
            }
            int height = baseHeight + top;
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null && params.height != height) {
                params.height = height;
                v.setLayoutParams(params);
            }
            if (v.getPaddingTop() != top) {
                v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            }
            if (contentBelow != null
                    && contentBelow.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams =
                        (ViewGroup.MarginLayoutParams) contentBelow.getLayoutParams();
                if (marginParams.topMargin != height) {
                    marginParams.topMargin = height;
                    contentBelow.setLayoutParams(marginParams);
                }
            }
            return insets;
        });
        header.requestApplyInsets();
    }

    /**
     * 拼接 url 请求参数
     *
     * @param params 参数列表
     * @return 拼接后的url
     */
    public static String getParamUrl(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        boolean start = true;
        StringBuilder urlBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                urlBuilder.append(start ? "" : "&")
                        .append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                if (start) {
                    start = false;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
        return urlBuilder.toString();
    }
}
