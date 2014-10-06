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
	ListViewAdapter listViewAdapter; // ������� ��� ������ ������������

	DB db; // ���� ������

	Context context;

	boolean bound = false; // ���� ���������� � ��������

	ServiceClient serviceClient; // ������ �������
	ServiceConnection sConn; // ��������� ��� ����������� ��������� �������

	BroadcastReceiver br; // �������� ����������������� ���������


	// �������� activity

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		connectToService(); // ������������ � �������

		context = DevicesActivity.this;

		setContentView(R.layout.viewnet_devices);

		listView_devices = (ListView) findViewById(R.id.listView_devices);

		deviceNameList = new ArrayList<String>();
		listViewAdapter = new ListViewAdapter(context, deviceNameList);  // ������� ������� 		
		listView_devices.setAdapter(listViewAdapter); 

		idDeviceList = new ArrayList<String>(); 
		deviceStatusList = new ArrayList<Integer>(); 
		recordIdList = new ArrayList<Integer>(); 
		allowNotificationList = new ArrayList<Integer>(); 


		// ������� ������ ��� ������ � ����� ������
		db = new DB(this, DB.TABLE_NAME_DEVICE_SETTINGS);

		// ������������ � ��
		db.open();

		// ������������ ����������� ���� ��� ListView
		registerForContextMenu(listView_devices);

		// �������� ������ �� ����
		getDataFromDB();


		// ������� ���� �� ������ ������������ - �������� ����������
		listView_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {				

				ContentValues cv = new ContentValues();
				
				// ���������� ������ �������� ��������� ����������				
				cv.put(DB.COLUMN_CURRENT_DEVICE, 0);
				db.updateRec(cv, DB.COLUMN_CURRENT_DEVICE + " = 1", null); 
				
				// ������������� ������ ������ ��������� ����������
				cv.clear();
				cv.put(DB.COLUMN_CURRENT_DEVICE, 1);						
				db.updateRecById(recordIdList.get(position), cv);
				
				getDataFromDB(); // ��������� listView
				
				// ��������� ������� ������� ����������
				serviceClient.socketClose();
				
				// �������� ������� ID ���������� ����������
				serviceClient.setIdDevice(idDeviceList.get(position));

			}
		});		
	}


	// �������� ������ �� ����

	void getDataFromDB(){
		deviceNameList.clear();
		idDeviceList.clear();
		deviceStatusList.clear();
		recordIdList.clear();
		allowNotificationList.clear();

		Cursor c = db.getAllData();

		// ������ ������� ������� �� ������ ������ �������
		if (c.moveToFirst()) {
			do {
				// �������� �������� �� ������� ��������
				deviceNameList.add(c.getString(c.getColumnIndex(DB.COLUMN_DEVICE_NAME)));
				idDeviceList.add(c.getString(c.getColumnIndex(DB.COLUMN_ID_DEVICE)));
				deviceStatusList.add(c.getInt(c.getColumnIndex(DB.COLUMN_CURRENT_DEVICE)));
				recordIdList.add(c.getInt(c.getColumnIndex(DB.COLUMN_ID)));
				allowNotificationList.add(c.getInt(c.getColumnIndex(DB.COLUMN_ALLOW_NOTIFICATIONS)));
			} while (c.moveToNext());
		}
		c.close();
		
		// ��������� ListView
		listViewAdapter.notifyDataSetChanged();
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


	// �������������� ��������

	public void onResume() {
		super.onResume();
		broadcastReceiving(); // ������� �������� ����������������� ��������� 
		if(bound){
			serviceClient.getConnectionStatus(); // �������� ������ ����������			
		}
		getDataFromDB();
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





	// �������� �������� ����

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MM_ADD, 0, "�������� ����������");
		return true;
	}

	// ��������� ������ ������� �������� ����

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {	

		// ���������� ������ ����������
		if (item.getItemId() == MM_ADD) {	
			
			Intent i = new Intent(context, SetDevicesActivity.class);	
			i.putExtra("recordId", -1);
			startActivity(i); // �������� ��������			
			return true;
		}
		
		return false;
	}



	// �������� ������������ ���� ��� ListView

	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);	
		menu.add(0, CM_EDIT_DEVICE, 0, "��������");
		menu.add(0, CM_DELETE_DEVICE, 0, "�������");
	}

	// ���������� ������ ������ ������������ ���� ��� ListView

	public boolean onContextItemSelected(android.view.MenuItem item) {

		// �������� �� ������ ������������ ���� ������ �� ������ ������
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		final int position = acmi.position; // �������� ������� ���������� �����������


		// ������������� ����������
		if (item.getItemId() == CM_EDIT_DEVICE) {

			Intent i = new Intent(context, SetDevicesActivity.class);
			i.putExtra("recordId", recordIdList.get(position));			
			i.putExtra("deviceName", deviceNameList.get(position));
			i.putExtra("idDevice", idDeviceList.get(position));
			i.putExtra("allowNotifications", allowNotificationList.get(position));
			
			startActivity(i); // �������� ��������

			return true;
		}


		// ������� ����������
		if (item.getItemId() == CM_DELETE_DEVICE) {			
			// ������� ��������������� ������ � ��
			db.delRecById(recordIdList.get(position));

			// ��������� ListView
			getDataFromDB();
			return true;

		}		

		return super.onContextItemSelected(item);
	}






	// ���������� onClick

	public void onClick(View v) {

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

			// ���������� ���������, �� �� ������������ view
			View view = convertView;
			if (view == null) {
				view = lInflater.inflate(R.layout.viewnet_devices_item, parent, false);
			}

			((TextView) view.findViewById(R.id.textView_deviceName)).setText(data.get(position));
			((TextView) view.findViewById(R.id.textView_idDevice)).setText(idDeviceList.get(position));

			ImageView imageView_deviceStatus = (ImageView) view.findViewById(R.id.imageView_deviceStatus);

			// �������� ������ ����������
			int deviceStatus = deviceStatusList.get(position);

			if (deviceStatus == 0) imageView_deviceStatus.setVisibility(View.INVISIBLE); 
			if (deviceStatus == 1) imageView_deviceStatus.setVisibility(View.VISIBLE); 


			return view;
		}
	}



}
