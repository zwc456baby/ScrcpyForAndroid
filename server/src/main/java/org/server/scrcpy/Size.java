package org.server.scrcpy;

import android.graphics.Rect;


import java.util.Objects;

public final class Size {
    private final int width;
    private final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Size rotate() {
        return new Size(height, width);
    }

    /**
     * Scala per entrare in maxSize (lato maggiore) conservando l'aspect, poi arrotonda a multiplo di 8.
     * maxSize &lt;= 0 = risoluzione nativa del device.
     */
    public Size scaleTo(int maxSize) {
        int w = width;
        int h = height;
        if (maxSize > 0) {
            if (w >= h) {
                if (w > maxSize) {
                    h = (int) ((long) h * maxSize / w);
                    w = maxSize;
                }
            } else if (h > maxSize) {
                w = (int) ((long) w * maxSize / h);
                h = maxSize;
            }
        }
        w = round8(w);
        h = round8(h);
        if (w < 8) {
            w = 8;
        }
        if (h < 8) {
            h = 8;
        }
        return new Size(w, h);
    }

    private static int round8(int value) {
        return (value + 4) & ~7;
    }

    public Rect toRect() {
        return new Rect(0, 0, width, height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Size size = (Size) o;
        return width == size.width
                && height == size.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "Size{"
                + "width=" + width
                + ", height=" + height
                + '}';
    }
}
