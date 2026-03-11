package org.server.scrcpy.model;


public class ControlPacket extends MediaPacket<ControlPacket> {

    public byte[] data;

    public ControlPacket() {
    }

    public ControlPacket(Type type, byte[] data) {
        this.type = type;
        this.data = data;
    }


    public static ControlPacket readHead(byte[] values) {
        ControlPacket controlPacket = new ControlPacket();

        // should be a type value - 1 byte
        byte typeValue = values[0];

        controlPacket.type = Type.getType(typeValue);

        controlPacket.data = null;

        return controlPacket;
    }

    public int headLength() {
        return 1;
    }

    @Override
    public ControlPacket fromArray(byte[] values) {
        ControlPacket controlPacket = new ControlPacket();

        // should be a type value - 1 byte
        byte typeValue = values[0];
        controlPacket.type = Type.getType(typeValue);

        int dataLength = values.length - controlPacket.headLength();
        byte[] data = new byte[dataLength];
        System.arraycopy(values, controlPacket.headLength(), data, 0, dataLength);
        controlPacket.data = data;

        return controlPacket;
    }

    // create byte array
    public static byte[] toArray(Type type, byte[] data) {

        // should be 4 bytes for packet size
        byte[] bytes = ByteUtils.intToBytes(1 + data.length);

        int packetSize = 5 + data.length; // 4 - inner packet size 1 - type + data.length
        byte[] values = new byte[packetSize];

        System.arraycopy(bytes, 0, values, 0, 4);

        // set type value
        values[4] = type.getType();
        // set data array
        System.arraycopy(data, 0, values, 5, data.length);
        return values;
    }
}
