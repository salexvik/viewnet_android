package com.salexvik.viewnet;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
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
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SetDevicesActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = SetDevicesActivity.class.getSimpleName();
	Context context;	
	boolean bound = false; // флаг соединения с сервисом

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений

	LinearLayout ll_setDevices_deviceName;
	LinearLayout ll_setDevices_idDevice;
	TextView tv_setDevices_deviceName;
	TextView tv_setDevices_idDevice;
	CheckBox cb_setDevices_allowNotifications;
	Button bt_setDevices_save;
	Button bt_setDevices_chancel;


	

	int recordId; 

	String action = "";


	// Создание activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		connectToService(); // подключаемся к сервису
		context = SetDevicesActivity.this;

		setContentView(R.layout.viewnet_set_devices);
		ll_setDevices_deviceName = (LinearLayout)findViewById(R.id.ll_setDevices_deviceName);
		ll_setDevices_idDevice = (LinearLayout)findViewById(R.id.ll_setDevices_idDevice);
		tv_setDevices_deviceName = (TextView)findViewById(R.id.tv_setDevices_deviceName);
		tv_setDevices_idDevice = (TextView)findViewById(R.id.tv_setDevices_idDevice);
		cb_setDevices_allowNotifications = (CheckBox)findViewById(R.id.cb_setDevices_allowNotifications);		
		bt_setDevices_save = (Button)findViewById(R.id.bt_setDevices_save);
		bt_setDevices_chancel = (Button)findViewById(R.id.bt_setDevices_chancel);
		

		// получаем данные из интента
		Bundle extras = getIntent().getExtras(); // контейнер с данными интента	

		recordId = extras.getInt("recordId");

		if (recordId == -1) action = "add";
		else action = "edit";

		// заполнение полей значениями

		if (action.equals("add")){			
			tv_setDevices_deviceName.setText("Новое устройство");
			tv_setDevices_idDevice.setText("");
			cb_setDevices_allowNotifications.setChecked(false); 			
		}

		if (action.equals("edit")){

			String deviceName = extras.getString("deviceName");
			String idDevice = extras.getString("idDevice");
			int allowNotifications = extras.getInt("allowNotifications");

			tv_setDevices_deviceName.setText(deviceName);
			tv_setDevices_idDevice.setText(idDevice);
			
			if (allowNotifications == 0) cb_setDevices_allowNotifications.setChecked(false);
			if (allowNotifications == 1) cb_setDevices_allowNotifications.setChecked(true);


		}

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

		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		final EditText input = new EditText(this);
		ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});	

		switch (v.getId()) {


		case R.id.ll_setDevices_deviceName:
			ad.setTitle("Имя устройства"); // заголовок для диалога
			input.setText(tv_setDevices_deviceName.getText().toString());
			ad.setView(input);	        
			ad.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();					
					tv_setDevices_deviceName.setText(value);						 					
				}
			});
			ad.show();
			break;

		case R.id.ll_setDevices_idDevice:
			ad.setTitle("Идентификатор устройства"); // заголовок для диалога
			input.setText(tv_setDevices_idDevice.getText().toString());
			ad.setView(input);	        
			ad.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();					
					tv_setDevices_idDevice.setText(value);						
				}
			});
			ad.show();
			break;
			
			
		case R.id.bt_setDevices_save:
			
			// проверка введенных значений
			String deviceName = tv_setDevices_deviceName.getText().toString();
			if (deviceName.equals("")){
				Other.showMessage("Ошибка", "Не указано имя устройства",context);
				break;
			}			
			String idDevice = tv_setDevices_idDevice.getText().toString();
			if (idDevice.toString().equals("")){
				Other.showMessage("Ошибка", "Не указан идентификатор устройства",context);
				break;
			}
			
			DB db = new DB(this, DB.TABLE_NAME_DEVICE_SETTINGS);
			db.open();
			ContentValues cv = new ContentValues();
			cv.put(DB.COLUMN_DEVICE_NAME, deviceName);
			cv.put(DB.COLUMN_ID_DEVICE, idDevice);	
			
			int allowNotifications;
			if(cb_setDevices_allowNotifications.isChecked()) allowNotifications = 1;
			else allowNotifications = 0;

			cv.put(DB.COLUMN_ALLOW_NOTIFICATIONS, allowNotifications);
						
			if (action.equals("add")) db.addRec(cv);			
			if (action.equals("edit")) db.updateRecById(recordId, cv);	
			
			db.close();
			finish();
			break;
			
			
		case R.id.bt_setDevices_chancel:
			finish();
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

				}



			}
		};
		// создаем фильтр для BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// регистрируем (включаем) BroadcastReceiver
		registerReceiver(br, intFilt);
	}	


}

