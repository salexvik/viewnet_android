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
import android.view.View.OnClickListener;
import android.widget.Button;





public class MainActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = MainActivity.class.getSimpleName();

	Button button_view;
	Button button_alarm;
	Button button_record;
	Button button_simInfo;	
	Button button_control;
	Button button_parameters;
	
	
	


	Context context;

	boolean bound = false; // флаг соединения с сервисом
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса
	ServiceClient serviceClient; // сервис клиента
	BroadcastReceiver br; // приемник широковещательных сообщений
	
	// Cоздание активити

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		context = MainActivity.this; // получаем контекст	

		connectToService(); // подключаемся к сервису


		// настраиваем визуальные компоненты

		setContentView(R.layout.viewnet_main);          

		button_view = (Button)findViewById(R.id.button_view);
		button_alarm = (Button)findViewById(R.id.button_alarm);
		button_record = (Button)findViewById(R.id.button_record);
		button_simInfo = (Button)findViewById(R.id.button_simInfo);
		button_control = (Button)findViewById(R.id.Button_control);
		button_parameters = (Button)findViewById(R.id.Button_parameters);

		userEventHandler(); // настраиваем обработчик кнопок    

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


	// Старт активити

	public void onStart() {
		Log.d(CLASS_NAME,"onStart()");
		super.onStart();
	}

	// Восстановление активити

	public void onResume() {
		Log.d(CLASS_NAME,"onResume()");
		super.onResume();
		broadcastReceiving(); // создаем приемник широковещательных сообщений  
		if(bound){
			serviceClient.getConnectionStatus(); // получаем статус соединения			
		}
	}

	// Приостановка активити

	public void onPause() {
		Log.d(CLASS_NAME,"onPause()");
		super.onPause();	
		unregisterReceiver(br); // отключаем приемник широковещательных сообщений
	}


	// Стоп активити

	public void onStop() {
		Log.d(CLASS_NAME,"onStop()");
		super.onStop();
	}

	// Уничтожение активити

	public void onDestroy() {
		super.onDestroy();
		Log.d(CLASS_NAME,"onDestroy()");	
				
		if (bound){
			unbindService(sConn);
			bound = false;
		}
	}

	// Сохранение состояния активити

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(CLASS_NAME, "onSaveInstanceState()");
	}	

	// Восстановление состояния активити после смены ориентации или удалении при нехватке памяти

	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(CLASS_NAME, "onRestoreInstanceState()");
		//        if(savedInstanceState.getBoolean("video")){
		//        	Log.d(activityName, "возобновляем показ видео");
		//        }

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

	// Создание главного меню

	@Override

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.viewnet_main_option_menu, menu);

//		menu.add(Menu.NONE, 0, Menu.NONE, "Добавить")
//		.setIcon(R.drawable.viewnet_media_record)
//		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//
//		menu.add(Menu.NONE, 1, Menu.NONE, "вставить")
//		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	// События главного меню




	@Override
	// обрабатываем выбор меню - настройки
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		
//		case 0:
//			Toast.makeText(this, "clicked on 0", Toast.LENGTH_SHORT).show();
//			return true;
//			
//		case 1:
//			Toast.makeText(this, "clicked on 1", Toast.LENGTH_SHORT).show();
//			return true;
//		
		
		case R.id.settings:
			Intent settings_intent = new Intent(context, DevicesActivity.class);
			startActivity(settings_intent); // вызываем активити с основными настройками 
			return true;

		}
		return false;
	}



	// Обработчики onClick

	private void userEventHandler(){

		// кнопка Сигнализация
		button_alarm.setOnClickListener(new OnClickListener() { // создаем обработчик нажатия
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, AlarmsActivity.class);
				startActivity(intent);
			}
		}); 


		// кнопка просмотр медиапотока                  
		button_view.setOnClickListener(new OnClickListener() { // создаем обработчик нажатия
			@Override
			public void onClick(View v) { 				
				Intent intent = new Intent(context, ViewActivity.class);
				startActivity(intent);
			}
		}); 

		// кнопка запись медиапотоков
		button_record.setOnClickListener(new OnClickListener() { // создаем обработчик нажатия
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, RecordActivity.class);
				startActivity(intent);
			}
		}); 

		// кнопка SIM-инфо
		button_simInfo.setOnClickListener(new OnClickListener() { // создаем обработчик нажатия
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, SimInfoActivity.class);
				startActivity(intent);
			}
		}); 
		
		// кнопка Управление
		button_control.setOnClickListener(new OnClickListener() { // создаем обработчик нажатия
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, ControlActivity.class);
				startActivity(intent);
			}
		}); 
		
		// кнопка Параметры
		button_parameters.setOnClickListener(new OnClickListener() { // создаем обработчик нажатия
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, ParametersActivity.class);
				startActivity(intent);
			}
		}); 
		
		 


	}

}   