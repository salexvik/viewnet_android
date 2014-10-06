package com.salexvik.viewnet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DB {

	private static final String CLASS_NAME = DB.class.getSimpleName();

	private static final String DB_NAME = "viewnet_DB";
	private static final int DB_VERSION = 1;	

	// ����� ������
	public static final String TABLE_NAME_DEVICE_SETTINGS = "deviceSettings";
	public static final String TABLE_NAME_NOTIFICATIONS = "notifications";
	
	// ����� �����
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TYPE_MSG = "type";
	public static final String COLUMN_TIMESTAMP_MSG = "timestamp";
	public static final String COLUMN_MSG = "msg";  
	public static final String COLUMN_ID_MSG = "idMsg";
	public static final String COLUMN_ID_DEVICE = "idDevice";

	public static final String COLUMN_PASSWORD = "password";
	public static final String COLUMN_DEVICE_NAME = "deviceName";
	public static final String COLUMN_VPN_CONNECTION_ADDRESS = "vpnConnectionAddress";
	public static final String COLUMN_VPN_CONNECTION_CONTROL_PORT = "vpnConnectionPort";
	public static final String COLUMN_VPN_CONNECTION_RTSP_PORT = "vpnConnectionRtspPort";
	public static final String COLUMN_CURRENT_DEVICE = "currentDevice";
	public static final String COLUMN_ALLOW_NOTIFICATIONS = "allowNotifications";

	
	// ������� �� �������� ������� �������� ���������
	public static final String TABLE_DEVICES_CREATE_STRING = "create table " + TABLE_NAME_DEVICE_SETTINGS + "(" +
			COLUMN_ID + " integer primary key autoincrement, " +
			COLUMN_DEVICE_NAME + " text, " +
			COLUMN_PASSWORD + " text, " +
			COLUMN_VPN_CONNECTION_ADDRESS + " text, " +
			COLUMN_VPN_CONNECTION_CONTROL_PORT + " text, " +
			COLUMN_VPN_CONNECTION_RTSP_PORT + " text, " +
			COLUMN_CURRENT_DEVICE + " integer, " +
			COLUMN_ALLOW_NOTIFICATIONS + " integer, " +
			COLUMN_ID_DEVICE + " text " +			
			");";

	// ������� �� �������� ������� �����������
	public static final String TABLE_NOTIFICATIONS_CREATE_STRING = "create table " + TABLE_NAME_NOTIFICATIONS + "(" +
			COLUMN_ID + " integer primary key autoincrement, " +
			COLUMN_TYPE_MSG + " text, " +
			COLUMN_TIMESTAMP_MSG + " text, " +
			COLUMN_MSG + " text," +
			COLUMN_ID_MSG + " text," +
			COLUMN_ID_DEVICE + " text" +
			");";	


	private final Context mCtx;

	private String dbTable;
	private DBHelper mDBHelper;
	private SQLiteDatabase mDB;

	public DB(Context ctx, String aTableName) {

		Log.i(CLASS_NAME, "����������� ������ DB");		
		mCtx = ctx;
		dbTable = aTableName;	
	}

	// ������� �����������
	public void open() {
		Log.i(CLASS_NAME, "open()");
		mDBHelper = new DBHelper(mCtx, DB_NAME, null, DB_VERSION);
		mDB = mDBHelper.getWritableDatabase();
	}

	// ������� �����������
	public void close() {
		if (mDBHelper!=null) mDBHelper.close();
	}

	// �������� ��� ������ �� ������� 
	public Cursor getAllData() {
		return mDB.query(dbTable, null, null, null, null, null, null);
	}

	// �������� ������ �� �������
	public Cursor getSelectionData(String aTableName, String[] aColumns, String aConditionString, String[] aSelectionArgs){
		return mDB.query(aTableName, aColumns, aConditionString, aSelectionArgs, null, null, null);
	}

	// �������� ������
	public void addRec(ContentValues cv) {		
		mDB.insert(dbTable, null, cv);
	}

	// �������� ������ �� �������� ���� _id
	public void updateRecById(long id, ContentValues cv) {
		mDB.update(dbTable, cv, COLUMN_ID + " = " + id, null);
	}

	// �������� ������ �� �������
	public void updateRec(ContentValues cv, String aConditionString, String[] aSelectionArgs) {
		mDB.update(dbTable, cv, aConditionString, aSelectionArgs);
	}


	// ������� ���� ������ �� �������� ���� _id
	public void delRecById(long id) {
		mDB.delete(dbTable, COLUMN_ID + " = " + id, null);
	}


	// ������� ������ �� �������
	public void delRec(String aConditionString, String[] aSelectionArgs) {
		mDB.delete(dbTable, aConditionString, aSelectionArgs);
	}




	// ����� �� �������� � ���������� ��
	private class DBHelper extends SQLiteOpenHelper {

		public DBHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		// ������� �������
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(TABLE_DEVICES_CREATE_STRING);
			db.execSQL(TABLE_NOTIFICATIONS_CREATE_STRING);
			Log.i(CLASS_NAME, "�������� ���� ������");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}
}
