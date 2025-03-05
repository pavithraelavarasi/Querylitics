package home;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/GenerateData")
public class RequestProcessor extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // Parse JSON request
            JSONObject jsonRequest = new JSONObject(sb.toString());
            String dbType = jsonRequest.getString("dbType");
            String tableName = jsonRequest.getString("tableName");
            String dbName = jsonRequest.has("dbName") ? jsonRequest.getString("dbName") : null;
            int numRecords = jsonRequest.getInt("numRecords"); // Get number of records
            String replicationFactor = jsonRequest.optString("replicationFactor", "1"); // Default: 1
            String classType = jsonRequest.optString("classType", "SimpleStrategy"); // Default: SimpleStrategy
            JSONArray columnsArray = jsonRequest.getJSONArray("columns");
            System.out.println("columns array ins req :" + columnsArray);
            // System.out.println(jsonRequest.toString());

            // Select appropriate database handler
            switch (dbType.toLowerCase()) {
                case "cassandra":
                	System.out.println("inside the cassandra processor ...");
                    jsonResponse = GenerateCassandraData.generateData(dbName, tableName, columnsArray, numRecords, replicationFactor, classType);
                    break;
                case "mysql":
                    System.out.println("Mysql switch");
                    System.out.println(columnsArray.toString()+" Column Array ");
                    jsonResponse = GenerateMysqlData.generateData(tableName, columnsArray, numRecords);
                    break;
                case "postgresql":
                    jsonResponse = GeneratePsqlData.generateData(tableName, columnsArray, numRecords);
                    break;
                case "mongodb":
                    	jsonResponse = GenerateMongoDBData.generateData(tableName, columnsArray, numRecords);
                    break;
                default:
                    jsonResponse.put("message", "failure");
                    jsonResponse.put("error", "Invalid database type");
            }

            out.print(jsonResponse.toString());
            // System.out.println(jsonRequest.toString()); // Debugging log
        } catch (Exception e) {
            jsonResponse.put("message", "failure");
            jsonResponse.put("error", e.getMessage());
            out.print(jsonResponse.toString());
        }
    }
}
