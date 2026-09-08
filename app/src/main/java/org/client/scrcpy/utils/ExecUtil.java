package org.client.scrcpy.utils;

import android.util.Log;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;


public class ExecUtil {

    public static String execCommend(String cmd) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        try {
            process = Runtime.getRuntime().exec("sh");
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
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd).directory(workDir);
            Map<String, String> envs = processBuilder.environment();
            for (String s : env.keySet()) {
                envs.put(s, env.get(s));
            }
            process = processBuilder.start();
            os = new DataOutputStream(process.getOutputStream());
            os.flush();
            os.close();
            os = null;
            StringBuilder successMsg = new StringBuilder();
            StringBuilder errorMsg = new StringBuilder();
            Thread outThread = drainAsync(process.getInputStream(), successMsg, false);
            Thread errThread = drainAsync(process.getErrorStream(), errorMsg, true);
            process.waitFor();
            outThread.join(2000);
            errThread.join(2000);
            return successMsg.toString();
        } catch (Exception e) {
            Log.i("TaskPrint", e.toString());
        } finally {
            try {
                if (os != null) {
                    os.close();
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

    private static Thread drainAsync(InputStream stream, StringBuilder sink, boolean logAll) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    synchronized (sink) {
                        if (!first) {
                            sink.append('\n');
                        }
                        first = false;
                        sink.append(line);
                    }
                    if (logAll || isServerLogLine(line)) {
                        Log.i("Scrcpy", line);
                    }
                }
            } catch (IOException ignored) {
            }
        }, "adb-drain");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static boolean isServerLogLine(String line) {
        return line.startsWith("INFO:")
                || line.startsWith("ERROR:")
                || line.startsWith("WARN:")
                || line.startsWith("DEBUG:")
                || line.contains("scrcpy")
                || line.contains("Display:")
                || line.contains("Encoder");
    }

}
