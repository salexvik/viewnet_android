package com.salexvik.viewnet;


import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class DevicesActivity extends ActionBarActivity {

	private static final String CLASS_NAME = DevicesActivity.class.getSimpleName();
	private static final int CM_EDIT_DEVICE = 0;
	private static final int CM_DELETE_DEVICE = 1;

	private static final int MM_ADD = 0;


	ListView listView_devices;
	private ArrayList<String> deviceNameList;
	private ArrayList<String>idDeviceList;
	private ArrayList<Integer> deviceStatusList;	
	private ArrayList<Integer>recordIdList;	
	private ArrayList<Integer>allowNotificationList;
	ListViewAdapter listViewAdapter; // адаптер для списка медиапотоков

	DB db; // база данных

	Context context;

	boolean bound = false; // флаг соединения с сервисом

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений


	// создание activity

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		connectToService(); // подключаемся к сервису

		context = DevicesActivity.this;

		setContentView(R.layout.viewnet_devices);

		listView_devices = (ListView) findViewById(R.id.listView_devices);

		deviceNameList = new ArrayList<String>();
		listViewAdapter = new ListViewAdapter(context, deviceNameList);  // создаем адаптер 		
		listView_devices.setAdapter(listViewAdapter); 

		idDeviceList = new ArrayList<String>(); 
		deviceStatusList = new ArrayList<Integer>(); 
		recordIdList = new ArrayList<Integer>(); 
		allowNotificationList = new ArrayList<Integer>(); 


		// создаем объект для работы с базой данных
		db = new DB(this, DB.TABLE_NAME_DEVICE_SETTINGS);

		// подключаемся к БД
		db.open();

		// регистрируем контекстное меню для ListView
		registerForContextMenu(listView_devices);

		// получаем данные из базы
		getDataFromDB();


		// событие Клик по списку медиапотоков - выбираем устройство
		listView_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {				

				ContentValues cv = new ContentValues();
				
				// сбрасываем статус текущего активного устройства				
				cv.put(DB.COLUMN_CURRENT_DEVICE, 0);
				db.updateRec(cv, DB.COLUMN_CURRENT_DEVICE + " = 1", null); 
				
				// устанавливаем статус нового активного устройства
				cv.clear();
				cv.put(DB.COLUMN_CURRENT_DEVICE, 1);						
				db.updateRecById(recordIdList.get(position), cv);
				
				getDataFromDB(); // обновляем listView
				
				// закрываем текущее сетевое соединение
				serviceClient.socketClose();
				
				// передаем сервису ID выбранного устройства
				serviceClient.setIdDevice(idDeviceList.get(position));

			}
		});		
	}


	// получаем данные из базы

	void getDataFromDB(){
		deviceNameList.clear();
		idDeviceList.clear();
		deviceStatusList.clear();
		recordIdList.clear();
		allowNotificationList.clear();

		Cursor c = db.getAllData();

		// ставим позицию курсора на первую строку выборки
		if (c.moveToFirst()) {
			do {
				// получаем значения по номерам столбцов
				deviceNameList.add(c.getString(c.getColumnIndex(DB.COLUMN_DEVICE_NAME)));
				idDeviceList.add(c.getString(c.getColumnIndex(DB.COLUMN_ID_DEVICE)));
				deviceStatusList.add(c.getInt(c.getColumnIndex(DB.COLUMN_CURRENT_DEVICE)));
				recordIdList.add(c.getInt(c.getColumnIndex(DB.COLUMN_ID)));
				allowNotificationList.add(c.getInt(c.getColumnIndex(DB.COLUMN_ALLOW_NOTIFICATIONS)));
			} while (c.moveToNext());
		}
		c.close();
		
		// обновляем ListView
		listViewAdapter.notifyDataSetChanged();
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


	// Восстановление активити

	public void onResume() {
		super.onResume();
		broadcastReceiving(); // создаем приемник широковещательных сообщений 
		if(bound){
			serviceClient.getConnectionStatus(); // получаем статус соединения			
		}
		getDataFromDB();
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





	// Создание главного меню

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MM_ADD, 0, "Добавить устройство");
		return true;
	}

	// Обработка выбора пунктов главного меню

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {	

		// Добавление нового устройства
		if (item.getItemId() == MM_ADD) {	
			
			Intent i = new Intent(context, SetDevicesActivity.class);	
			i.putExtra("recordId", -1);
			startActivity(i); // вызываем активити			
			return true;
		}
		
		return false;
	}



	// Создание контекстного меню для ListView

	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);	
		menu.add(0, CM_EDIT_DEVICE, 0, "Изменить");
		menu.add(0, CM_DELETE_DEVICE, 0, "Удалить");
	}

	// Обработчик выбора пункта контекстного меню для ListView

	public boolean onContextItemSelected(android.view.MenuItem item) {

		// получаем из пункта контекстного меню данные по пункту списка
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		final int position = acmi.position; // получаем позицию выбранного медиапотока


		// редактировать устройство
		if (item.getItemId() == CM_EDIT_DEVICE) {

			Intent i = new Intent(context, SetDevicesActivity.class);
			i.putExtra("recordId", recordIdList.get(position));			
			i.putExtra("deviceName", deviceNameList.get(position));
			i.putExtra("idDevice", idDeviceList.get(position));
			i.putExtra("allowNotifications", allowNotificationList.get(position));
			
			startActivity(i); // вызываем активити

			return true;
		}


		// удалить устройство
		if (item.getItemId() == CM_DELETE_DEVICE) {			
			// удаляем соответствующую запись в БД
			db.delRecById(recordIdList.get(position));

			// обновляем ListView
			getDataFromDB();
			return true;

		}		

		return super.onContextItemSelected(item);
	}






	// обработчик onClick

	public void onClick(View v) {

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



	class ListViewAdapter extends BaseAdapter{
		Context ctx;
		LayoutInflater lInflater;
		ArrayList<String> data;

		ListViewAdapter(Context context, ArrayList<String> arrayList) {
			ctx = context;
			data = arrayList;
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

			// используем созданные, но не используемые view
			View view = convertView;
			if (view == null) {
				view = lInflater.inflate(R.layout.viewnet_devices_item, parent, false);
			}

			((TextView) view.findViewById(R.id.textView_deviceName)).setText(data.get(position));
			((TextView) view.findViewById(R.id.textView_idDevice)).setText(idDeviceList.get(position));

			ImageView imageView_deviceStatus = (ImageView) view.findViewById(R.id.imageView_deviceStatus);

			// получаем статус устройства
			int deviceStatus = deviceStatusList.get(position);

			if (deviceStatus == 0) imageView_deviceStatus.setVisibility(View.INVISIBLE); 
			if (deviceStatus == 1) imageView_deviceStatus.setVisibility(View.VISIBLE); 


			return view;
		}
	}



}
