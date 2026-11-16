package org.server.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IInterface;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
public final class PowerManager {
    private final IInterface manager;
    private final Method isScreenOnMethod;
    private final Method userActivityMethod;

    public PowerManager(IInterface manager) {
        this.manager = manager;
        try {
            @SuppressLint("ObsoleteSdkInt") // we may lower minSdkVersion in the future
                    String methodName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH ? "isInteractive" : "isScreenOn";
            isScreenOnMethod = manager.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        Method ua;
        try {
            ua = manager.getClass().getMethod("userActivity", long.class, boolean.class);
        } catch (NoSuchMethodException e) {
            ua = null;
        }
        userActivityMethod = ua;
    }

    public boolean isScreenOn() {
        try {
            return (Boolean) isScreenOnMethod.invoke(manager);
        } catch (InvocationTargetException | IllegalAccessException e) {
            // throw new AssertionError(e);
            return false;
        }
    }

    /**
     * Simulate user activity to keep the device awake (scrcpy --keep-active).
     * Returns false when the hidden API is unavailable on this device.
     */
    public boolean userActivity(long uptimeMillis, boolean noChangeLights) {
        if (userActivityMethod == null) {
            return false;
        }
        try {
            userActivityMethod.invoke(manager, uptimeMillis, noChangeLights);
            return true;
        } catch (InvocationTargetException | IllegalAccessException e) {
            return false;
        }
    }
}
