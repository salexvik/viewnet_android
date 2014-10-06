package com.salexvik.viewnet;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ControlActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = ControlActivity.class.getSimpleName();

	Context context;	
	boolean bound = false; // флаг соединения с сервисом

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений


	Button button_rebootDevice;
	Button button_changePassword;


	// Создание activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");

		connectToService(); // подключаемся к сервису

		context = ControlActivity.this;
		setContentView(R.layout.viewnet_control);

		button_rebootDevice = (Button)findViewById(R.id.button_rebootDevice);
		button_changePassword = (Button)findViewById(R.id.button_changePassword);
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
		//getSupportMenuInflater().inflate(R.menu.viewnet_alarms_option_menu, menu);
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


		AlertDialog.Builder ad = new AlertDialog.Builder(this); // инициализация диалога
		final EditText input = new EditText(this);

		switch (v.getId()) {

		// кнопка Перезагрузка устройства
		case R.id.button_rebootDevice:	    	
			ad.setTitle("Перезагрузка устройства");
			ad.setMessage("Хорошо подумали?");			
			ad.setPositiveButton("Да, хорошо, перезагружай!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {													
					// отправляем  команду перезагрузку устройства			
					serviceClient.sendServer(XML.stringToTag("id", "controller")
							+XML.stringToTag("command", "stop"));				
				}
			});
			ad.setNegativeButton("Не, ну его нафиг!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Если отменили.
				}
			});
			ad.show();
			break;





		// кнопка Смена пароля
		case R.id.button_changePassword:	    	
			ad.setTitle("Изменение пароля");
			ad.setMessage("Введите новый пароль:");
			ad.setView(input);			
			ad.setPositiveButton("Изменить", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {										
					Editable value = input.getText();						
					String inputPassword = Other.getMd5(value.toString());					
					// отправляем  команду на смену пароля				
					serviceClient.sendServer(XML.stringToTag("id", "changePassword") + XML.stringToTag("newPassword", inputPassword));					
				}
			});
			ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Если отменили.
				}
			});
			ad.show();
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

					final String inData = intent.getStringExtra("inData"); // получаем строку с принятыми данными 					
					String id = XML.getString(inData, "id"); // получаем идентификатор принятых данных

					// уведомление о неудачной аунтетификации
					if(id.equals("authenticationFailed")){
						Log.e(CLASS_NAME, "Аутентификация не пройдена");							
						serviceClient.authFailed(inData, context); 		
					}

					// уведомление об успешной смене пароля
					if(id.equals("changePasswordSuccessfully")){
						Log.d(CLASS_NAME, "Пароль успешно изменен");
						Toast.makeText(context, "Пароль успешно изменен", Toast.LENGTH_SHORT).show();	

						// сохраняем пароль						
						serviceClient.setPassword(XML.getString(inData, "newPassword"));
					}
					
					
					// уведомление об ошибке смены пароля
					if(id.equals("changePasswordFailed")){
						Log.e(CLASS_NAME, "Ошибка смены пароля");
						Toast.makeText(context, "Ошибка смены пароля", Toast.LENGTH_SHORT).show();
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
