package org.client.scrcpy.utils;

import android.util.Log;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;


public class ExecUtil {

    public static String execCommend(String cmd, String[] env, File workDir) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        try {
            process = Runtime.getRuntime().exec("sh", env, workDir);
            os = new DataOutputStream(process.getOutputStream());
            os.write(cmd.getBytes());
            os.writeBytes("\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            int result = process.waitFor();
            // get command result
            StringBuilder successMsg = new StringBuilder();
            StringBuilder errorMsg = new StringBuilder();
            successResult = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
            return successMsg.toString();
        } catch (Exception e) {
            Log.i("TaskPrint", e.toString());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
        return "";
    }

    public static String adbCommend(String[] cmd, Map<String, String> env, File workDir) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd).directory(workDir);
            Map<String, String> envs = processBuilder.environment();
            for (String s : env.keySet()) {
                envs.put(s, env.get(s));
            }
            process = processBuilder.start();
            os = new DataOutputStream(process.getOutputStream());
            os.flush();
            int result = process.waitFor();
            // get command result
            StringBuilder successMsg = new StringBuilder();
            StringBuilder errorMsg = new StringBuilder();
            successResult = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            boolean successFirst = true;
            errorResult = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));
            boolean errorFirst = true;
            String s;
            while ((s = successResult.readLine()) != null) {
                if (!successFirst) {
                    successMsg.append("\n");
                } else {
                    successFirst = false;
                }
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                if (!errorFirst) {
                    errorMsg.append("\n");
                } else {
                    errorFirst = false;
                }
                errorMsg.append(s);
            }
            Log.i("Scrcpy", errorMsg.toString());
            return successMsg.toString();
        } catch (Exception e) {
            Log.i("TaskPrint", e.toString());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
        return "";
    }

}
