#ifndef __currenttime_H
#define __currenttime_H

#include "stm32f10x.h"
#include "Key.h"
#include "OLED.h"
// 时间结构体
typedef struct {
    uint8_t hours;
    uint8_t minutes;
    uint8_t seconds;
    uint8_t isSettingMode;  // 设置模式标志
    uint8_t settingIndex;   // 0:小时 1:分钟 2:秒
} TimeStruct;

extern TimeStruct currentTime;  // 默认时间12:00:00
extern float temperature;  // 温度值


// 更新时间显示
void UpdateTimeDisplay(void);
// 处理时间设置
void HandleTimeSetting(uint8_t increment);
// 按键功能执行函数（非循环版）
void Key_Nums(void) ;

#endif
