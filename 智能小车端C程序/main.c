#include <reg52.h> //51头文件
#include <intrins.h>   //包含nop等系统函数
#include "QXA51.h"//QX-A51智能小车配置文件
unsigned char pwm_left_val = 140;//左电机占空比值 取值范围0-170，0最快
unsigned char pwm_right_val = 150;//右电机占空比值取值范围0-170 ,0最快
unsigned char pwm_t;//周期
unsigned char control=0X01;//车运动控制全局变量，默认开机为停车状态
int na;
int soundId = 0;
/******************************
超声波定义
*******************************/
sbit RX = P2^0;//ECHO超声波模块回响端
sbit TX = P2^1;//TRIG超声波模块触发端
unsigned long S = 0;//距离
bit      flag = 0;//超出测量范围标志位
unsigned char count=0;
unsigned char SEH_count = 15;  //0-30, 0为正右方, 30为正左方
// 显示管
sbit DU  = P2^6;   //数码管段选
sbit WE  = P2^7;   //数码管位选
unsigned int  time = 0;//传输时间
unsigned int  timer=0;

void Delay10us(unsigned char i)    	//10us延时函数 启动超声波模块时使用
{ 
   unsigned char j; 
	do{ 
		j = 10; 
		do{ 
			_nop_(); 
		}while(--j); 
	}while(--i); 
}

void delay(unsigned int z)//毫秒级延时
{
	unsigned int x,y;
	for(x = z; x > 0; x--)
		for(y = 114; y > 0 ; y--);
}	
/*小车前进*/
void forward()
{
	left_motor_go; //左电机前进
	right_motor_go; //右电机前进
}
/*小车左转*/
void left_run()
{
	left_motor_stop; //左电机停止
	right_motor_go; //右电机前进	
}
/*小车右转*/
void right_run()
{
	right_motor_stop;//右电机停止
	left_motor_go;    //左电机前进
}

/*PWM控制使能 小车后退*/
void backward()
{
	left_motor_back; //左电机后退
	right_motor_back; //右电机后退	
}
/*PWM控制使能 小车高速右转*/
void right_rapidly()
{
	left_motor_go;
	right_motor_back;	
}
/*小车高速左转*/
void left_rapidly()
{
	right_motor_go;
	left_motor_back;	
}
//小车停车
void stop()
{
		left_motor_stop; //左电机后退
	right_motor_stop; //右电机后退	
}


void keyscan()
{
	for(;;)	//死循环
	{
		if(key_s2 == 0)// 实时检测S2按键是否被按下
		{
			delay(5); //软件消抖
			if(key_s2 == 0)//再检测S2是否被按下
			{
				while(!key_s2);//松手检测
				beep = 0;	//使能有源蜂鸣器
				delay(200);//200毫秒延时
				beep = 1;	//关闭有源蜂鸣器
				break;		//退出FOR死循环
			}
		}
	}	
}

//初始化
void Init(void)
{
   	EA = 1;	    //开总中断

   	SCON |= 0x50; 	// SCON: 模式1, 8-bit UART, 使能接收
	T2CON |= 0x34; //设置定时器2为串口波特率发生器并启动定时器2
	TL2 = RCAP2L = (65536-(FOSC/32/BAUD)); //设置波特率
	TH2 = RCAP2H = (65536-(FOSC/32/BAUD)) >> 8;
	ES= 1; 			//打开串口中断

	TMOD |= 0x01;//定时器0工作模块1,16位定时模式。T0用测ECH0脉冲长度
	TH0 = 0;
	TL0 = 0;//T0,16位定时计数用于记录ECHO高电平时间
	ET0 = 1;//允许定时器0中断

	TMOD |= 0x20;	//定时器1，8位自动重装模块
	TH1 = 220;     
    TL1 = 220;	   //11.0592M晶振下占空比最大比值是256,输出100HZ
	TR1 = 1;//启动定时器1
	ET1 = 1;//允许定时器1中断
}

/**************************************************
超声波程序
***************************************************/
void  StartModule() 		         //启动超声波模块
{
	TX=1;			                     //启动一次模块
  	Delay10us(2);
  	TX=0;
}

/*计算超声波所测距离*/
void Conut(void)
{
	time=TH0*256+TL0;
	TH0=0;
	TL0=0;
	S=(float)(time*1.085)*0.17;     //算出来是MM
	if((S>=7000)||flag==1) 
	 {	 
	  flag=0;
	 }
}

/*超声波避障*/
void	Avoid()
{
	if(S < 400)//设置避障距离 ，单位毫米	刹车距离
	{
		stop();//停车
		backward();//后退
		delay(100);//后退时间越长、距离越远。后退是为了留出车辆转向的空间
		do{
			left_rapidly();//高速左转
			delay(90);//时间越长 转向角度越大，与实际行驶环境有关
			stop();//停车
			delay(200);//时间越长 停止时间越久长
			forward();

			StartModule();	//启动模块测距，再次判断是否
			while(!RX);		//当RX（ECHO信号回响）为零时等待
			TR0=1;			    //开启计数
			while(RX);			//当RX为1计数并等待
			TR0=0;				//关闭计数
			Conut();			//计算距离
			}while(S < 280);//判断前面障碍物距离
	}
	else
	{
		forward();//前进
	}	
}


//黑线寻迹
void BlackLine()
{
		//为0 没有识别到黑线 为1识别到黑线
	if(left_led1 == 1 && right_led1 == 1)//左右寻迹探头识别到黑线
	{
		forward();//前进
	}
	else
	{
		if(left_led1 == 1 && right_led1 == 0)//小车右边出线，左转修正
		{
			left_run();//左转
		}
		if(left_led1 == 0 && right_led1 == 1)//小车左边出线，右转修正
		{
			right_run();//右转
		}		
	}	
}

//红外避障
void IRAvoid()
{
				//为0 识别障碍物 为1没有识别到障碍物
	if(left_led2 == 1 && right_led2 == 1)//左右都没识别到障碍物
	{
		pwm_left_val = 140;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 150;//右电机占空比值取值范围0-170 ,0最快
		forward();//前进
	}
	if(left_led2 == 1 && right_led2 == 0)//小车右侧识别到障碍物，左转躲避
	{
		pwm_left_val = 180;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 110;//右电机占空比值取值范围0-170 ,0最快
		left_run();//左转
		delay(40);//左转40毫秒（实现左小弯转）
	}
	if(left_led2 == 0 && right_led2 == 1)//小车左侧识别到障碍物，右转躲避
	{
		pwm_left_val = 100;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 180;//右电机占空比值取值范围0-170 ,0最快
		right_run();//右转
		delay(40);//右转40毫秒（实现右小弯转）
	}
	if(left_led2 == 0 && right_led2 == 0) //小车左右两侧都识别到障碍物，后退掉头
	{
		pwm_left_val = 150;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 160;//右电机占空比值取值范围0-170 ,0最快	
		backward();//后退
		delay(100);//后退的时间影响后退的距离。后退时间越长、后退距离越远。
		pwm_left_val = 140;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 150;//右电机占空比值取值范围0-170 ,0最快
		right_run();	//右转
		delay(180);//延时时间越长，转向角度越大。	
	}		
}

// 超声波魔术手
void UltrasonicHand() {
	StartModule();	//启动模块测距
	while(!RX);		//当RX（ECHO信号回响）为零时等待
	TR0=1;			    //开启计数
	while(RX);			//当RX为1计数并等待
	TR0=0;				//关闭计数
	Conut();			//计算距离
	if(S > 150)//设置避障距离（单位毫米）
	{
		forward();
	}
	else
	{
		backward();	
	}
	delay(65);			//测试周期不低于60MS
}

//超声波避障
void Ultrasonic()
{
	StartModule();	//启动模块测距
	while(!RX);		//当RX（ECHO信号回响）为零时等待
	TR0=1;			    //开启计数
	while(RX);			//当RX为1计数并等待
	TR0=0;				//关闭计数
	Conut();			//计算距离
	Avoid();			//避障
	delay(65);			//测试周期不低于60MS	
}

//红外物体跟随
void IRTracking()
{
	// 为0 识别障碍物 为1没有识别到障碍物
	if(left_led2 == 0 && right_led2 == 0)//左右识别到障碍物，前进跟随
	{
		forward();//前进
	}
	else if(left_led2 == 1 && right_led2 == 0)//小车右侧识别到障碍物，右转跟随
	{
		right_run();//右转
	}
	else if(left_led2 == 0 && right_led2 == 1)//小车左侧识别到障碍物，左转跟随
	{
		left_run();//左转
	}
	else {
		stop();
	}
}

// 自动寻光
void findLight() {
	//为0 识别到光源 为1没有识别到光源
	if(front_sensor == 0 && left_sensor == 1 && right_sensor == 1)//只有前方寻光传感器识别到光源才前进
	{
		pwm_left_val = 100;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 100;//右电机占空比值取值范围0-170 ,0最快
		forward();//前进
	}
	else if(front_sensor == 0 && left_sensor == 0 && right_sensor == 1)//前面和左面同时有信号
	{
		//用PWN信号转向
		pwm_left_val = 170;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 70;//右电机占空比值取值范围0-170 ,0最快
		left_motor_go; //右电机前进
		right_motor_go; //右电机前进
	}
	else if(front_sensor == 0 && left_sensor == 1 && right_sensor == 0)//前面和右面同时有信号
	{
		//用PWN信号转向
		pwm_left_val = 70;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 170;//右电机占空比值取值范围0-170 ,0最快
		left_motor_go; //右电机前进
		right_motor_go; //右电机前进
	}
	else if(front_sensor == 1 && left_sensor == 1 && right_sensor == 0)//只有右边寻光传感器识别到光源才右转
	{
		pwm_left_val = 100;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 100;//右电机占空比值取值范围0-170 ,0最快	
		right_run();//右转
	}
	else if(front_sensor == 1 && left_sensor == 0 && right_sensor == 1)//只有左边寻光传感器识别到光源才左转
	{
		pwm_left_val = 100;//左电机占空比值 取值范围0-170，0最快
		pwm_right_val = 100;//右电机占空比值取值范围0-170 ,0最快
		left_run();//左转
	}
	else if(front_sensor == 0 && left_sensor == 0 && right_sensor == 0)	// 前面和左面和右面都有信号 选择前进
	{
		forward();	// 前进
	}
	else
	{
		stop();
	}
}

// 声感
void sound() {
	if(voice == 0){
		soundId++;
		if(soundId % 2 == 0) {
		  	forward();
			delay(300);	 
			stop();
		} else if(soundId % 3 == 0){

		    backward();
			delay(300);
			stop();
		} else if(soundId % 5 == 0) {
			left_rapidly();
			delay(300);
			stop();
		} else {
			right_rapidly();
			delay(300);
			stop();
		}
	} else {
		stop();
	}
}

void main()
{
	Init();//定时器、串口初始化
	while(1)
	{
		if(control==0X66)//	停车命令
	   {
	   	stop();	//  停车 
	   }
	    // 云台旋转	 SEH_count -> [0,30]
		// 0x70 -> 0x8D, 112 ->  142
	   	if(control >= 0x70 && control <= 0x8D) {
			//na = (int)control;
	   		//SEH_count = na - 112;	
	   	}
		switch(control)
		{
			case 0X02:	forward();			break;	// 前进
			case 0X03:	backward();			break;	// 后退
			case 0X04:	left_run();			break;	// 左转
			case 0X05:	right_run();		break;	// 右转
			case 0X01:	stop();				break;	// 停车
			case 0X06:	left_rapidly();		break;	// 左旋转
			case 0X07:	right_rapidly();	break;	// 右旋转
			case 0X08:	beep = 0;			break;	// 鸣笛
			case 0X09:	beep = 1;			break;	// 停止鸣笛
			case 0x0A:	head_light = 1;		break;	// 前照灯
			case 0x0B:	head_light = 0;		break;	// 熄灯
			case 0X0C:	Ultrasonic();		break;	// 超声波避障
			case 0X0D:	BlackLine();		break;	// 黑线寻迹
			case 0X0E:	IRAvoid();			break;	// 红外避障
			case 0X0F:	IRTracking();		break;	// 红外-跟随物体
			case 0X10:	findLight();		break;	// 自动寻光
			case 0X11:  UltrasonicHand();	break;	// 超声波魔术手
			case 0X12:  sound();			break;	// 声感
		}
	}
}

//定时器0中断
void timer0() interrupt 1
{
	flag = 1;					 
}
  
void timer1() interrupt 3 		 //T1中断用来计数器溢出
{
	pwm_t++;//周期计时加
	if(pwm_t == 255)
		pwm_t = EN1 = EN2 = 0;
	if(pwm_left_val == pwm_t)//左电机占空比	
		EN1 = 1;		
	if(pwm_right_val == pwm_t)//右电机占空比
		EN2 = 1;

   	TR1 = 0;      //关闭定时器1
    TH1 = 0xff;   //重装初值0.1ms
    TL1 = 0xa3;
    //舵机1
    if(count <= SEH_count) //控制占空比左右
    {
        //如果count的计数小于（5-25）也就是0.5ms-2.5ms则这段小t周期持续高电平。产生方波
        //Servo = 1;
    }
    else
    {
        //Servo = 0;
		//TR0 = 0; //关定时器0， 若不关有BUG, 转动几次后就短路了
    }

	//count++;
    if (count >= 200) //T = 20ms则定时器计数变量清0
    {
        //count = 0;
    }
	TR1 = 1; //开启定时器1 			 
}
 /******************************************************************/
/* 串口中断程序*/
/******************************************************************/
void UART_SER () interrupt 4
{
	unsigned char n; 	//定义临时变量

	if(RI) 		//判断是接收中断产生
	{
		RI=0; 	//标志位清零
		n=SBUF; //读入缓冲区的值

		control=n;
		// 小车挡位
		switch(n)
		{
			case 0x21: pwm_left_val = 170; pwm_right_val = 170;	break;	// 一档
			case 0x22: pwm_left_val = 136; pwm_right_val = 136;	break;	// 二档
			case 0x23: pwm_left_val = 102; pwm_right_val = 102;	break;	// 三档
			case 0x24: pwm_left_val = 68; pwm_right_val = 68;	break;	// 四档
			case 0x25: pwm_left_val = 0; pwm_right_val = 0;		break;	// 五档
		}
	
	}

}