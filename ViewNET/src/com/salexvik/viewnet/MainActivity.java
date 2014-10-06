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

	boolean bound = false; // ���� ���������� � ��������
	ServiceConnection sConn; // ��������� ��� ����������� ��������� �������
	ServiceClient serviceClient; // ������ �������
	BroadcastReceiver br; // �������� ����������������� ���������
	
	// C������� ��������

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		context = MainActivity.this; // �������� ��������	

		connectToService(); // ������������ � �������


		// ����������� ���������� ����������

		setContentView(R.layout.viewnet_main);          

		button_view = (Button)findViewById(R.id.button_view);
		button_alarm = (Button)findViewById(R.id.button_alarm);
		button_record = (Button)findViewById(R.id.button_record);
		button_simInfo = (Button)findViewById(R.id.button_simInfo);
		button_control = (Button)findViewById(R.id.Button_control);
		button_parameters = (Button)findViewById(R.id.Button_parameters);

		userEventHandler(); // ����������� ���������� ������    

	}



	// ����������� � �������

	private void connectToService(){
		Intent intent = new Intent(this, ServiceClient.class);
		sConn = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.d(CLASS_NAME, "onServiceConnected()");
				serviceClient = ((ServiceClient.ClientBinder) binder).getService(); 
				bound = true; 
				serviceClient.getConnectionStatus(); // �������� ������ ����������
			}
			public void onServiceDisconnected(ComponentName name) {
				Log.d(CLASS_NAME, "onServiceDisconnected()");
				bound = false;
			}
		}; 				
		startService(intent); // ��������� ������
		bindService(intent, sConn, 0); // ������������ � �������
	}


	// ����� ��������

	public void onStart() {
		Log.d(CLASS_NAME,"onStart()");
		super.onStart();
	}

	// �������������� ��������

	public void onResume() {
		Log.d(CLASS_NAME,"onResume()");
		super.onResume();
		broadcastReceiving(); // ������� �������� ����������������� ���������  
		if(bound){
			serviceClient.getConnectionStatus(); // �������� ������ ����������			
		}
	}

	// ������������ ��������

	public void onPause() {
		Log.d(CLASS_NAME,"onPause()");
		super.onPause();	
		unregisterReceiver(br); // ��������� �������� ����������������� ���������
	}


	// ���� ��������

	public void onStop() {
		Log.d(CLASS_NAME,"onStop()");
		super.onStop();
	}

	// ����������� ��������

	public void onDestroy() {
		super.onDestroy();
		Log.d(CLASS_NAME,"onDestroy()");	
				
		if (bound){
			unbindService(sConn);
			bound = false;
		}
	}

	// ���������� ��������� ��������

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(CLASS_NAME, "onSaveInstanceState()");
	}	

	// �������������� ��������� �������� ����� ����� ���������� ��� �������� ��� �������� ������

	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(CLASS_NAME, "onRestoreInstanceState()");
		//        if(savedInstanceState.getBoolean("video")){
		//        	Log.d(activityName, "������������ ����� �����");
		//        }

	}


	// ��������� ������ �� �������

	private void broadcastReceiving(){

		// ������� BroadcastReceiver
		br = new BroadcastReceiver() {
			// �������� ��� ��������� ���������
			public void onReceive(Context context, Intent intent) {

				if(intent.getStringExtra("id").equals("connectionStatus")){

					String connectionStatus = intent.getStringExtra("connectionStatus"); 

					// ����������� �����������
					if(connectionStatus.equals("connecting")){
						getSupportActionBar().setLogo(R.drawable.circle_yellow);
					}

					// ���������� �����������
					if(connectionStatus.equals("connected")){
						getSupportActionBar().setLogo(R.drawable.circle_green);
					}

					// ��� ����������
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
					}
				}


				if(intent.getStringExtra("id").equals("socketData")){

					String inData = intent.getStringExtra("inData"); // �������� ������ � ��������� ������� 					
					String id = XML.getString(inData, "id"); // �������� ������������� �������� ������



					// ����������� � ��������� ��������������
					if(id.equals("authenticationFailed")){
						Log.e(CLASS_NAME, "�������������� �� ��������");							
						serviceClient.authFailed(inData, context); 		
					}
																				
					
				}
			}
		};
		// ������� ������ ��� BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// ������������ (��������) BroadcastReceiver
		registerReceiver(br, intFilt);

	}

	// �������� �������� ����

	@Override

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.viewnet_main_option_menu, menu);

//		menu.add(Menu.NONE, 0, Menu.NONE, "��������")
//		.setIcon(R.drawable.viewnet_media_record)
//		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//
//		menu.add(Menu.NONE, 1, Menu.NONE, "��������")
//		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	// ������� �������� ����




	@Override
	// ������������ ����� ���� - ���������
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
			startActivity(settings_intent); // �������� �������� � ��������� ����������� 
			return true;

		}
		return false;
	}



	// ����������� onClick

	private void userEventHandler(){

		// ������ ������������
		button_alarm.setOnClickListener(new OnClickListener() { // ������� ���������� �������
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, AlarmsActivity.class);
				startActivity(intent);
			}
		}); 


		// ������ �������� �����������                  
		button_view.setOnClickListener(new OnClickListener() { // ������� ���������� �������
			@Override
			public void onClick(View v) { 				
				Intent intent = new Intent(context, ViewActivity.class);
				startActivity(intent);
			}
		}); 

		// ������ ������ ������������
		button_record.setOnClickListener(new OnClickListener() { // ������� ���������� �������
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, RecordActivity.class);
				startActivity(intent);
			}
		}); 

		// ������ SIM-����
		button_simInfo.setOnClickListener(new OnClickListener() { // ������� ���������� �������
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, SimInfoActivity.class);
				startActivity(intent);
			}
		}); 
		
		// ������ ����������
		button_control.setOnClickListener(new OnClickListener() { // ������� ���������� �������
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, ControlActivity.class);
				startActivity(intent);
			}
		}); 
		
		// ������ ���������
		button_parameters.setOnClickListener(new OnClickListener() { // ������� ���������� �������
			@Override
			public void onClick(View v) { 
				Intent intent = new Intent(context, ParametersActivity.class);
				startActivity(intent);
			}
		}); 
		
		 


	}

}   