#ifndef __QXA51_H__
#define __QXA51_H__

// 电机驱动IO定义
sbit IN1 = P1^2;	//值为1, 左电机反转
sbit IN2 = P1^3;	//值为1, 左电机正转
sbit IN3 = P1^6;	//值为1, 右电机正转
sbit IN4 = P1^7;	//值为1, 右电机反转
sbit EN1 = P1^4;	//值为1, 左电机使能
sbit EN2 = P1^5;	//值为1, 右电机使能

// 智能识别
sbit left_led1 = P3^3;		//左寻迹信号为0 没有识别到黑线 为1识别到黑线
sbit right_led1 = P3^2;		//右寻迹信号为0 没有识别到黑线 为1识别到黑线
sbit left_led2 = P3^4;		//左避障信号为0 识别障碍物 为1没有识别到障碍物
sbit right_led2 = P3^5;		//右避障信号为0 识别障碍物 为1没有识别到障碍物

// 近光灯
sbit head_light = P2^2;		// 1 为点亮

//寻光传感器
sbit left_sensor = P2^7;  //左寻光传感器   为低则表示识别到光源
sbit front_sensor = P2^5; //前寻光传感器   为低则表示识别到光源
sbit right_sensor = P2^6; //右寻光传感器   为低则表示识别到光源

bit Right_PWM_ON=1;	           //右电机PWM开关
bit Left_PWM_ON =1;			   //左电机PWM开关
											
/*按键定义*/
sbit key_s2 = P3^0;
sbit key_s3 = P3^1;
sbit beep = P2^3;//蜂鸣器
sbit Servo = P3^6;//舵机接口

sbit voice = P1^0;//接声控模块

#define FOSC 11059200L //系统晶振频率
#define BAUD 9600 //UART 波特率

#define left_motor_en		EN1 = 1				// 左电机使能
#define left_motor_stop		IN1 = 0, IN2 = 0	// 左电机停止
#define	right_motor_en		EN2 = 1				// 右电机使能
#define	right_motor_stop	IN3 = 0, IN4 = 0	// 右电机停止

#define left_motor_go		IN1 = 0, IN2 = 1	// 左电机正转
#define left_motor_back		IN1 = 1, IN2 = 0	// 左电机反转
#define right_motor_go		IN3 = 1, IN4 = 0	// 右电机正转
#define right_motor_back	IN3 = 0, IN4 = 1	// 右电机反转

#endif