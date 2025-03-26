#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import bluetooth
import time
import struct
import threading
import logging
from datetime import datetime

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class RaspberryBluetooth:
    def __init__(self):
        # 蓝牙服务UUID
        self.UUID = "00001101-0000-1000-8000-00805F9B34FB"
        
        # 数据包常量
        self.DATA_HEADER = 0x55
        self.PROTOCOL_VERSION = 0x01
        self.DATA_FOOTER = 0xAA
        
        # 数据包长度
        self.PACKET_LENGTH = 8
        
        # 蓝牙连接相关
        self.server_sock = None
        self.client_sock = None
        self.client_info = None
        self.is_running = False
        self.receive_thread = None
        
        # 模拟数据
        self.current_time = datetime.now()
        self.current_temperature = 25.5
        
    def start_server(self):
        """启动蓝牙服务器"""
        try:
            self.server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
            self.server_sock.bind(("", bluetooth.PORT_ANY))
            self.server_sock.listen(1)
            
            # 注册服务
            bluetooth.advertise_service(
                self.server_sock,
                "ElectronicClock",
                service_id=self.UUID,
                service_classes=[self.UUID, bluetooth.SERIAL_PORT_CLASS],
                profiles=[bluetooth.SERIAL_PORT_PROFILE]
            )
            
            logger.info("蓝牙服务器启动成功，等待连接...")
            self.is_running = True
            
            # 启动接收线程
            self.receive_thread = threading.Thread(target=self._accept_connection)
            self.receive_thread.daemon = True
            self.receive_thread.start()
            
        except Exception as e:
            logger.error(f"启动蓝牙服务器失败: {str(e)}")
            self.stop_server()
            
    def _accept_connection(self):
        """接受客户端连接"""
        while self.is_running:
            try:
                self.client_sock, self.client_info = self.server_sock.accept()
                logger.info(f"接受连接来自: {self.client_info}")
                
                # 启动数据接收线程
                receive_thread = threading.Thread(target=self._receive_data)
                receive_thread.daemon = True
                receive_thread.start()
                
            except Exception as e:
                if self.is_running:
                    logger.error(f"接受连接失败: {str(e)}")
                break
                
    def _receive_data(self):
        """接收数据线程"""
        while self.is_running and self.client_sock:
            try:
                # 读取8字节数据包
                data = self.client_sock.recv(self.PACKET_LENGTH)
                if len(data) == self.PACKET_LENGTH:
                    self._process_packet(data)
                else:
                    logger.warning(f"收到不完整数据包: {len(data)}字节")
                    
            except Exception as e:
                logger.error(f"接收数据失败: {str(e)}")
                break
                
    def _process_packet(self, packet):
        """处理接收到的数据包"""
        try:
            # 验证数据包格式
            if packet[0] != self.DATA_HEADER or packet[-1] != self.DATA_FOOTER:
                logger.warning("数据包格式错误")
                return
                
            # 验证版本号
            if packet[1] != self.PROTOCOL_VERSION:
                logger.warning("协议版本不匹配")
                return
                
            # 获取命令和数据
            command = packet[2]
            data = packet[3:6]
            
            # 验证校验和
            checksum = (command + sum(data)) % 256
            if checksum != packet[6]:
                logger.warning("校验和错误")
                return
                
            # 处理命令
            self._handle_command(command, data)
            
        except Exception as e:
            logger.error(f"处理数据包失败: {str(e)}")
            
    def _handle_command(self, command, data):
        """处理不同的命令"""
        try:
            if command == 0x01:  # 获取时间
                self._send_time()
            elif command == 0x02:  # 设置时间
                self._set_time(data)
            elif command == 0x03:  # 重置时间
                self._reset_time()
            elif command == 0x04:  # 获取温度
                self._send_temperature()
            elif command == 0x05:  # 重置温度
                self._reset_temperature()
            else:
                logger.warning(f"未知命令: {hex(command)}")
                
        except Exception as e:
            logger.error(f"处理命令失败: {str(e)}")
            
    def _send_time(self):
        """发送当前时间"""
        try:
            now = datetime.now()
            time_data = struct.pack('BBB', now.hour, now.minute, now.second)
            self._send_packet(0x01, time_data)
        except Exception as e:
            logger.error(f"发送时间失败: {str(e)}")
            
    def _set_time(self, data):
        """设置时间"""
        try:
            hour, minute, second = struct.unpack('BBB', data)
            self.current_time = self.current_time.replace(
                hour=hour, minute=minute, second=second
            )
            logger.info(f"时间已设置为: {hour:02d}:{minute:02d}:{second:02d}")
        except Exception as e:
            logger.error(f"设置时间失败: {str(e)}")
            
    def _reset_time(self):
        """重置时间"""
        try:
            self.current_time = datetime.now()
            logger.info("时间已重置为当前系统时间")
        except Exception as e:
            logger.error(f"重置时间失败: {str(e)}")
            
    def _send_temperature(self):
        """发送当前温度"""
        try:
            temp_data = struct.pack('BBB', 
                int(self.current_temperature),
                int((self.current_temperature % 1) * 100),
                0
            )
            self._send_packet(0x04, temp_data)
        except Exception as e:
            logger.error(f"发送温度失败: {str(e)}")
            
    def _reset_temperature(self):
        """重置温度"""
        try:
            self.current_temperature = 25.5  # 模拟温度
            logger.info("温度已重置")
        except Exception as e:
            logger.error(f"重置温度失败: {str(e)}")
            
    def _send_packet(self, command, data):
        """发送数据包"""
        try:
            # 构造数据包
            packet = bytearray([
                self.DATA_HEADER,
                self.PROTOCOL_VERSION,
                command
            ])
            
            # 添加数据
            packet.extend(data)
            
            # 计算校验和
            checksum = (command + sum(data)) % 256
            packet.append(checksum)
            
            # 添加数据尾
            packet.append(self.DATA_FOOTER)
            
            # 发送数据包
            if self.client_sock:
                self.client_sock.send(bytes(packet))
                logger.debug(f"发送数据包: {packet.hex()}")
                
        except Exception as e:
            logger.error(f"发送数据包失败: {str(e)}")
            
    def stop_server(self):
        """停止蓝牙服务器"""
        self.is_running = False
        
        if self.client_sock:
            try:
                self.client_sock.close()
            except:
                pass
                
        if self.server_sock:
            try:
                self.server_sock.close()
            except:
                pass
                
        logger.info("蓝牙服务器已停止")

def main():
    """主函数"""
    try:
        # 创建蓝牙服务器实例
        bt_server = RaspberryBluetooth()
        
        # 启动服务器
        bt_server.start_server()
        
        # 保持主线程运行
        while True:
            time.sleep(1)
            
    except KeyboardInterrupt:
        logger.info("程序被用户中断")
    except Exception as e:
        logger.error(f"程序运行错误: {str(e)}")
    finally:
        bt_server.stop_server()

if __name__ == "__main__":
    main() 