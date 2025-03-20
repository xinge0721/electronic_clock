#include "usart2.h"
#include "IO_Core.h"
 #include "assert.h"
 
 
 
 
 
typedef struct {
    GPIO_TypeDef* GPIOx;
    uint16_t GPIO_Pin;
    USART_TypeDef* USARTx;
} USART_GPIO_Mapping;

// ��������ӳ�������STM32F103C8T6Ĭ�����ã�
const USART_GPIO_Mapping usartMap[] = {
    // USART1��APB2��
    {GPIOA, GPIO_Pin_9,  USART1},  // USART1_TX
    {GPIOA, GPIO_Pin_10, USART1},  // USART1_RX

    // USART2��APB1��
    {GPIOA, GPIO_Pin_2,  USART2},  // USART2_TX
    {GPIOA, GPIO_Pin_3,  USART2},  // USART2_RX

    // USART3��APB1��
    {GPIOB, GPIO_Pin_10, USART3},  // USART3_TX
    {GPIOB, GPIO_Pin_11, USART3},  // USART3_RX
};

// ��ѯ������ͨ��GPIO�����źŻ�ȡ��Ӧ��USART
USART_TypeDef* GetUSARTFromGPIO(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin) {
    for (uint8_t i = 0; i < sizeof(usartMap)/sizeof(usartMap[0]); i++) {
        if (usartMap[i].GPIOx == GPIOx && usartMap[i].GPIO_Pin == GPIO_Pin) {
            return usartMap[i].USARTx;
        }
    }
    return NULL; // δ�ҵ�ƥ����
}



uart::uart(GPIO_TypeDef* _TX_GPIOx, uint16_t _TX_Pin, 
			 GPIO_TypeDef* _RX_GPIOx, uint16_t _RX_Pin,
			 int bound)
				:TX(_TX_GPIOx,_TX_Pin,GPIO_Mode_AF_PP),
				 RX(_RX_GPIOx,_RX_Pin,GPIO_Mode_IN_FLOATING)
	{
	
		USART_TypeDef* USART_TX = GetUSARTFromGPIO(_TX_GPIOx,_TX_Pin);
		USART_TypeDef* USART_RX = GetUSARTFromGPIO(_RX_GPIOx,_RX_Pin);

		// ��������Ƿ�ӳ�䵽��Ч�Ĵ���
		if (USART_TX == NULL || USART_RX == NULL) {
				assert(0 && "Invalid USART pin mapping!"); // ��ȷ����ԭ��
				return;
		}

		// ����Ƿ�ʹ��ͬһ����
		if (USART_TX != USART_RX) {
				assert(0 && "USART_TX and USART_RX must use the same timer!");
				return;
		}

		// ��uart���캯������ӣ�
		if (Uart == USART1) {
				RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1, ENABLE);
		} else if (Uart == USART2) {
				RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2, ENABLE);
		} else if (Uart == USART3) {
				RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART3, ENABLE);
		}
		Uart = USART_TX; 
		   //USART ��ʼ������
		USART_InitTypeDef USART_InitStructure;

		USART_InitStructure.USART_BaudRate = bound;//���ڲ�����
		USART_InitStructure.USART_WordLength = USART_WordLength_8b;//�ֳ�Ϊ8λ���ݸ�ʽ
		USART_InitStructure.USART_StopBits = USART_StopBits_1;//һ��ֹͣλ
		USART_InitStructure.USART_Parity = USART_Parity_No;//����żУ��λ
		USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//��Ӳ������������
		USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;	//�շ�ģʽ
		USART_Init(Uart, &USART_InitStructure);     //��ʼ������
		USART_ITConfig(Uart, USART_IT_RXNE, ENABLE);//�������ڽ����ж�
		USART_Cmd(Uart, ENABLE);                    //ʹ�ܴ���
		
		
		NVIC_InitTypeDef NVIC_InitStructure;
		if(Uart == USART1)// ����ʵ�� USART ѡ��ͨ��
			NVIC_InitStructure.NVIC_IRQChannel = USART1_IRQn; 
		else if(Uart == USART2)
			NVIC_InitStructure.NVIC_IRQChannel = USART2_IRQn; 
		else
			NVIC_InitStructure.NVIC_IRQChannel = USART3_IRQn; 
		NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 0;
		NVIC_InitStructure.NVIC_IRQChannelSubPriority = 0;
		NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;
		NVIC_Init(&NVIC_InitStructure);
	}
/**
* ��    �������ڷ���һ���ֽ�
* ��    ����Byte Ҫ���͵�һ���ֽ�
* �� �� ֵ����
*/
void uart::Serial_SendByte(uint8_t Byte)
{
	USART_SendData(Uart, Byte);		//���ֽ�����д�����ݼĴ�����д���USART�Զ�����ʱ����
	while (USART_GetFlagStatus(Uart, USART_FLAG_TXE) == RESET);	//�ȴ��������
	/*�´�д�����ݼĴ������Զ����������ɱ�־λ���ʴ�ѭ�������������־λ*/
}
	

/**
  * ��    �������ڷ���һ������
  * ��    ����Array Ҫ����������׵�ַ
  * ��    ����Length Ҫ��������ĳ���
  * �� �� ֵ����
  */
void uart::Serial_SendArray(uint8_t *Array, uint16_t Length)
{
	uint16_t i;
	for (i = 0; i < Length; i ++)		//��������
	{
		Serial_SendByte(Array[i]);		//���ε���Serial_SendByte����ÿ���ֽ�����
	}
}

/**
  * ��    �������ڷ���һ���ַ���
  * ��    ����String Ҫ�����ַ������׵�ַ
  * �� �� ֵ����
  */
void uart::Serial_SendString(char *String)
{
	uint8_t i;
	for (i = 0; String[i] != '\0'; i ++)//�����ַ����飨�ַ������������ַ���������־λ��ֹͣ
	{
		Serial_SendByte(String[i]);		//���ε���Serial_SendByte����ÿ���ֽ�����
	}
}

/**
  * ��    �����η��������ڲ�ʹ�ã�
  * �� �� ֵ������ֵ����X��Y�η�
  */
uint32_t uart::Serial_Pow(uint32_t X, uint32_t Y)
{
	uint32_t Result = 1;	//���ý����ֵΪ1
	while (Y --)			//ִ��Y��
	{
		Result *= X;		//��X�۳˵����
	}
	return Result;
}

/**
  * ��    �������ڷ�������
  * ��    ����Number Ҫ���͵����֣���Χ��0~4294967295
  * ��    ����Length Ҫ�������ֵĳ��ȣ���Χ��0~10
  * �� �� ֵ����
  */
void uart::Serial_SendNumber(uint32_t Number, uint8_t Length)
{
	uint8_t i;
	for (i = 0; i < Length; i ++)		//�������ֳ��ȱ������ֵ�ÿһλ
	{
		Serial_SendByte(Number / Serial_Pow(10, Length - i - 1) % 10 + '0');	//���ε���Serial_SendByte����ÿλ����
	}
}

/**
* ��    ������ȡ���ڽ��ձ�־λ
* ��    ������
* �� �� ֵ�����ڽ��ձ�־λ����Χ��0~1�����յ����ݺ󣬱�־λ��1����ȡ���־λ�Զ�����
*/
uint8_t uart::Serial_GetRxFlag(void)
{
	if (Serial_RxFlag == 1)			//�����־λΪ1
	{
		Serial_RxFlag = 0;
		return 1;					//�򷵻�1�����Զ������־λ
	}
	return 0;						//�����־λΪ0���򷵻�0
}

/**
* ��    ������ȡ���ڽ��յ�����
* ��    ������
* �� �� ֵ�����յ����ݣ���Χ��0~255
*/
uint8_t uart::Serial_GetRxData(void)
{
	return Serial_RxData;			//���ؽ��յ����ݱ���
}


void uart::Serial_data(uint8_t RXdata)
{
	static u8 i = 0;
	if(i != 8)
		dispose_data[i++] = RXdata;
	else
	{
		i = 0;
		for(int j = 0 ; j < 8 ; j++)
		{
			OK_data[j] = dispose_data[j];
		}
		OK = 1;
	}
}

uart uart2(GPIOA,GPIO_Pin_2,GPIOA,GPIO_Pin_3,115200);


/**************************************************************************
�������ܣ�����1�����ж�
��ڲ�������
����  ֵ����
**************************************************************************/
void USART1_IRQHandler(void) {
    if (USART_GetITStatus(USART1, USART_IT_RXNE) != RESET) {
        USART_ClearITPendingBit(USART1, USART_IT_RXNE);
        uint8_t data = USART_ReceiveData(USART1);
//      uart1.Serial_SendByte(Uart_Receive);

    }
}

/**************************************************************************
�������ܣ�����2�����ж�
��ڲ�������
����  ֵ����
**************************************************************************/
void USART2_IRQHandler(void)
{

	if(USART_GetITStatus(USART2,USART_IT_RXNE)!=RESET)//�����жϱ�־λ����
	{
		USART_ClearITPendingBit(USART2, USART_IT_RXNE); // ����жϱ�־
		uint8_t Uart_Receive=USART_ReceiveData(USART2);//������յ�����
		uart2.Serial_SendByte(Uart_Receive);

	}
}
/**************************************************************************
�������ܣ�����3�����ж�
��ڲ�������
����  ֵ����
**************************************************************************/
void USART3_IRQHandler(void) {
    if (USART_GetITStatus(USART3, USART_IT_RXNE) != RESET) {
        USART_ClearITPendingBit(USART3, USART_IT_RXNE);
        uint8_t data = USART_ReceiveData(USART3);
//				uart3.Serial_SendByte(Uart_Receive);

    }
}



