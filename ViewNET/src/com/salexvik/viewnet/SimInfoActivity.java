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
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SimInfoActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = AlarmsActivity.class.getSimpleName();

	Context context;	
	boolean bound = false; // флаг соединения с сервисом

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений

	TextView textView_networkName;
	TextView textView_signalLevel;
	TextView textView_simBalance;
	TextView textView_simNumber;

	Button button_sendUssdCommand;

	// Создание activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");

		connectToService(); // подключаемся к сервису

		context = SimInfoActivity.this;
		setContentView(R.layout.viewnet_sim_info);

		textView_networkName = (TextView)findViewById(R.id.textView_networkName);
		textView_signalLevel = (TextView)findViewById(R.id.textView_signalLevel);
		textView_simBalance = (TextView)findViewById(R.id.textView_simBalance);
		textView_simNumber = (TextView)findViewById(R.id.textView_simNumber);

		button_sendUssdCommand = (Button)findViewById(R.id.button_sendUssdCommand);
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

		switch (v.getId()) {


		case R.id.button_sendUssdCommand:			
			ussdDialog("");			
			break;



		}

	}


	// Диалог отправки USSD запроса

	private void ussdDialog(String aText){

		AlertDialog.Builder alert = new AlertDialog.Builder(context);			
		alert.setTitle("Выполнение USSD команды");
		if (!aText.equals("")) alert.setMessage(aText);
		final EditText input = new EditText(context);	
		input.setInputType(InputType.TYPE_CLASS_PHONE); // тип клавиатуры - номер телефона	

		// Настраиваем автоматический показ клавиатуры
		// задержка нужна для того, чтобы диалог успел создаться
		input.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				keyboard.showSoftInput(input, 0);
			}
		}, 50);

		alert.setView(input);
		alert.setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {										
				String ussdCommand = input.getText().toString();										
				serviceClient.sendServer(XML.stringToTag("id", "requestUssd")
						+ XML.stringToTag("ussdCommand", ussdCommand));
			}
		});
		alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Если отменили.
			}
		});

		alert.show();
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

						// Запрос информации СИМ карты
						serviceClient.sendServer(XML.stringToTag("id", "requestSimInfo"));
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


					// получаем информацию СИМ карты

					if(id.equals("responseSimInfo")){	

						String networkName = XML.getString(inData, "networkName");
						if(networkName.equals("")) textView_networkName.setText(" ???");						
						else textView_networkName.setText(networkName);	

						String signalLevel = XML.getString(inData, "signalLevel");
						if(signalLevel.equals("")) textView_signalLevel.setText(" ???");						
						else textView_signalLevel.setText(signalLevel);

						String simBalance = XML.getString(inData, "simBalance");
						if(simBalance.equals("")) textView_simBalance.setText(" ???");						
						else textView_simBalance.setText(simBalance);

						String simNumber = XML.getString(inData, "simNumber");
						if(simNumber.equals("")) textView_simNumber.setText(" ???");						
						else textView_simNumber.setText(simNumber);

					}

					// получаем результат USSD запроса

					if(id.equals("responseUssd")){	

						String result = XML.getString(inData, "result");

						if (result.equals("")){
							ussdDialog("Ошибка USSD запроса");							
						}
						else {
							ussdDialog(result);
						}

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
