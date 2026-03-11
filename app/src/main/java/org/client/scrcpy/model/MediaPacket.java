package org.client.scrcpy.model;

/**
 * Created by Alexandr Golovach on 27.06.16.
 */
public abstract class MediaPacket<T> {

    public Type type;

    abstract int headLength();

    abstract public T fromArray(byte[] array);

    public enum Type {

        VIDEO((byte) 1), AUDIO((byte) 0), CONTROL((byte) 2), COMMAND((byte) 3);

        private byte type;

        Type(byte type) {
            this.type = type;
        }

        public static Type getType(byte value) {
            for (Type type : Type.values()) {
                if (type.getType() == value) {
                    return type;
                }
            }

            return null;
        }

        public byte getType() {
            return type;
        }
    }
}
