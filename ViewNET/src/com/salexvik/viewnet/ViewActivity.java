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

	boolean bound = false; // ���� ���������� � ��������
	ServiceConnection sConn; // ��������� ��� ����������� ��������� �������
	ServiceClient serviceClient; // ������ �������
	ListView listView_mediaStreamsName;
	private ArrayList<String> mediaStreamsNameList; // ������ ���� ��������� ������������	
	private ArrayList<String> mediaStreamsIdList; // ������ ��������������� ��������� ������������	
	private ArrayAdapter<String> mediaStreamsNameListAdapter; // ������� ��� ������ ������������

	BroadcastReceiver br; // �������� ����������������� ���������

	String mediaStreamId;	

	// C������� ��������

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		connectToService(); // ������������ � �������

		context = ViewActivity.this; // �������� ��������	

		// ����������� ���������� ����������

		setContentView(R.layout.viewnet_view);          

		listView_mediaStreamsName = (ListView)findViewById(R.id.listView_mediaStreams); 

		mediaStreamsNameList = new ArrayList<String>(); // ������� ������ ���� ��������� ������������
		mediaStreamsNameListAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, mediaStreamsNameList); // ������� 
		listView_mediaStreamsName.setAdapter(mediaStreamsNameListAdapter); // ����������� ������� � ListView ������������
		mediaStreamsIdList = new ArrayList<String>();  // ������� ������ ��������������� ��������� ������������

		registerForContextMenu(listView_mediaStreamsName); // ������������ ����������� ���� ��� ������ ���� ������������    


		// ������� ���� �� ������ ������������ - �������� ������� �� ������ RTSP ������� 
		listView_mediaStreamsName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {				
				mediaStreamId = mediaStreamsIdList.get(position);	
				serviceClient.sendServer(XML.stringToTag("id", "startRtsp") 
						+ XML.stringToTag("mediaStreamId", mediaStreamId));
				// ������ ���� ����������� � ������ RTSP ������� ��� ����������� � �����������...
			}
		});
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




	// �������� �������� ����

	@Override

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.viewnet_view_option_menu, menu);
		return true;
	}

	// ������� �������� ����

	@Override
	// ������������ ����� ���� - ���������
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		case R.id.add_new_mediastream_to_view:
			// ���������� ������ �� ������ ��� �������� ������ �����������
			serviceClient.sendServer(XML.stringToTag("id", "requestDataForAddNewMediaStream"));
			return true;
			
		case R.id.VLC_settings:	
			Intent i = new Intent(context, PreferencesActivity.class);						
			startActivity(i); 
			return true;
			
		}
		return false;
	}


	// �������� ������������ ���� ��� ListView

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);	
		menu.add(0, CM_RENAME_ID, 0, "�������������");
		menu.add(0, CM_EDIT_ID, 0, "��������");
		menu.add(0, CM_DELETE_ID, 0, "�������");
	}

	// ���������� ������ ������ ������������ ���� ��� ListView

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {

		// �������� ���� � ������ ������
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		final int posMediaStreamName = acmi.position; // �������� ������� ���������� �����������

		if (item.getItemId() == CM_RENAME_ID) { // ������������� ����������...

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("����� ���:");
			//	alert.setMessage("���������");
			// ������� ���� �����
			final EditText input = new EditText(this);			
			// �������� � ���� ����� ���������� ���
			input.setText(mediaStreamsNameList.get(posMediaStreamName));
			alert.setView(input);
			alert.setPositiveButton("��", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "������� ��� �����������", Toast.LENGTH_SHORT).show();
					}
					else{							
						// ���������� ������� �� �������������� �����������	
						serviceClient.sendServer(XML.stringToTag("id", "changeMediaStreams")
								+ XML.stringToTag("mediaStreamType", mediaStreamType)
								+ XML.stringToTag("operationId", "renameMediaStream")
								+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(posMediaStreamName)) 
								+ XML.stringToTag("newName", value.toString())); 
					}
				}
			});
			alert.setNegativeButton("������", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// ���� ��������.
				}
			});
			alert.show();			
		}

		if (item.getItemId() == CM_EDIT_ID) { // �������� ����������...
			// ���������� ������ �� ������ ��� �������������� �����������	
			serviceClient.sendServer(XML.stringToTag("id", "requestDataForEditMediaStream") 
					+ XML.stringToTag("mediaStreamType", mediaStreamType)
					+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(posMediaStreamName)));			
			return true;
		}

		if (item.getItemId() == CM_DELETE_ID) { // ������� ����������

			// ���������� ������� �� �������� �����������	
			serviceClient.sendServer(XML.stringToTag("id", "changeMediaStreams") 
					+ XML.stringToTag("mediaStreamType", mediaStreamType)
					+ XML.stringToTag("operationId", "deleteMediaStream")
					+ XML.stringToTag("mediaStreamId", mediaStreamsIdList.get(posMediaStreamName))); 
			return true;
		}
		return super.onContextItemSelected(item);
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
						mediaStreamsNameListAdapter.clear(); // ������� ������ ������������
					}

					// ���������� �����������
					if(connectionStatus.equals("connected")){
						Log.d(CLASS_NAME, "������ �� ������ ���� ��������� ������������ � �� id");	
						getSupportActionBar().setLogo(R.drawable.circle_green);
						serviceClient.sendServer(XML.stringToTag("id", "requestMediastreamsDataToView")); // ������ �� ������ ���� ��������� ������������ � �� id

					}

					// ��� ����������
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
						mediaStreamsNameListAdapter.clear(); // ������� ������ ������������
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


					// ����������� �� ������ RTSP �������
					if(id.equals("rtspServerError")){
						Log.e(CLASS_NAME, "������ RTSP �������");						
						Toast.makeText(context, "������ RTSP �������", Toast.LENGTH_SHORT).show();						
					}

					// ����������� � ���, ��� RTSP ������ �����������
					if(id.equals("rtspStarts")){
						Log.d(CLASS_NAME, "RTSP ������ �����������...");
					}


					// ����������� � ���, ��� ��������� ��� ���������� ����� ��� ����� ���������� ������ �������
					if(id.equals("deviceBusy")){						
						Log.e(CLASS_NAME, "��������� ����� ��� ����� ���������� ������ �������");					
						AlertDialog.Builder alert = new AlertDialog.Builder(context);			
						alert.setTitle("������������� ����������� � ����������");
						alert.setMessage("� ��������� ������ ����������� ��� ������ ���������� ����� ��� ����� ���������� ������������ ��� ������."
								+ "���� ���������� �����������, ������ ����� ��������������, � ������������ ������������� ����� ��������� ����������");			
						alert.setPositiveButton("����������", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {										
								
								// �������� ����������  ������� �� ������ RTSP ������� � ������ forcedStart								
								serviceClient.sendServer(XML.getString(inData, "oldData")
										+ XML.boolToTag("forcedStart", true));
							}
						});
						alert.setNegativeButton("������", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// ���� ��������.
							}
						});
						alert.show();
						
						
					}
					
					
					// ����������� � ���, ��� RTSP ������ ����� � �����������
					if(id.equals("rtspStarted")){
						Log.d(CLASS_NAME, "RTSP ������ �������");						

						// ������������ � �����������...
						Intent i = new Intent(context, VideoPlayerActivity.class);	
						i.setAction(Intent.ACTION_VIEW); 									
						i.setData(Uri.parse("rtsp://" + serviceClient.getDeviceAddress() + ":" + serviceClient.getDeviceRtspPort() + "/" + mediaStreamId));			
						startActivity(i);
					}



					// �������� ������ ��� ������������� �����������
					if(id.equals("responseDataForEditMediaStream")){
						Log.d(CLASS_NAME, "�������� ������ ��� �������������� �����������");

						Intent i = new Intent(context, SetMediaStreamsActivity.class);
						i.putExtra("id", "edit"); // �������� ������������� ��������
						i.putExtra("mediaStreamType", mediaStreamType); // �������� ��� �����������
						// �������� � ���������� �������� ���������� ���������� � ��������� ����� � ����� ����������
						i.putExtra("mediaStream", XML.getString(inData, "mediaStream")); 
						i.putExtra("videoDevices", XML.getString(inData, "videoDevices"));
						i.putExtra("audioDevices", XML.getString(inData, "audioDevices"));
						startActivity(i); // �������� �������� � ����������� �����������
					}

					// �������� ������ ��� �������� ������ �����������
					if(id.equals("responseDataForAddNewMediaStream")){
						Log.d(CLASS_NAME, "�������� ������ ��� �������� ������ �����������");

						Intent i = new Intent(context, SetMediaStreamsActivity.class);
						i.putExtra("id", "add"); // �������� ������������� ��������
						i.putExtra("mediaStreamType", mediaStreamType); // �������� ��� �����������
						// �������� � ���������� �������� ��������� ����� � ����� ���������� 
						i.putExtra("videoDevices", XML.getString(inData, "videoDevices"));
						i.putExtra("audioDevices", XML.getString(inData, "audioDevices"));				
						startActivity(i); // �������� �������� � ����������� �����������						
					}


					// ��������� ������ ���� ��������� ������������ � �� id
					if(id.equals("responseMediastreamsDataToView")){						
						mediaStreamsNameList.clear();
						mediaStreamsNameList.addAll(XML.getStringArrayList(inData, "mediaStreamName")); // ��������� ������ ���� ��������� ������������
						mediaStreamsNameListAdapter.notifyDataSetChanged(); // ��������� ��������� � ListView ������ ���� ������������						
						mediaStreamsIdList.clear();
						mediaStreamsIdList.addAll(XML.getStringArrayList(inData, "mediaStreamId")); // ��������� ������ ��������������� ��������� ������������
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
