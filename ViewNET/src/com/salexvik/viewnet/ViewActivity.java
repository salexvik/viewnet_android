package com.salexvik.viewnet;

import java.util.ArrayList;

import org.videolan.vlc.gui.PreferencesActivity;
import org.videolan.vlc.gui.video.VideoPlayerActivity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ViewActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = ViewActivity.class.getSimpleName();
	private static final String mediaStreamType = "view";
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
	private ArrayAdapter<String> mediaStreamsNameListAdapter; // адаптер для списка медиапотоков

	BroadcastReceiver br; // приемник широковещательных сообщений

	String mediaStreamId;	

	// Cоздание активити

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		connectToService(); // подключаемся к сервису

		context = ViewActivity.this; // получаем контекст	

		// настраиваем визуальные компоненты

		setContentView(R.layout.viewnet_view);          

		listView_mediaStreamsName = (ListView)findViewById(R.id.listView_mediaStreams); 

		mediaStreamsNameList = new ArrayList<String>(); // создаем массив имен доступных медиапотоков
		mediaStreamsNameListAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, mediaStreamsNameList); // адаптер 
		listView_mediaStreamsName.setAdapter(mediaStreamsNameListAdapter); // привязываем адаптер к ListView медиапотоков
		mediaStreamsIdList = new ArrayList<String>();  // создаем массив идентификаторов доступных медиапотоков

		registerForContextMenu(listView_mediaStreamsName); // регестрируем контекстное меню для списка имен медиапотоков    


		// событие Клик по списку медиапотоков - отправка команды на запуск RTSP сервера 
		listView_mediaStreamsName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {				
				mediaStreamId = mediaStreamsIdList.get(position);	
				serviceClient.sendServer(XML.stringToTag("id", "startRtsp") 
						+ XML.stringToTag("mediaStreamId", mediaStreamId));
				// дальше ждем уведомление о старте RTSP сервера для подключения к медиапотоку...
			}
		});
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
		getMenuInflater().inflate(R.menu.viewnet_view_option_menu, menu);
		return true;
	}

	// События главного меню

	@Override
	// обрабатываем выбор меню - настройки
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		case R.id.add_new_mediastream_to_view:
			// отправляем запрос на данные для создания нового медиапотока
			serviceClient.sendServer(XML.stringToTag("id", "requestDataForAddNewMediaStream"));
			return true;
			
		case R.id.VLC_settings:	
			Intent i = new Intent(context, PreferencesActivity.class);						
			startActivity(i); 
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
						mediaStreamsNameListAdapter.clear(); // очищаем список медиапотоков
					}

					// соединение установлено
					if(connectionStatus.equals("connected")){
						Log.d(CLASS_NAME, "Запрос на список имен доступных медиапотоков и их id");	
						getSupportActionBar().setLogo(R.drawable.circle_green);
						serviceClient.sendServer(XML.stringToTag("id", "requestMediastreamsDataToView")); // запрос на список имен доступных медиапотоков и их id

					}

					// нет соединения
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
						mediaStreamsNameListAdapter.clear(); // очищаем список медиапотоков
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


					// уведомление об ошибки RTSP сервера
					if(id.equals("rtspServerError")){
						Log.e(CLASS_NAME, "Ошибка RTSP сервера");						
						Toast.makeText(context, "Ошибка RTSP сервера", Toast.LENGTH_SHORT).show();						
					}

					// уведомление о том, что RTSP сервер запускается
					if(id.equals("rtspStarts")){
						Log.d(CLASS_NAME, "RTSP сервер запускается...");
					}


					// уведомление о том, что требуемое для трансляции видео или аудио устройство занято записью
					if(id.equals("deviceBusy")){						
						Log.e(CLASS_NAME, "Требуемое видео или аудио устройство занято записью");					
						AlertDialog.Builder alert = new AlertDialog.Builder(context);			
						alert.setTitle("Подтверждение подключения к трансляции");
						alert.setMessage("В настоящий момент необходимое для начала трансляции видео или аудио устройство используется для записи."
								+ "Если продолжить подключение, запись будет приостановлена, и возобновится автоматически после окончания трансляции");			
						alert.setPositiveButton("Продолжить", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {										
								
								// повторно отправляем  команду на запуск RTSP сервера с флагом forcedStart								
								serviceClient.sendServer(XML.getString(inData, "oldData")
										+ XML.boolToTag("forcedStart", true));
							}
						});
						alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Если отменили.
							}
						});
						alert.show();
						
						
					}
					
					
					// уведомление о том, что RTSP сервер готов к подключению
					if(id.equals("rtspStarted")){
						Log.d(CLASS_NAME, "RTSP сервер запущен");						

						// подключаемся к медиапотоку...
						Intent i = new Intent(context, VideoPlayerActivity.class);	
						i.setAction(Intent.ACTION_VIEW); 									
						i.setData(Uri.parse("rtsp://" + serviceClient.getDeviceAddress() + ":" + serviceClient.getDeviceRtspPort() + "/" + mediaStreamId));			
						startActivity(i);
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


					// получение списка имен доступных медиапотоков и их id
					if(id.equals("responseMediastreamsDataToView")){						
						mediaStreamsNameList.clear();
						mediaStreamsNameList.addAll(XML.getStringArrayList(inData, "mediaStreamName")); // заполняем массив имен доступных медиапотоков
						mediaStreamsNameListAdapter.notifyDataSetChanged(); // применяем изменение к ListView списка имен медиапотоков						
						mediaStreamsIdList.clear();
						mediaStreamsIdList.addAll(XML.getStringArrayList(inData, "mediaStreamId")); // заполняем массив идентификаторов доступных медиапотоков
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
