package executePage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONException;

@WebServlet("/executeQuery")
public class DbQueryExecutorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject jsonResponse = new JSONObject();

        try (BufferedReader reader = request.getReader()) {
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject jsonRequest = new JSONObject(sb.toString());

            // Extract required parameters using getString()
            String dbType = jsonRequest.getString("dbType").toLowerCase(); // Ensure lowercase comparison
            String databaseName = jsonRequest.getString("databaseName"); // Extract database name
            String query = jsonRequest.getString("query"); // Extract query

            // Execute query based on database type
            switch (dbType) {
                case "mysql":
                    jsonResponse = MysqlFetcher.executeQuery(databaseName, query);
                    break;
                case "postgresql":
                    jsonResponse = PostgreSQLFetcher.executeQuery(databaseName, query);
                    break;
//                case "mongodb":
//                    jsonResponse = MongoDBFetcher.executeQuery(databaseName, query);
//                    break;
                case "cassandra":
                    jsonResponse = CassandraFetcher.executeQuery(databaseName, query);
                    break;
//                default:
//                    jsonResponse.put("error", "Invalid dbType. Supported: mysql, postgresql, mongodb, cassandra.");
            	}

        } catch (JSONException e) {
            jsonResponse.put("error", "Missing required parameters: dbType, databaseName, or query.");
        } catch (Exception e) {
            jsonResponse.put("error", "Exception occurred: " + e.getMessage());
        }

        // Send JSON response
        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString(4)); // Pretty print JSON
            out.flush();
        }
    }
}
