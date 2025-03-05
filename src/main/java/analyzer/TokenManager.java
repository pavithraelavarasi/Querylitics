package analyzer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.TimeZone;

import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenManager {
	static final long serialVersionUID = 1L;
	static final String CLIENT_ID = "1000.Y1AB2INPPY4H2S3VL86MSAITMOFPCP";
	static final String CLIENT_SECRET ="0222e86cacb4dec08feeaf7beab504125cf47b56fb";
	static final String TOKEN_URL = "https://accounts.csez.zohocorpin.com/oauth/v2/token";
	static String DB_URL = "jdbc:mysql://localhost:3306/ChatAI";
    static String DB_USER = "pavi";
    static String DB_PASSWORD = "Pavi@2001";
	public static Connection conn = null;
	
	static {
		  try {
		        System.out.println("------------------------ inside the Token Manager ---------------------");
		        Class.forName("com.mysql.cj.jdbc.Driver");
		        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		  }
		  catch (Exception e) {
			  e.printStackTrace();
		  }
		}
	private String getExistAccToken()  {
		try {
        String query = "SELECT accessToken FROM tokens";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("accessToken");
            }
        }
		}
		catch (Exception e) {
			e.printStackTrace();
		}
        return null;
    }	
	public boolean isAccessTokenExpired() {
	    try {
	        String accessToken = getExistAccToken();
	        if (accessToken == null || accessToken.isEmpty()) {
	            System.out.println("No access token found.");
	            return true; // Token doesn't exist, assume expired.
	        }

	        String query = "SELECT createdAt FROM tokens WHERE accessToken = ?";
	        try (PreparedStatement stmt = conn.prepareStatement(query)) {
	            stmt.setString(1, accessToken);
	            try (ResultSet rs = stmt.executeQuery()) {
	            	if (rs.next()) {
	                    Timestamp createdAtTimestamp = rs.getTimestamp("createdAt");
	                    System.out.println("Time from table : " + createdAtTimestamp);
	                    LocalDateTime createdAt = createdAtTimestamp.toInstant()
	                            .atZone(ZoneId.systemDefault()).toLocalDateTime();

	                    LocalDateTime currentTime = LocalDateTime.now();
	                    System.out.println("Time from lap : " + currentTime);
	                    Duration duration = Duration.between(createdAt, currentTime);
	                    long minutesPassed = duration.toMinutes();

	                    if (minutesPassed > 60) {
	                        System.out.println("Token expired! More than one hour has passed.");
	                    } else {
	                        System.out.println("Token is still valid.");
	                    }
	                } else {
	                    System.out.println("No token found in the database.");
	                }
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return true; // Assume expired if no record is found
	}


    private String getExistRefreshToken()  {
    	try {
        String query = "SELECT refreshToken FROM tokens";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("refreshToken");
            }
        }
    	}
    	catch (Exception e) {
			e.printStackTrace();
		}
        return null;
        
    }

    private void updateTokensInDatabase(String newAccessToken, String newRefreshToken)  {
    	try {
        String query = "UPDATE tokens SET accessToken = ?, refreshToken = ?, createdAt = NOW()";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newAccessToken);
            stmt.setString(2, newRefreshToken);
            
            stmt.executeUpdate();
        }
        System.out.println("Tokens updated successfully in the database.");
    	}
    	catch (Exception e) {
			e.printStackTrace();
		}
    }

    private String refreshAccessToken() {
    	try {
        String refreshToken = getExistRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            System.err.println("No refresh token found. Cannot refresh access token.");
            return null;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build();

        System.out.println("Sending token refresh request...");

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to refresh token: " + response.message());
                return null;
            }

            String responseBody = response.body().string();
            System.out.println("Response body: " + responseBody);

            JSONObject jsonResponse = new JSONObject(responseBody);
            if (jsonResponse.has("access_token")) {
                String newAccessToken = jsonResponse.getString("access_token");
                String newRefreshToken = jsonResponse.optString("refresh_token", refreshToken);

                System.out.println("New Access Token: " + newAccessToken);
                System.out.println("New Refresh Token: " + newRefreshToken);

                // Store the new tokens in the database
                updateTokensInDatabase(newAccessToken, newRefreshToken);
                return newAccessToken;
            } else {
                System.err.println("Access token not found in response.");
            }
        }
    	}
    	catch (Exception e) {
			e.printStackTrace();
		}
        return null;
    }

    public String getValidAccessToken() {
    	try {
        if (isAccessTokenExpired()) {
            return refreshAccessToken();
        }
    	}
    	catch (Exception e) {
    		e.printStackTrace();
			// TODO: handle exception
		}
        return getExistAccToken();

    }
}