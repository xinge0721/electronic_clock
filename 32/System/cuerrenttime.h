#ifndef __currenttime_H
#define __currenttime_H

#include "stm32f10x.h"
#include "Key.h"
#include "OLED.h"
// ʱ��ṹ��
typedef struct {
    uint8_t hours;
    uint8_t minutes;
    uint8_t seconds;
    uint8_t isSettingMode;  // ����ģʽ��־
    uint8_t settingIndex;   // 0:Сʱ 1:���� 2:��
} TimeStruct;

extern TimeStruct currentTime;  // Ĭ��ʱ��12:00:00
extern float temperature;  // �¶�ֵ


// ����ʱ����ʾ
void UpdateTimeDisplay(void);
// ����ʱ������
void HandleTimeSetting(uint8_t increment);
// ��������ִ�к�������ѭ���棩
void Key_Nums(void) ;

#endif
