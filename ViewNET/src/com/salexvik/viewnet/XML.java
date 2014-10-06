package com.salexvik.viewnet;

import java.util.ArrayList;

public class XML {


	// ���������� ���������� �������� � ������ ������� XML

	public static String stringToTag(String aTagName, String aValue){		
		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		return startTag + aValue + endTag;	    
	}

	// ���������� �������� �������� � ������ ������� XML

	public static String boolToTag(String aTagName, boolean aValue){		
		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";

		if (aValue) return startTag + "true" + endTag;
		else return startTag + "false" + endTag;			    
	}



	// ��������� ���������� �������� � ������ ������� XML

	public static String setString(String aStringXML, String aTagName, String aNewValue){

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		int posStartTag = 0;
		int posEndTag = 0;
		String newStringXML = "";

		posStartTag = aStringXML.indexOf(startTag);		
		if(posStartTag == -1) return "";
		posEndTag = aStringXML.indexOf(endTag);
		if(posEndTag == -1) return "";	

		newStringXML = aStringXML.substring(0, posStartTag + startTag.length()) + aNewValue + aStringXML.substring(posEndTag);

		return newStringXML;

	}



	// ��������� ���������� �������� �� ������ � ������� XML

	public static String getString(String aStringXML, String aTagName){

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		int posStartTag = 0;
		int posEndTag = 0;
		String result = "";

		posStartTag = aStringXML.indexOf(startTag);		
		if(posStartTag == -1) return "";
		posEndTag = aStringXML.indexOf(endTag);
		if(posEndTag == -1) return "";		
		result = aStringXML.substring(posStartTag + startTag.length(), posEndTag);				
		return result;		
	}



	// ��������� �������� �������� �� ������ � ������� XML

	public static boolean getBool(String aStringXML, String aTagName){

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		int posStartTag = 0;
		int posEndTag = 0;
		boolean result = false;

		posStartTag = aStringXML.indexOf(startTag);		
		if(posStartTag == -1) return result;
		posEndTag = aStringXML.indexOf(endTag);
		if(posEndTag == -1) return result;	

		if (aStringXML.substring(posStartTag + startTag.length(), posEndTag).equals("true")) result = true;
		if (aStringXML.substring(posStartTag + startTag.length(), posEndTag).equals("false")) result = false;

		return result;		
	}





	// ��������� ���� �� ������ � ������� XML

	public static String getTag(String aStringXML, String aTagName){

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		int posStartTag = 0;
		int posEndTag = 0;
		String result = "";

		posStartTag = aStringXML.indexOf(startTag);		
		if(posStartTag == -1) return "";
		posEndTag = aStringXML.indexOf(endTag);
		if(posEndTag == -1) return "";		
		result = aStringXML.substring(posStartTag, posEndTag  + endTag.length());				
		return result;		
	}



	// ��������� ArrayList �� ��������� ���������� �� ������ � ������� XML

	public static ArrayList<String> getStringArrayList(String aStringXML, String aTagName){

		ArrayList<String> arrayList = new ArrayList<String>();

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		int posStartTag = 0;
		int posEndTag = 0;


		while(aStringXML.indexOf(startTag) != -1){

			posStartTag = aStringXML.indexOf(startTag);		
			if(posStartTag == -1) return arrayList;
			posEndTag = aStringXML.indexOf(endTag);
			if(posEndTag == -1) return arrayList;				
			arrayList.add(aStringXML.substring(posStartTag + startTag.length(), posEndTag));
			aStringXML = aStringXML.substring(posEndTag + endTag.length(), aStringXML.length());			
		}

		return arrayList;
	}


	// ��������� ArrayList � �������� ���������� �� ������ � ������� XML

	public static ArrayList<Boolean> getBoolArrayList(String aStringXML, String aTagName){

		ArrayList<Boolean> arrayList = new ArrayList<Boolean>();

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		int posStartTag = 0;
		int posEndTag = 0;


		while(aStringXML.indexOf(startTag) != -1){

			posStartTag = aStringXML.indexOf(startTag);		
			if(posStartTag == -1) return arrayList;
			posEndTag = aStringXML.indexOf(endTag);
			if(posEndTag == -1) return arrayList;

			if(aStringXML.substring(posStartTag + startTag.length(), posEndTag).equals("true")) arrayList.add(true);
			if(aStringXML.substring(posStartTag + startTag.length(), posEndTag).equals("false")) arrayList.add(false);

			aStringXML = aStringXML.substring(posEndTag + endTag.length(), aStringXML.length());			
		}

		return arrayList;
	}



	// ��������� ������� ��������� �������� �� ������ � ������� XML

	public static String[] getStringArray(String aStringXML, String aTagName){

		ArrayList<String> list = getStringArrayList(aStringXML, aTagName);			
		String[] s = (String[])list.toArray(new String[0]);

		return s;
	}



	//������ �������� � ������ ������� XML

	public static String arrayValuesToXMLString(String aIdArrayValues, String aTagName, ArrayList<String> aArrayValues){

		String tempString = "";

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";

		// �������� � ����� �� ������� �������

		for (int i=0; i<aArrayValues.size(); i++){
			tempString += startTag + aArrayValues.get(i) + endTag;		
		}

		startTag = "<" + aIdArrayValues + ">";
		endTag = "</" + aIdArrayValues + ">";

		return startTag + tempString + endTag;
	}


	// �������� ���� �������� �������� �� ������ � ������� XML

	public static String delTagsFromXML(String aDataXML, String aTagName){

		String startTag = "<" + aTagName + ">";
		String endTag = "</" + aTagName + ">";
		int posStartTag = 0;
		int posEndTag = 0;


		while((posStartTag = aDataXML.indexOf(startTag)) != -1){
			posEndTag = aDataXML.indexOf(endTag);
			if(posEndTag == -1) return aDataXML;
			aDataXML = aDataXML.substring(0, posStartTag) + aDataXML.substring(posEndTag + endTag.length());
		}

		return aDataXML;
	}





}




