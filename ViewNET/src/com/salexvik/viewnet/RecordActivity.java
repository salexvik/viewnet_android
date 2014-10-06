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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;



public class RecordActivity extends ActionBarActivity  {

	static final String CLASS_NAME = RecordActivity.class.getSimpleName();
	private static final String mediaStreamType = "record";
	private static final int CM_RENAME_ID = 0;
	private static final int CM_EDIT_ID = 1; 
	private static final int CM_DELETE_ID = 2;


	Context context;

	boolean bound = false; // флаг соединения с сервисом
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса
	ServiceClient serviceClient; // сервис клиента
	ListView listView_mediaStreamsName;
	private ArrayList<String> mediaStreamsNameList; // список имен доступных медиапотоков	
	private ArrayList<String> mediaStreamsIdList; // список идентификаторов доступных медиапотоков	
	private ArrayList<String> mediaStreamsRecordStatusList; // список статусов записи доступных медиапотоков		
	RecordAdapter recordAdapter; // адаптер для списка медиапотоков
	
	BroadcastReceiver br; // приемник широковещательных сообщений

	String mediaStreamId;	

	// Cоздание активити

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		connectToService(); // подключаемся к сервису

		context = RecordActivity.this; // получаем контекст	

		// настраиваем визуальные компоненты

		setContentView(R.layout.viewnet_view);          

		listView_mediaStreamsName = (ListView)findViewById(R.id.listView_mediaStreams); 

		mediaStreamsNameList = new ArrayList<String>(); // создаем массив имен доступных медиапотоков
		recordAdapter = new RecordAdapter(context, mediaStreamsNameList);  // создаем адаптер 
		listView_mediaStreamsName.setAdapter(recordAdapter); // привязываем адаптер к ListView медиапотоков
		mediaStreamsIdList = new ArrayList<String>();  // создаем массив идентификаторов доступных медиапотоков
		mediaStreamsRecordStatusList = new ArrayList<String>();  // создаем массив статусов записи медиапотоков

		registerForContextMenu(listView_mediaStreamsName); // регестрируем контекстное меню для списка имен медиапотоков    
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
		getMenuInflater().inflate(R.menu.viewnet_record_option_menu, menu);
		return true;
	}

	// Обработка выбора пунктов главного меню

	@Override
	// обрабатываем выбор меню - настройки
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		case R.id.add_new_mediastream_to_record:
			// отправляем запрос на данные для создания нового медиапотока
			serviceClient.sendServer(XML.stringToTag("id", "requestDataForAddNewMediaStream"));
			return true;
		}
		return false;
	}


	// Создание контекстного меню для ListView

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);	
		menu.add(0, CM_RENAME_ID, 0, "Переименовать");
		menu.add(0, CM_EDIT_ID, 0, "Изменить");
		menu.add(0, CM_DELETE_ID, 0, "Удалить");
	}

	// Обработчик выбора пункта контекстного меню для ListView

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {

		// получаем инфу о пункте списка
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		final int posMediaStreamName = acmi.position; // получаем позицию выбранного медиапотока

		if (item.getItemId() == CM_RENAME_ID) { // переименовать медиапоток...

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("Новое имя:");
			//	alert.setMessage("Сообщение");
			// Добавим поле ввода
			final EditText input = new EditText(this);			
			// Помещаем в поле ввода изменяемое имя
			input.setText(mediaStreamsNameList.get(posMediaStreamName));
			alert.setView(input);
			alert.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "Введите имя медиапотока", Toast.LENGTH_SHORT).show();
					}
					else{							
						// отправляем команду на переименование медиапотока	
						serviceClient.sendServer(XML.stringToTag("id", "changeMediaStreams")
								+ XML.stringToTag("mediaStreamType", mediaStreamType)
								+ XML.stringToTag("operationId", "renameMediaStream")
								+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(posMediaStreamName)) 
								+ XML.stringToTag("newName", value.toString())); 
					}
				}
			});
			alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Если отменили.
				}
			});
			alert.show();			
		}

		if (item.getItemId() == CM_EDIT_ID) { // изменить медиапоток...
			// отправляем запрос на данные для редактирования медиапотока	
			serviceClient.sendServer(XML.stringToTag("id", "requestDataForEditMediaStream") 
					+ XML.stringToTag("mediaStreamType", mediaStreamType)
					+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(posMediaStreamName)));			
			return true;
		}

		if (item.getItemId() == CM_DELETE_ID) { // удалить медиапоток

			// отправляем команду на удаление медиапотока	
			serviceClient.sendServer(XML.stringToTag("id", "changeMediaStreams") 
					+ XML.stringToTag("mediaStreamType", mediaStreamType)
					+ XML.stringToTag("operationId", "deleteMediaStream")
					+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(posMediaStreamName))); 
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
						recordAdapter.clear(); // очищаем список медиапотоков
					}

					// соединение установлено
					if(connectionStatus.equals("connected")){
						Log.d(CLASS_NAME, "Запрос на список имен доступных медиапотоков и их id");	
						getSupportActionBar().setLogo(R.drawable.circle_green);
						serviceClient.sendServer(XML.stringToTag("id", "requestMediastreamsDataToRecord")); 

					}

					// нет соединения
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
						recordAdapter.clear(); // очищаем список медиапотоков
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


					// получаем данные для редактировани медиапотока
					if(id.equals("responseDataForEditMediaStream")){
						Log.d(CLASS_NAME, "Получены данные для редактирования медиапотока");

						Intent i = new Intent(context, SetMediaStreamsActivity.class);
						i.putExtra("id", "edit"); // передаем идентификатор действия
						i.putExtra("mediaStreamType", mediaStreamType); // передаем тип медиапотока
						// передаем в вызываемую активити изменяемый медиапоток и доступные видео и аудио устройства
						i.putExtra("mediaStream", XML.getString(inData, "mediaStream")); 
						i.putExtra("videoDevices", XML.getString(inData, "videoDevices"));
						i.putExtra("audioDevices", XML.getString(inData, "audioDevices"));
						startActivity(i); // вызываем активити с настройками медиапотока
					}

					// получаем данные для создания нового медиапотока
					if(id.equals("responseDataForAddNewMediaStream")){
						Log.d(CLASS_NAME, "Получены данные для создания нового медиапотока");

						Intent i = new Intent(context, SetMediaStreamsActivity.class);
						i.putExtra("id", "add"); // передаем идентификатор действия
						i.putExtra("mediaStreamType", mediaStreamType); // передаем тип медиапотока
						// передаем в вызываемую активити доступные видео и аудио устройства 
						i.putExtra("videoDevices", XML.getString(inData, "videoDevices"));
						i.putExtra("audioDevices", XML.getString(inData, "audioDevices"));				
						startActivity(i); // вызываем активити с настройками медиапотока						
					}


					// получение списка имен, id и статусов медиапотоков для записи
					if(id.equals("responseMediastreamsDataToRecord")){						
						mediaStreamsNameList.clear();
						mediaStreamsNameList.addAll(XML.getStringArrayList(inData, "mediaStreamName")); // заполняем массив имен доступных медиапотоков								
						mediaStreamsIdList.clear();
						mediaStreamsIdList.addAll(XML.getStringArrayList(inData, "mediaStreamId")); // заполняем массив идентификаторов доступных медиапотоков
						mediaStreamsRecordStatusList.clear();
						mediaStreamsRecordStatusList.addAll(XML.getStringArrayList(inData, "mediaStreamRecordStatus")); // заполняем массив статусов записи
						recordAdapter.notifyDataSetChanged(); // применяем изменение к ListView списка имен медиапотоков
					
					}
					
					
					// получение статуса записи медиапотока после его изменения
					if(id.equals("recordStatus")){
						
						int statusPosition = mediaStreamsIdList.indexOf(XML.getString(inData, "mediaStreamId"));						
						mediaStreamsRecordStatusList.set(statusPosition, XML.getString(inData, "recordStatus"));						
						recordAdapter.notifyDataSetChanged(); // применяем изменение к ListView списка имен медиапотоков						
					}
					

				}
			}
		};
		// создаем фильтр для BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// регистрируем (включаем) BroadcastReceiver
		registerReceiver(br, intFilt);
	}
	
	
	
	class RecordAdapter extends BaseAdapter{
		Context ctx;
		LayoutInflater lInflater;
		ArrayList<String> data;
		Animation anim = null;
		

		RecordAdapter(Context context, ArrayList<String> mediaStreamsNameList) {
			ctx = context;
			data = mediaStreamsNameList;
			lInflater = (LayoutInflater) ctx
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			
			final int pos = position;
			
			// используем созданные, но не используемые view
			View view = convertView;
			if (view == null) {
				view = lInflater.inflate(R.layout.viewnet_record_item, parent, false);
			}

			((TextView) view.findViewById(R.id.textView_mediaStreamName_record)).setText(data.get(position));
			
			ImageView imageView =   (ImageView) view.findViewById(R.id.Image_RecordStatus);
			
			final Button Button_startStopRecord =   (Button) view.findViewById(R.id.button_startStopRecord);
			Button_startStopRecord.setEnabled(true);
			
			final String recordStatus = mediaStreamsRecordStatusList.get(position);
						
			if (recordStatus.equals("started")){	
				anim = AnimationUtils.loadAnimation(ctx, R.anim.viewnet_anim_record);
				imageView.startAnimation(anim);				
				Button_startStopRecord.setText("Стоп");				
			}
						
			if (recordStatus.equals("stop")){	
				imageView.clearAnimation();
				imageView.setVisibility(View.INVISIBLE);				
				Button_startStopRecord.setText("Старт");	
			}
			
			if (recordStatus.equals("starting")){	
				imageView.clearAnimation();
				imageView.setVisibility(View.VISIBLE);				
				Button_startStopRecord.setText("Стоп");
			}
			
			
			Button_startStopRecord.setOnClickListener(new OnClickListener() { 
				@Override
				public void onClick(View v) { 
					
					Button_startStopRecord.setEnabled(false);
					
					if (recordStatus.equals("started") || recordStatus.equals("starting")){
						serviceClient.sendServer(XML.stringToTag("id", "stopRecord") 
								+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(pos)));
					}
										
					if (recordStatus.equals("stop")){
						serviceClient.sendServer(XML.stringToTag("id", "startRecord") 
								+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(pos)));
					}
				}
			}); 
			
			
			return view;
		}
	}
}  




