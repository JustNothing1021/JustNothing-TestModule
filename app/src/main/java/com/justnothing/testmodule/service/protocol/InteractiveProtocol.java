package com.justnothing.testmodule.service.protocol;

import com.justnothing.testmodule.utils.functions.Logger;
import java.io.*;
import java.nio.ByteBuffer;

public class InteractiveProtocol {

    public static class ProtocolLogger extends Logger {
        @Override
        public String getTag() {
            return "InteractiveProtocol";
        }
    }

    public static final ProtocolLogger logger = new ProtocolLogger();

    // 协议常量
    public static final byte[] START_MARKER = {0x00, 0x11, 0x45, 0x14};
    public static final byte[] END_MARKER = {0x01, (byte)0x91, (byte)0x98, 0x10};

    // 消息类型
    public static final byte TYPE_CLIENT_COMMAND = 0x01;
    public static final byte TYPE_SERVER_OUTPUT = 0x02;
    public static final byte TYPE_SERVER_ERROR = 0x03;
    public static final byte TYPE_SERVER_INPUT_REQUEST = 0x04;
    public static final byte TYPE_INPUT_RESPONSE = 0x05;
    public static final byte TYPE_SERVER_PING = 0x06;
    public static final byte TYPE_CLIENT_PING = 0x07;
    public static final byte TYPE_SERVER_PONG = 0x08;
    public static final byte TYPE_CLIENT_PONG = 0x09;
    public static final byte TYPE_INPUT_PING = 0x10;
    public static final byte TYPE_INPUT_PONG = 0x11;
    public static final byte TYPE_COMMAND_END = 0x12;

    public static String getMessageTypeName(byte type) {
        return switch (type) {
            case TYPE_CLIENT_COMMAND -> "CLIENT_COMMAND";
            case TYPE_SERVER_OUTPUT -> "SERVER_OUTPUT";
            case TYPE_SERVER_ERROR -> "SERVER_ERROR";
            case TYPE_SERVER_INPUT_REQUEST -> "SERVER_INPUT_REQUEST";
            case TYPE_INPUT_RESPONSE -> "INPUT_RESPONSE";
            case TYPE_SERVER_PING -> "SERVER_PING";
            case TYPE_CLIENT_PING -> "CLIENT_PING";
            case TYPE_SERVER_PONG -> "SERVER_PONG";
            case TYPE_CLIENT_PONG -> "CLIENT_PONG";
            case TYPE_INPUT_PING -> "INPUT_PING";
            case TYPE_INPUT_PONG -> "INPUT_PONG";
            case TYPE_COMMAND_END -> "COMMAND_END";
            default -> "UNKNOWN(" + type + ")";
        };
    }

    /**
     * 编码消息包
     */
    public static byte[] encodeMessage(byte type, byte[] data) {
        // 数据长度（不包括起始、类型、长度、结束标记）
        int dataLength = data != null ? data.length : 0;

        // 总包长 = 起始标记(4) + 类型(1) + 长度(4) + 数据 + 结束标记(4)
        int totalLength = 13 + dataLength;

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);

        // 写入起始标记
        buffer.put(START_MARKER);

        // 写入消息类型
        buffer.put(type);

        // 写入数据长度（大端序）
        buffer.putInt(dataLength);

        // 写入数据
        if (data != null && dataLength > 0) {
            buffer.put(data);
        }

        // 写入结束标记
        buffer.put(END_MARKER);

        return buffer.array();
    }

    /**
     * 解码消息包
     * @return [消息类型, 数据] 或 null（如果不是有效包）
     */
    public static Object[] decodeMessage(byte[] packet) {
        if (packet.length < 13) {
            return null; // 包太短
        }

        // 检查起始标记
        for (int i = 0; i < 4; i++) {
            if (packet[i] != START_MARKER[i]) {
                return null; // 起始标记不匹配
            }
        }

        // 获取消息类型
        byte type = packet[4];

        // 获取数据长度
        int dataLength = ByteBuffer.wrap(packet, 5, 4).getInt();

        // 检查包长度
        int expectedLength = 13 + dataLength;
        if (packet.length != expectedLength) {
            return null; // 包长度不匹配
        }

        // 检查结束标记
        int endMarkerStart = 9 + dataLength;
        for (int i = 0; i < 4; i++) {
            if (packet[endMarkerStart + i] != END_MARKER[i]) {
                return null; // 结束标记不匹配
            }
        }

        // 提取数据
        byte[] data = null;
        if (dataLength > 0) {
            data = new byte[dataLength];
            System.arraycopy(packet, 9, data, 0, dataLength);
        }

        return new Object[]{type, data};
    }

    /**
     * 读取一个完整的消息包
     */
    public static Object[] readMessage(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("InputStream不能为null");
        }

        byte[] header = new byte[9];
        int bytesRead = 0;

        while (bytesRead < 9) {
            int n = input.read(header, bytesRead, 9 - bytesRead);
            if (n == -1) {
                logger.warn("读取包头时流结束");
                return null;
            }
            bytesRead += n;
        }

        for (int i = 0; i < 4; i++) {
            if (header[i] != START_MARKER[i]) {
                logger.error("无效的起始标记: " + bytesToHex(header, 0, 4));
                throw new IOException("Invalid packet start marker");
            }
        }

        byte type = header[4];
        int dataLength = ByteBuffer.wrap(header, 5, 4).getInt();

        if (dataLength < 0 || dataLength > 1024 * 1024) {
            logger.error("无效的数据长度: " + dataLength);
            throw new IOException("Invalid data length: " + dataLength);
        }

        byte[] packet = new byte[13 + dataLength];
        System.arraycopy(header, 0, packet, 0, 9);

        if (dataLength > 0) {
            bytesRead = 0;
            while (bytesRead < dataLength) {
                int n = input.read(packet, 9 + bytesRead, dataLength - bytesRead);
                if (n == -1) {
                    logger.warn("读取数据时流结束，已读取: " + bytesRead + "/" + dataLength);
                    return null;
                }
                bytesRead += n;
            }
        }

        bytesRead = 0;
        while (bytesRead < 4) {
            int n = input.read(packet, 9 + dataLength + bytesRead, 4 - bytesRead);
            if (n == -1) {
                logger.warn("读取结束标记时流结束");
                return null;
            }
            bytesRead += n;
        }

        int endMarkerStart = 9 + dataLength;
        for (int i = 0; i < 4; i++) {
            if (packet[endMarkerStart + i] != END_MARKER[i]) {
                logger.error("无效的结束标记: " + bytesToHex(packet, endMarkerStart, 4));
                throw new IOException("Invalid packet end marker");
            }
        }

        byte[] data = null;
        if (dataLength > 0) {
            data = new byte[dataLength];
            System.arraycopy(packet, 9, data, 0, dataLength);
        }

        return new Object[]{type, data};

    }

    private static String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    /**
     * 写入消息包（同步）
     */
    public static synchronized void writeMessage(OutputStream output, byte type, byte[] data) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("OutputStream不能为null");
        }

        try {
            byte[] packet = encodeMessage(type, data);
            output.write(packet);
            output.flush();
        } catch (IOException e) {
            logger.error("写入消息失败: 类型=" + getMessageTypeName(type), e);
            throw e;
        }
    }
}