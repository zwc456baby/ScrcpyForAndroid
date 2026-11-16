package org.server.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.Surface;


@SuppressLint("PrivateApi")
public final class SurfaceControl {

    private static final Class<?> CLASS;

    static {
        try {
            CLASS = Class.forName("android.view.SurfaceControl");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private SurfaceControl() {
        // only static methods
    }

    public static void openTransaction() {
        try {
            CLASS.getMethod("openTransaction").invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void closeTransaction() {
        try {
            CLASS.getMethod("closeTransaction").invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
        try {
            CLASS.getMethod("setDisplayProjection", IBinder.class, int.class, Rect.class, Rect.class)
                    .invoke(null, displayToken, orientation, layerStackRect, displayRect);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
        try {
            CLASS.getMethod("setDisplayLayerStack", IBinder.class, int.class).invoke(null, displayToken, layerStack);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        try {
            CLASS.getMethod("setDisplaySurface", IBinder.class, Surface.class).invoke(null, displayToken, surface);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static IBinder createDisplay(String name, boolean secure) throws Exception {
        return (IBinder) CLASS.getMethod("createDisplay", String.class, boolean.class).invoke(null, name, secure);
    }

    public static void destroyDisplay(IBinder displayToken) {
        try {
            CLASS.getMethod("destroyDisplay", IBinder.class).invoke(null, displayToken);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    // Display power modes (android.view.SurfaceControl, hidden API)
    public static final int POWER_MODE_OFF = 0;
    public static final int POWER_MODE_NORMAL = 2;

    /**
     * Get the physical display ids (API 26+). Returns null when unavailable.
     */
    public static long[] getPhysicalDisplayIds() {
        try {
            return (long[]) CLASS.getMethod("getPhysicalDisplayIds").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the token of a physical display (API 26+). Returns null when unavailable.
     */
    public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
        try {
            return (IBinder) CLASS.getMethod("getPhysicalDisplayToken", long.class).invoke(null, physicalDisplayId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set the power mode of a display (scrcpy -S / --turn-screen-off).
     * Returns false when the hidden API is unavailable on this device.
     */
    public static boolean setDisplayPowerMode(IBinder displayToken, int mode) {
        try {
            CLASS.getMethod("setDisplayPowerMode", IBinder.class, int.class).invoke(null, displayToken, mode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
