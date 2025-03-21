package com.example.anzuo;

// 蓝牙通信协议类
public class BluetoothProtocol {
    // 协议固定值
    private static final byte HEADER = 0x55;    // 帧头
    private static final byte FOOTER = (byte) 0xAA;  // 帧尾
    private static final byte PROTOCOL_VERSION = 0x01;  // 协议版本

    // 生成完整的8字节通信数据
    public static byte[] generateCommand(byte mainCommand, byte data1, byte data2, byte data3) {
        byte[] command = new byte[8];
        
        command[0] = HEADER;           // 帧头 0x55
        command[1] = PROTOCOL_VERSION; // 协议版本 0x01
        command[2] = mainCommand;      // 主指令
        command[3] = data1;           // 数据位一
        command[4] = data2;           // 数据位二
        command[5] = data3;           // 数据位三
        command[6] = calculateChecksum(mainCommand, data1, data2, data3); // 校验位
        command[7] = FOOTER;          // 帧尾 0xAA

        return command;
    }

    // 将16位整数拆分为两个字节
    public static byte[] splitInt16ToBytes(int value) {
        byte[] result = new byte[2];
        result[0] = (byte) ((value >> 8) & 0xFF);  // 高8位
        result[1] = (byte) (value & 0xFF);         // 低8位
        return result;
    }

    // 计算校验和：(主指令 + 数据位一 + 数据位二 + 数据位三) % 256
    private static byte calculateChecksum(byte mainCommand, byte data1, byte data2, byte data3) {
        int sum = (mainCommand & 0xFF) + (data1 & 0xFF) + (data2 & 0xFF) + (data3 & 0xFF);
        return (byte) (sum % 256);
    }

    // 验证接收到的数据是否符合协议格式
    public static boolean validateReceivedData(byte[] data) {
        if (data == null || data.length != 8) {
            return false;
        }

        // 检查帧头和帧尾
        if (data[0] != HEADER || data[7] != FOOTER) {
            return false;
        }

        // 检查协议版本
        if (data[1] != PROTOCOL_VERSION) {
            return false;
        }

        // 验证校验和
        byte calculatedChecksum = calculateChecksum(data[2], data[3], data[4], data[5]);
        return calculatedChecksum == data[6];
    }
} 