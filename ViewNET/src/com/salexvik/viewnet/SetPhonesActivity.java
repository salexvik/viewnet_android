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
import android.graphics.Color;
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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SetPhonesActivity extends ActionBarActivity  {




	static final String CLASS_NAME = SetPhonesActivity.class.getSimpleName();
	private static final int CM_RENAME_ID = 0;
	private static final int CM_SET_PHONE_NUMBER_ID = 1; 
	private static final int CM_DELETE_ID = 2;


	Context context;

	boolean bound = false; // ���� ���������� � ��������
	ServiceConnection sConn; // ��������� ��� ����������� ��������� �������
	ServiceClient serviceClient; // ������ �������
	ListView listView_phoneName;
	private ArrayList<String> phoneNameList; // ������ ���� ��������� ���������� �������	
	private ArrayList<String> phoneNumberList; // ������ ���������� �������
	private ArrayList<String> phoneIdList; // ������ ��������������� ��������� ���������� �������	
	private ArrayList<Boolean> phoneStatusList; // ������ �������� ��������� ���������� �������		
	PhoneNameAdapter phoneNameAdapter; // ������� ��� ������ ���� ��������� ���������� �������

	BroadcastReceiver br; // �������� ����������������� ���������

	// C������� ��������

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS_NAME,"onCreate()");  

		connectToService(); // ������������ � �������

		context = SetPhonesActivity.this; // �������� ��������	

		// ����������� ���������� ����������

		setContentView(R.layout.viewnet_set_phones);          

		listView_phoneName = (ListView)findViewById(R.id.listView_telName); 

		phoneNameList = new ArrayList<String>(); // ������� ������ ���� ���������� �������
		phoneNumberList =  new ArrayList<String>(); // ������� ������ ���������� �������
		phoneIdList = new ArrayList<String>();  // ������� ������ ��������������� ���������� �������
		phoneStatusList = new ArrayList<Boolean>();  // ������� ������ �������� ���������� �������
		phoneNameAdapter = new PhoneNameAdapter(context, phoneNameList);  // ������� ������� ��� ���� ���������� �������	
		listView_phoneName.setAdapter(phoneNameAdapter); // ����������� ������� � ListView ���� ���������� �������

		registerForContextMenu(listView_phoneName); // ������������ ����������� ���� ��� ������ ���� ���������� �������  
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
		getMenuInflater().inflate(R.menu.viewnet_sms_option_menu, menu);
		return true;
	}

	// ������� �������� ����

	@Override
	// ������������ ����� ���� - ���������
	public boolean onOptionsItemSelected(MenuItem item) {	
		switch (item.getItemId()) {
		case R.id.add_new_telNumber:

			// ���������� ������ ��������

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("����� ��������:");
			//	alert.setMessage("���������");
			// ������� ���� �����
			final EditText input = new EditText(this);			
			alert.setView(input);
			alert.setPositiveButton("��", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "�� ������ ����� ��������!", Toast.LENGTH_SHORT).show();
					}
					else{			

						// ��������� �������� � �������
						phoneNameList.add(value.toString()); 
						phoneNumberList.add(value.toString());
						phoneStatusList.add(true);
						phoneIdList.add(Other.getRandomString());

						int pos = phoneNameList.size() -1;
						String phone = XML.stringToTag("phoneName", phoneNameList.get(pos)) 
								+ XML.stringToTag("phoneNumber", phoneNumberList.get(pos))
								+ XML.boolToTag("phoneStatus", phoneStatusList.get(pos))
								+ XML.stringToTag("phoneId", phoneIdList.get(pos));		
						// ���������� ������� �� ���������� ��������	
						serviceClient.sendServer(XML.stringToTag("id", "changePhones")
								+ XML.stringToTag("operationId", "addPhone")
								+ XML.stringToTag("phone", phone));
					}
				}
			});
			alert.setNegativeButton("������", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// ���� ��������.
				}
			});
			alert.show();

			return true;
		}
		return false;
	}


	// �������� ������������ ���� ��� ListView

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,	ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);	
		menu.add(0, CM_RENAME_ID, 0, "�������������");
		menu.add(0, CM_SET_PHONE_NUMBER_ID, 0, "�������� ����� ��������");
		menu.add(0, CM_DELETE_ID, 0, "�������");
	}

	// ���������� ������ ������ ������������ ���� ��� ListView

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {

		// �������� ���� � ������ ������
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		final int posPhoneName = acmi.position; // �������� ������� ���������� ����� ��������

		if (item.getItemId() == CM_RENAME_ID) { //�������� ���

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("����� ���:");
			//	alert.setMessage("���������");
			// ������� ���� �����
			final EditText input = new EditText(this);			
			// �������� � ���� ����� ���������� ���
			input.setText(phoneNameList.get(posPhoneName));
			alert.setView(input);
			alert.setPositiveButton("��", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "������� ���", Toast.LENGTH_SHORT).show();
					}
					else{	
						phoneNameList.set(posPhoneName, value.toString()); // ������ �������� � ������� ����
						// ���������� ������� �� ������  ��������	
						serviceClient.sendServer(XML.stringToTag("id", "changePhones")
								+ XML.stringToTag("operationId", "setPhoneName")								
								+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))
								+ XML.stringToTag("phoneName", phoneNameList.get(posPhoneName)));  
					}
				}
			});
			alert.setNegativeButton("������", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// ���� ��������.
				}
			});
			alert.show();	
			
			return true;
		}

		if (item.getItemId() == CM_SET_PHONE_NUMBER_ID) { // �������� ����� ��������

			AlertDialog.Builder alert = new AlertDialog.Builder(this);			
			alert.setTitle("����� ����� ��������:");
			//	alert.setMessage("���������");
			// ������� ���� �����
			final EditText input = new EditText(this);			
			// �������� � ���� ����� ���������� ���
			input.setText(phoneNumberList.get(posPhoneName));
			alert.setView(input);
			alert.setPositiveButton("��", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();

					if (value.toString().equals("")){
						Toast.makeText(getApplicationContext(), "������� ����� ��������", Toast.LENGTH_SHORT).show();
					}
					else{							
						phoneNumberList.set(posPhoneName, value.toString()); // ������ �������� � ������� �������
						// ���������� ������� �� ������  ������	
						serviceClient.sendServer(XML.stringToTag("id", "changePhones")
								+ XML.stringToTag("operationId", "setPhoneNumber")								
								+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))
								+ XML.stringToTag("phoneNumber", phoneNumberList.get(posPhoneName))); 
					}
				}
			});
			alert.setNegativeButton("������", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// ���� ��������.
				}
			});
			alert.show();

			return true;
		}

		if (item.getItemId() == CM_DELETE_ID) { // ������� �������

			// ���������� ������� �� �������� ����������� ������
			serviceClient.sendServer(XML.stringToTag("id", "changePhones")
					+ XML.stringToTag("operationId", "deletePhone")
					+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))); 

			// ������� �������� �� ��������...
			phoneNameList.remove(posPhoneName);
			phoneNumberList.remove(posPhoneName);
			phoneStatusList.remove(posPhoneName);
			phoneIdList.remove(posPhoneName);
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
						phoneNameAdapter.clear(); // ������� ������ ���� ���������
					}

					// ���������� �����������
					if(connectionStatus.equals("connected")){
						Log.d(CLASS_NAME, "������ �� ������ ���� ���������, �������, �� id � �������");	
						getSupportActionBar().setLogo(R.drawable.circle_green);
						serviceClient.sendServer(XML.stringToTag("id", "requestPhones")); 

					}

					// ��� ����������
					if(connectionStatus.equals("disconnected")){
						getSupportActionBar().setLogo(R.drawable.circle_red);
						phoneNameAdapter.clear(); // ������� ������ ������������
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
					
					// ������������� ��������� ���������� ��������� (����� ��������, ���������� ��� ��������������)
					if(id.equals("changePhonesSuccessfully")){
						phoneNameAdapter.notifyDataSetChanged(); // ��������� ��������� � ListView ������ ���� ������������
						Toast.makeText(context, "���������", Toast.LENGTH_SHORT).show();
					}



					// ��������� ������ ����, �������, id � �������� ���������
					if(id.equals("responsePhones")){						
						phoneNameList.clear();
						phoneNumberList.clear();
						phoneIdList.clear();
						phoneStatusList.clear();

						phoneNameList.addAll(XML.getStringArrayList(inData, "phoneName")); // ��������� ������ 	
						phoneNumberList.addAll(XML.getStringArrayList(inData, "phoneNumber")); // ��������� ������						
						phoneIdList.addAll(XML.getStringArrayList(inData, "phoneId")); // ��������� ������ 				
						phoneStatusList.addAll(XML.getBoolArrayList(inData, "phoneStatus")); // ��������� ������ 						
						phoneNameAdapter.notifyDataSetChanged(); // ��������� ��������� � ListView ������ ���� ������������					
					}



				}
			}
		};
		// ������� ������ ��� BroadcastReceiver
		IntentFilter intFilt = new IntentFilter("com.salexvik.viewnet.br");
		// ������������ (��������) BroadcastReceiver
		registerReceiver(br, intFilt);
	}




	class PhoneNameAdapter extends BaseAdapter{
		Context ctx;
		LayoutInflater lInflater;
		ArrayList<String> data;


		PhoneNameAdapter(Context context, ArrayList<String> phoneNameList) {
			ctx = context;
			data = phoneNameList;
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

			final int posPhoneName = position;

			// ���������� ���������, �� �� ������������ view
			View view = convertView;
			if (view == null) {
				view = lInflater.inflate(R.layout.viewnet_phones_item, parent, false);
			}


			final CheckBox checkBox_phoneStatus = (CheckBox) view.findViewById(R.id.checkBox_phoneStatus);
			TextView textView_phoneName = (TextView) view.findViewById(R.id.textView_setDeviceName);

			checkBox_phoneStatus.setEnabled(true);

			checkBox_phoneStatus.setChecked(phoneStatusList.get(position));			
			textView_phoneName.setText(data.get(position));
			
			if (checkBox_phoneStatus.isChecked())
				textView_phoneName.setTextColor(Color.WHITE);
				else textView_phoneName.setTextColor(Color.DKGRAY);
			

			checkBox_phoneStatus.setOnClickListener(new OnClickListener() { 
				@Override
				public void onClick(View v) { 

					checkBox_phoneStatus.setEnabled(false); // ��������� �� ����� ���������� �������

					phoneStatusList.set(posPhoneName, checkBox_phoneStatus.isChecked()); // ������ �������� � ������� ��������
					// ���������� ������� �� ������  ��������	
					serviceClient.sendServer(XML.stringToTag("id", "changePhones")
							+ XML.stringToTag("operationId", "setPhoneStatus")								
							+ XML.stringToTag("phoneId", phoneIdList.get(posPhoneName))
							+ XML.boolToTag("phoneStatus", phoneStatusList.get(posPhoneName)));  
				}
			}); 


			return view;
		}
	}


}
