package com.salexvik.viewnet;

import java.util.Timer;
import java.util.TimerTask;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class AlarmsActivity extends ActionBarActivity  implements LoaderCallbacks<Cursor>  {

	private static final String CLASS_NAME = AlarmsActivity.class.getSimpleName();
	private static final int CM_DELETE_ID = 1;
	private static final int CM_DELETE_ALL = 2;

	Context context;	
	boolean bound = false; // флаг соединения с сервисом

	private Timer refreshTimer;
	private TimerTask tTask;

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений


	DB db; // база данных
	SimpleCursorAdapter scAdapter;
	ListView lvData;


	// Создание activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");
		
		// создаем объект для работы с базой данных
		db = new DB(this, DB.TABLE_NAME_NOTIFICATIONS);

		// подключаемся к БД
		db.open();

		connectToService(); // подключаемся к сервису

		context = AlarmsActivity.this;
		setContentView(R.layout.viewnet_alarms);


		// формируем столбцы сопоставления
		String[] from = new String[] { DB.COLUMN_TYPE_MSG, DB.COLUMN_TIMESTAMP_MSG, DB.COLUMN_MSG, DB.COLUMN_DEVICE_NAME};
		int[] to = new int[] { R.id.textView_typeMessage, R.id.textView_timestampMessage, R.id.textView_textMessage, R.id.textView_devName};

		// создааем адаптер и настраиваем список
		scAdapter = new SimpleCursorAdapter(this, R.layout.viewnet_alarms_item, null, from, to, 0);
		lvData = (ListView) findViewById(R.id.lvData);
		lvData.setAdapter(scAdapter);

		// создаем лоадер для чтения данных
		getSupportLoaderManager().initLoader(0, null, this);

		// удаляем уведомление из системного статус-бара
		cancelNotification(context, EventIntentService.NOTIFICATION_ID);

		// регистрируем контекстное меню для ListView
		registerForContextMenu(lvData);

		// запуск таймера обновления ListView
		startRefreshTimer(); 
	}


	// таймер для обновления listView с данными из базы

	void startRefreshTimer() {
		refreshTimer = new Timer(); // создаем таймер для обновления ListView
		if (tTask != null) tTask.cancel();
		tTask = new TimerTask() {
			public void run() {
				Log.i(CLASS_NAME, "onTimer()");
				getSupportLoaderManager().getLoader(0).forceLoad();  	
				cancelNotification(context, EventIntentService.NOTIFICATION_ID);
			}
		};
		refreshTimer.schedule(tTask, 2000, 2000);
		
	}



	// Восстановление активити

	public void onResume() {
		super.onResume();	
		broadcastReceiving(); // создаем приемник широковещательных сообщений 
		if(bound){
			serviceClient.getConnectionStatus(); // получаем статус соединения			
		}
		// удаляем уведомление из системного статус-бара
		cancelNotification(context, EventIntentService.NOTIFICATION_ID);
	}

	// Приостановка активити

	public void onPause() {
		super.onPause();	
		unregisterReceiver(br); // отключаем приемник широковещательных сообщений
	}

	// Уничтожение активити

	public void onDestroy() {
		super.onDestroy();
		Log.i(CLASS_NAME, "onDestroy()");
		// отключаемся от сервиса
		if (bound){
			unbindService(sConn);
			bound = false;
		}
		if (refreshTimer != null) refreshTimer.cancel(); // отключаем таймер обновления listView		
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


	// удаление уведомления из системного статус-бара

	public static void cancelNotification(Context ctx, int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
		nMgr.cancel(notifyId);
	}


	// Создание главного меню

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.viewnet_alarms_option_menu, menu);
		return true;
	}

	// Обработка выбора пунктов главного меню

	@Override
	// обрабатываем выбор меню - настройки
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		case R.id.set_notif:
			// вызываем activity настроек уведомлений
			Intent intent = new Intent(context, SetAlarmsActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}


	// Создание контекстного меню для ListView

	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);	
		menu.add(0, CM_DELETE_ID, 0, "Удалить выбранное");
		menu.add(0, CM_DELETE_ALL, 0, "Удалить все");
	}

	// Обработчик выбора пункта контекстного меню для ListView

	public boolean onContextItemSelected(android.view.MenuItem item) {

		if (item.getItemId() == CM_DELETE_ID) {
			// получаем из пункта контекстного меню данные по пункту списка
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item
					.getMenuInfo();
			// извлекаем id записи и удаляем соответствующую запись в БД
			db.delRecById(acmi.id);
			// получаем новый курсор с данными
			getSupportLoaderManager().getLoader(0).forceLoad();
			return true;
		}

		if (item.getItemId() == CM_DELETE_ALL) {
			// удаляем все записи из таблицы
			db.delRec(null, null);
			// получаем новый курсор с данными
			getSupportLoaderManager().getLoader(0).forceLoad();
			return true;
		}
		return super.onContextItemSelected(item);
	}



	// обработчик onClick

	public void onClick(View v) {

		switch (v.getId()) {


		//		case R.id.button_refrash:			
		//			
		//			break;



		}

	}





	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
		return new MyCursorLoader(this, db);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		scAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}


	static class MyCursorLoader extends CursorLoader {

		DB db;

		public MyCursorLoader(Context context, DB db) {
			super(context);
			this.db = db;
		}

		@Override
		public Cursor loadInBackground() {

			//Объединяем таблицу notification с таблицей devices и получаем необходимые для отображения поля

			String tableName = DB.TABLE_NAME_NOTIFICATIONS + " as NF inner join " + DB.TABLE_NAME_DEVICE_SETTINGS + " as DS on NF." + DB.COLUMN_ID_DEVICE + " = DS." + DB.COLUMN_ID_DEVICE;
			String columnsArray[] = { "NF." + DB.COLUMN_ID + " as " + DB.COLUMN_ID, DB.COLUMN_DEVICE_NAME, DB.COLUMN_TYPE_MSG,  DB.COLUMN_ID_MSG, DB.COLUMN_MSG, DB.COLUMN_TIMESTAMP_MSG};			
			
			Cursor cursor  = db.getSelectionData(tableName, columnsArray, null, null);
						
//			if (cursor.moveToFirst()) {
//				String str;
//				do {
//					str = "";
//					for (String cn : cursor.getColumnNames()) {
//						str = str.concat(cn + " = " + cursor.getString(cursor.getColumnIndex(cn)) + "; ");
//					}
//					Log.d(CLASS_NAME, str);
//				} while (cursor.moveToNext());
//			}

			return cursor;
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
