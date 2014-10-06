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
	boolean bound = false; // ���� ���������� � ��������

	ServiceClient serviceClient; // ������ �������
	ServiceConnection sConn; // ��������� ��� ����������� ��������� �������

	BroadcastReceiver br; // �������� ����������������� ���������


	Button button_rebootDevice;
	Button button_changePassword;


	// �������� activity

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");

		connectToService(); // ������������ � �������

		context = ControlActivity.this;
		setContentView(R.layout.viewnet_control);

		button_rebootDevice = (Button)findViewById(R.id.button_rebootDevice);
		button_changePassword = (Button)findViewById(R.id.button_changePassword);
	}


	// �������������� ��������

	public void onResume() {
		super.onResume();	
		broadcastReceiving(); // ������� �������� ����������������� ��������� 
		if(bound){
			serviceClient.getConnectionStatus(); // �������� ������ ����������			
		}
	}

	// ������������ ��������

	public void onPause() {
		super.onPause();	
		unregisterReceiver(br); // ��������� �������� ����������������� ���������
	}

	// ����������� ��������

	public void onDestroy() {
		super.onDestroy();
		// ����������� �� �������
		if (bound){
			unbindService(sConn);
			bound = false;
		}
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






	// �������� �������� ����

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//getSupportMenuInflater().inflate(R.menu.viewnet_alarms_option_menu, menu);
		return true;
	}

	// ��������� ������ ������� �������� ����

	@Override
	// ������������ ����� ���� - ���������
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {

		//		case R.id.set_telephones_for_sms:
		//			// �������� activity �������� ���������� ������� ��� SMS ����������
		//			Intent intent = new Intent(context, PhonesActivity.class);
		//			startActivity(intent);
		//			return true;

		}
		return false;
	}



	// ���������� onClick

	public void onClick(View v) {


		AlertDialog.Builder ad = new AlertDialog.Builder(this); // ������������� �������
		final EditText input = new EditText(this);

		switch (v.getId()) {

		// ������ ������������ ����������
		case R.id.button_rebootDevice:	    	
			ad.setTitle("������������ ����������");
			ad.setMessage("������ ��������?");			
			ad.setPositiveButton("��, ������, ������������!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {													
					// ����������  ������� ������������ ����������			
					serviceClient.sendServer(XML.stringToTag("id", "controller")
							+XML.stringToTag("command", "stop"));				
				}
			});
			ad.setNegativeButton("��, �� ��� �����!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// ���� ��������.
				}
			});
			ad.show();
			break;





		// ������ ����� ������
		case R.id.button_changePassword:	    	
			ad.setTitle("��������� ������");
			ad.setMessage("������� ����� ������:");
			ad.setView(input);			
			ad.setPositiveButton("��������", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {										
					Editable value = input.getText();						
					String inputPassword = Other.getMd5(value.toString());					
					// ����������  ������� �� ����� ������				
					serviceClient.sendServer(XML.stringToTag("id", "changePassword") + XML.stringToTag("newPassword", inputPassword));					
				}
			});
			ad.setNegativeButton("������", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// ���� ��������.
				}
			});
			ad.show();
			break;


		}

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

					final String inData = intent.getStringExtra("inData"); // �������� ������ � ��������� ������� 					
					String id = XML.getString(inData, "id"); // �������� ������������� �������� ������

					// ����������� � ��������� ��������������
					if(id.equals("authenticationFailed")){
						Log.e(CLASS_NAME, "�������������� �� ��������");							
						serviceClient.authFailed(inData, context); 		
					}

					// ����������� �� �������� ����� ������
					if(id.equals("changePasswordSuccessfully")){
						Log.d(CLASS_NAME, "������ ������� �������");
						Toast.makeText(context, "������ ������� �������", Toast.LENGTH_SHORT).show();	

						// ��������� ������						
						serviceClient.setPassword(XML.getString(inData, "newPassword"));
					}
					
					
					// ����������� �� ������ ����� ������
					if(id.equals("changePasswordFailed")){
						Log.e(CLASS_NAME, "������ ����� ������");
						Toast.makeText(context, "������ ����� ������", Toast.LENGTH_SHORT).show();
					}



				}



			}
		};
		// ������� ������ ��� BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// ������������ (��������) BroadcastReceiver
		registerReceiver(br, intFilt);
	}	


}
