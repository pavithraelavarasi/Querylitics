package home;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/allDb")  // Defines the servlet's URL mapping
public class CompareAllDb extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");  // Response is JSON format
        response.setCharacterEncoding("UTF-8");      // Set UTF-8 encoding
        PrintWriter out = response.getWriter();      // Get response writer
        JSONObject jsonResponse = new JSONObject();  // JSON response object
        Map<String, Long> dbTimings = new LinkedHashMap<>(); // Stores execution times for databases
        
        try {
            // Read JSON request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // Parse request JSON
            JSONObject jsonRequest = new JSONObject(sb.toString());
            System.out.println("jsonreq : 4 " + jsonRequest);
            String tableName = jsonRequest.getString("tableName"); // Extract table name
            JSONArray columnsArray = jsonRequest.getJSONArray("columns"); // Extract column list
            String currentDB = jsonRequest.getString("chosedDB"); // Extract current DB
            int currentTime = jsonRequest.getInt("chosedDBTime");   // Current database execution time
            int numRecords = jsonRequest.getInt("numRecords");     // Number of records to insert

            System.out.println("tablname :" + tableName + "\nColumnsArr : " + columnsArray);

            JSONObject jsonObject;
            Object timeString;

            if (!currentDB.equals("mysql")) {  
                jsonObject = GenerateMysqlData.generateData(tableName, columnsArray, numRecords);
                timeString = jsonObject.get("runtime");
                dbTimings.put("MySQL", convertToLong(timeString.toString()));
            }

            // **PostgreSQL Execution** (Skip if PostgreSQL is the current DB)
            if (!currentDB.equals("postgresql")) {  
                jsonObject = GeneratePsqlData.generateData(tableName, columnsArray, numRecords);
                timeString = jsonObject.get("runtime");
                dbTimings.put("PostgreSQL", convertToLong(timeString.toString()));
            }

            // **Cassandra Execution** (Skip if Cassandra is the current DB)
            if (!currentDB.equals("cassandra")) {  
                jsonObject = GenerateCassandraData.generateData("data", tableName, columnsArray, numRecords, "1", "SimpleStrategy");
                timeString = jsonObject.get("runtime");
                dbTimings.put("Cassandra", convertToLong(timeString.toString()));
            }

            // **MongoDB Execution** (Skip if MongoDB is the current DB)
           if (!currentDB.equals("mongodb")) {  
                jsonObject = GenerateMongoDBData.generateData(tableName, columnsArray, numRecords);
                timeString = jsonObject.get("runtime");
                dbTimings.put("MongoDB", convertToLong(timeString.toString()));
            }
            
            dbTimings.put(currentDB, (long) currentTime);

            // Convert timing results to JSON
            JSONArray dbResults = new JSONArray();
            for (Map.Entry<String, Long> entry : dbTimings.entrySet()) {
                JSONObject dbObject = new JSONObject();
                dbObject.put("dbName", entry.getKey());
                dbObject.put("timeTaken", entry.getValue());
                dbResults.put(dbObject);
            }

            jsonResponse.put("results", dbResults);  // Add results to JSON
            jsonResponse.put("message", "success");  // Success message

        } catch (Exception e) {
            jsonResponse.put("message", "failure");
            e.printStackTrace();
            jsonResponse.put("error", e.getMessage());
        }

        System.out.println("JSON Response: " + jsonResponse.toString());  // Log response

        // **Fix: Send response to the client**
        out.write(jsonResponse.toString());  
        out.flush();  // Ensure all data is written
        out.close();  // Close writer
    }

    // Utility method to safely convert string to long
    public long convertToLong(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            System.err.println("Invalid input: " + input);
            return -1;  // Return -1 in case of conversion error
        }
    }
}




