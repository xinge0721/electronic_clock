#ifndef __USART2_H
#define __USART2_H

#include "stm32f10x.h"
#include "IO_Core.h"

class uart {
private:
	GPIO TX;
	GPIO RX;
	USART_TypeDef* Uart;
	uint8_t Serial_RxData;		//���崮�ڽ��յ����ݱ���
	uint8_t Serial_RxFlag;		//���崮�ڽ��յı�־λ����
	uint32_t Serial_Pow(uint32_t X, uint32_t Y);
	uint8_t dispose_data[8];		//���崮�ڽ��յı�־λ����
	uint8_t OK_data[8];		//���崮�ڽ��յı�־λ����
	uint8_t OK;
public:
    uart(GPIO_TypeDef* _TX_GPIOx, uint16_t _TX_Pin, 
         GPIO_TypeDef* _RX_GPIOx, uint16_t _RX_Pin,
         int bound);
    void Serial_SendByte(uint8_t Byte);
    void Serial_SendArray(uint8_t *Array, uint16_t Length);
    void Serial_SendString(char *String);
    void Serial_SendNumber(uint32_t Number, uint8_t Length);
    uint8_t Serial_GetRxFlag(void);
    uint8_t Serial_GetRxData(void);
		void Serial_data(uint8_t RXdata);
};

// �����жϴ�����
void USART2_IRQHandler(void);

#endif
