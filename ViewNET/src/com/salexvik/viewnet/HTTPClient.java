package com.salexvik.viewnet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;



public class HTTPClient {
	
	private static final String CLASS_NAME = HTTPClient.class.getSimpleName();
	
		
	// получение данных
	
	public static String getDataForVPNConnection(String aUrl){
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;

		String responseString = "";
		HttpParams httpParams = httpclient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 5*1000); // задаем таймаут соединения
		try {
			response = httpclient.execute(new HttpGet(aUrl));
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				responseString = out.toString();
			} else {
				//Closes the connection.
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			Log.e(CLASS_NAME, "Исключение при HTTP соединении" + e.toString());
		} catch (IOException e) {
			Log.e(CLASS_NAME, "Исключение при получении данных для VPN соединения " + e.toString());
		}
		
		
		return responseString;
		
	}
	
}


