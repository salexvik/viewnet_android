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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class ParametersActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = ParametersActivity.class.getSimpleName();

	Context context;	
	boolean bound = false; // флаг соединения с сервисом

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений
	
	
	TextView textView_externalBatteryVoltage;
	

	// Создание activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");

		connectToService(); // подключаемся к сервису

		context = ParametersActivity.this;
		setContentView(R.layout.viewnet_parameters);
		
		textView_externalBatteryVoltage = (TextView)findViewById(R.id.textView_externalBatteryVoltage);
		
		
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






	// Создание главного меню

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//getMenuInflater().inflate(R.menu.viewnet_alarms_option_menu, menu);
		return true;
	}

	// Обработка выбора пунктов главного меню

	@Override
	// обрабатываем выбор меню - настройки
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		
//		case R.id.set_telephones_for_sms:
//			// вызываем activity настроек телефонных номеров для SMS оповещения
//			Intent intent = new Intent(context, PhonesActivity.class);
//			startActivity(intent);
//			return true;
			
		}
		return false;
	}



	// обработчик onClick

	public void onClick(View v) {
		
		switch (v.getId()) {
		
		
//		case R.id.button_testSms:			
//			serviceClient.sendServer(XML.stringToTag("id", "testSms"));
//			break;
		
		
		
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
						
						// Запрос на значение напряжения внешнего аккумулятора
						serviceClient.sendServer(XML.stringToTag("id", "controller")
								+XML.stringToTag("command", "start")); 
					}

					// нет соединения
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
					}
				}					

				if(intent.getStringExtra("id").equals("socketData")){

					final String inData = intent.getStringExtra("inData"); // получаем строку с принятыми данными 					
					String id = XML.getString(inData, "id"); // получаем идентификатор принятых данных


					// уведомление о неудачной аунтетификации
					if(id.equals("authenticationFailed")){
						Log.e(CLASS_NAME, "Аутентификация не пройдена");							
						serviceClient.authFailed(inData, context); 		
					}
					
										
					// получаем значение напряжения внешнего аккумулятора					
					if(id.equals("externalBatteryVoltage")){
						CharSequence voltage = XML.getString(inData, "voltage");
						textView_externalBatteryVoltage.setText(voltage + " V");
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

