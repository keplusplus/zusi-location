package de.esudo.zusilocation;

import android.renderscript.ScriptGroup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ZusiNodeReaderOldToDelete {
    public static String read(InputStream is) throws IOException {
        StringBuilder builder = new StringBuilder();

        while(true) {
            byte[] bytes = new byte[4];
            int i = is.read(bytes);
            if(i != 4) {
                return null;
            }
            for(int ii = 0; ii < bytes.length; ii++) {
                builder.append(byteToString(bytes[ii]));
            }
            builder.append("\n");

            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int len = Integer.reverseBytes(buffer.getInt());

            if(len == 0) {
                byte[] id = new byte[2];
                int r = is.read(id);
                builder.append(byteToString(id[0])).append(byteToString(id[1]));
                builder.append("\n");
                return builder.append(read(is)).toString();
            } else if(len == -1) {
                return builder.toString();
            } else {
                readAttribute(is, builder, len);
            }
            builder.append("\n");
        }
    }

    public static void readAttribute(InputStream is, StringBuilder builder, int len) throws IOException {
        byte[] buffer = new byte[len];
        int r = is.read(buffer);
        for(int i = 0; i < len; i++) {
            builder.append(byteToString(buffer[i]));
        }
    }

    private static String byteToString(byte b) {
        char[] hexChars = "0123456789ABCDEF".toCharArray();
        char c1 = hexChars[(b & 0xF0) >>> 4];
        char c2 = hexChars[b & 0x0F];
        return "0x" + c1 + c2 + " ";
    }
}
