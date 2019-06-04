package com.example.carcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class SmartCar extends Activity {
	private int light_on = 0;		// 判断是否开灯
	private int cloud_angle = 127;	// 云台初始角度 112 + 15
	/*普通按键定义*/
	private ImageView sound;
	private ImageView findLight;
	private ImageView hand;
	private ImageView light;
	private ImageView beep;
	private ImageView up;
	private ImageView down;
	private ImageView left;
	private ImageView right;
	private ImageView turnLeft;
	private ImageView turnRight;
	private ImageView leftSpeedUp;
	private ImageView leftSpeedDown;
	private ImageView rightSpeedUp;
	private ImageView rightSpeedDown;
	private ImageView stop;
	private TextView leftSpeed;
	private TextView rightSpeed;
	private int level=1;
	/*功能性按键定义*/
	private ImageView blackLine;
	private ImageView infraredRay;
	private ImageView ultrasonic;
	private ImageView trace;
	/*蓝牙部分定义*/
	private TextView status;
	private final static int REQUEST_CONNECT_DEVICE = 1; // 宏定义查询设备句柄
	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号
	private InputStream is; // 输入流，用来接收蓝牙数据
	protected static final int REQUEST_ENABLE = 0;
	BluetoothDevice _device = null; // 蓝牙设备
	BluetoothSocket _socket = null; // 蓝牙通信socket
	boolean _discoveryFinished = false;
	boolean bRun = true;
	boolean bThread = false;
	boolean hex=true;
	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter(); // 获取本地蓝牙适配器，即蓝牙设备
	private ImageView bluetoothBtn;
	private long firstTime=0;  //记录第几次点击返回
//	private int speed_l = 51;
//	private int speed_r = 151;
	private int left_level = 1;
	private int right_level = 1;
	

	@Override
	// onCreate activity的生命周期
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//获取全屏状态
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);//设置屏幕状态为按重力感应旋转
		// 沉浸式
		  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
	            Window window = getWindow();
	            // Translucent status bar
	            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
	            // Translucent navigation bar
	            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
	        }
		setContentView(R.layout.activity_car);
		
		/*初始化按键及监听状态*/
		sound = (ImageView) findViewById(R.id.sound);
		sound.setOnClickListener(new soundOnClickListener());
		findLight = (ImageView) findViewById(R.id.findLight);
		findLight.setOnClickListener(new findLightOnClickListener());
		hand = (ImageView) findViewById(R.id.hand);
		hand.setOnClickListener(new handOnClickListener());
		light = (ImageView) findViewById(R.id.light);
		light.setOnClickListener(new lightOnClickListener());
		stop = (ImageView) findViewById(R.id.pause);
		stop.setOnClickListener(new stopOnClickListener());
		blackLine = (ImageView) findViewById(R.id.blackLine);
		blackLine.setOnClickListener(new blackLineOnClickListener());
		infraredRay =  (ImageView) findViewById(R.id.infraredRay);
		infraredRay.setOnClickListener(new infraredRayOnClickListener());
		ultrasonic =  (ImageView) findViewById(R.id.ultrasonic);
		ultrasonic.setOnClickListener(new ultrasonicOnClickListener());
		trace =  (ImageView) findViewById(R.id.trace);
		trace.setOnClickListener(new ultrasonicFollowOnClickListener());
		beep = (ImageView) findViewById(R.id.beep);
		beep.setOnTouchListener(new beepOnTouchListener());
		leftSpeed = (TextView) findViewById(R.id.right_speed);	// R.java错误没及时更新
		rightSpeed = (TextView) findViewById(R.id.left_speed);
		up = (ImageView)findViewById(R.id.up);
		up.setOnTouchListener(new upOnTouchListener());
		down = (ImageView) findViewById(R.id.down);
		down.setOnTouchListener(new downOnTouchListener());
		left = (ImageView) findViewById(R.id.left);
		left.setOnTouchListener(new leftOnTouchListener());
		right = (ImageView) findViewById(R.id.right);
		right.setOnTouchListener(new rightOnTouchListener());
		turnLeft = (ImageView) findViewById(R.id.turnLeft);
		turnLeft.setOnTouchListener(new turnLeftOnTouchListener());
		turnRight = (ImageView) findViewById(R.id.turnRight);
		turnRight.setOnTouchListener(new turnRightOnTouchListener());
		bluetoothBtn = (ImageView) findViewById(R.id.bluetooth);
		leftSpeedUp = (ImageView) findViewById(R.id.left_faster);
		leftSpeedUp.setOnClickListener(new leftSpeedUpOnClickListener());
		leftSpeedDown = (ImageView) findViewById(R.id.left_slower);
		leftSpeedDown.setOnClickListener(new leftSpeedDownOnClickListener());
		rightSpeedUp = (ImageView) findViewById(R.id.right_faster);
		rightSpeedUp.setOnClickListener(new rightSpeedUpOnClickListener());
		rightSpeedDown = (ImageView) findViewById(R.id.right_slower);
		rightSpeedDown.setOnClickListener(new rightSpeedDownOnClickListener());
		bluetoothBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				connect();
			}
		});
		
		if (_bluetooth == null) {
			Toast.makeText(this, "本机没有找到蓝牙硬件或驱动！", Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}
		if (_bluetooth.isEnabled() == false) { // 如果蓝牙服务不可用则提示	
			Toast.makeText(SmartCar.this, " 打开蓝牙中...",
					Toast.LENGTH_SHORT).show();	
					
			new Thread() {
				public void run() {
					if (_bluetooth.isEnabled() == false) {
						_bluetooth.enable();
					}
				}
			}.start();	
		}
		
		// 若蓝牙还未打开
		if (_bluetooth.isEnabled() == false)
		{		
			Toast.makeText(SmartCar.this, "等待蓝牙打开，5秒后，尝试连接！", Toast.LENGTH_SHORT).show();
			new Handler().postDelayed(new Runnable(){   //延迟执行
				@Override
				public void run(){
					if (_bluetooth.isEnabled() == false)
					{
						Toast.makeText(SmartCar.this, "自动打开蓝牙失败，请手动打开蓝牙！", Toast.LENGTH_SHORT).show();
						//询问打开蓝牙
						Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enabler, REQUEST_ENABLE);	
					}
					else
						connect(); //自动进入连接
					}
			}, 5000);
		}
		else
		{
			connect(); //自动进入连接
		}	
		// Sends(level + 10);//发送初始速度值
	}
	
	// 接收Intent活动结果，响应startActivityForResult()
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE: // 连接结果，由DeviceListActivity设置返回
			// 响应返回结果
			if (resultCode == Activity.RESULT_OK) { // 连接成功，由DeviceListActivity设置返回
				// MAC地址，由DeviceListActivity设置返回
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// 得到蓝牙设备句柄
				_device = _bluetooth.getRemoteDevice(address);
				// 用服务号得到socket
				try {
					_socket = _device.createRfcommSocketToServiceRecord(UUID
							.fromString(MY_UUID));
				} catch (IOException e) {

					Toast.makeText(this, "连接失败,无法得到Socket！"+e, Toast.LENGTH_SHORT).show();
			    	SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
					sharedata.clear();
					sharedata.commit();	 

				}
 
				
				// 连接socket
				try {
					_socket.connect();

					Toast.makeText(this, "连接" + _device.getName() + "成功！",
							Toast.LENGTH_SHORT).show();
					//tvcon.setText(_device.getName() + "\n"+ _device.getAddress());
					
					SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
					sharedata.putString(String.valueOf(0),_device.getName());
					sharedata.putString(String.valueOf(1),_device.getAddress());
					sharedata.commit();	  
					
					bluetoothBtn.setImageDrawable(getResources().getDrawable((R.drawable.bluetooth_ready)));
					
					// bluetoothBtn.setText(getResources().getString(R.string.delete));
					
			        //注册异常断开接收器  等连接成功后注册
					IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
					  this.registerReceiver(mReceiver, filter);
					  
				} catch (IOException e) {
					bluetoothBtn.setImageDrawable(getResources().getDrawable((R.drawable.bluetooth)));
					// bluetoothBtn.setText(getResources().getString(R.string.add));
					try {
						Toast.makeText(this, "连接失败！"+e, Toast.LENGTH_SHORT)
								.show();
						_socket.close();
						_socket = null;
				    	SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
						sharedata.clear();
						sharedata.commit();	 
					} catch (IOException ee) {
					}
					return;
				}

				// 打开接收线程
				try {
					is = _socket.getInputStream(); // 得到蓝牙数据输入流
				} catch (IOException e) {
					Toast.makeText(this, "异常：打开接收线程！"+e, Toast.LENGTH_SHORT).show();
					bluetoothBtn.setImageDrawable(getResources().getDrawable((R.drawable.bluetooth)));
					// bluetoothBtn.setText(getResources().getString(R.string.add));
					
			    	SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
					sharedata.clear();
					sharedata.commit();	 
					return;
				}

			}
			break;
		default:
			break;
		}
	}
	
	// 系统广播接收器
	// 动态注册广播接收器
	// 动态注册: 启动程序才接收广播
	// 静态注册: 无须启动程序也可接收广播
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {     
		@Override
        public void onReceive(Context context, Intent intent) {
			   String action = intent.getAction();
				if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {	
					disconnect();    			
            }
        }
    };
    
    public void disconnect()
	{
    	//取消注册异常断开接收器  
		this.unregisterReceiver(mReceiver);
		light_on = 0;
    	SharedPreferences.Editor sharedata = getSharedPreferences("Add", 0).edit();
		sharedata.clear();
		sharedata.commit();	 
    	
		Toast.makeText(this, "线路已断开，请重新连接！", Toast.LENGTH_SHORT).show();
		// 关闭连接socket
		try {
			bRun = false; // 一定要放在前面
			is.close();
			_socket.close();
			_socket = null;
			bRun = false;
			bluetoothBtn.setImageDrawable(getResources().getDrawable((R.drawable.bluetooth)));
			// bluetoothBtn.setText(getResources().getString(R.string.add));
		} catch (IOException e) {
		}
	}    
    
    public void connect()
	{
		if (_bluetooth.isEnabled() == false) { // 如果蓝牙服务不可用则提示
			//询问打开蓝牙
			// Intent 组件间通信的枢纽
			Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enabler, REQUEST_ENABLE);		
			return;
		}		
		
		// 如未连接设备则打开DeviceListActivity进行设备搜索				
		if (_socket == null) {
			//tvcon.setText("");
			Intent serverIntent = new Intent(SmartCar.this,
					DeviceListActivity.class); // 跳转程序设置
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); // 设置返回宏定义
		} else {
			disconnect();
		}
		return;
	
	}

    public void Sends(int v) //发送数据
    {
    	int i=0;
    	int n=0;
    	try{
    		if(_socket != null)
    		{
    		OutputStream os = _socket.getOutputStream();   //蓝牙连接输出流
    		os.write(v);	
    		}
    		else
    		{
    			Toast.makeText(SmartCar.this, "请先连接蓝牙", Toast.LENGTH_LONG).show();  			
    		}
    	
    	}catch(IOException e){  	    		
    	}  	
    }
    
    // public void debounce( callback, int delay)
    
    // onDestroy activity生命周期中的销毁阶段
	public void onDestroy() {
		super.onDestroy();
		if (_socket != null) // 关闭连接socket
			try {
				_socket.close();
			} catch (IOException e) {
			}
		
		//_bluetooth.disable(); //关闭蓝牙服务
		
		 android.os.Process.killProcess(android.os.Process.myPid()); // 终止线程
	}

	public boolean onKeyDown(int keyCode,KeyEvent event)
	{
		if(keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN){
            if (System.currentTimeMillis()-firstTime>2000){
                Toast.makeText(SmartCar.this,"再次点击返回退出",Toast.LENGTH_SHORT).show();	                
                firstTime=System.currentTimeMillis();
            }else{
                finish();
                System.exit(0);
                
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
		}
	

	// 监听停车
	class stopOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Sends(0x66);	// 停车
		}
		
	}
	// 监听前进
	class upOnTouchListener implements OnTouchListener		//获取按下状态，发送命令，松开发送停止命令
	{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN)	//按下
	        {
				Sends(0x02);	//发送前进命令	
	         }  
			if (event.getAction() == MotionEvent.ACTION_UP)		//弹起
	        {
	    	    Sends(0x66);
	        }
			//返回true  才触发ACTION_UP
			return true;
		}
	}
	
	// 监听后退
	class downOnTouchListener implements OnTouchListener	//获取按下状态，发送命令，松开发送停止命令
	{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN)	//按下
	        {
				
				Sends(0x03);	//发送后退命令	
	         }  
			if (event.getAction() == MotionEvent.ACTION_UP)		//弹起
	        {
				Sends(0x66);
	        }
			//返回true  才触发ACTION_UP
			return true;
		}
	}
	
	// 监听右转
	class rightOnTouchListener implements OnTouchListener	//获取按下状态，发送命令，松开发送停止命令
	{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN)	//按下
	        {		
			
				Sends(0x05);	//发送右转命令	
	         }  
			if (event.getAction() == MotionEvent.ACTION_UP)		//弹起
	        {
				Sends(0x66);
	        }
			//返回true  才触发ACTION_UP
			return true;
		}
	}
	
	// 监听左转
	class leftOnTouchListener implements OnTouchListener	//获取按下状态，发送命令，松开发送停止命令
	{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN)	//按下
	        {
				
				Sends(0x04);	//发送左转命令	
	         }  
			if (event.getAction() == MotionEvent.ACTION_UP)		//弹起
	        {
				Sends(0x66);
	        }
			//返回true  才触发ACTION_UP
			return true;
		}
	}
	
	// 监听顺时针旋转按钮
	class turnRightOnTouchListener implements OnTouchListener	//获取按下状态，发送命令，松开发送停止命令
	{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN)	//按下
	        {
				
				Sends(0x07);	//发送右旋转命令	
	         }  
	    	//返回true  表示事件处理完毕，会中断系统对该事件的处理。false 表示未处理完毕,继续调用onTouch即ACTION_DOWN(前), 不会执行ACTION_UP(后)
			return false;
		}
	}
	
	// 监听逆时针旋转按钮
	class turnLeftOnTouchListener implements OnTouchListener//获取按下状态，发送命令，松开发送停止命令
	{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN)	//按下
	        {
				
				Sends(0x06);	//发送左旋转命令	
	         }  
		    //false 表示未处理完毕,继续执行事件
			return false;
		}
	}
	
	// 监听右电机加速按钮
	class rightSpeedUpOnClickListener implements OnClickListener	
	{

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if(right_level < 5)
				right_level+=1;
			switch(right_level)
			{
				case 1:
					rightSpeed.setText("1档");
					break;
				case 2:
					rightSpeed.setText("2档");
					break;
				case 3:
					rightSpeed.setText("3档");
					break;
				case 4:
					rightSpeed.setText("4档");
					break;
				case 5:
					rightSpeed.setText("5档");
					break;
				default:
					right_level = 1;
					break;
			}
			Sends(right_level + 32);	//发送前进命令	
		}	
	}
	
	// 监听右电机减速按钮
	class rightSpeedDownOnClickListener implements OnClickListener	
	{

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if(right_level > 1)
				right_level-=1;
			switch(right_level)
			{	
				case 1:
					rightSpeed.setText("1档");
					break;
				case 2:
					rightSpeed.setText("2档");
					break;
				case 3:
					rightSpeed.setText("3档");
					break;
				case 4:
					rightSpeed.setText("4档");
					break;
				case 5:
					rightSpeed.setText("5档");
					break;
				default:
					right_level = 1;
					break;
			}
			Sends(right_level + 32);	//发送前进命令	
		}
	}
	
		// 监听左电机加速按钮
		class leftSpeedUpOnClickListener implements OnClickListener	
		{

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				// level不能大于5
				if(left_level < 5)
					left_level+=1;
				switch(left_level)
				{
					case 1:
						leftSpeed.setText("1档");
						break;
					case 2:
						leftSpeed.setText("2档");
						break;
					case 3:
						leftSpeed.setText("3档");
						break;
					case 4:
						leftSpeed.setText("4档");
						break;
					case 5:
						leftSpeed.setText("5档");
						break;
					default:
						left_level = 1;
						break;
				}
				Sends(left_level + 32);		
			}	
		}
		
		// 监听左电机减速按钮
		class leftSpeedDownOnClickListener implements OnClickListener	
		{

			@Override
			public void onClick(View arg0) {
				// level不能为0
				if(left_level > 1)
					left_level-=1;
				// TODO Auto-generated method stub
				switch(left_level)
				{	
					case 1:
						leftSpeed.setText("1档");
						break;
					case 2:
						leftSpeed.setText("2档");
						break;
					case 3:
						leftSpeed.setText("3档");
						break;
					case 4:
						leftSpeed.setText("4档");
						break;
					case 5:
						leftSpeed.setText("5档");
						break;
					default:
						left_level = 1;
						break;
				}
				Sends(left_level + 32);		
			}
		}
	
	// 鸣笛
	class beepOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN)	//按下
	        {
				Sends(0x08);	// 喇叭	
	         }  
			if (event.getAction() == MotionEvent.ACTION_UP)		//弹起
	        {
	    		Sends(0x09);	// 停止鸣笛
	        }
			//返回true, 才触发ACTION_UP, 表示事件结束, 不会再调用onTouch
			return true;
			}
		}
	
	// 黑线寻迹
	class blackLineOnClickListener implements OnClickListener {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Sends(0x0D);	// 黑线寻迹
		}
	}
	
	// 红外线避障
	class infraredRayOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Sends(0x0E);	// 红外避障
		}
		
	}
	
	// 超声波避障
	class ultrasonicOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Sends(0x0C);	// 超声波避障
		}
		
	}
	
	// 红外线跟随
	class ultrasonicFollowOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Sends(0x0F);	// 红外线跟随
		}
		
	}
	
	// 近光灯
	class lightOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(light_on % 2 == 0) {	// 计数点灯
				Sends(0x0A);	// 近光灯
			} else {	// 偶数熄灯
				Sends(0x0B);
			}
			light_on++;
		}
	}
	
	// 自动寻光
	class findLightOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
//			if(cloud_angle < 142) {
//				cloud_angle += 5; 
//				Sends(cloud_angle);
//			}
			
			Sends(0X10);
		}
		
	}
	
	// 超声波魔术手
	class handOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
//			if(cloud_angle > 112) {
//				cloud_angle -= 5;
//				Sends(cloud_angle);
//			}
			Sends(0X11);	
		}
		
	}
	
	// 声感
	class soundOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Sends(0X12);
		}
		
	}
}

