package org.server.scrcpy.model;

/**
 * Created by Alexandr Golovach on 27.06.16.
 * https://www.github.com/alexmprog/VideoCodec
 */

public class CommandPacket extends MediaPacket<CommandPacket> {

    private final static int headLen = 2;  //1 type and 4 cmd type
    public byte[] data;

    public byte cmdType = CmdType.JSON_EXTRA_CMD.getFlag();

    public CommandPacket() {
    }

    public CommandPacket(Type type, byte[] data) {
        this.type = type;
        this.data = data;
    }


    public static CommandPacket readHead(byte[] values) {
        CommandPacket controlPacket = new CommandPacket();

        // should be a type value - 1 byte
        byte typeValue = values[0];

        controlPacket.type = Type.getType(typeValue);
        controlPacket.cmdType = values[1];

        controlPacket.data = null;

        return controlPacket;
    }

    // create byte array
    public static byte[] toArray(Type type, CmdType cmdType, byte[] data) {

        // should be 4 bytes for packet size
        byte[] bytes = ByteUtils.intToBytes(headLen + data.length);

        int packetSize = headLen + 4 + data.length; // 4: int length
        byte[] values = new byte[packetSize];

        System.arraycopy(bytes, 0, values, 0, 4);

        // set type value
        values[4] = type.getType();
        values[5] = cmdType.getFlag();
        // set data array
        System.arraycopy(data, 0, values, 6, data.length);
        return values;
    }


    @Override
    int headLength() {
        return headLen;
    }

    @Override
    public CommandPacket fromArray(byte[] values) {
        CommandPacket controlPacket = new CommandPacket();

        // should be a type value - 1 byte
        byte typeValue = values[0];
        controlPacket.type = Type.getType(typeValue);
        controlPacket.cmdType = values[1];

        int dataLength = values.length - controlPacket.headLength();
        byte[] data = new byte[dataLength];
        System.arraycopy(values, controlPacket.headLength(), data, 0, dataLength);
        controlPacket.data = data;

        return controlPacket;
    }

    public enum CmdType {

        JSON_EXTRA_CMD((byte) 0), VIDEO_NEW_KEY_FRAME((byte) 1);

        private byte type;

        CmdType(byte type) {
            this.type = type;
        }

        public static CmdType getFlag(byte value) {
            for (CmdType type : CmdType.values()) {
                if (type.getFlag() == value) {
                    return type;
                }
            }

            return null;
        }

        public byte getFlag() {
            return type;
        }
    }
}
