package de.esudo.zusilocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ZusiReader {
    private final InputStream mInputStream;

    public ZusiReader(InputStream is) {
        mInputStream = is;
    }

    public String readAsString() throws IOException {
        Node root = read();
        if(root == null) return null;

        return "\n" + root.toString();
    }

    public Node read() throws IOException {
        byte[] bytes = readBytes(4);
        if(bytes == null) return null;
        if(bytes.length < 4) throw new IOException("Unexpected end of Zusi TCP server stream");

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int length = Integer.reverseBytes(buffer.getInt());

        if(length != 0) throw new IOException("Unexpected start of Zusi TCP transmission");

        return readNode();
    }

    private Node readNode() throws IOException {
        byte[] id = readBytes(2);
        if(id == null || id.length < 2) throw new IOException("Unexpected end of Zusi TCP server stream");
        ByteBuffer idBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        idBuffer.put(id);
        idBuffer.position(0);
        Node node = new Node(idBuffer.getInt());

        while(true) {
            byte[] bytes = readBytes(4);
            if(bytes == null || bytes.length < 4) throw new IOException("Unexpected end of Zusi TCP server stream");

            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int length = Integer.reverseBytes(buffer.getInt());

            if(length == 0) {
                node.addNode(readNode());
            } else if(length == -1) {
                return node;
            } else {
                node.addAttribute(readAttribute(length));
            }
        }
    }

    private Attribute readAttribute(int len) throws IOException {
        byte[] idBytes = readBytes(2);
        if(idBytes == null || idBytes.length < 2) throw new IOException("Unexpected end of Zusi TCP server stream");
        ByteBuffer idBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        idBuffer.put(idBytes);
        idBuffer.position(0);
        int id = idBuffer.getInt();

        byte[] data = readBytes(len - 2);
        if(data == null || data.length < (len - 2)) throw new IOException("Unexpected end of Zusi TCP server stream");

        return new Attribute(id, data);
    }

    private byte[] readBytes(int len) throws IOException {
        byte[] buffer = new byte[len];
        int r = mInputStream.read(buffer);
        if(r == -1) return null;
        if(r < len) {
            byte[] bufferCut = new byte[r];
            for(int i = 0; i < r; i++) {
                bufferCut[i] = buffer[i];
            }
            return bufferCut;
        }
        return buffer;
    }

    public static String bytesToString(byte[] b) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < b.length; i++) {
            if(i > 0) builder.append(" ");
            builder.append(byteToString(b[i]));
        }
        return builder.toString();
    }

    private static String byteToString(byte b) {
        char[] hexChars = "0123456789ABCDEF".toCharArray();
        char c1 = hexChars[(b & 0xF0) >>> 4];
        char c2 = hexChars[b & 0x0F];
        return "0x" + c1 + c2;
    }

    public static void appendWhitespaces(StringBuilder builder, int count) {
        for(int i = 0; i < count; i++) {
            builder.append(" ");
        }
    }
}
