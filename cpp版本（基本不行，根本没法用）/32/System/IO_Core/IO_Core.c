#include "IO_Core.h"

// 全局变量（用于PWM和编码器）
static TIM_TypeDef* current_pwm_timer = NULL;
static TIM_TypeDef* current_encoder_timer = NULL;

// GPIO基本操作
void GPIO_Init_Custom(GPIO_TypeDef* GPIOx, uint16_t Pin, GPIOMode_TypeDef mode)
{
    // 使能GPIO时钟
    if(GPIOx == GPIOA) RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);
    else if(GPIOx == GPIOB) RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB, ENABLE);
    else if(GPIOx == GPIOC) RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOC, ENABLE);
    
    GPIO_InitTypeDef GPIO_InitStructure;
    GPIO_InitStructure.GPIO_Pin = Pin;
    GPIO_InitStructure.GPIO_Mode = mode;
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_Init(GPIOx, &GPIO_InitStructure);
}

void GPIO_Write(GPIO_TypeDef* GPIOx, uint16_t Pin, uint8_t state)
{
    if(state) GPIO_SetBits(GPIOx, Pin);
    else GPIO_ResetBits(GPIOx, Pin);
}

uint8_t GPIO_Read(GPIO_TypeDef* GPIOx, uint16_t Pin)
{
    return GPIO_ReadInputDataBit(GPIOx, Pin);
}

// PWM相关函数
void PWM_Init_Custom(GPIO_TypeDef* Oc1_GPIO, uint16_t Oc1_Pin,
                    GPIO_TypeDef* Oc4_GPIO, uint16_t Oc4_Pin,
                    uint16_t arr, uint16_t psc)
{
    // 配置GPIO
    GPIO_Init_Custom(Oc1_GPIO, Oc1_Pin, GPIO_Mode_AF_PP);
    GPIO_Init_Custom(Oc4_GPIO, Oc4_Pin, GPIO_Mode_AF_PP);
    
    // 确定使用哪个定时器（这里假设使用TIM2）
    RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM2, ENABLE);
    current_pwm_timer = TIM2;
    
    // 配置定时器
    TIM_TimeBaseInitTypeDef TIM_TimeBaseStructure;
    TIM_TimeBaseStructure.TIM_Period = arr;
    TIM_TimeBaseStructure.TIM_Prescaler = psc;
    TIM_TimeBaseStructure.TIM_ClockDivision = 0;
    TIM_TimeBaseStructure.TIM_CounterMode = TIM_CounterMode_Up;
    TIM_TimeBaseInit(current_pwm_timer, &TIM_TimeBaseStructure);
    
    // 配置PWM
    TIM_OCInitTypeDef TIM_OCInitStructure;
    TIM_OCInitStructure.TIM_OCMode = TIM_OCMode_PWM1;
    TIM_OCInitStructure.TIM_OutputState = TIM_OutputState_Enable;
    TIM_OCInitStructure.TIM_Pulse = 0;
    TIM_OCInitStructure.TIM_OCPolarity = TIM_OCPolarity_High;
    
    TIM_OC1Init(current_pwm_timer, &TIM_OCInitStructure);
    TIM_OC4Init(current_pwm_timer, &TIM_OCInitStructure);
    
    TIM_OC1PreloadConfig(current_pwm_timer, TIM_OCPreload_Enable);
    TIM_OC4PreloadConfig(current_pwm_timer, TIM_OCPreload_Enable);
    
    TIM_Cmd(current_pwm_timer, ENABLE);
}

void PWM_SetOc1(uint16_t value)
{
    if(current_pwm_timer)
        TIM_SetCompare1(current_pwm_timer, value);
}

void PWM_SetOc4(uint16_t value)
{
    if(current_pwm_timer)
        TIM_SetCompare4(current_pwm_timer, value);
}

// 编码器相关函数
void Encoder_Init(GPIO_TypeDef* EN1_GPIO, uint16_t EN1_Pin,
                 GPIO_TypeDef* EN2_GPIO, uint16_t EN2_Pin,
                 uint16_t arr, uint16_t psc)
{
    // 配置GPIO
    GPIO_Init_Custom(EN1_GPIO, EN1_Pin, GPIO_Mode_IN_FLOATING);
    GPIO_Init_Custom(EN2_GPIO, EN2_Pin, GPIO_Mode_IN_FLOATING);
    
    // 使用TIM3作为编码器
    RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM3, ENABLE);
    current_encoder_timer = TIM3;
    
    TIM_TimeBaseInitTypeDef TIM_TimeBaseStructure;
    TIM_TimeBaseStructure.TIM_Period = arr;
    TIM_TimeBaseStructure.TIM_Prescaler = psc;
    TIM_TimeBaseStructure.TIM_ClockDivision = TIM_CKD_DIV1;
    TIM_TimeBaseStructure.TIM_CounterMode = TIM_CounterMode_Up;
    TIM_TimeBaseInit(current_encoder_timer, &TIM_TimeBaseStructure);
    
    TIM_EncoderInterfaceConfig(current_encoder_timer, TIM_EncoderMode_TI12, TIM_ICPolarity_Rising, TIM_ICPolarity_Rising);
    TIM_Cmd(current_encoder_timer, ENABLE);
}

int16_t Encoder_GetRight(void)
{
    if(current_encoder_timer)
    {
        int16_t value = (int16_t)TIM_GetCounter(current_encoder_timer);
        TIM_SetCounter(current_encoder_timer, 0);
        return value;
    }
    return 0;
}

void Encoder_SetRight(int16_t* Temp)
{
    if(current_encoder_timer && Temp)
        *Temp = Encoder_GetRight();
} 