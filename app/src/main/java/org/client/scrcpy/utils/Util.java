package org.client.scrcpy.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.TypedValue;

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
