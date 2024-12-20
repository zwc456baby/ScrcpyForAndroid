package org.server.scrcpy;

public final class DisplayInfo {
    private int displayId;
    private final Size size;
    private final int rotation;
    private int layerStack;
    private int flags;

    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 0x00000001;

    public DisplayInfo(Size size, int rotation) {
        this.size = size;
        this.rotation = rotation;
    }

    public DisplayInfo(int displayId, Size size, int rotation, int layerStack, int flags) {
        this.displayId = displayId;
        this.size = size;
        this.rotation = rotation;
        this.layerStack = layerStack;
        this.flags = flags;
    }

    public int getDisplayId() {
        return displayId;
    }

    public Size getSize() {
        return size;
    }

    public int getRotation() {
        return rotation;
    }

    public int getLayerStack() {
        return layerStack;
    }

    public int getFlags() {
        return flags;
    }
}
