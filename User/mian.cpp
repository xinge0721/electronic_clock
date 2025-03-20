#include "stm32f10x.h"                  // Device header
#include "pivot.h"
//初始化
OLED oled(GPIOB, GPIO_Pin_8, GPIOB, GPIO_Pin_9);

//按键功能执行函数
void Key_Nums()
{
if(KeyCfg[0].KEY_Event != KEY_Event_SingleClick) //如果按下了按键一
{
	if(KeyCfg[0].KEY_Event == KEY_Event_SingleClick)//如果短按
	{

	}
	else if(KeyCfg[0].KEY_Event == KEY_Event_DoubleClick)// 如果双击
	{

	}
	else if(KeyCfg[0].KEY_Event == KEY_Event_LongPress)// 如果长按
	{

	}
	KeyCfg[0].KEY_Event = KEY_Event_Null;//清空标志位

}		
else if(KeyCfg[1].KEY_Event != KEY_Event_SingleClick) {//如果按下了按键二
	if(KeyCfg[1].KEY_Event == KEY_Event_SingleClick)//如果短按
	{

	}
	else if(KeyCfg[1].KEY_Event == KEY_Event_DoubleClick)// 如果双击
	{

	}
	else if(KeyCfg[1].KEY_Event == KEY_Event_LongPress)// 如果长按
	{

	}
	KeyCfg[1].KEY_Event = KEY_Event_Null;//清空标志位

}		

else if(KeyCfg[2].KEY_Event != KEY_Event_SingleClick) {//如果按下了按键三
	if(KeyCfg[2].KEY_Event == KEY_Event_SingleClick)//如果短按
	{

	}
	else if(KeyCfg[2].KEY_Event == KEY_Event_DoubleClick)// 如果双击
	{

	}
	else if(KeyCfg[2].KEY_Event == KEY_Event_LongPress)// 如果长按
	{

	}
	KeyCfg[2].KEY_Event = KEY_Event_Null;//清空标志位

}		
else if(KeyCfg[3].KEY_Event != KEY_Event_SingleClick) {//如果按下了按键四
	if(KeyCfg[3].KEY_Event == KEY_Event_SingleClick)//如果短按
	{

	}
	else if(KeyCfg[3].KEY_Event == KEY_Event_DoubleClick)// 如果双击
	{

	}
	else if(KeyCfg[3].KEY_Event == KEY_Event_LongPress)// 如果长按
	{

	}
	KeyCfg[3].KEY_Event = KEY_Event_Null;//清空标志位

}		

}


int main()
{
	Hardware_Init();


	while(1)
	{
		Key_Nums();   //执行任务
				
	}
	
}


void TIM2_IRQHandler(void)
{
	if (TIM_GetITStatus(TIM2, TIM_IT_Update) == SET)
	{
		
		
		// 处理所有按键（假设4个按键）
		// 刷新所有按键的状态
		for (u8 i = 0; i < 4; i++) {
				KEY_ReadStateMachine(i); // data参数改为0-3
		}
		TIM_ClearITPendingBit(TIM2, TIM_IT_Update);
	}
}

