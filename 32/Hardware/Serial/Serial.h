#ifndef __SERIAL_H
#define __SERIAL_H

#include <stdio.h>
#include "stm32f10x.h"

// 数据包格式：
// packet[0] = 帧头 (0x55)
// packet[1] = 协议版本 (0x01)
// packet[2] = 主命令
// packet[3] = 数据1
// packet[4] = 数据2
// packet[5] = 数据3
// packet[6] = 校验和
// packet[7] = 帧尾 (0xAA)

#define PACKET_SIZE 8
#define PACKET_HEADER 0x55
#define PACKET_FOOTER 0xAA
#define PACKET_VERSION 0x01

void Serial_Init(void);
void Serial_SendByte(uint8_t byte);
void Serial_SendArray(uint8_t *array, uint16_t length);
void Serial_SendPacket(uint8_t command, uint8_t data1, uint8_t data2, uint8_t data3);
uint8_t* Serial_GetPacket(void);
uint8_t Serial_IsPacketValid(void);

void Serial_SendString(char *String);
void Serial_SendNumber(uint32_t Number, uint8_t Length);
void Serial_Printf(char *format, ...);
void Serial_Send8BitPacket(uint8_t Data);

uint8_t Serial_GetRxFlag(void);
uint8_t Serial_GetRxData(void);

#endif
