package de.esudo.zusilocation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Attribute {
    private int mId;
    private byte[] mData;

    public Attribute(int id, byte[] data) {
        this.mId = id;
        this.mData = data;
    }

    public int getId() {
        return mId;
    }

    public byte[] getData() {
        return mData;
    }

    public int length() {
        return 2 + mData.length;
    }

    public String toString(int layer) {
        StringBuilder builder = new StringBuilder();

        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(length());
        byte[] length = buffer.array();
        ZusiReader.appendWhitespaces(builder, layer * 2);
        builder.append(ZusiReader.bytesToString(length));
        builder.append(" (Length: ").append(length()).append(")");
        builder.append("\n");

        ZusiReader.appendWhitespaces(builder, layer * 2);
        ByteBuffer idBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        idBuffer.putShort((short) mId);
        byte[] id = idBuffer.array();
        builder.append(ZusiReader.bytesToString(id));
        builder.append(" (Attribute ID: ").append(mId).append(")");
        builder.append("\n");

        ZusiReader.appendWhitespaces(builder, layer * 2);
        builder.append(ZusiReader.bytesToString(mData));
        builder.append(" (Data as text: ");
        for(int i = 0; i < mData.length; i++) {
            builder.append((char) mData[i]);
        }
        builder.append(")");
        builder.append("\n");

        return builder.toString();
    }


}
