import org.vertx.java.core.Handler;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

public class MSB extends Verticle {
		
	private String[] databaseInstances = new String[2];		
	/* 
	 * init -initializes the variables which store the 
	 *	     DNS of your database instances
	 */
	private void init() {
		/* Add the DNS of your database instances here */
		databaseInstances[0] = "<FIRST DCI'S DNS>";
		databaseInstances[1] = "<SECOND DCI'S DNS>";
	}
	
	/*
	 * checkBackend - verifies that the DCI are running before starting this server
	 */	
	private boolean checkBackend() {
    	try{
    		if(sendRequest(generateURL(0,"1")) == null ||
            	sendRequest(generateURL(1,"1")) == null)
        		return true;
    	} catch (Exception ex) {
    		System.out.println("Exception is " + ex);
    	}

    	return false;
	}

	/*
	 * sendRequest
	 * Input: URL
	 * Action: Send a HTTP GET request for that URL and get the response
	 * Returns: The response
	 */
	private String sendRequest(String requestUrl) throws Exception {
		 
		URL url = new URL(requestUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
 
		BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), "UTF-8"));
		
		String responseCode = Integer.toString(connection.getResponseCode());
		if(responseCode.startsWith("2")){
			String inputLine;
			StringBuffer response = new StringBuffer();
 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			return response.toString();
    	} else {
    		System.out.println("Unable to connect to "+requestUrl+
    		". Please check whether the instance is up and also the security group settings"); 
    		return null;
    	}   
	}
	/*
	 * generateURL
	 * Input: Instance ID of the Data Center id
	 * Returns: URL which can be used to retrieve the user's details
	 * 			from the data center instance
	 * Additional info: the user's details are cached on backend instance
	 */
	private String generateURL(Integer instanceID, String key) {
		return "http://" + databaseInstances[instanceID] + "/target?targetID=" + key;
	}
	
	/*
	 * generateRangeURL
	 * Input: 	Instance ID of the Data Center
	 * 		  	startRange - starting range (id)
	 *			endRange - ending range (id)
	 * Returns: URL which can be used to retrieve the details of all
	 * 			user in the range from the data center instance
	 * Additional info: the details of the last 1000 user are cached
	 * 					in the database instance
	 * 				
	 */
	private String generateRangeURL(Integer instanceID, String startRange, String endRange) {
		return "http://" + databaseInstances[instanceID] + "/range?start_range="
				+ (startRange) + "&end_range=" + (endRange);
	}

	/* 
	 * retrieveDetails - you have to modify this function to achieve a higher RPS value
	 * Input: the targetID
	 * Returns: The result from querying the database instance
	 */
	private String retrieveDetails(String targetID) {
		try{
			return sendRequest(generateURL(0, targetID));
		} catch (Exception ex){
			System.out.println(ex);
			return null;
		}
	}

	private String retrieveDetails(String start, String end) {
		try{
			return sendRequest(generateRangeURL(0, start, end));
		} catch (Exception ex){
			System.out.println(ex);
			return null;
		}
	}
	
	/* 
	 * processRequest - calls the retrieveDetails function with the id
	 */
	private void processRequest(String id, HttpServerRequest req) {
		String result = retrieveDetails(id);
		if(result != null)
			req.response().end(result);	
		else
			req.response().end("No resopnse received");
	}

	private void processRequest(String start, String end, HttpServerRequest req) {
		String result = retrieveDetails(start, end);
		if(result != null)
			req.response().end(result);	
		else
			req.response().end("No resopnse received");
	}
	
	/*
	 * start - starts the server
	 */
  	public void start() {
  		init();
		if(!checkBackend()){
  			vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
				public void handle(HttpServerRequest req) {
				    String query_type = req.path();		
				    req.response().headers().set("Content-Type", "text/plain");
				    if(query_type.equals("/target")) {
					    String key = req.params().get("targetID");
					    processRequest(key,req);
				    } else if (query_type.equals("/range")) {
				    	String start = req.params().get("start_range");
				    	String end = req.params().get("end_range");
				    	processRequest(start, end, req);
				    }
			    }               
			}).listen(80);
		} else {
			System.out.println("Please make sure that both your DCI are up and running");
		}
	}
}
