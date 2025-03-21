package com.example.anzuo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import android.Manifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 蓝牙数据管理类
 * 处理蓝牙连接及8字节数据包的收发
 * 数据包格式：
 * 55 01 02 xx xx xx CC AA
 * 其中：
 * - 55: 数据头(固定)
 * - 01: 版本号(固定)
 * - 02: 主指令(变化)
 * - xx xx xx: 三个数据位(变化)
 * - CC: 校验位 = (主指令+三个数据位)%256
 * - AA: 数据尾(固定)
 */
public class BluetoothDataManager {
    // 日志标签
    private static final String TAG = "user";
    
    // SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // 数据包常量
    private static final byte DATA_HEADER = (byte) 0x55;
    private static final byte PROTOCOL_VERSION = (byte) 0x01;
    private static final byte DATA_FOOTER = (byte) 0xAA;
    
    // 数据包长度
    private static final int PACKET_LENGTH = 8;
    
    // 上下文引用，用于权限检查
    private Context context;
    
    // 蓝牙连接相关对象
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    
    // 线程控制标志
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // 数据接收线程
    private Thread receiveThread;
    
    // 接口：数据接收回调
    public interface OnDataReceivedListener {
        void onDataReceived(byte command, byte[] data);
    }
    
    // 接口：连接状态回调
    public interface OnConnectionStateChangeListener {
        void onConnected(BluetoothDevice device);
        void onDisconnected();
        void onConnectionFailed(Exception e);
    }
    
    // 回调对象
    private OnDataReceivedListener dataReceivedListener;
    private OnConnectionStateChangeListener connectionStateChangeListener;
    
    /**
     * 构造函数
     * @param adapter 蓝牙适配器
     */
    public BluetoothDataManager(BluetoothAdapter adapter) {
        this.bluetoothAdapter = adapter;
    }
    
    /**
     * 构造函数（带上下文）
     * @param adapter 蓝牙适配器
     * @param context 上下文，用于权限检查
     */
    public BluetoothDataManager(BluetoothAdapter adapter, Context context) {
        this.bluetoothAdapter = adapter;
        this.context = context;
    }
    
    /**
     * 设置上下文
     * @param context 上下文，用于权限检查
     */
    public void setContext(Context context) {
        this.context = context;
    }
    
    /**
     * 设置数据接收监听器
     * @param listener 监听器实例
     */
    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.dataReceivedListener = listener;
    }
    
    /**
     * 设置连接状态变化监听器
     * @param listener 监听器实例
     */
    public void setOnConnectionStateChangeListener(OnConnectionStateChangeListener listener) {
        this.connectionStateChangeListener = listener;
    }
    
    /**
     * 检查是否有蓝牙连接权限
     * @return 是否有权限
     */
    private boolean hasBluetoothConnectPermission() {
        if (context == null) {
            Log.w(TAG, "无法检查权限：上下文为空");
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                   == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) 
                   == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * 检查是否有蓝牙扫描权限
     * @return 是否有权限
     */
    private boolean hasBluetoothScanPermission() {
        if (context == null) {
            Log.w(TAG, "无法检查权限：上下文为空");
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) 
                   == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) 
                   == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * 连接到蓝牙设备
     * @param device 要连接的蓝牙设备
     */
    public void connect(final BluetoothDevice device) {
        // 保存设备引用
        this.bluetoothDevice = device;
        
        // 确保之前的连接已关闭
        disconnect();
        
        // 在新线程中执行连接操作
        new Thread(() -> {
            try {
                // 检查蓝牙连接权限
                if (context != null && !hasBluetoothConnectPermission()) {
                    throw new SecurityException("缺少蓝牙连接权限");
                }
                
                // 创建RFCOMM连接Socket
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                
                // 确保停止蓝牙扫描
                if (context != null && hasBluetoothScanPermission() && bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                
                // 建立连接
                bluetoothSocket.connect();
                
                // 获取输入输出流
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                
                // 标记运行状态为true
                isRunning.set(true);
                
                // 启动数据接收线程
                startReceiving();
                
                // 回调连接成功
                if (connectionStateChangeListener != null) {
                    connectionStateChangeListener.onConnected(device);
                }
                
                Log.d(TAG, "已连接到设备: " + device.getName());
                
            } catch (SecurityException e) {
                // 权限被拒绝处理
                Log.e(TAG, "连接失败(权限被拒绝): " + e.getMessage());
                
                // 回调连接失败
                if (connectionStateChangeListener != null) {
                    connectionStateChangeListener.onConnectionFailed(e);
                }
                
                // 确保资源被释放
                disconnect();
            } catch (IOException e) {
                // 连接失败处理
                Log.e(TAG, "连接失败: " + e.getMessage());
                
                // 回调连接失败
                if (connectionStateChangeListener != null) {
                    connectionStateChangeListener.onConnectionFailed(e);
                }
                
                // 确保资源被释放
                disconnect();
            }
        }).start();
    }
    
    /**
     * 断开蓝牙连接
     */
    public void disconnect() {
        // 停止接收线程
        isRunning.set(false);
        
        // 等待接收线程结束
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        
        // 关闭输入流
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭输入流失败: " + e.getMessage());
            }
            inputStream = null;
        }
        
        // 关闭输出流
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭输出流失败: " + e.getMessage());
            }
            outputStream = null;
        }
        
        // 安全地关闭蓝牙Socket
        if (bluetoothSocket != null) {
            try {
                // 检查权限
                if (context != null && !hasBluetoothConnectPermission()) {
                    Log.w(TAG, "关闭蓝牙连接可能需要权限");
                }
                
                bluetoothSocket.close();
            } catch (SecurityException e) {
                Log.e(TAG, "关闭蓝牙Socket权限被拒绝: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "关闭蓝牙Socket失败: " + e.getMessage());
            }
            bluetoothSocket = null;
        }
        
        // 回调断开连接
        if (connectionStateChangeListener != null) {
            connectionStateChangeListener.onDisconnected();
        }
        
        Log.d(TAG, "蓝牙连接已断开");
    }
    
    /**
     * 启动数据接收线程
     */
    private void startReceiving() {
        receiveThread = new Thread(() -> {
            // 接收缓冲区
            byte[] buffer = new byte[1024];
            // 缓冲区中的有效数据长度
            int bufferPosition = 0;
            // 标记数据包开始位置
            int packetStartIndex = -1;
            
            while (isRunning.get()) {
                try {
                    // 检查输入流是否可用
                    if (inputStream == null) {
                        break;
                    }
                    
                    // 尝试读取数据
                    int bytesRead = inputStream.read(buffer, bufferPosition, buffer.length - bufferPosition);
                    
                    if (bytesRead > 0) {
                        // 更新缓冲区位置
                        bufferPosition += bytesRead;
                        
                        // 处理缓冲区中的数据
                        int processedBytes = processReceivedData(buffer, bufferPosition);
                        
                        // 移动未处理的数据到缓冲区开始位置
                        if (processedBytes > 0) {
                            for (int i = 0; i < bufferPosition - processedBytes; i++) {
                                buffer[i] = buffer[processedBytes + i];
                            }
                            bufferPosition -= processedBytes;
                        }
                        
                        // 防止缓冲区溢出，如果快满了就清空
                        if (bufferPosition > 900) {
                            Log.w(TAG, "缓冲区即将溢出，清空未处理数据");
                            bufferPosition = 0;
                        }
                    }
                } catch (IOException e) {
                    if (isRunning.get()) {
                        Log.e(TAG, "接收数据时发生错误: " + e.getMessage());
                        // 连接中断，回调断开连接
                        if (connectionStateChangeListener != null) {
                            connectionStateChangeListener.onDisconnected();
                        }
                        break;
                    }
                }
            }
        });
        
        receiveThread.start();
    }
    
    /**
     * 处理接收到的数据
     * @param buffer 数据缓冲区
     * @param length 有效数据长度
     * @return 处理的字节数
     */
    private int processReceivedData(byte[] buffer, int length) {
        int processedBytes = 0;
        
        // 确保至少有一个完整数据包的长度
        if (length < PACKET_LENGTH) {
            return 0;
        }
        
        // 搜索多个数据包
        for (int i = 0; i <= length - PACKET_LENGTH; i++) {
            // 检查数据包结构
            if (buffer[i] == DATA_HEADER && buffer[i+1] == PROTOCOL_VERSION && buffer[i+7] == DATA_FOOTER) {
                // 获取主指令和数据位
                byte command = buffer[i+2];
                byte[] data = new byte[3];
                data[0] = buffer[i+3];
                data[1] = buffer[i+4];
                data[2] = buffer[i+5];
                
                // 计算校验和
                byte checksum = (byte)((command + data[0] + data[1] + data[2]) & 0xFF);
                
                // 验证校验和
                if (checksum == buffer[i+6]) {
                    // 校验通过，回调数据接收事件
                    if (dataReceivedListener != null) {
                        dataReceivedListener.onDataReceived(command, data);
                    }
                    
                    // 记录日志
                    Log.d(TAG, "收到有效数据包: " + 
                          bytesToHex(new byte[]{buffer[i], buffer[i+1], command, 
                                              data[0], data[1], data[2], 
                                              checksum, buffer[i+7]}));
                    
                    // 更新已处理字节数
                    processedBytes = i + PACKET_LENGTH;
                } else {
                    Log.w(TAG, "校验和错误: 计算值=" + String.format("%02X", checksum) + 
                          ", 接收值=" + String.format("%02X", buffer[i+6]));
                }
            }
        }
        
        return processedBytes;
    }
    
    /**
     * 发送数据包
     * @param command 主指令
     * @param data 三个数据位
     * @return 是否发送成功
     */
    public boolean sendData(byte command, byte[] data) {
        // 参数验证
        if (data == null || data.length != 3) {
            Log.e(TAG, "数据位必须为3个字节");
            return false;
        }
        
        // 检查连接状态
        if (outputStream == null || bluetoothSocket == null || !bluetoothSocket.isConnected()) {
            Log.e(TAG, "蓝牙未连接，无法发送数据");
            return false;
        }
        
        // 检查权限
        if (context != null && !hasBluetoothConnectPermission()) {
            Log.e(TAG, "发送数据可能需要蓝牙连接权限");
            return false;
        }
        
        try {
            // 计算校验和
            byte checksum = (byte)((command + data[0] + data[1] + data[2]) & 0xFF);
            
            // 构建数据包
            byte[] packet = new byte[PACKET_LENGTH];
            packet[0] = DATA_HEADER;     // 数据头
            packet[1] = PROTOCOL_VERSION; // 版本号
            packet[2] = command;         // 主指令
            packet[3] = data[0];         // 数据位1
            packet[4] = data[1];         // 数据位2
            packet[5] = data[2];         // 数据位3
            packet[6] = checksum;        // 校验位
            packet[7] = DATA_FOOTER;     // 数据尾
            
            // 发送数据
            outputStream.write(packet);
            outputStream.flush();
            
            // 记录日志
            Log.d(TAG, "发送数据包: " + bytesToHex(packet));
            
            return true;
            
        } catch (SecurityException e) {
            Log.e(TAG, "发送数据权限被拒绝: " + e.getMessage());
            
            // 连接可能已断开，通知监听器
            if (connectionStateChangeListener != null) {
                connectionStateChangeListener.onDisconnected();
            }
            
            return false;
        } catch (IOException e) {
            Log.e(TAG, "发送数据失败: " + e.getMessage());
            
            // 连接可能已断开，通知监听器
            if (connectionStateChangeListener != null) {
                connectionStateChangeListener.onDisconnected();
            }
            
            return false;
        }
    }
    
    /**
     * 安全地检查蓝牙是否已连接
     * @return 连接状态
     */
    public boolean isConnected() {
        try {
            // 检查权限
            if (context != null && !hasBluetoothConnectPermission()) {
                Log.w(TAG, "检查蓝牙连接状态需要权限");
                return false;
            }
            
            return bluetoothSocket != null && bluetoothSocket.isConnected();
        } catch (SecurityException e) {
            Log.e(TAG, "检查连接状态权限被拒绝: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 安全地获取连接的设备
     * @return 蓝牙设备
     */
    public BluetoothDevice getConnectedDevice() {
        try {
            // 检查权限
            if (context != null && !hasBluetoothConnectPermission()) {
                Log.w(TAG, "获取连接设备需要权限");
                return null;
            }
            
            return bluetoothDevice;
        } catch (SecurityException e) {
            Log.e(TAG, "获取连接设备权限被拒绝: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 要转换的字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex).append(" ");
        }
        return hexString.toString().toUpperCase();
    }
} 