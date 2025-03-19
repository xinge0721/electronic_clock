#include "IO_Core.h" 
#include "assert.h"

// �����붨ʱ����ӳ�������STM32F103C8T6�ֲ����ã�
const TIM_GPIO_Mapping timMap[] = {
    // TIM1��APB2��
    {GPIOA, GPIO_Pin_8,  TIM1}, // TIM1_CH1
    {GPIOA, GPIO_Pin_9,  TIM1}, // TIM1_CH2
    {GPIOA, GPIO_Pin_10, TIM1}, // TIM1_CH3
    {GPIOA, GPIO_Pin_11, TIM1}, // TIM1_CH4
    
    // TIM2��APB1��
    {GPIOA, GPIO_Pin_0,  TIM2}, // TIM2_CH1
    {GPIOA, GPIO_Pin_1,  TIM2}, // TIM2_CH2
    {GPIOA, GPIO_Pin_2,  TIM2}, // TIM2_CH3
    {GPIOA, GPIO_Pin_3,  TIM2}, // TIM2_CH4
    
    // TIM3��APB1��
    {GPIOA, GPIO_Pin_6,  TIM3}, // TIM3_CH1
    {GPIOA, GPIO_Pin_7,  TIM3}, // TIM3_CH2
    {GPIOB, GPIO_Pin_0,  TIM3}, // TIM3_CH3
    {GPIOB, GPIO_Pin_1,  TIM3}, // TIM3_CH4
    
    // TIM4��APB1��
    {GPIOB, GPIO_Pin_6,  TIM4}, // TIM4_CH1
    {GPIOB, GPIO_Pin_7,  TIM4}, // TIM4_CH2
    {GPIOB, GPIO_Pin_8,  TIM4}, // TIM4_CH3
    {GPIOB, GPIO_Pin_9,  TIM4}, // TIM4_CH4
};

// ��ʱ����ѯ����
TIM_TypeDef* GetTimerFromGPIO(GPIO_TypeDef* GPIOx, uint16_t GPIO_Pin)
{
    for(uint8_t i=0; i<sizeof(timMap)/sizeof(timMap[0]); i++)
    {
        if(timMap[i].GPIOx == GPIOx && timMap[i].Pin == GPIO_Pin)
        {
            return timMap[i].TIMx;
        }
    }
    return NULL;
}


uint32_t GetGpioClock(GPIO_TypeDef* GPIOx) {
	if 			(GPIOx == GPIOA) return RCC_APB2Periph_GPIOA;
	else if (GPIOx == GPIOB) return RCC_APB2Periph_GPIOB;
	else if (GPIOx == GPIOC) return RCC_APB2Periph_GPIOC;
	else if (GPIOx == GPIOD) return RCC_APB2Periph_GPIOD;
	else if (GPIOx == GPIOE) return RCC_APB2Periph_GPIOE;
	else if (GPIOx == GPIOF) return RCC_APB2Periph_GPIOF;
	else if (GPIOx == GPIOG) return RCC_APB2Periph_GPIOG;
	else return 0; // ������
}

uint32_t GetTIMClock(TIM_TypeDef* TIMx) {
	if 			(TIMx == TIM2) return RCC_APB1Periph_TIM2;
	else if (TIMx == TIM3) return RCC_APB1Periph_TIM3;
	else if (TIMx == TIM4) return RCC_APB1Periph_TIM4;
	else if (TIMx == TIM5) return RCC_APB1Periph_TIM5;
	else if (TIMx == TIM6) return RCC_APB1Periph_TIM6;
	else if (TIMx == TIM7) return RCC_APB1Periph_TIM7;
	else if (TIMx == TIM12) return RCC_APB1Periph_TIM12;
	else if (TIMx == TIM13) return RCC_APB1Periph_TIM13;
	else if (TIMx == TIM14) return RCC_APB1Periph_TIM14;
	else return 0; // ������
}
	GPIO::GPIO(GPIO_TypeDef* _GPIOx, uint16_t _Pin, GPIOMode_TypeDef mode)
	: GPIOx(_GPIOx),
	Pin(_Pin) 
{
	RCC_APB2PeriphClockCmd(GetGpioClock(GPIOx), ENABLE);

	GPIO_InitTypeDef init;
	init.GPIO_Pin = Pin;
	init.GPIO_Speed = GPIO_Speed_50MHz;

	init.GPIO_Mode = mode;

	GPIO_Init(GPIOx, &init);
}

	// ͨ��IO����
void GPIO::Write(bool state) {
GPIO_WriteBit(GPIOx, Pin, state ? Bit_SET : Bit_RESET);
}

bool GPIO::Read(void) {
return GPIO_ReadInputDataBit(GPIOx, Pin);
}
	
#include "PWM.h"

PWM::PWM(GPIO_TypeDef* _Oc1, u16 _Oc1Pin,
					GPIO_TypeDef* _Oc4, u16 _Oc4Pin,
					u16 arr,u16 psc)
					:Oc1(_Oc1,_Oc1Pin,GPIO_Mode_AF_PP),
					Oc4(_Oc4,_Oc4Pin,GPIO_Mode_AF_PP)
{
		
		TIM_TypeDef* OC1_tim = GetTimerFromGPIO(_Oc1, _Oc1Pin);
		TIM_TypeDef* Oc4_tim = GetTimerFromGPIO(_Oc4, _Oc4Pin);

		// ��������Ƿ�ӳ�䵽��Ч�Ķ�ʱ��
		if (OC1_tim == NULL || Oc4_tim == NULL) {
				assert(0 && "Invalid PWM pin mapping!"); // ��ȷ����ԭ��
				return;
		}

		// ����Ƿ�ʹ��ͬһ��ʱ��
		if (OC1_tim != Oc4_tim) {
				assert(0 && "Oc1 and Oc4 must use the same timer!");
				return;
		}

		this->pwm_tim = OC1_tim; // ֱ��ʹ��Oc1_tim����ȷ��Oc1_tim == Oc4_tim��

		// ���ݶ�ʱ������ʹ��ʱ��
		if (pwm_tim == TIM1) {
				RCC_APB2PeriphClockCmd(RCC_APB2Periph_TIM1, ENABLE);
		} else {
				RCC_APB1PeriphClockCmd(GetTIMClock(pwm_tim), ENABLE);
		}

		TIM_TimeBaseInitTypeDef  TIM_TimeBaseStructure;  // ���嶨ʱ��ʱ���׼��ʼ���ṹ�����

		TIM_TimeBaseStructure.TIM_ClockDivision = TIM_CKD_DIV1;
		TIM_TimeBaseStructure.TIM_CounterMode = TIM_CounterMode_Up;
		TIM_TimeBaseStructure.TIM_Period = arr;
		TIM_TimeBaseStructure.TIM_Prescaler = psc;
		TIM_TimeBaseStructure.TIM_RepetitionCounter = 0;
		
		TIM_TimeBaseInit(pwm_tim, &TIM_TimeBaseStructure);  // ��ʼ��TIM
		
    TIM_OCInitTypeDef TIM_OCInitStructure;
    TIM_OCStructInit(&TIM_OCInitStructure); // ��ʼ��TIM_OCInitStruct�ṹ������г�Ա
    TIM_OCInitStructure.TIM_OCMode = TIM_OCMode_PWM1; // ����ΪPWMģʽ1
    TIM_OCInitStructure.TIM_OCPolarity = TIM_OCPolarity_High; // ������Ը�
    TIM_OCInitStructure.TIM_OutputState = TIM_OutputState_Enable; // �Ƚ����ʹ��
    TIM_OCInitStructure.TIM_Pulse = 0; // ��ʼռ�ձ�����Ϊ0

		TIM_OC1Init(pwm_tim, &TIM_OCInitStructure);  // GPIOA_Pin8 -> TIM1_CH1
		TIM_OC4Init(pwm_tim, &TIM_OCInitStructure);  // GPIOA_Pin11 -> TIM1_CH4
		
    TIM_OC1PreloadConfig(pwm_tim, TIM_OCPreload_Enable); // ʹ��ͨ��2������Ƚ�Ԥװ�ؼĴ���
    TIM_OC4PreloadConfig(pwm_tim, TIM_OCPreload_Enable); // ʹ��ͨ��3������Ƚ�Ԥװ�ؼĴ���

    // ʹ��TIM5
    TIM_Cmd(pwm_tim, ENABLE);
		if (pwm_tim == TIM1) 
    TIM_CtrlPWMOutputs(TIM1, ENABLE);
}	

void PWM::setOc1(u16 value) 
{
		 TIM_SetCompare1(pwm_tim, value);
}

void PWM::setOc4(u16 value) 
{
		 TIM_SetCompare4(pwm_tim, value);
}

encoder::encoder(GPIO_TypeDef* _EN1, u16 _EN1Pin,
                GPIO_TypeDef* _EN2, u16 _EN2Pin,
                u16 arr, u16 psc)
    : EN1(_EN1, _EN1Pin, GPIO_Mode_IN_FLOATING),  // �ڲ�����
      EN2(_EN2, _EN2Pin, GPIO_Mode_IN_FLOATING) 
{
		Delay_ms(20);
	
    TIM_TypeDef* EN1_tim = GetTimerFromGPIO(_EN1, _EN1Pin);
    TIM_TypeDef* EN2_tim = GetTimerFromGPIO(_EN2, _EN2Pin);

    // ��������Ƿ�ӳ�䵽��Ч�Ķ�ʱ��
    if (EN1_tim == NULL || EN2_tim == NULL) {
        assert(0 && "Invalid encoder pin mapping!");
        return;
    }

    // ����Ƿ�ʹ��ͬһ��ʱ��
    if (EN1_tim != EN2_tim) {
        assert(0 && "EN1 and EN2 must use the same timer!");
        return;
    }

    this->timx = EN1_tim;

    // ʹ�ܶ�ʱ��ʱ��
    if (timx == TIM1) {
        RCC_APB2PeriphClockCmd(RCC_APB2Periph_TIM1, ENABLE);
    } else {
        RCC_APB1PeriphClockCmd(GetTIMClock(timx), ENABLE);
    }

    // ��ʱ��ʱ����ʼ����������ģʽ�Զ������������
    TIM_TimeBaseInitTypeDef TIM_TimeBaseInitStructure;
    TIM_TimeBaseStructInit(&TIM_TimeBaseInitStructure);
    TIM_TimeBaseInitStructure.TIM_Period = arr;        // �Զ���װֵ
    TIM_TimeBaseInitStructure.TIM_Prescaler = psc;     // Ԥ��Ƶ��
    TIM_TimeBaseInitStructure.TIM_ClockDivision = TIM_CKD_DIV1;
    TIM_TimeBaseInitStructure.TIM_RepetitionCounter = 0;
    TIM_TimeBaseInit(timx, &TIM_TimeBaseInitStructure);

    // �������ӿ����ã�˫���ش�����4��Ƶ��
    TIM_EncoderInterfaceConfig(
        timx,
        TIM_EncoderMode_TI12,
        TIM_ICPolarity_Rising,   // CH1 ˫����
        TIM_ICPolarity_Rising    // CH2 ˫����
    );

    // ���벶���˲������ã��ʶ��˲���
    TIM_ICInitTypeDef TIM_ICInitStructure;
    TIM_ICStructInit(&TIM_ICInitStructure);
    TIM_ICInitStructure.TIM_ICFilter = 10; // �����˲�ǿ��

    // ����ͨ��1
    TIM_ICInitStructure.TIM_Channel = TIM_Channel_1;
    TIM_ICInit(timx, &TIM_ICInitStructure);

    // ����ͨ��2
    TIM_ICInitStructure.TIM_Channel = TIM_Channel_2;
    TIM_ICInit(timx, &TIM_ICInitStructure);
		
//    // ����ͨ��3
//    TIM_ICInitStructure.TIM_Channel = TIM_Channel_3;
//    TIM_ICInit(timx, &TIM_ICInitStructure);

//    // ����ͨ��4
//    TIM_ICInitStructure.TIM_Channel = TIM_Channel_4;
//    TIM_ICInit(timx, &TIM_ICInitStructure);
		
		
		TIM_ITConfig(timx,TIM_IT_Update,ENABLE);//������������жϱ�־λ
		
		TIM_SetCounter(timx,0);//���㶨ʱ������ֵ
			// ������ʱ��
    TIM_Cmd(timx, ENABLE);
}

int16_t encoder::Right(void) {
		int16_t Temp;
		Temp = TIM_GetCounter(timx);
		TIM_SetCounter(timx, 0);
		return Temp;
}

void encoder::Right(int16_t &Temp) {
		Temp += TIM_GetCounter(timx);
		TIM_SetCounter(timx, 0);
}

//void TIM2_IRQHandler(void)
//{
//	if(TIM_GetITStatus(TIM2,TIM_IT_Update)!=0)
//	{
//		TIM_ClearITPendingBit(TIM2,TIM_IT_Update);
//	}
//}
//	
//void TIM3_IRQHandler(void)
//{
//	if(TIM_GetITStatus(TIM3,TIM_IT_Update)!=0)
//	{
//		TIM_ClearITPendingBit(TIM3,TIM_IT_Update);
//	}
//}

//void TIM4_IRQHandler(void)
//{
//	if(TIM_GetITStatus(TIM4,TIM_IT_Update)!=0)
//	{
//		TIM_ClearITPendingBit(TIM4,TIM_IT_Update);
//	}
//}
