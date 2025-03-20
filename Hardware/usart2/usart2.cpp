#include "usart2.h"
#include "IO_Core.h"
 #include "assert.h"
 
 
 
 
 
typedef struct {
    GPIO_TypeDef* GPIOx;
    uint16_t GPIO_Pin;
    USART_TypeDef* USARTx;
} USART_GPIO_Mapping;

// 串口引脚映射表（基于STM32F103C8T6默认配置）
const USART_GPIO_Mapping usartMap[] = {
    // USART1（APB2）
    {GPIOA, GPIO_Pin_9,  USART1},  // USART1_TX
    {GPIOA, GPIO_Pin_10, USART1},  // USART1_RX

    // USART2（APB1）
    {GPIOA, GPIO_Pin_2,  USART2},  // USART2_TX
    {GPIOA, GPIO_Pin_3,  USART2},  // USART2_RX

    // USART3（APB1）
    {GPIOB, GPIO_Pin_10, USART3},  // USART3_TX
    {GPIOB, GPIO_Pin_11, USART3},  // USART3_RX
};

// 查询函数：通过GPIO和引脚号获取对应的USART
USART_TypeDef* GetUSARTFromGPIO(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin) {
    for (uint8_t i = 0; i < sizeof(usartMap)/sizeof(usartMap[0]); i++) {
        if (usartMap[i].GPIOx == GPIOx && usartMap[i].GPIO_Pin == GPIO_Pin) {
            return usartMap[i].USARTx;
        }
    }
    return NULL; // 未找到匹配项
}



uart::uart(GPIO_TypeDef* _TX_GPIOx, uint16_t _TX_Pin, 
			 GPIO_TypeDef* _RX_GPIOx, uint16_t _RX_Pin,
			 int bound)
				:TX(_TX_GPIOx,_TX_Pin,GPIO_Mode_AF_PP),
				 RX(_RX_GPIOx,_RX_Pin,GPIO_Mode_IN_FLOATING)
	{
	
		USART_TypeDef* USART_TX = GetUSARTFromGPIO(_TX_GPIOx,_TX_Pin);
		USART_TypeDef* USART_RX = GetUSARTFromGPIO(_RX_GPIOx,_RX_Pin);

		// 检查引脚是否映射到有效的串口
		if (USART_TX == NULL || USART_RX == NULL) {
				assert(0 && "Invalid USART pin mapping!"); // 明确错误原因
				return;
		}

		// 检查是否使用同一串口
		if (USART_TX != USART_RX) {
				assert(0 && "USART_TX and USART_RX must use the same timer!");
				return;
		}

		// 在uart构造函数中添加：
		if (Uart == USART1) {
				RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1, ENABLE);
		} else if (Uart == USART2) {
				RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2, ENABLE);
		} else if (Uart == USART3) {
				RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART3, ENABLE);
		}
		Uart = USART_TX; 
		   //USART 初始化设置
		USART_InitTypeDef USART_InitStructure;

		USART_InitStructure.USART_BaudRate = bound;//串口波特率
		USART_InitStructure.USART_WordLength = USART_WordLength_8b;//字长为8位数据格式
		USART_InitStructure.USART_StopBits = USART_StopBits_1;//一个停止位
		USART_InitStructure.USART_Parity = USART_Parity_No;//无奇偶校验位
		USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//无硬件数据流控制
		USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;	//收发模式
		USART_Init(Uart, &USART_InitStructure);     //初始化串口
		USART_ITConfig(Uart, USART_IT_RXNE, ENABLE);//开启串口接受中断
		USART_Cmd(Uart, ENABLE);                    //使能串口
		
		
		NVIC_InitTypeDef NVIC_InitStructure;
		if(Uart == USART1)// 根据实际 USART 选择通道
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
* 函    数：串口发送一个字节
* 参    数：Byte 要发送的一个字节
* 返 回 值：无
*/
void uart::Serial_SendByte(uint8_t Byte)
{
	USART_SendData(Uart, Byte);		//将字节数据写入数据寄存器，写入后USART自动生成时序波形
	while (USART_GetFlagStatus(Uart, USART_FLAG_TXE) == RESET);	//等待发送完成
	/*下次写入数据寄存器会自动清除发送完成标志位，故此循环后，无需清除标志位*/
}
	

/**
  * 函    数：串口发送一个数组
  * 参    数：Array 要发送数组的首地址
  * 参    数：Length 要发送数组的长度
  * 返 回 值：无
  */
void uart::Serial_SendArray(uint8_t *Array, uint16_t Length)
{
	uint16_t i;
	for (i = 0; i < Length; i ++)		//遍历数组
	{
		Serial_SendByte(Array[i]);		//依次调用Serial_SendByte发送每个字节数据
	}
}

/**
  * 函    数：串口发送一个字符串
  * 参    数：String 要发送字符串的首地址
  * 返 回 值：无
  */
void uart::Serial_SendString(char *String)
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
uint32_t uart::Serial_Pow(uint32_t X, uint32_t Y)
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
void uart::Serial_SendNumber(uint32_t Number, uint8_t Length)
{
	uint8_t i;
	for (i = 0; i < Length; i ++)		//根据数字长度遍历数字的每一位
	{
		Serial_SendByte(Number / Serial_Pow(10, Length - i - 1) % 10 + '0');	//依次调用Serial_SendByte发送每位数字
	}
}

/**
* 函    数：获取串口接收标志位
* 参    数：无
* 返 回 值：串口接收标志位，范围：0~1，接收到数据后，标志位置1，读取后标志位自动清零
*/
uint8_t uart::Serial_GetRxFlag(void)
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
uint8_t uart::Serial_GetRxData(void)
{
	return Serial_RxData;			//返回接收的数据变量
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
函数功能：串口1接收中断
入口参数：无
返回  值：无
**************************************************************************/
void USART1_IRQHandler(void) {
    if (USART_GetITStatus(USART1, USART_IT_RXNE) != RESET) {
        USART_ClearITPendingBit(USART1, USART_IT_RXNE);
        uint8_t data = USART_ReceiveData(USART1);
//      uart1.Serial_SendByte(Uart_Receive);

    }
}

/**************************************************************************
函数功能：串口2接收中断
入口参数：无
返回  值：无
**************************************************************************/
void USART2_IRQHandler(void)
{

	if(USART_GetITStatus(USART2,USART_IT_RXNE)!=RESET)//接收中断标志位拉高
	{
		USART_ClearITPendingBit(USART2, USART_IT_RXNE); // 清除中断标志
		uint8_t Uart_Receive=USART_ReceiveData(USART2);//保存接收的数据
		uart2.Serial_SendByte(Uart_Receive);

	}
}
/**************************************************************************
函数功能：串口3接收中断
入口参数：无
返回  值：无
**************************************************************************/
void USART3_IRQHandler(void) {
    if (USART_GetITStatus(USART3, USART_IT_RXNE) != RESET) {
        USART_ClearITPendingBit(USART3, USART_IT_RXNE);
        uint8_t data = USART_ReceiveData(USART3);
//				uart3.Serial_SendByte(Uart_Receive);

    }
}



