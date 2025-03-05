package analyzer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;


import com.mysql.cj.protocol.x.SyncFlushDeflaterOutputStream;

@WebServlet("/projectAnalyzer")
public class ProjectAnalyzer extends HttpServlet {
 
//	String assId = "928000000056941";
	String assId = "928000000076317";
	private static String conversationId = null;
	TokenManager tManager = new TokenManager();
	String accessToken = tManager.getValidAccessToken();	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    System.out.println("Inside doPost");

	    // Read JSON body from request
	    StringBuilder jsonBuffer = new StringBuilder();
	    String line;
	    try (BufferedReader reader = request.getReader()) {
	        while ((line = reader.readLine()) != null) {
	            jsonBuffer.append(line);
	        }
	    }
	    	
	    // Convert string to JSON
	    JSONObject jsonRequest = new JSONObject(jsonBuffer.toString());
	    String queryContent = jsonRequest.optString("content", "").trim();

	    System.out.println("Received Query: " + queryContent);

	    if (queryContent.isEmpty()) {
	        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query content is required");
	        return;
	    }

	    // Generate a conversation ID for tracking AI responses
	    String convoId = generateConversationId(queryContent);
	    System.out.println("Using convo ID: " + convoId);

	    // Fetch AI response
	    String aiResponse = getAIResponse(convoId);

	    // Retry logic if AI response is null
	    int retryCount = 0;
	    while (aiResponse == null && retryCount < 3) {
	        try {
	            Thread.sleep(1500);  // Wait for 1.5 seconds
	            aiResponse = getAIResponse(convoId);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	        retryCount++;
	    }

	    // If AI response is still null, return an error message
	    if (aiResponse == null || aiResponse.isEmpty()) {
	    	aiResponse = getAIResponse(convoId);
//	        aiResponse = "AI is still processing the request. Please try again later.";
	    }

	    // Prepare JSON response
	    JSONObject jsonResponse = new JSONObject();
	    jsonResponse.put("result", aiResponse);

	    // Send response to frontend
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    response.getWriter().write(jsonResponse.toString());

	    System.out.println("Sent Response: " + jsonResponse);
	}


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		System.out.print("hello");
	    doPost(request, response);  // Redirect GET to POST
	    }



  /* public String getAssId(String accessToken) throws IOException {
    System.out.println("Access token: " + accessToken);
    final MediaType MEDIA_TYPE = MediaType.parse("application/json");

    JSONObject requestBody = new JSONObject();
    requestBody.put("name", "assistant_ai");
    requestBody.put("model", "gpt-4o");
    requestBody.put("ai_vendor", "openai");
    requestBody.put("description", "Database Advisor AI");

    String instructions = "You are an expert database advisor. Your job is to analyze user queries and recommend the most suitable database based on the structure of the query or the project requirements.\n\n" +
            "Instructions:\n" +
            "- If the user provides an SQL query, analyze the query syntax and determine the best-suited database (e.g., MySQL, PostgreSQL, MongoDB, Cassandra, etc.).\n" +
            "- If the user provides a project idea, identify key data storage needs (structured, semi-structured, unstructured, high-volume, real-time, etc.) and recommend an appropriate database.\n" +
            "- If the input is not related to databases, do not respond or politely inform the user that you only provide database recommendations.\n\n" +
            "Response Guidelines:\n" +
            "- Clearly state the recommended database and provide a brief justification.\n" +
            "- If multiple databases are suitable, suggest alternatives and explain their pros/cons.\n" +
            "- Avoid off-topic responses. Do not provide coding help, general AI responses, or unrelated advice.\n\n" +
            "Example Responses:\n\n" +
            "User Input:\n" +
            "\"SELECT * FROM users WHERE email LIKE '%@gmail.com%';\"\n\n" +
            "AI Response:\n" +
            "*This query is best suited for relational databases like MySQL or PostgreSQL because it involves structured data storage and SQL querying.*\n\n" +
            "User Input:\n" +
            "\"I’m building a real-time chat application with millions of messages per second. Which database should I use?\"\n\n" +
            "AI Response:\n" +
            "*For a real-time chat application handling high-speed writes, a NoSQL database like Cassandra or Redis is recommended. Cassandra is great for scalability, while Redis excels in caching and fast retrieval.*\n\n" +
            "User Input:\n" +
            "\"How do I implement OAuth in Java?\"\n\n" +
            "AI Response:\n" +
            "*I'm specialized in database recommendations. Please ask me about database selection for your project or SQL query analysis.*";

    requestBody.put("instructions", instructions);

    System.out.println("JSON Object: " + requestBody);

    RequestBody body = RequestBody.create(MEDIA_TYPE, requestBody.toString());
    Request request = new Request.Builder()
            .url("https://platformai.csez.zohocorpin.com/internalapi/v2/ai/assistant")
            .addHeader("portal_id", "ZS")  
            .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)  
            .addHeader("Content-Type", "application/json")
            .post(body) 
            .build();

    System.out.println("Request: " + request);

    // Send the request
    OkHttpClient client = new OkHttpClient();
    try (Response response = client.newCall(request).execute()) {
        String responseBody = response.body().string();
        System.out.println("Full response: " + responseBody);

        if (response.isSuccessful()) {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONObject data = jsonResponse.optJSONObject("data");
            return (data != null) ? data.optString("assistant_id", null) : null;
        } else {
            System.out.println("Request failed with code ASSID: " + response.code());
            System.out.println("Error response: " + responseBody);  // Print error details
            return null;
        }
    }
}*/

	private String generateConversationId(String userMessage) throws IOException {
	    JSONObject requestBody = new JSONObject();
	//    String assId = getAssId(accessToken);
	    requestBody.put("assistant_id", assId);
	
	    JSONArray messages = new JSONArray();
	    JSONObject message = new JSONObject();
	    message.put("role", "user");
	//        message.put("content", userMessage); 
	    message.put("content", userMessage != null ? userMessage.trim() : ""+" which database should i use?"); // Ensure content is not empty
	
	    messages.put(message);
	
	    requestBody.put("messages", messages);
	
	    System.out.println("Request Body: " + requestBody);
	
	    RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody.toString());
	    Request request = new Request.Builder()
	            .url("https://platformai.csez.zohocorpin.com/internalapi/v2/ai/assistant/chat")
	            .addHeader("portal_id", "ZS")
	            .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)  // FIXED: Corrected OAuth token format
	            .addHeader("Content-Type", "application/json")
	            .post(body)
	            .build();
	
	    OkHttpClient client = new OkHttpClient();
	    try (Response response = client.newCall(request).execute()) {
	        String responseBody = response.body() != null ? response.body().string() : "";
	        System.out.println("Full Response: " + responseBody);
	
	        if (response.isSuccessful()) {
	            JSONObject jsonResponse = new JSONObject(responseBody);
	            return jsonResponse.optJSONObject("data").optString("conversation_id", null);
	        } else {
	            System.out.println("Request failed with code: " + response.code());
	            System.out.println("Error response: " + responseBody);
	        }
	    }
	    return null;
	}


	public String getAIResponse(String conversationId) throws IOException  {
	
	    String url = "https://platformai.csez.zohocorpin.com/internalapi/v2/ai/assistant/chat?conversation_id=" + conversationId;
	
	    Request request = new Request.Builder()
	            .url(url)  
	            .addHeader("portal_id", "ZS") 
	            .addHeader("Authorization", "Zoho-oauthtoken " + accessToken) 
	            .addHeader("Content-Type", "application/json")
	            .get()
	            .build();
	    
	    System.out.println("AI request ------" + request);
	    
	    //OkHttpClient client = new OkHttpClient();
	    OkHttpClient client = new OkHttpClient.Builder()
	            .connectTimeout(30, TimeUnit.SECONDS)  
	            .readTimeout(60, TimeUnit.SECONDS)     
	            .writeTimeout(60, TimeUnit.SECONDS)    
	            .build();
	    
	    try (Response response = client.newCall(request).execute()) {
	        if (response.isSuccessful() && response.body() != null) {
	            
	            // Store response body in a variable
	            String responseBody = response.body().string();
	            
	            System.out.println("Response Body: " + responseBody);  // Debugging
	            
	            JSONObject jsonResponse = new JSONObject(responseBody);
	            
	            // if status is inprogress thread.sleep(1 sec){call the method inside this method recursion)}
	            if (jsonResponse.has("data")) {  
	                JSONObject dataObject = jsonResponse.getJSONObject("data");  // Get the "data" key
	                if(dataObject.getString("status").equals("in_progress") || dataObject.getString("status").equals("queued") ) {
	                	getAIResponse(conversationId);
	                	//return null;
	                }
	                if (dataObject.has("messages")) {  
	                    JSONArray messagesArray = dataObject.getJSONArray("messages");  // Get the "messages" array
	                    
	                    if (messagesArray.length() > 0) {  
	                        JSONObject firstMessage = messagesArray.getJSONObject(0);  // Get the first message
	                        
	                        if (firstMessage.has("content")) {  
	                            String aiResponse = firstMessage.getString("content");  
	                            System.out.println("AI Response: " + aiResponse);
	                            return aiResponse;
	                        } else {
	                            System.out.println("'content' key not found in the first message.");
	                        }
	                    } else {
	                        System.out.println("'messages' array is empty.");
	                    }
	                } else {
	                    System.out.println("'messages' key is missing in 'data'.");
	                }
	            } else {
	                System.out.println("'data' key is missing in the response.");
	            }
	        } else {
	            System.out.println("Request failed with code: " + response.code());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return "";
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	}
}
