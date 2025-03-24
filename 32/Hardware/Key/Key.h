#ifndef __KEY_H
#define __KEY_H	 
#include "sys.h"

/**
 * @brief 按键状态枚举
 * 按键状态机的各种状态定义
 */
typedef enum _KEY_StatusList_TypeDef 
{
	KEY_Status_Idle = 0					, // 空闲状态 - 无按键按下
	KEY_Status_Debounce   		  , // 消抖状态 - 按键刚被按下，等待消抖
	KEY_Status_ConfirmPress		  , // 确认按下状态 - 消抖后确认按键被按下	
	KEY_Status_ConfirmPressLong	, // 确认长按状态 - 按键被长时间按下	
	KEY_Status_WaiteAgain		    , // 等待再次按下状态 - 等待双击的第二次按下
	KEY_Status_SecondPress      , // 第二次按下状态 - 双击的第二次按下
}KEY_StatusList_TypeDef;

/**
 * @brief 按键事件枚举
 * 定义按键可能触发的各种事件类型
 */
typedef enum _KEY_EventList_TypeDef 
{
	KEY_Event_Null 		   = 0x00,  // 无事件
	KEY_Event_SingleClick  = 0x01,  // 单击事件
	KEY_Event_DoubleClick  = 0x02,  // 双击事件
	KEY_Event_LongPress    = 0x04   // 长按事件
}KEY_EventList_TypeDef;

/**
 * @brief 按键动作枚举
 * 定义按键的物理动作状态
 */
typedef enum
{ 
	KEY_Action_Press = 0,   // 按键按下
	KEY_Action_Release      // 按键释放
}KEY_Action_TypeDef;

/**
 * @brief 按键引脚电平枚举
 * 定义按键引脚的电平状态
 */
typedef enum { 
    KEY_PinLevel_Low = 0,   // 低电平
    KEY_PinLevel_High       // 高电平
} KEY_PinLevel_TypeDef;

/**
 * @brief 按键配置结构体
 * 包含按键的所有配置参数和状态信息
 */
typedef struct _KEY_Configure_TypeDef 
{
	uint16_t                       KEY_Count;        // 按键计数器，用于计时
	KEY_Action_TypeDef             KEY_Action;       // 按键当前动作：按下=1，抬起=0
	KEY_StatusList_TypeDef         KEY_Status;       // 按键当前状态
	KEY_EventList_TypeDef          KEY_Event;        // 按键事件
	KEY_PinLevel_TypeDef          (*KEY_ReadPin_Fcn)(u8 data);  // 读取按键IO电平函数指针
}KEY_Configure_TypeDef;

// 按键配置全局数组，保存所有按键的状态
extern KEY_Configure_TypeDef KeyCfg[];

/**
 * @brief 按键初始化函数
 * 初始化按键相关的GPIO接口
 */
void KEY_Init(void);

/**
 * @brief 按键状态机处理函数
 * @param data 要处理的按键索引
 * 根据按键的物理状态更新按键的逻辑状态和事件
 */
void KEY_ReadStateMachine(u8 data);

#endif
