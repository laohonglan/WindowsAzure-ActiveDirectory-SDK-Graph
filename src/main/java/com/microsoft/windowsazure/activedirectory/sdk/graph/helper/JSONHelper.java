package com.microsoft.windowsazure.activedirectory.sdk.graph.helper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.microsoft.azure.activedirectory.sampleapp.config.SampleConfig;
import com.microsoft.azure.activedirectory.sampleapp.controllers.RoleServlet;
import com.microsoft.windowsazure.activedirectory.sdk.graph.exceptions.SampleAppException;

/**
 * This class provides the methods to parse JSON Data from a JSON Formatted String.
 * @author Azure Active Directory Contributor
 *
 */
public class JSONHelper {

	private static Logger logger  = Logger.getLogger(JSONHelper.class);
	
	
	JSONHelper(){
	//	PropertyConfigurator.configure("log4j.properties");
	}
	/**
	 * This method parses an JSON Array out of a collection of JSON Objects within
	 * a string.
	 * @param jSonData The JSON String that holds the collection.
	 * @return An JSON Array that would contains all the collection object.
	 * @throws SampleAppException 
	 */  
	public static JSONArray fetchDirectoryObjectJSONArray(JSONObject jsonObject) throws SampleAppException {
		JSONArray jsonArray = new JSONArray();	
		jsonArray = jsonObject.optJSONObject("responseMsg").optJSONArray("value");
		return jsonArray;
	}
	
	/**
	 * This method parses an JSON Object out of a collection of JSON Objects within a string
	 * @param jsonObject
	 * @return An JSON Object that would contains the DirectoryObject.
	 * @throws SampleAppException
	 */
	public static JSONObject fetchDirectoryObjectJSONObject(JSONObject jsonObject) throws SampleAppException {
		JSONObject jObj = new JSONObject();	
		jObj = jsonObject.optJSONObject("responseMsg");
		return jObj;
	} 
	
	/**
	 * This method parses the skip token from a json formatted string.
	 * @param jsonData The JSON Formatted String.
	 * @return The skipToken.
	 * @throws SampleAppException 
	 */
	public static String fetchNextSkiptoken(JSONObject jsonObject) throws SampleAppException {
		String skipToken = "";	
		// Parse the skip token out of the string.
		skipToken =jsonObject.optJSONObject("responseMsg").optString("odata.nextLink");
		
		if(!skipToken.equalsIgnoreCase("")){				
			// Remove the unnecessary prefix from the skip token.
			int index = skipToken.indexOf("$skiptoken=") + ( new String("$skiptoken=") ).length();
			skipToken = skipToken.substring(index);
		}	
		return skipToken;
	}
	
	/**
	 * @param jsonObject
	 * @return
	 * @throws SampleAppException
	 */
	public static String fetchDeltaLink(JSONObject jsonObject) throws SampleAppException {
		String deltaLink = "";	
		// Parse the skip token out of the string.
		deltaLink = jsonObject.optJSONObject("responseMsg").optString("aad.deltaLink");
		if(deltaLink == null || deltaLink.length() == 0){
			deltaLink = jsonObject.optJSONObject("responseMsg").optString("aad.nextLink");
			logger.info("deltaLink empty, nextLink ->" + deltaLink);

		}
		if(!deltaLink.equalsIgnoreCase("")){				
			// Remove the unnecessary prefix from the skip token.
			int index = deltaLink.indexOf("deltaLink=") + ( new String("deltaLink=") ).length();
			deltaLink = deltaLink.substring(index);
		}	
		return deltaLink;
	}
	
   /**
	 * This method would create a string consisting of a JSON document with all the necessary elements
	 * set from the HttpServletRequest request.	
	 * @param request The HttpServletRequest
	 * @return the string containing the JSON document.
	 * @throws SampleAppException If there is any error processing the request.
	 */
	@SuppressWarnings("unchecked")
	public static String createJSONString(HttpServletRequest request, String controller) throws SampleAppException {
		JSONObject obj = new JSONObject();
		try{	
			Field[] allFields = Class.forName("com.microsoft.windowsazure.activedirectory.sdk.graph.models." + controller).getDeclaredFields();
			String[] allFieldStr = new String[allFields.length];
			for(int i = 0 ; i < allFields.length; i ++){
				allFieldStr[i] = allFields[i].getName();
			}
			List<String> allFieldStringList = Arrays.asList(allFieldStr);
			Enumeration<String> fields = request.getParameterNames();

			while(fields.hasMoreElements()){

				String fieldName = fields.nextElement();
				String param = request.getParameter(fieldName);
				if(allFieldStringList.contains(fieldName)){
					if(param == null || param.length() == 0){
						if( ! fieldName.equalsIgnoreCase("password")){
							obj.put(fieldName, JSONObject.NULL);
						}		
					}else{
						if( fieldName.equalsIgnoreCase("password")){
							obj.put("passwordProfile", new JSONObject("{\"password\": \"" + param + "\"}"));
						}else{
							obj.put(fieldName, param);

						}
					}
				}	
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}		
		return obj.toString();
	}
	
	/**
	 * 
	 * @param key  
	 * @param value
	 * @return string format of this JSON obje
	 * @throws SampleAppException
	 */
	public static String createJSONString(String key, String value) throws SampleAppException {
	
		JSONObject obj = new JSONObject();
		try {
			obj.put(key, value);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return obj.toString();
	}
	
	/**
	 * This is a generic method that copies the simple attribute values from an argument jsonObject to an argument
	 * generic object.
	 * @param jsonObject The jsonObject from where the attributes are to be copied.
	 * @param destObject The object where the attributes should be copied into.
	 * @throws SampleAppException Throws a SampleAppException when the operation are unsuccessful.
	 */
    public static <T> void convertJSONObjectToDirectoryObject(JSONObject jsonObject, T destObject) throws SampleAppException{
    	
    	// Get the list of all the field names.
    	Field[] fieldList = destObject.getClass().getDeclaredFields();
    
    	// For all the declared field.
    	for(int i = 0; i < fieldList.length; i++){
    		// If the field is of type String, that is
    		// if it is a simple attribute.
    		if(fieldList[i].getType().equals(String.class)){
				try {
					// Invoke the corresponding set method of the destObject using the argument taken from the jsonObject.
					destObject.getClass().getMethod(String.format("set%s", WordUtils.capitalize(fieldList[i].getName())), 
							new Class[]{String.class}).
					invoke(destObject, 
							new Object[]{jsonObject.optString(fieldList[i].getName())});
				} catch (Exception e) {
					throw new SampleAppException(
							SampleConfig.internalError, SampleConfig.internalErrorMessage, e);
				} 
    		}
    	}
    }
    
    public static JSONArray joinJSONArrays(JSONArray a, JSONArray b){
    	JSONArray comb = new JSONArray();
    	for(int i = 0 ; i < a.length(); i ++){
    		comb.put(a.optJSONObject(i));
    	}
    	for(int i = 0 ; i < b.length(); i ++){
    		comb.put(b.optJSONObject(i));
    	}
    	return comb;
    }
    
}
