#include "stm32f10x.h"                  // Device header


class LED
{
	LED()
	{
		RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);		//����GPIOA��ʱ��
		
		/*GPIO��ʼ��*/
		GPIO_InitTypeDef GPIO_InitStructure;
		GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP;
		GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1 | GPIO_Pin_2;
		GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
		GPIO_Init(GPIOA, &GPIO_InitStructure);						//��PA1��PA2���ų�ʼ��Ϊ�������
		
		/*����GPIO��ʼ�����Ĭ�ϵ�ƽ*/
		GPIO_SetBits(GPIOA, GPIO_Pin_1 | GPIO_Pin_2);				//����PA1��PA2����Ϊ�ߵ�ƽ
	}

};

/**
  * ��    ����LED��ʼ��
  * ��    ������
  * �� �� ֵ����
  */
void LED_Init(void)
{
	/*����ʱ��*/
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);		//����GPIOA��ʱ��
	
	/*GPIO��ʼ��*/
	GPIO_InitTypeDef GPIO_InitStructure;
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP;
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1 | GPIO_Pin_2;
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
	GPIO_Init(GPIOA, &GPIO_InitStructure);						//��PA1��PA2���ų�ʼ��Ϊ�������
	
	/*����GPIO��ʼ�����Ĭ�ϵ�ƽ*/
	GPIO_SetBits(GPIOA, GPIO_Pin_1 | GPIO_Pin_2);				//����PA1��PA2����Ϊ�ߵ�ƽ
}

/**
  * ��    ����LED1����
  * ��    ������
  * �� �� ֵ����
  */
void LED1_ON(void)
{
	GPIO_ResetBits(GPIOA, GPIO_Pin_1);		//����PA1����Ϊ�͵�ƽ
}

/**
  * ��    ����LED1�ر�
  * ��    ������
  * �� �� ֵ����
  */
void LED1_OFF(void)
{
	GPIO_SetBits(GPIOA, GPIO_Pin_1);		//����PA1����Ϊ�ߵ�ƽ
}

/**
  * ��    ����LED1״̬��ת
  * ��    ������
  * �� �� ֵ����
  */
void LED1_Turn(void)
{
	if (GPIO_ReadOutputDataBit(GPIOA, GPIO_Pin_1) == 0)		//��ȡ����Ĵ�����״̬�������ǰ��������͵�ƽ
	{
		GPIO_SetBits(GPIOA, GPIO_Pin_1);					//������PA1����Ϊ�ߵ�ƽ
	}
	else													//���򣬼���ǰ��������ߵ�ƽ
	{
		GPIO_ResetBits(GPIOA, GPIO_Pin_1);					//������PA1����Ϊ�͵�ƽ
	}
}

/**
  * ��    ����LED2����
  * ��    ������
  * �� �� ֵ����
  */
void LED2_ON(void)
{
	GPIO_ResetBits(GPIOA, GPIO_Pin_2);		//����PA2����Ϊ�͵�ƽ
}

/**
  * ��    ����LED2�ر�
  * ��    ������
  * �� �� ֵ����
  */
void LED2_OFF(void)
{
	GPIO_SetBits(GPIOA, GPIO_Pin_2);		//����PA2����Ϊ�ߵ�ƽ
}

/**
  * ��    ����LED2״̬��ת
  * ��    ������
  * �� �� ֵ����
  */
void LED2_Turn(void)
{
	if (GPIO_ReadOutputDataBit(GPIOA, GPIO_Pin_2) == 0)		//��ȡ����Ĵ�����״̬�������ǰ��������͵�ƽ
	{                                                  
		GPIO_SetBits(GPIOA, GPIO_Pin_2);               		//������PA2����Ϊ�ߵ�ƽ
	}                                                  
	else                                               		//���򣬼���ǰ��������ߵ�ƽ
	{                                                  
		GPIO_ResetBits(GPIOA, GPIO_Pin_2);             		//������PA2����Ϊ�͵�ƽ
	}
}
