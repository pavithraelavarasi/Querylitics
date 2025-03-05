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

@WebServlet("/dbFetcher")
public class DbFetcherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject jsonResponse = new JSONObject();

        try {
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject jsonRequest = new JSONObject(sb.toString());

            // Extract required parameters using getString() to enforce presence
            String database = jsonRequest.getString("database");
            String table = jsonRequest.getString("table");

            // Fetch data from all databases
            jsonResponse.put("mysql", MysqlFetcher.fetchData("dummy", table));
            jsonResponse.put("postgresql", PostgreSQLFetcher.fetchData("dummy", table));
            jsonResponse.put("mongodb", MongoDBFetcher.fetchData("dummy", table));
            jsonResponse.put("cassandra", CassandraFetcher.fetchData("data", table));

        } catch (Exception e) {
            jsonResponse.put("error", e.getMessage());
        }

        // Send JSON response
        System.out.println(jsonResponse);
        PrintWriter out = response.getWriter();
        out.print(jsonResponse.toString(4)); // Pretty print JSON
        out.flush();
    }
}
