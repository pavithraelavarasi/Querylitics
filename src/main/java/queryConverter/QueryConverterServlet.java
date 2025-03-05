package queryConverter;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import analyzer.TokenManager;

@WebServlet("/convert")
public class QueryConverterServlet extends HttpServlet {
    
    private static final String CHAT_API_URL = "https://platformai.csez.zohocorpin.com/internalapi/v2/ai/assistant/chat";
    private static final String PORTAL_ID = "ZS";
    private static final String ASSISTANT_ID = "928000000070455"; // Using the same assistant ID as in QueryAnalyzer
    
    private TokenManager tokenManager;
    private String accessToken;

    @Override
    public void init() throws ServletException {
        super.init();
        tokenManager = new TokenManager();
        accessToken = tokenManager.getValidAccessToken();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StringBuilder requestBody = new StringBuilder();
        String line;

        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }

        System.out.println("Received Request Body: " + requestBody.toString());

        JSONObject clientRequest = new JSONObject(requestBody.toString());
        String sourceDb = clientRequest.getString("sourceDb");
        String targetDb = clientRequest.getString("targetDb");
        String sourceQuery = clientRequest.getString("sourceQuery");

        String instruction = String.format(
            "Convert the following query from %s to %s:\n%s\nIf it is not convertible, provide a short message. If convertible, provide only the query without additional text.",
            sourceDb, targetDb, sourceQuery
        );

        // Check token validity and refresh if needed
        if (accessToken == null || tokenManager.isAccessTokenExpired()) {
            accessToken = tokenManager.getValidAccessToken();
        }

        String conversationId = generateConversationId(instruction);
        String aiResponse = getAiResponse(conversationId);

        // Retry logic if response is not ready
        int retryCount = 0;
        while (aiResponse == null && retryCount < 3) {
            try {
                Thread.sleep(1500); // Wait 1.5 seconds
                aiResponse = getAiResponse(conversationId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retryCount++;
        }

        JSONObject jsonResponse = new JSONObject();
        if (aiResponse != null && !aiResponse.isEmpty()) {
            jsonResponse.put("recommendation", cleanResponse(aiResponse));
        } else {
            jsonResponse.put("error", "Unable to process the query at this time.");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
    }

    private String generateConversationId(String instruction) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("assistant_id", ASSISTANT_ID);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", instruction);
        messages.put(message);

        requestBody.put("messages", messages);

        RequestBody body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            requestBody.toString()
        );

        Request request = new Request.Builder()
                .url(CHAT_API_URL)
                .addHeader("portal_id", PORTAL_ID)
                .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                System.out.println("Conversation Creation Response: " + responseBody);
                JSONObject jsonResponse = new JSONObject(responseBody);
                return jsonResponse.getJSONObject("data").getString("conversation_id");
            } else {
                System.out.println("Failed to create conversation: " + response.code());
            }
        }
        return null;
    }

    private String getAiResponse(String conversationId) throws IOException {
        String url = CHAT_API_URL + "?conversation_id=" + conversationId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("portal_id", PORTAL_ID)
                .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                System.out.println("AI Response Body: " + responseBody);

                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONObject dataObject = jsonResponse.getJSONObject("data");

                if ("in_progress".equals(dataObject.getString("status")) || "queued".equals(dataObject.getString("status"))) {
                    return null; // Still processing
                }

                JSONArray messagesArray = dataObject.getJSONArray("messages");
                if (messagesArray.length() > 0) {
                    JSONObject firstMessage = messagesArray.getJSONObject(0);
                    return firstMessage.getString("content");
                }
            } else {
                System.out.println("AI API request failed with code: " + response.code());
                if (response.code() == 401) {
                    accessToken = tokenManager.getValidAccessToken();
                }
            }
        }
        return "";
    }

    public String cleanResponse(String response) {
        if (response == null) return "";
        return response.replace("\n", "")
                       .replace("`", "")
                       .replace("[", "")
                       .replace("]", "")
                       .replace("\"", "")
                       .replace("\\", "")
                       .trim();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            if (TokenManager.conn != null && !TokenManager.conn.isClosed()) {
                TokenManager.conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}