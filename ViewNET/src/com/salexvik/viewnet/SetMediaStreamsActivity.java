package com.salexvik.viewnet;

import org.videolan.vlc.gui.video.VideoPlayerActivity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
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
import android.widget.Toast;


public class SetMediaStreamsActivity extends ActionBarActivity  {

	private static final String CLASS_NAME = SetMediaStreamsActivity.class.getSimpleName();
	private static final String testMediaStreamId = "testMediaStreamId"; // тестовая ссылка для подключения к медиапотоку

	TextView textView_mediaStreamName;	
	TextView textView_videoDevice;
	TextView textView_audioDevice;
	TextView textView_resolution;
	TextView textView_fps;
	TextView textView_codec;
	TextView textView_bitrate;
	TextView textView_channel;
	TextView textView_recordingTime;


	LinearLayout groop_mediaStreamName;	
	LinearLayout groop_videoDevice;
	LinearLayout groop_audioDevice;
	LinearLayout groop_resolution;
	LinearLayout groop_fps;
	LinearLayout groop_codec;
	LinearLayout groop_bitrate;
	LinearLayout groop_channel;
	LinearLayout groop_recordingTime;

	LinearLayout videoGroop;
	LinearLayout audioGroop;
	LinearLayout recordSettingsGroop;



	CheckBox checkBox_videoEnable;
	CheckBox checkBox_audioEnable;
	CheckBox checkBox_autoStartRecord;



	Button button_mediaStreamTest;
	Button button_mediaStreamSave;

	Context context;

	boolean bound = false; // флаг соединения с сервисом

	private String availableVideoDevicesString; // строка с доступными видеоустройствами
	private String availableAudioDevicesString; // строка с доступными аудиоустройствами
	private String editMediaStream = ""; // редактируемый медиапоток
	private String mediaStreamType; // тип медиапотока
	private String operationId; // идентификатор режима (добавление или редактирования)

	ServiceClient serviceClient; // сервис клиента
	ServiceConnection sConn; // интерфейс для мониторинга состояния сервиса

	BroadcastReceiver br; // приемник широковещательных сообщений



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





	// Создание activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		connectToService(); // подключаемся к сервису

		context = SetMediaStreamsActivity.this;  

		setContentView(R.layout.viewnet_set_mediastream);

		videoGroop = (LinearLayout)findViewById(R.id.VideoGroop);
		audioGroop = (LinearLayout)findViewById(R.id.AudioGroop);
		recordSettingsGroop = (LinearLayout)findViewById(R.id.RecordSettingsGroop);

		textView_mediaStreamName = (TextView)findViewById(R.id.tv_setDevices_deviceName);
		textView_videoDevice = (TextView)findViewById(R.id.textView_videoDevice);
		textView_audioDevice = (TextView)findViewById(R.id.textView_audioDevice);
		textView_resolution = (TextView)findViewById(R.id.textView_resolution);
		textView_fps = (TextView)findViewById(R.id.textView_fps);
		textView_codec = (TextView)findViewById(R.id.textView_codec);	
		textView_bitrate = (TextView)findViewById(R.id.TextView_bitrate);	
		textView_channel = (TextView)findViewById(R.id.TextView_channel);	
		textView_recordingTime = (TextView)findViewById(R.id.textView_recordingTime);

		checkBox_videoEnable = (CheckBox)findViewById(R.id.checkBox_videoEnable);
		checkBox_audioEnable = (CheckBox)findViewById(R.id.checkBox_audioEnable);
		checkBox_autoStartRecord = (CheckBox)findViewById(R.id.checkBox_autoStartRecord);
		
		button_mediaStreamTest = (Button)findViewById(R.id.button_mediaStreamTest);
		button_mediaStreamSave = (Button)findViewById(R.id.button_mediaStreamSave);

		groop_mediaStreamName = (LinearLayout)findViewById(R.id.ll_setDevices_deviceName);
		groop_videoDevice = (LinearLayout)findViewById(R.id.groop_videoDevice);
		groop_audioDevice = (LinearLayout)findViewById(R.id.groop_audioDevice);
		groop_resolution = (LinearLayout)findViewById(R.id.groop_resolution);
		groop_fps = (LinearLayout)findViewById(R.id.groop_fps);
		groop_codec = (LinearLayout)findViewById(R.id.groop_codec);	
		groop_bitrate = (LinearLayout)findViewById(R.id.groop_bitrate);	
		groop_channel = (LinearLayout)findViewById(R.id.groop_channel);		
		groop_recordingTime = (LinearLayout)findViewById(R.id.groop_recordingTime);	


		// получаем данные из интента
		Bundle extras = getIntent().getExtras(); // контейнер с данными интента	
		// получаем иденификатор действия
		operationId = extras.getString("id"); 
		// получаем тип медиапотока
		mediaStreamType = extras.getString("mediaStreamType");
		// получаем строку с доступными видеоустройствами
		availableVideoDevicesString = extras.getString("videoDevices");      
		// получаем строку с доступными аудиоустройствами
		availableAudioDevicesString =  extras.getString("audioDevices");    


		// скрываем неиспользуемые поля настроек		
		if (mediaStreamType.equals("view")){
			recordSettingsGroop.setVisibility(View.GONE); 
		}



		// режим добавления нового медиапотока

		if (operationId.equals("add")){    

			textView_mediaStreamName.setText("Новый медиапоток");			
			visibleVideoSet(false); // скрываем поля
			visibleAudioSet(false); // скрываем поля
			blockButtonTestSave();
		}



		// режим редактирования медиапотока

		if (operationId.equals("edit")){

			// получаем из интента редактируемый медиапоток
			editMediaStream = extras.getString("mediaStream"); // строка редактируемого медиапотока       


			//получаем имя редактируемого медиапотока   
			textView_mediaStreamName.setText(XML.getString(editMediaStream, "mediaStreamName"));


			// получаем текущие параметры  


			if (XML.getBool(editMediaStream, "videoEnable")){
				checkBox_videoEnable.setChecked(true);
				visibleVideoSet(true); // показываем поля настроек видео
			}
			else{
				checkBox_videoEnable.setChecked(false);
				visibleVideoSet(false); // скрываем поля настроек видео
			}

			String tempString = XML.getString(editMediaStream, "videoDevice");			        	
			if (XML.getStringArrayList(availableVideoDevicesString, "videoDevice").indexOf(tempString) == -1){
				textView_videoDevice.setTextColor(Color.RED);		// если видеоустройство в настоящий момент не доступно
			}else  textView_videoDevice.setTextColor(Color.BLACK);   	
			textView_videoDevice.setText(tempString);
			textView_resolution.setText(XML.getString(editMediaStream, "resolution"));
			textView_fps.setText(XML.getString(editMediaStream, "fps"));
			
			
			// показываем или скрываем поля в зависимости от используемого кодека
			String codec = XML.getString(editMediaStream, "codec");
			textView_codec.setText(codec); 						
			if (codec.equals("h264") || codec.equals("h264 camera support")){
				groop_bitrate.setVisibility(View.VISIBLE);
				textView_bitrate.setText(XML.getString(editMediaStream, "bitrate")); 
			}
			else groop_bitrate.setVisibility(View.GONE);
			

			if (XML.getBool(editMediaStream, "audioEnable")){
				checkBox_audioEnable.setChecked(true);
				visibleAudioSet(true); // показываем поля настроек аудио
			}
			else{
				checkBox_audioEnable.setChecked(false);
				visibleAudioSet(false); // скрываем поля настроек аудио
			}

			tempString = XML.getString(editMediaStream, "audioDevice");        	
			if (XML.getStringArrayList(availableAudioDevicesString, "audioDevice").indexOf(tempString) == -1){
				textView_audioDevice.setTextColor(Color.RED);		// если аудиоустройство в настоящий момент не доступно 
			}else textView_audioDevice.setTextColor(Color.BLACK);	
			textView_audioDevice.setText(tempString);			
			textView_channel.setText(XML.getString(editMediaStream, "channel"));  

			if (mediaStreamType.equals("record")){
				checkBox_autoStartRecord.setChecked(XML.getBool(editMediaStream, "autoStartRecord"));	
				textView_recordingTime.setText(XML.getString(editMediaStream, "recordingTime")); 
			}

		}	
	}



	// функция скрытия/показа полей с настройками видео	
	private void visibleVideoSet(boolean aVisible){		
		if (aVisible){
			videoGroop.setVisibility(View.VISIBLE);			
		}
		else{			
			videoGroop.setVisibility(View.GONE);
		}		
	}

	// функция скрытия/показа полей с настройками аудио	
	private void visibleAudioSet(boolean aVisible){	
		if (aVisible){			
			audioGroop.setVisibility(View.VISIBLE);
		}
		else{			
			audioGroop.setVisibility(View.GONE);
		}			
	}


	// функция блокировки/разблокировки кнопок test и save

	private void blockButtonTestSave(){
		if (!checkBox_videoEnable.isChecked() && !checkBox_audioEnable.isChecked()){
			button_mediaStreamTest.setEnabled(false);
			button_mediaStreamSave.setEnabled(false);
		}
		else{
			button_mediaStreamTest.setEnabled(true);
			button_mediaStreamSave.setEnabled(true);
		}
	}




	// обработчик onClick

	public void onClick(View v) {

		AlertDialog.Builder ad = new AlertDialog.Builder(context); // инициализация диалога
		ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		}); 
		final String[] tempArray;

		switch (v.getId()) {


		case R.id.groop_deviceName:
			ad.setTitle("Имя медиапотока"); // заголовок для диалога
			final EditText input = new EditText(this);

			// Помещаем в поле ввода изменяемое имя
			input.setText(textView_mediaStreamName.getText().toString());
			ad.setView(input);	        
			ad.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();					
					if (value.toString().equals("")){
						Toast.makeText(context, "Введите имя медиапотока", Toast.LENGTH_SHORT).show();
					}
					else textView_mediaStreamName.setText(value);
				}
			});
			ad.show();
			break;


		case R.id.checkBox_videoEnable:

			visibleVideoSet(checkBox_videoEnable.isChecked());

			blockButtonTestSave();
			break;

		case R.id.checkBox_audioEnable:

			visibleAudioSet(checkBox_audioEnable.isChecked());

			blockButtonTestSave();
			break;			

		case R.id.groop_videoDevice:	    	
			ad.setTitle("Видеоустройство"); // заголовок для диалога	        
			tempArray = XML.getStringArray(availableVideoDevicesString, "videoDevice");	        
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					textView_videoDevice.setText(tempArray[item]);
					textView_videoDevice.setTextColor(Color.BLACK);
				}
			}); 
			ad.show();
			break;

		case R.id.groop_audioDevice:    	
			ad.setTitle("Аудиоустройство"); // заголовок для диалога	        
			tempArray = XML.getStringArray(availableAudioDevicesString, "audioDevice");	        
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					textView_audioDevice.setText(tempArray[item]);
					textView_audioDevice.setTextColor(Color.BLACK);
				}
			});
			ad.show();
			break;

		case R.id.groop_resolution:	    	
			ad.setTitle("Разрешение"); // заголовок для диалога	        
			tempArray =  getResources().getStringArray(R.array.resolutions_array);     
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					textView_resolution.setText(tempArray[item]);
				}
			});
			ad.show();
			break;

		case R.id.groop_fps:
			ad.setTitle("FPS"); // заголовок для диалога	        
			tempArray =  getResources().getStringArray(R.array.FPS_array);     
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					textView_fps.setText(tempArray[item]);
				}
			});
			ad.show();
			break;

		case R.id.groop_codec:
			ad.setTitle("Кодек"); // заголовок для диалога	        
			tempArray =  getResources().getStringArray(R.array.codecs_array);     
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					String codec = tempArray[item];
					textView_codec.setText(codec);
					
					if (codec.equals("h264") || codec.equals("h264 camera support")) groop_bitrate.setVisibility(View.VISIBLE);
					else groop_bitrate.setVisibility(View.GONE);
				}
			});
			ad.show();
			break;

		case R.id.groop_bitrate:
			ad.setTitle("Битрейт"); // заголовок для диалога	        
			tempArray =  getResources().getStringArray(R.array.bitrates_array);     
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					textView_bitrate.setText(tempArray[item]);
				}
			});
			ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});  
			ad.show();
			break;

		case R.id.groop_channel:
			ad.setTitle("Количество каналов"); // заголовок для диалога	        
			tempArray =  getResources().getStringArray(R.array.channels_array);     
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					textView_channel.setText(tempArray[item]);
				}
			});
			ad.show();
			break;


		case R.id.groop_recordingTime:
			ad.setTitle("Продолжительность записи файла в минутах"); // заголовок для диалога	        
			tempArray =  getResources().getStringArray(R.array.recordingTimes_array);     
			ad.setItems (tempArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					textView_recordingTime.setText(tempArray[item]);
				}
			});
			ad.show();
			break;


		case R.id.button_mediaStreamTest:

			if (mediaStreamType.equals("view")){	
				serviceClient.sendServer(XML.stringToTag("id", "startRtsp")
						+ XML.stringToTag("mediaStream", getCurrentSetMediaStream() 
						+ XML.stringToTag("mediaStreamId", testMediaStreamId)));	// отправляем
			}
			break;

		case R.id.button_mediaStreamSave:

			// Проверяем заполнение поля "имя медиапотока"
			if (textView_mediaStreamName.getText().toString().equals("")){
				Toast.makeText(getApplicationContext(), "Не указано имя медиапотока!", Toast.LENGTH_SHORT).show();
				return;
			}	


			// если находимся в режиме добавления нового медиапотока

			if (operationId.equals("add")){ 									

				// отправляем на устройство команду на добавление нового медиапотока и настроенный медиапоток
				serviceClient.sendServer(XML.stringToTag("id", "changeMediaStreams")
						+ XML.stringToTag("mediaStreamType", mediaStreamType)
						+ XML.stringToTag("operationId", "addMediaStream")
						+ XML.stringToTag("mediaStream", getCurrentSetMediaStream()
								+ XML.stringToTag("mediaStreamId", Other.getRandomString())));
			}


			// если находимся в режиме редактирования медиапотока...

			if (operationId.equals("edit")){

				// отправляем новые параметры медиапотока со старым id 
				serviceClient.sendServer(XML.stringToTag("id", "changeMediaStreams")
						+ XML.stringToTag("mediaStreamType", mediaStreamType)
						+ XML.stringToTag("operationId", "replaceMediastream")
						+ XML.stringToTag("mediaStream", getCurrentSetMediaStream()
								+ XML.getTag(editMediaStream, "mediaStreamId")));			
			}

			finish();		

			break;
		}
	}






	// функция получения строки настроек медиапотока из параметров заданных пользователем

	private String getCurrentSetMediaStream(){
		String currentMediaStream = XML.stringToTag("mediaStreamName", textView_mediaStreamName.getText().toString());

		currentMediaStream += XML.boolToTag("videoEnable", checkBox_videoEnable.isChecked());		
		currentMediaStream += XML.stringToTag("videoDevice", textView_videoDevice.getText().toString());
		currentMediaStream += XML.stringToTag("resolution", textView_resolution.getText().toString());
		currentMediaStream += XML.stringToTag("fps", textView_fps.getText().toString());
		currentMediaStream += XML.stringToTag("codec", textView_codec.getText().toString());	
		currentMediaStream += XML.stringToTag("bitrate", textView_bitrate.getText().toString());

		currentMediaStream += XML.boolToTag("audioEnable", checkBox_audioEnable.isChecked());	
		currentMediaStream += XML.stringToTag("audioDevice", textView_audioDevice.getText().toString());		
		currentMediaStream += XML.stringToTag("channel", textView_channel.getText().toString());

		if (mediaStreamType.equals("record")){
			currentMediaStream += XML.boolToTag("autoStartRecord", checkBox_autoStartRecord.isChecked());
			currentMediaStream += XML.stringToTag("recordingTime", textView_recordingTime.getText().toString());
		}

		return currentMediaStream;
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


					// уведомление об ошибки RTSP сервера
					if(id.equals("rtspServerError")){
						Log.e(CLASS_NAME, "Ошибка RTSP сервера");						
						Toast.makeText(context, "Ошибка RTSP сервера", Toast.LENGTH_SHORT).show();						
					}
					
					
					// уведомление о том, что требуемое для трансляции видео или аудио устройство занято записью
					if(id.equals("deviceBusy")){
						Log.e(CLASS_NAME, "Требуемое видео или аудио устройство занято записью");						
						Toast.makeText(context, "Требуемое видео или аудио устройство занято записью", Toast.LENGTH_SHORT).show();						
					}


					// уведомление о том, что RTSP сервер запускается
					if(id.equals("rtspStarts")){
						Log.d(CLASS_NAME, "RTSP сервер запускается...");
					}

					// уведомление о том, что RTSP сервер готов к подключению
					if(id.equals("rtspStarted")){
						Log.d(CLASS_NAME, "RTSP сервер запущен");						

						// подключаемся к медиапотоку...
						Intent i = new Intent(context, VideoPlayerActivity.class);	
						i.setAction(Intent.ACTION_VIEW); 							
						i.setData(Uri.parse("rtsp://" + serviceClient.getDeviceAddress() + ":" + serviceClient.getDeviceRtspPort() + "/" + testMediaStreamId));			
						startActivity(i);
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
