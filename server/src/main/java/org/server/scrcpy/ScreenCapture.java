package org.server.scrcpy;

import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import org.server.scrcpy.wrappers.ServiceManager;
import org.server.scrcpy.wrappers.SurfaceControl;


public class ScreenCapture {

    private final Device device;
    private IBinder display;
    private VirtualDisplay virtualDisplay;

    public ScreenCapture(Device device) {
        this.device = device;
    }

    public void start(Surface surface) {
        Rect deviceRect = device.getScreenInfo().getDeviceSize().toRect();
        Rect videoRect = device.getScreenInfo().getVideoSize().toRect();
        int layerStack = device.getLayerStack();

        if (display != null) {
            SurfaceControl.destroyDisplay(display);
            display = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        // Come scrcpy ufficiale: su Android 14+ createDisplay non esiste più.
        // Prima l'API nascosta DisplayManager.createVirtualDisplay(displayId),
        // poi SurfaceControl, infine AUTO_MIRROR.
        try {
            virtualDisplay = ServiceManager.getDisplayManager()
                    .createVirtualDisplay("scrcpy", videoRect.width(), videoRect.height(),
                            device.getDisplayId(), surface);
            Ln.i("Display: using DisplayManager API (mirror displayId=" + device.getDisplayId()
                    + " " + videoRect.width() + "x" + videoRect.height() + ")");
            return;
        } catch (Exception displayManagerException) {
            Ln.e("DisplayManager capture failed, trying SurfaceControl", displayManagerException);
            try {
                display = createDisplay();
                setDisplaySurface(display, surface, deviceRect, videoRect, layerStack);
                Ln.i("Display: using SurfaceControl API (layerStack=" + layerStack + ")");
                return;
            } catch (Exception surfaceControlException) {
                Ln.e("SurfaceControl capture failed, trying AUTO_MIRROR VirtualDisplay", surfaceControlException);
                try {
                    virtualDisplay = ServiceManager.getDisplayManager()
                            .createMirrorVirtualDisplay("scrcpy", videoRect.width(), videoRect.height(),
                                    device.getDensityDpi(), surface);
                    Ln.i("Display: using AUTO_MIRROR VirtualDisplay dpi=" + device.getDensityDpi());
                } catch (Exception mirrorException) {
                    Ln.e("AUTO_MIRROR failed", mirrorException);
                    throw new AssertionError("Could not create display");
                }
            }
        }
    }

    public void release() {
        device.setRotationListener(null);
        if (display != null) {
            SurfaceControl.destroyDisplay(display);
            display = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    private static IBinder createDisplay() throws Exception {
        // Since Android 12 (preview), secure displays could not be created with shell permissions anymore.
        // On Android 12 preview, SDK_INT is still R (not S), but CODENAME is "S".
        boolean secure = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S".equals(
                Build.VERSION.CODENAME));
        return SurfaceControl.createDisplay("scrcpy", secure);
    }

    private static void setDisplaySurface(IBinder display, Surface surface, Rect deviceRect, Rect displayRect, int layerStack) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, 0, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, layerStack);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }
}
