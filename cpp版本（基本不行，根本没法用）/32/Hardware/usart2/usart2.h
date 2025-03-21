#ifndef __USART2_H
#define __USART2_H

#include "stm32f10x.h"
#include <stdio.h>
#include <stdarg.h>

#ifdef __cplusplus
extern "C" {
#endif

// 全局变量声明
extern uint8_t Serial_RxData;    // 串口接收数据缓存
extern uint8_t Serial_RxFlag;    // 串口接收标志位
extern uint8_t OK_data[8];       // 数据缓存数组
extern uint8_t OK;               // 计数器

// 函数声明
void Serial_Init(void);                                      // 串口初始化
void Serial_SendByte(uint8_t Byte);                         // 发送一个字节
void Serial_SendArray(uint8_t *Array, uint16_t Length);     // 发送数组
void Serial_SendString(char *String);                       // 发送字符串
void Serial_SendNumber(uint32_t Number, uint8_t Length);    // 发送数字
void Serial_Printf(char *format, ...);                      // 格式化打印
void Serial_ProcessData(uint8_t RxData);                    // 处理接收数据
uint8_t Serial_GetRxFlag(void);                            // 获取接收标志位
uint8_t Serial_GetRxData(void);                            // 获取接收数据

#ifdef __cplusplus
}
#endif

#endif
