package de.esudo.zusilocation;

import org.w3c.dom.Attr;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Node {
    private int mId;
    private List<Node> mNodes;
    private List<Attribute> mAttributes;

    public Node(int id) {
        this.mId = id;
        mNodes = new ArrayList<>();
        mAttributes = new ArrayList<>();
    };

    public int getId() {
        return mId;
    }

    public List<Node> getNodes() {
        return mNodes;
    }

    public void addNode(Node node) {
        mNodes.add(node);
    }

    public List<Attribute> getAttributes() {
        return mAttributes;
    }

    public void addAttribute(Attribute attribute) {
        mAttributes.add(attribute);
    }

    public Node findNodeById(int id) {
        for(Node n : mNodes) {
            if(n.getId() == id) return n;
        }

        return null;
    }

    public Attribute findAttributeById(int id) {
        for(Attribute a : mAttributes) {
            if(a.getId() == id) return a;
        }

        return null;
    }

    public String toString(int layer) {
        StringBuilder builder = new StringBuilder();
        byte[] nodeBegin = { 0x00, 0x00, 0x00, 0x00 };
        byte[] nodeEnd = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

        ZusiReader.appendWhitespaces(builder, layer * 2);
        builder.append(ZusiReader.bytesToString(nodeBegin));
        builder.append("\n");

        ZusiReader.appendWhitespaces(builder, layer * 2);
        ByteBuffer idBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        idBuffer.putShort((short) mId);
        byte[] id = idBuffer.array();
        builder.append(ZusiReader.bytesToString(id));
        builder.append(" (Node ID: ").append(mId).append(")");
        builder.append("\n");

        for(Node n : mNodes) {
            builder.append(n.toString(layer + 1));
        }

        for(Attribute a : mAttributes) {
            builder.append(a.toString(layer + 1));
        }

        ZusiReader.appendWhitespaces(builder, layer * 2);
        builder.append(ZusiReader.bytesToString(nodeEnd));
        builder.append("\n");

        return builder.toString();
    }

    @Override
    public String toString() {
        return toString(0);
    }
}
