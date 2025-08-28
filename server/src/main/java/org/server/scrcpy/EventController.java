package org.server.scrcpy;

import android.os.Build;
import android.os.SystemClock;
import android.util.Pair;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.server.scrcpy.wrappers.InputManager;
import org.server.scrcpy.control.PointersState;
import org.server.scrcpy.control.Pointer;
import org.server.scrcpy.device.Point;

import java.io.IOException;


public class EventController {

    private final Device device;
    private final DroidConnection connection;
    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];
    private long lastMouseDown;

    private final PointersState pointersState = new PointersState();

    private float then;
    private boolean hit = false;
    private boolean proximity = false;

    public EventController(Device device, DroidConnection connection) {
        this.device = device;
        this.connection = connection;
        initPointers();
    }

    private void setPointerCoords(Point point) {
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = point.getX();
        coords.y = point.getY();
    }

    private void setScroll(int hScroll, int vScroll) {
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);
    }

    private void initPointers() {
        for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 0;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }

    public void control() throws IOException {
        // on start, turn screen on
        turnScreenOn();

        while (true) {
            //           handleEvent();
            int[] buffer = connection.NewreceiveControlEvent();
            if (buffer != null) {
                long now = SystemClock.uptimeMillis();
                if (buffer[2] == 0 && buffer[3] == 0) {
                    if (buffer[0] == 28) {
                        proximity = true;           // Proximity event
                    } else if (buffer[0] == 29) {
                        proximity = false;
                    } else {
                        injectKeycode(buffer[0]);
                    }
                } else {
                    int action = buffer[0];
                    if (action == MotionEvent.ACTION_UP && (!device.isScreenOn() || proximity)) {
                        if (hit) {
                            if (now - then < 250) {
                                then = 0;
                                hit = false;
                                injectKeycode(KeyEvent.KEYCODE_POWER);
                            } else {
                                then = now;
                            }
                        } else {
                            hit = true;
                            then = now;
                        }

                    } else {
//                        if (action == MotionEvent.ACTION_DOWN) {
//                            lastMouseDown = now;
//                        }
//                        int button = buffer[1];
//                        int X = buffer[2];
//                        int Y = buffer[3];
////                        new android.graphics.Point(point.getX(), point.getY())
//                        Point point = new Point(X, Y);
//                        Point newpoint = device.NewgetPhysicalPoint(point);
//                        setPointerCoords(newpoint);
//                        MotionEvent event = MotionEvent.obtain(lastMouseDown, now, action, 1, pointerProperties, pointerCoords, 0, button, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
//                        injectEvent(event);

                        // 为支持多点触控，新增 buffer[4] 这个字节
                        Point point = new Point(buffer[2], buffer[3]);
                        Point newpoint = device.NewgetPhysicalPoint(point);
                        injectTouch(action, buffer[4], newpoint, buffer[1]);
                    }
                }
            }
        }
    }


    /**
     * 为尽快支持多点触控，暂时去除原版 scrcpy 中的鼠标按下功能，可能导致鼠标控制出现问题
     * TODO： 后续需要参照原版 scrcpy 实现鼠标操作
     * 详情参考：scrcpy/server/src/main/java/com/genymobile/scrcpy/control/Controller.java
     */
    private boolean injectTouch(int action, long pointerId, Point point, int button) {
        long now = SystemClock.uptimeMillis();

        int pointerIndex = pointersState.getPointerIndex(pointerId);
        if (pointerIndex == -1) {
            Ln.w("Too many pointers for touch event");
            return false;
        }
        Pointer pointer = pointersState.get(pointerIndex);
        pointer.setPoint(point);
//        pointer.setPressure(pressure);
        pointer.setPressure(1.0f);

        int source;
//        boolean activeSecondaryButtons = ((actionButton | buttons) & ~MotionEvent.BUTTON_PRIMARY) != 0;
//        if (pointerId == POINTER_ID_MOUSE && (action == MotionEvent.ACTION_HOVER_MOVE || activeSecondaryButtons)) {
//            // real mouse event, or event incompatible with a finger
//            pointerProperties[pointerIndex].toolType = MotionEvent.TOOL_TYPE_MOUSE;
//            source = InputDevice.SOURCE_MOUSE;
//            pointer.setUp(buttons == 0);
//        } else {
//            // POINTER_ID_GENERIC_FINGER, POINTER_ID_VIRTUAL_FINGER or real touch from device
//            pointerProperties[pointerIndex].toolType = MotionEvent.TOOL_TYPE_FINGER;
//            source = InputDevice.SOURCE_TOUCHSCREEN;
//            // Buttons must not be set for touch events
//            buttons = 0;
//            pointer.setUp(action == MotionEvent.ACTION_UP);
//        }

        pointerProperties[pointerIndex].toolType = MotionEvent.TOOL_TYPE_FINGER;
        source = InputDevice.SOURCE_TOUCHSCREEN;

        boolean pointerUp = action == MotionEvent.ACTION_UP;
        int actionType = action & MotionEvent.ACTION_MASK;
        if (actionType == MotionEvent.ACTION_POINTER_UP) {
            pointerUp = true;
        }
        pointer.setUp(pointerUp);

        int pointerCount = pointersState.update(pointerProperties, pointerCoords);
        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastMouseDown = now;
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            // 与原版 scrcpy 相比，Android 传输的触控信息已经包含 ACTION_POINTER_UP ，此处需要新增兼容，否则多点触控会出现异常
            if (action == MotionEvent.ACTION_UP || actionType == MotionEvent.ACTION_POINTER_UP) {
                action = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                // Ln.w("按钮 Pointer 抬起");
            } else if (action == MotionEvent.ACTION_DOWN || actionType == MotionEvent.ACTION_POINTER_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                // Ln.w("按钮 Pointer 按下");
            }
        }
        // Ln.w("按钮事件，" + action + " ,lastMouseDown: " + lastMouseDown + " , now: " + now + " , pointerId: " + pointerId + " pointerCount: " + pointerCount);

        /* If the input device is a mouse (on API >= 23):
         *   - the first button pressed must first generate ACTION_DOWN;
         *   - all button pressed (including the first one) must generate ACTION_BUTTON_PRESS;
         *   - all button released (including the last one) must generate ACTION_BUTTON_RELEASE;
         *   - the last button released must in addition generate ACTION_UP.
         *
         * Otherwise, Chrome does not work properly: <https://github.com/Genymobile/scrcpy/issues/3635>
         */
//        if (Build.VERSION.SDK_INT >= AndroidVersions.API_23_ANDROID_6_0 && source == InputDevice.SOURCE_MOUSE) {
//            if (action == MotionEvent.ACTION_DOWN) {
//                if (actionButton == buttons) {
//                    // First button pressed: ACTION_DOWN
//                    MotionEvent downEvent = MotionEvent.obtain(lastTouchDown, now, MotionEvent.ACTION_DOWN, pointerCount, pointerProperties,
//                            pointerCoords, 0, buttons, 1f, 1f, DEFAULT_DEVICE_ID, 0, source, 0);
//                    if (!Device.injectEvent(downEvent, targetDisplayId, Device.INJECT_MODE_ASYNC)) {
//                        return false;
//                    }
//                }
//
//                // Any button pressed: ACTION_BUTTON_PRESS
//                MotionEvent pressEvent = MotionEvent.obtain(lastTouchDown, now, MotionEvent.ACTION_BUTTON_PRESS, pointerCount, pointerProperties,
//                        pointerCoords, 0, buttons, 1f, 1f, DEFAULT_DEVICE_ID, 0, source, 0);
//                if (!InputManager.setActionButton(pressEvent, actionButton)) {
//                    return false;
//                }
//                if (!Device.injectEvent(pressEvent, targetDisplayId, Device.INJECT_MODE_ASYNC)) {
//                    return false;
//                }
//
//                return true;
//            }
//
//            if (action == MotionEvent.ACTION_UP) {
//                // Any button released: ACTION_BUTTON_RELEASE
//                MotionEvent releaseEvent = MotionEvent.obtain(lastTouchDown, now, MotionEvent.ACTION_BUTTON_RELEASE, pointerCount, pointerProperties,
//                        pointerCoords, 0, buttons, 1f, 1f, DEFAULT_DEVICE_ID, 0, source, 0);
//                if (!InputManager.setActionButton(releaseEvent, actionButton)) {
//                    return false;
//                }
//                if (!Device.injectEvent(releaseEvent, targetDisplayId, Device.INJECT_MODE_ASYNC)) {
//                    return false;
//                }
//
//                if (buttons == 0) {
//                    // Last button released: ACTION_UP
//                    MotionEvent upEvent = MotionEvent.obtain(lastTouchDown, now, MotionEvent.ACTION_UP, pointerCount, pointerProperties,
//                            pointerCoords, 0, buttons, 1f, 1f, DEFAULT_DEVICE_ID, 0, source, 0);
//                    if (!Device.injectEvent(upEvent, targetDisplayId, Device.INJECT_MODE_ASYNC)) {
//                        return false;
//                    }
//                }
//
//                return true;
//            }
//        }

        MotionEvent event = MotionEvent.obtain(lastMouseDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, button, 1f, 1f,
                0, 0, source, 0);

        // return Device.injectEvent(event, targetDisplayId, Device.INJECT_MODE_ASYNC);
        return injectEvent(event);
    }

    private boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectEvent(event);
    }

    private boolean injectKeycode(int keyCode) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0)
                && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0);
    }

    private boolean injectEvent(InputEvent event) {
        return device.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private boolean turnScreenOn() {
        return device.isScreenOn() || injectKeycode(KeyEvent.KEYCODE_POWER);
    }

}
