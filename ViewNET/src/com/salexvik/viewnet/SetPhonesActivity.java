package com.salexvik.viewnet;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SetPhonesActivity extends ActionBarActivity  {




	static final String CLASS_NAME = SetPhonesActivity.class.getSimpleName();
	private static final int CM_RENAME_ID = 0;
	private static final int CM_SET_PHONE_NUMBER_ID = 1; 
	private static final int CM_DELETE_ID = 2;


	Context context;

	boolean bound = false; // флаг соединения с сервисом
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса
	ServiceClient serviceClient; // сервис клиента
	ListView listView_phoneName;
	private ArrayList<String> phoneNameList; // список имен доступных телефонных номеров	
	private ArrayList<String> phoneNumberList; // список телефонных номеров
	private ArrayList<String> phoneIdList; // список идентификаторов доступных телефонных номеров	
	private ArrayList<Boolean> phoneStatusList; // список статусов доступных телефонных номеров		
	PhoneNameAdapter phoneNameAdapter; // адаптер для списка имен доступных телефонных номеров

	BroadcastReceiver br; // приемник широковещательных сообщений

	// Cоздание активити

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		connectToService(); // подключаемся к сервису

		context = SetPhonesActivity.this; // получаем контекст	

		// настраиваем визуальные компоненты

		setContentView(R.layout.viewnet_set_phones);          

		listView_phoneName = (ListView)findViewById(R.id.listView_telName); 

		phoneNameList = new ArrayList<String>(); // создаем массив имен телефонных номеров
		phoneNumberList =  new ArrayList<String>(); // создаем массив телефонных номеров
		phoneIdList = new ArrayList<String>();  // создаем массив идентификаторов телефонных номеров
		phoneStatusList = new ArrayList<Boolean>();  // создаем массив статусов телефонных номеров
		phoneNameAdapter = new PhoneNameAdapter(context, phoneNameList);  // создаем адаптер для имен телефонных номеров	
		listView_phoneName.setAdapter(phoneNameAdapter); // привязываем адаптер к ListView имен телефонных номеров

		registerForContextMenu(listView_phoneName); // регестрируем контекстное меню для списка имен телефонных номеров  
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




	// Создание главного меню

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.viewnet_sms_option_menu, menu);
		return true;
	}

	// События главного меню

	@Override
	// обрабатываем выбор меню - настройки
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		case R.id.add_new_telNumber:

			// Добавление нового телефона

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("Номер телефона:");
			//	alert.setMessage("Сообщение");
			// Добавим поле ввода
			final EditText input = new EditText(this);			
			alert.setView(input);
			alert.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "Не введен номер телефона!", Toast.LENGTH_SHORT).show();
					}
					else{			

						// добавляем значения в массивы
						phoneNameList.add(value.toString()); 
						phoneNumberList.add(value.toString());
						phoneStatusList.add(true);
						phoneIdList.add(Other.getRandomString());

						int pos = phoneNameList.size() -1;
						String phone = XML.stringToTag("phoneName", phoneNameList.get(pos)) 
								+ XML.stringToTag("phoneNumber", phoneNumberList.get(pos))
								+ XML.boolToTag("phoneStatus", phoneStatusList.get(pos))
								+ XML.stringToTag("phoneId", phoneIdList.get(pos));		
						// отправляем команду на добавление телефона	
						serviceClient.sendServer(XML.stringToTag("id", "changePhones")
								+ XML.stringToTag("operationId", "addPhone")
								+ XML.stringToTag("phone", phone));
					}
				}
			});
			alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Если отменили.
				}
			});
			alert.show();

			return true;
		}
		return false;
	}


	// Создание контекстного меню для ListView

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);	
		menu.add(0, CM_RENAME_ID, 0, "Переименовать");
		menu.add(0, CM_SET_PHONE_NUMBER_ID, 0, "Изменить номер телефона");
		menu.add(0, CM_DELETE_ID, 0, "Удалить");
	}

	// Обработчик выбора пункта контекстного меню для ListView

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {

		// получаем инфу о пункте списка
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		final int posPhoneName = acmi.position; // получаем позицию выбранного имени телефона

		if (item.getItemId() == CM_RENAME_ID) { //изменить имя

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("Новое имя:");
			//	alert.setMessage("Сообщение");
			// Добавим поле ввода
			final EditText input = new EditText(this);			
			// Помещаем в поле ввода изменяемое имя
			input.setText(phoneNameList.get(posPhoneName));
			alert.setView(input);
			alert.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "Введите имя", Toast.LENGTH_SHORT).show();
					}
					else{	
						phoneNameList.set(posPhoneName, value.toString()); // меняем значение в массиве имен
						// отправляем команду на замену  телефона	
						serviceClient.sendServer(XML.stringToTag("id", "changePhones")
								+ XML.stringToTag("operationId", "setPhoneName")								
								+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))
								+ XML.stringToTag("phoneName", phoneNameList.get(posPhoneName)));  
					}
				}
			});
			alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Если отменили.
				}
			});
			alert.show();	
			
			return true;
		}

		if (item.getItemId() == CM_SET_PHONE_NUMBER_ID) { // изменить номер телефона

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("Новый номер телефона:");
			//	alert.setMessage("Сообщение");
			// Добавим поле ввода
			final EditText input = new EditText(this);			
			// Помещаем в поле ввода изменяемое имя
			input.setText(phoneNumberList.get(posPhoneName));
			alert.setView(input);
			alert.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "Введите номер телефона", Toast.LENGTH_SHORT).show();
					}
					else{							
						phoneNumberList.set(posPhoneName, value.toString()); // меняем значение в массиве номеров
						// отправляем команду на замену  номера	
						serviceClient.sendServer(XML.stringToTag("id", "changePhones")
								+ XML.stringToTag("operationId", "setPhoneNumber")								
								+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))
								+ XML.stringToTag("phoneNumber", phoneNumberList.get(posPhoneName))); 
					}
				}
			});
			alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Если отменили.
				}
			});
			alert.show();

			return true;
		}

		if (item.getItemId() == CM_DELETE_ID) { // удалить телефон

			// отправляем команду на удаление телефонного номера
			serviceClient.sendServer(XML.stringToTag("id", "changePhones")
					+ XML.stringToTag("operationId", "deletePhone")
					+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))); 

			// удаляем значения из массивов...
			phoneNameList.remove(posPhoneName);
			phoneNumberList.remove(posPhoneName);
			phoneStatusList.remove(posPhoneName);
			phoneIdList.remove(posPhoneName);
			return true;
		}
		return super.onContextItemSelected(item);
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
						phoneNameAdapter.clear(); // очищаем список имен телефонов
					}

					// соединение установлено
					if(connectionStatus.equals("connected")){
						Log.d(CLASS_NAME, "Запрос на список имен телефонов, номеров, их id и статуса");	
						getSupportActionBar().setLogo(R.drawable.circle_green);
						serviceClient.sendServer(XML.stringToTag("id", "requestPhones")); 

					}

					// нет соединения
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
						phoneNameAdapter.clear(); // очищаем список медиапотоков
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
					
					// подтверждение изменения параметров телефонов (после удаления, добавления или редактирования)
					if(id.equals("changePhonesSuccessfully")){
						phoneNameAdapter.notifyDataSetChanged(); // применяем изменение к ListView списка имен медиапотоков
						Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show();
					}



					// получение списка имен, номеров, id и статусов телефонов
					if(id.equals("responsePhones")){						
						phoneNameList.clear();
						phoneNumberList.clear();
						phoneIdList.clear();
						phoneStatusList.clear();

						phoneNameList.addAll(XML.getStringArrayList(inData, "phoneName")); // заполняем массив 	
						phoneNumberList.addAll(XML.getStringArrayList(inData, "phoneNumber")); // заполняем массив						
						phoneIdList.addAll(XML.getStringArrayList(inData, "phoneId")); // заполняем массив 				
						phoneStatusList.addAll(XML.getBoolArrayList(inData, "phoneStatus")); // заполняем массив 						
						phoneNameAdapter.notifyDataSetChanged(); // применяем изменение к ListView списка имен медиапотоков					
					}



				}
			}
		};
		// создаем фильтр для BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// регистрируем (включаем) BroadcastReceiver
		registerReceiver(br, intFilt);
	}




	class PhoneNameAdapter extends BaseAdapter{
		Context ctx;
		LayoutInflater lInflater;
		ArrayList<String> data;


		PhoneNameAdapter(Context context, ArrayList<String> phoneNameList) {
			ctx = context;
			data = phoneNameList;
			lInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}


		public void clear()
		{
			data.clear();
			notifyDataSetChanged();
		}

		// кол-во элементов
		@Override
		public int getCount() {
			return data.size();
		}

		// элемент по позиции
		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		// id по позиции
		@Override
		public long getItemId(int position) {
			return position;
		}



		// пункт списка
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final int posPhoneName = position;

			// используем созданные, но не используемые view
			View view = convertView;
			if (view == null) {
				view = lInflater.inflate(R.layout.viewnet_phones_item, parent, false);
			}


			final CheckBox checkBox_phoneStatus = (CheckBox) view.findViewById(R.id.checkBox_phoneStatus);
			TextView textView_phoneName = (TextView) view.findViewById(R.id.textView_setDeviceName);

			checkBox_phoneStatus.setEnabled(true);

			checkBox_phoneStatus.setChecked(phoneStatusList.get(position));			
			textView_phoneName.setText(data.get(position));
			
			if (checkBox_phoneStatus.isChecked())
				textView_phoneName.setTextColor(Color.WHITE);
				else textView_phoneName.setTextColor(Color.DKGRAY);
			

			checkBox_phoneStatus.setOnClickListener(new OnClickListener() { 
				@Override
				public void onClick(View v) { 

					checkBox_phoneStatus.setEnabled(false); // блокируем на время выполнения запроса

					phoneStatusList.set(posPhoneName, checkBox_phoneStatus.isChecked()); // меняем значение в массиве статусов
					// отправляем команду на замену  телефона	
					serviceClient.sendServer(XML.stringToTag("id", "changePhones")
							+ XML.stringToTag("operationId", "setPhoneStatus")								
							+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))
							+ XML.boolToTag("phoneStatus", phoneStatusList.get(posPhoneName)));  
				}
			}); 


			return view;
		}
	}


}
