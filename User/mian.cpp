#include "stm32f10x.h"                  // Device header
#include "pivot.h"
//��ʼ��
OLED oled(GPIOB, GPIO_Pin_8, GPIOB, GPIO_Pin_9);

//��������ִ�к���
void Key_Nums()
{
if(KeyCfg[0].KEY_Event != KEY_Event_SingleClick) //��������˰���һ
{
	if(KeyCfg[0].KEY_Event == KEY_Event_SingleClick)//����̰�
	{

	}
	else if(KeyCfg[0].KEY_Event == KEY_Event_DoubleClick)// ���˫��
	{

	}
	else if(KeyCfg[0].KEY_Event == KEY_Event_LongPress)// �������
	{

	}
	KeyCfg[0].KEY_Event = KEY_Event_Null;//��ձ�־λ

}		
else if(KeyCfg[1].KEY_Event != KEY_Event_SingleClick) {//��������˰�����
	if(KeyCfg[1].KEY_Event == KEY_Event_SingleClick)//����̰�
	{

	}
	else if(KeyCfg[1].KEY_Event == KEY_Event_DoubleClick)// ���˫��
	{

	}
	else if(KeyCfg[1].KEY_Event == KEY_Event_LongPress)// �������
	{

	}
	KeyCfg[1].KEY_Event = KEY_Event_Null;//��ձ�־λ

}		

else if(KeyCfg[2].KEY_Event != KEY_Event_SingleClick) {//��������˰�����
	if(KeyCfg[2].KEY_Event == KEY_Event_SingleClick)//����̰�
	{

	}
	else if(KeyCfg[2].KEY_Event == KEY_Event_DoubleClick)// ���˫��
	{

	}
	else if(KeyCfg[2].KEY_Event == KEY_Event_LongPress)// �������
	{

	}
	KeyCfg[2].KEY_Event = KEY_Event_Null;//��ձ�־λ

}		
else if(KeyCfg[3].KEY_Event != KEY_Event_SingleClick) {//��������˰�����
	if(KeyCfg[3].KEY_Event == KEY_Event_SingleClick)//����̰�
	{

	}
	else if(KeyCfg[3].KEY_Event == KEY_Event_DoubleClick)// ���˫��
	{

	}
	else if(KeyCfg[3].KEY_Event == KEY_Event_LongPress)// �������
	{

	}
	KeyCfg[3].KEY_Event = KEY_Event_Null;//��ձ�־λ

}		

}


int main()
{
	Hardware_Init();


	while(1)
	{
		Key_Nums();   //ִ������
				
	}
	
}


void TIM2_IRQHandler(void)
{
	if (TIM_GetITStatus(TIM2, TIM_IT_Update) == SET)
	{
		
		
		// �������а���������4��������
		// ˢ�����а�����״̬
		for (u8 i = 0; i < 4; i++) {
				KEY_ReadStateMachine(i); // data������Ϊ0-3
		}
		TIM_ClearITPendingBit(TIM2, TIM_IT_Update);
	}
}

