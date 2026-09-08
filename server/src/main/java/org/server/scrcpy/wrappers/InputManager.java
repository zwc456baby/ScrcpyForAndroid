package org.server.scrcpy.wrappers;

import android.os.IInterface;
import android.view.InputEvent;

import org.server.scrcpy.Ln;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class InputManager {

    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;

    private final IInterface manager;
    private final Method injectInputEventMethod;

    public InputManager(IInterface manager) {
        this.manager = manager;
        try {
            injectInputEventMethod = manager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public static boolean setDisplayId(InputEvent event, int displayId) {
        try {
            Method method = InputEvent.class.getMethod("setDisplayId", int.class);
            method.invoke(event, displayId);
            return true;
        } catch (NoSuchMethodException e) {
            return true;
        } catch (Exception e) {
            Ln.e("Could not set displayId on input event", e);
            return false;
        }
    }

    public boolean injectInputEvent(InputEvent inputEvent, int mode) {
        try {
            return (Boolean) injectInputEventMethod.invoke(manager, inputEvent, mode);
        } catch (InvocationTargetException e) {
            Ln.e("injectInputEvent failed", e.getCause() != null ? e.getCause() : e);
            return false;
        } catch (IllegalAccessException e) {
            Ln.e("injectInputEvent failed", e);
            return false;
        }
    }
}
