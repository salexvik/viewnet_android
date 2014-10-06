package com.salexvik.viewnet;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SetAlarmsActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = SetAlarmsActivity.class.getSimpleName();
	Context context;	
	boolean bound = false; // флаг соединения с сервисом

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений

	Button button_testSms;
	Button button_testGcm;
	Button button_testNotification;
	Button button_setPhones;


 
	// Создание activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		connectToService(); // подключаемся к сервису
		context = SetAlarmsActivity.this;

		setContentView(R.layout.viewnet_set_alarms);
		button_testSms = (Button)findViewById(R.id.button_testSms);
		button_testGcm = (Button)findViewById(R.id.button_testGcm);
		button_testNotification = (Button)findViewById(R.id.button_testNotification);
		button_setPhones = (Button)findViewById(R.id.button_setPhones);

	}


	// Восстановление активити

	public void onResume() {
		super.onResume();	
		broadcastReceiving(); // создаем приемник широковещательных сообщений 
		if(bound){
			serviceClient.getConnectionStatus(); // получаем статус соединения			
		}
	}

	// Приостановка активити

	public void onPause() {
		super.onPause();	
		unregisterReceiver(br); // отключаем приемник широковещательных сообщений
	}

	// Уничтожение активити

	public void onDestroy() {
		super.onDestroy();
		// отключаемся от сервиса
		if (bound){
			unbindService(sConn);
			bound = false;
		}
	}


	// Подключение к сервису

	private void connectToService(){
		Intent intent = new Intent(this, ServiceClient.class);
		sConn = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.d(CLASS_NAME, "onServiceConnected()");
				serviceClient = ((ServiceClient.ClientBinder) binder).getService(); 
				bound = true; 
				serviceClient.getConnectionStatus(); // получаем статус соединения
			}
			public void onServiceDisconnected(ComponentName name) {
				Log.d(CLASS_NAME, "onServiceDisconnected()");
				bound = false;
			}
		}; 				
		startService(intent); // запускаем сервис
		bindService(intent, sConn, 0); // подключаемся к сервису		
	}





	// обработчик onClick

	public void onClick(View v) {

		switch (v.getId()) {


		case R.id.button_testSms:			
			serviceClient.sendServer(XML.stringToTag("id", "testSms"));
			break;

		case R.id.button_testGcm:			
			serviceClient.sendServer(XML.stringToTag("id", "testGcm"));
			break;

		case R.id.button_testNotification:			
			serviceClient.sendServer(XML.stringToTag("id", "testNotification"));
			break;

		case R.id.button_setPhones:			
			// вызываем activity настроек телефонных номеров для смс уведомлений
			Intent intent = new Intent(context, SetPhonesActivity.class);
			startActivity(intent);
			break;


		}

	}







	// Обработка данных из сервиса

	private void broadcastReceiving(){

		// создаем BroadcastReceiver
		br = new BroadcastReceiver() {
			// действия при получении сообщений
			public void onReceive(Context context, Intent intent) {

				if(intent.getStringExtra("id").equals("connectionStatus")){

					String connectionStatus = intent.getStringExtra("connectionStatus"); 

					// выполняется подключение
					if(connectionStatus.equals("connecting")){
						getSupportActionBar().setLogo(R.drawable.circle_yellow);
					}

					// соединение установлено
					if(connectionStatus.equals("connected")){									
						getSupportActionBar().setLogo(R.drawable.circle_green);
					}

					// нет соединения
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
					}
				}	



				if(intent.getStringExtra("id").equals("socketData")){

					String inData = intent.getStringExtra("inData"); // получаем строку с принятыми данными 					
					String id = XML.getString(inData, "id"); // получаем идентификатор принятых данных


					// уведомление о неудачной аунтетификации
					if(id.equals("authenticationFailed")){
						Log.e(CLASS_NAME, "Аутентификация не пройдена");							
						serviceClient.authFailed(inData, context); 		
					}


				}



			}
		};
		// создаем фильтр для BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// регистрируем (включаем) BroadcastReceiver
		registerReceiver(br, intFilt);
	}	


}
