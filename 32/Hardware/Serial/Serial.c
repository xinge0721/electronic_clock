#include "stm32f10x.h"                  // Device header
#include <stdio.h>
#include <stdarg.h>
#include "Serial.h"

uint8_t Serial_RxData;		//定义串口接收的数据变量
uint8_t Serial_RxFlag;		//定义串口接收的标志位变量

// 接收缓冲区
uint8_t rxBuffer[PACKET_SIZE];
uint8_t isPacketComplete = 0;

/**
  * 函    数：计算校验和
  * 参    数：command, data1, data2, data3
  * 返 回 值：校验和
  */
static uint8_t calculateChecksum(uint8_t command, uint8_t data1, uint8_t data2, uint8_t data3)
{ 
    return (command + data1 + data2 + data3) % 256;
}

/**
  * 函    数：串口初始化
  * 参    数：无
  * 返 回 值：无
  */

void Serial_Init(void)
{
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2, ENABLE);
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);
	
	GPIO_InitTypeDef GPIO_InitStructure;
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_2;
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
	GPIO_Init(GPIOA, &GPIO_InitStructure);
	
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IPU;
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_3;
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
	GPIO_Init(GPIOA, &GPIO_InitStructure);
	
	USART_InitTypeDef USART_InitStructure;
	USART_InitStructure.USART_BaudRate = 115200;
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;
	USART_InitStructure.USART_Mode = USART_Mode_Tx | USART_Mode_Rx;
	USART_InitStructure.USART_Parity = USART_Parity_No;
	USART_InitStructure.USART_StopBits = USART_StopBits_1;
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;
	USART_Init(USART2, &USART_InitStructure);
	
	USART_ITConfig(USART2, USART_IT_RXNE, ENABLE);
	
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);
	
	NVIC_InitTypeDef NVIC_InitStructure;
	NVIC_InitStructure.NVIC_IRQChannel = USART2_IRQn;
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 1;
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 1;
	NVIC_Init(&NVIC_InitStructure);
	
	USART_Cmd(USART2, ENABLE);
}

/**
  * 函    数：串口发送一个字节
  * 参    数：byte 要发送的一个字节
  * 返 回 值：无
  */
void Serial_SendByte(uint8_t byte)
{
	USART_SendData(USART2, byte);
	while (USART_GetFlagStatus(USART2, USART_FLAG_TXE) == RESET);
}

/**
  * 函    数：串口发送一个数组
  * 参    数：array 要发送数组的首地址
  * 参    数：length 要发送数组的长度
  * 返 回 值：无
  */
void Serial_SendArray(uint8_t *array, uint16_t length)
{
	uint16_t i;
	for (i = 0; i < length; i++)
	{
		Serial_SendByte(array[i]);
	}
}

/**
  * 函    数：串口发送一个字符串
  * 参    数：String 要发送字符串的首地址
  * 返 回 值：无
  */
void Serial_SendString(char *String)
{
	uint8_t i;
	for (i = 0; String[i] != '\0'; i ++)//遍历字符数组（字符串），遇到字符串结束标志位后停止
	{
		Serial_SendByte(String[i]);		//依次调用Serial_SendByte发送每个字节数据
	}
}

/**
  * 函    数：次方函数（内部使用）
  * 返 回 值：返回值等于X的Y次方
  */
uint32_t Serial_Pow(uint32_t X, uint32_t Y)
{
	uint32_t Result = 1;	//设置结果初值为1
	while (Y --)			//执行Y次
	{
		Result *= X;		//将X累乘到结果
	}
	return Result;
}

/**
  * 函    数：串口发送数字
  * 参    数：Number 要发送的数字，范围：0~4294967295
  * 参    数：Length 要发送数字的长度，范围：0~10
  * 返 回 值：无
  */
void Serial_SendNumber(uint32_t Number, uint8_t Length)
{
	uint8_t i;
	for (i = 0; i < Length; i ++)		//根据数字长度遍历数字的每一位
	{
		Serial_SendByte(Number / Serial_Pow(10, Length - i - 1) % 10 + '0');	//依次调用Serial_SendByte发送每位数字
	}
}

/**
  * 函    数：使用printf需要重定向的底层函数
  * 参    数：保持原始格式即可，无需变动
  * 返 回 值：保持原始格式即可，无需变动
  */
int fputc(int ch, FILE *f)
{
	Serial_SendByte(ch);			//将printf的底层重定向到自己的发送字节函数
	return ch;
}

/**
  * 函    数：自己封装的prinf函数
  * 参    数：format 格式化字符串
  * 参    数：... 可变的参数列表
  * 返 回 值：无
  */
void Serial_Printf(char *format, ...)
{
	char String[100];				//定义字符数组
	va_list arg;					//定义可变参数列表数据类型的变量arg
	va_start(arg, format);			//从format开始，接收参数列表到arg变量
	vsprintf(String, format, arg);	//使用vsprintf打印格式化字符串和参数列表到字符数组中
	va_end(arg);					//结束变量arg
	Serial_SendString(String);		//串口发送字符数组（字符串）
}

/**
  * 函    数：获取串口接收标志位
  * 参    数：无
  * 返 回 值：串口接收标志位，范围：0~1，接收到数据后，标志位置1，读取后标志位自动清零
  */
uint8_t Serial_GetRxFlag(void)
{
	if (Serial_RxFlag == 1)			//如果标志位为1
	{
		Serial_RxFlag = 0;
		return 1;					//则返回1，并自动清零标志位
	}
	return 0;						//如果标志位为0，则返回0
}

/**
  * 函    数：获取串口接收的数据
  * 参    数：无
  * 返 回 值：接收的数据，范围：0~255
  */
uint8_t Serial_GetRxData(void)
{
	return Serial_RxData;			//返回接收的数据变量
}

/**
  * 函    数：发送完整数据包
  * 参    数：command 主指令, data1-3 数据位
  * 返 回 值：无
  */
void Serial_SendPacket(uint8_t command, uint8_t data1, uint8_t data2, uint8_t data3)
{
    uint8_t packet[PACKET_SIZE];
    
    packet[0] = PACKET_HEADER;
    packet[1] = PACKET_VERSION;
    packet[2] = command;
    packet[3] = data1;
    packet[4] = data2;
    packet[5] = data3;
    packet[6] = calculateChecksum(command, data1, data2, data3);
    packet[7] = PACKET_FOOTER;
    
    Serial_SendArray(packet, PACKET_SIZE);
}

/**
  * 函    数：获取接收到的数据包
  * 返 回 值：数据包指针
  */
uint8_t* Serial_GetPacket(void)
{
    if (isPacketComplete)
    {
        return rxBuffer;
    }
    return NULL;
}

/**
  * 函    数：验证数据包是否有效
  * 返 回 值：1为有效，0为无效
  */
uint8_t Serial_IsPacketValid(void)
{
    if (!isPacketComplete) return 0;
    
    // 检查帧头和帧尾
    if (rxBuffer[0] != PACKET_HEADER || rxBuffer[7] != PACKET_FOOTER)
        return 0;
    
    // 检查协议版本
    if (rxBuffer[1] != PACKET_VERSION)
        return 0;
    
    // 验证校验和
    uint8_t checksum = calculateChecksum(rxBuffer[2], rxBuffer[3], rxBuffer[4], rxBuffer[5]);
    if (checksum != rxBuffer[6])
        return 0;
    
    return 1;
}

/**
  * 函    数：USART2中断函数
  * 参    数：无
  * 返 回 值：无
  */
void USART2_IRQHandler(void)
{
	static uint8_t cont;
    if (USART_GetITStatus(USART2, USART_IT_RXNE) == SET)
    {
        uint8_t RxData = USART_ReceiveData(USART2);
        switch(cont)
				{
					case 0 : 
					case 1 :
					case 2 :
					case 3 :
					case 4 :
					case 5 :
					case 6 :rxBuffer[cont++] = RxData;break;
					case 7 :rxBuffer[cont] = RxData;isPacketComplete = 1;cont = 0;break;
					
				}
        USART_ClearITPendingBit(USART2, USART_IT_RXNE);
    }
}
#include "cuerrenttime.h"

void USART_data(void)
{
	//只有数组接受到数据，才能正式修改他
	if(isPacketComplete)
	{
		if(Serial_IsPacketValid())
		{
			switch(rxBuffer[2])  // 判断主命令
			{
				case 0x01:  // 校准时间
					currentTime.hours = rxBuffer[3];    // 时
					currentTime.minutes = rxBuffer[4];  // 分
					currentTime.seconds = rxBuffer[5];  // 秒

					break;
					
				case 0x02:  // 校准温度
					temperature  =  (rxBuffer[3] % 100) ;        // 小数点前俩位
					temperature  += (rxBuffer[4] % 100) * 0.01;  // 小数点后俩位
					temperature  *= rxBuffer[5] == 0xFF ? 1 : -1;  // 是正还是负
	
					break;
			}
		}
		isPacketComplete = 0;  // 处理完后清除完整包标志
	}
}
