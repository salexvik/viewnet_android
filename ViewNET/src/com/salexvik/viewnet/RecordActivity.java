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

	boolean bound = false; // ���� ���������� � ��������
	ServiceConnection sConn; // ��������� ��� ����������� ��������� �������
	ServiceClient serviceClient; // ������ �������
	ListView listView_mediaStreamsName;
	private ArrayList<String> mediaStreamsNameList; // ������ ���� ��������� ������������	
	private ArrayList<String> mediaStreamsIdList; // ������ ��������������� ��������� ������������	
	private ArrayList<String> mediaStreamsRecordStatusList; // ������ �������� ������ ��������� ������������		
	RecordAdapter recordAdapter; // ������� ��� ������ ������������
	
	BroadcastReceiver br; // �������� ����������������� ���������

	String mediaStreamId;	

	// C������� ��������

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		connectToService(); // ������������ � �������

		context = RecordActivity.this; // �������� ��������	

		// ����������� ���������� ����������

		setContentView(R.layout.viewnet_view);          

		listView_mediaStreamsName = (ListView)findViewById(R.id.listView_mediaStreams); 

		mediaStreamsNameList = new ArrayList<String>(); // ������� ������ ���� ��������� ������������
		recordAdapter = new RecordAdapter(context, mediaStreamsNameList);  // ������� ������� 
		listView_mediaStreamsName.setAdapter(recordAdapter); // ����������� ������� � ListView ������������
		mediaStreamsIdList = new ArrayList<String>();  // ������� ������ ��������������� ��������� ������������
		mediaStreamsRecordStatusList = new ArrayList<String>();  // ������� ������ �������� ������ ������������

		registerForContextMenu(listView_mediaStreamsName); // ������������ ����������� ���� ��� ������ ���� ������������    
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
		getMenuInflater().inflate(R.menu.viewnet_record_option_menu, menu);
		return true;
	}

	// ��������� ������ ������� �������� ����

	@Override
	// ������������ ����� ���� - ���������
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		case R.id.add_new_mediastream_to_record:
			// ���������� ������ �� ������ ��� �������� ������ �����������
			serviceClient.sendServer(XML.stringToTag("id", "requestDataForAddNewMediaStream"));
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
						recordAdapter.clear(); // ������� ������ ������������
					}

					// ���������� �����������
					if(connectionStatus.equals("connected")){
						Log.d(CLASS_NAME, "������ �� ������ ���� ��������� ������������ � �� id");	
						getSupportActionBar().setLogo(R.drawable.circle_green);
						serviceClient.sendServer(XML.stringToTag("id", "requestMediastreamsDataToRecord")); 

					}

					// ��� ����������
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
						recordAdapter.clear(); // ������� ������ ������������
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


					// ��������� ������ ����, id � �������� ������������ ��� ������
					if(id.equals("responseMediastreamsDataToRecord")){						
						mediaStreamsNameList.clear();
						mediaStreamsNameList.addAll(XML.getStringArrayList(inData, "mediaStreamName")); // ��������� ������ ���� ��������� ������������								
						mediaStreamsIdList.clear();
						mediaStreamsIdList.addAll(XML.getStringArrayList(inData, "mediaStreamId")); // ��������� ������ ��������������� ��������� ������������
						mediaStreamsRecordStatusList.clear();
						mediaStreamsRecordStatusList.addAll(XML.getStringArrayList(inData, "mediaStreamRecordStatus")); // ��������� ������ �������� ������
						recordAdapter.notifyDataSetChanged(); // ��������� ��������� � ListView ������ ���� ������������
					
					}
					
					
					// ��������� ������� ������ ����������� ����� ��� ���������
					if(id.equals("recordStatus")){
						
						int statusPosition = mediaStreamsIdList.indexOf(XML.getString(inData, "mediaStreamId"));						
						mediaStreamsRecordStatusList.set(statusPosition, XML.getString(inData, "recordStatus"));						
						recordAdapter.notifyDataSetChanged(); // ��������� ��������� � ListView ������ ���� ������������						
					}
					

				}
			}
		};
		// ������� ������ ��� BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// ������������ (��������) BroadcastReceiver
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

		// ���-�� ���������
		@Override
		public int getCount() {
			return data.size();
		}

		// ������� �� �������
		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		// id �� �������
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		

		// ����� ������
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			final int pos = position;
			
			// ���������� ���������, �� �� ������������ view
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
				Button_startStopRecord.setText("����");				
			}
						
			if (recordStatus.equals("stop")){	
				imageView.clearAnimation();
				imageView.setVisibility(View.INVISIBLE);				
				Button_startStopRecord.setText("�����");	
			}
			
			if (recordStatus.equals("starting")){	
				imageView.clearAnimation();
				imageView.setVisibility(View.VISIBLE);				
				Button_startStopRecord.setText("����");
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




