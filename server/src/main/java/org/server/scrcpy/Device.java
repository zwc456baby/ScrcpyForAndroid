package org.server.scrcpy;

import org.server.scrcpy.device.Point;
import android.os.Build;
import android.os.RemoteException;
import android.view.IRotationWatcher;
import android.view.InputEvent;

import org.server.scrcpy.wrappers.ServiceManager;

public final class Device {

    private static final int DISPLAY_ID_DEFAULT = 0;

    // private final ServiceManager serviceManager = new ServiceManager();
    private ScreenInfo screenInfo;
    private RotationListener rotationListener;
    private int displayId = DISPLAY_ID_DEFAULT;
    private int layerStack;
    private int densityDpi = 160;

    public Device(Options options) {
        screenInfo = computeScreenInfo(options.getMaxSize());
        registerRotationWatcher(new IRotationWatcher.Stub() {
            @Override
            public void onRotationChanged(int rotation) throws RemoteException {
                synchronized (Device.this) {
                    screenInfo = screenInfo.withRotation(rotation);

                    // notify
                    if (rotationListener != null) {
                        rotationListener.onRotationChanged(rotation);
                    }
                }
            }
        });
    }

    public static String getDeviceName() {
        return Build.MODEL;
    }

    public int getDisplayId() {
        return displayId;
    }

    public int getLayerStack() {
        return layerStack;
    }

    public int getDensityDpi() {
        return densityDpi;
    }

    public synchronized ScreenInfo getScreenInfo() {
        return screenInfo;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private ScreenInfo computeScreenInfo(int maxSize) {
        // Compute the video size and the padding of the content inside this video.
        // Principle:
        // - scale down the great side of the screen to maxSize (if necessary);
        // - scale down the other side so that the aspect ratio is preserved;
        // - round this value to the nearest multiple of 8 (H.264 only accepts multiples of 8)
        DisplayInfo displayInfo;
        try {
            displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(DISPLAY_ID_DEFAULT);
        } catch (Exception e) {
            displayInfo = ServiceManager.getDisplayManager().getDisplayInfo();
        }
        displayId = displayInfo.getDisplayId();
        layerStack = displayInfo.getLayerStack();
        densityDpi = displayInfo.getDensityDpi();
        boolean rotated = (displayInfo.getRotation() & 1) != 0;
        Size deviceSize = displayInfo.getSize();
        Size videoSize = deviceSize.scaleTo(maxSize);
        Ln.i("Screen size device=" + deviceSize.getWidth() + "x" + deviceSize.getHeight()
                + " video=" + videoSize.getWidth() + "x" + videoSize.getHeight());
        return new ScreenInfo(deviceSize, videoSize, rotated);
    }

    public Point getPhysicalPoint(Position position) {
        @SuppressWarnings("checkstyle:HiddenField") // it hides the field on purpose, to read it with a lock
                ScreenInfo screenInfo = getScreenInfo(); // read with synchronization
        Size videoSize = screenInfo.getVideoSize();
        Size clientVideoSize = position.getScreenSize();
        if (!videoSize.equals(clientVideoSize)) {
            // The client sends a click relative to a video with wrong dimensions,
            // the device may have been rotated since the event was generated, so ignore the event
            return null;
        }
        Size deviceSize = screenInfo.getDeviceSize();
        Point point = position.getPoint();
        int scaledX = point.getX() * deviceSize.getWidth() / videoSize.getWidth();
        int scaledY = point.getY() * deviceSize.getHeight() / videoSize.getHeight();
        return new Point(scaledX, scaledY);
    }

    public boolean injectInputEvent(InputEvent inputEvent, int mode) {
        if (!org.server.scrcpy.wrappers.InputManager.setDisplayId(inputEvent, displayId)) {
            Ln.w("Could not set displayId=" + displayId + " on input event");
        }
        boolean ok = ServiceManager.getInputManager().injectInputEvent(inputEvent, mode);
        if (!ok) {
            Ln.w("injectInputEvent failed displayId=" + displayId);
        }
        return ok;
    }

    public boolean isScreenOn() {
        return ServiceManager.getPowerManager().isScreenOn();
    }

    public void registerRotationWatcher(IRotationWatcher rotationWatcher) {
        ServiceManager.getWindowManager().registerRotationWatcher(rotationWatcher);
    }

    public synchronized void setRotationListener(RotationListener rotationListener) {
        this.rotationListener = rotationListener;
    }

    public Point NewgetPhysicalPoint(Point point) {
        // Il client mappa già i tocchi in pixel del device remoto.
        return point;
    }


    public interface RotationListener {
        void onRotationChanged(int rotation);
    }

}
