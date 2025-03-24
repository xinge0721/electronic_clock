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
    uint8_t reserved;       // 保留字段，替代isSettingMode
    uint8_t settingIndex;   // 0:小时 1:分钟 2:秒
} TimeStruct;

extern TimeStruct currentTime;  // 默认时间12:00:00
extern float temperature;  // 温度值


// 显示时间显示
void UpdateTimeDisplay(void);
// 处理时间设置
void HandleTimeSetting(uint8_t increment);
// 处理按键扫描函数（循环）
void Key_Nums(void) ;
void cuerrenttime_Init(void);

#endif
