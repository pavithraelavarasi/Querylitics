package cassandraPlayground;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.json.JSONObject;

@WebServlet("/executeCassQuery")
public class CassandraQueryExecutor extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private CqlSession session;

    @Override
    public void init() throws ServletException {
    	String keyspace = "queryexecutor"; // Replace with your desired keyspace name
        session = CqlSession.builder()
                            .withKeyspace(keyspace) // Specify the keyspace to use
                            .build(); // 
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Read the JSON body from the request
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        // Parse the JSON request body
        String requestBody = stringBuilder.toString();
        JSONObject jsonRequest = new JSONObject(requestBody);
        String userQuery = jsonRequest.optString("query", "").trim(); // Use optString to avoid exceptions if key is missing

        JSONObject jsonResponse = new JSONObject();

        if (userQuery.isEmpty()) {
            jsonResponse.put("error", "Query cannot be empty");
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        long startTime = System.nanoTime();
        try {
            SimpleStatement statement = SimpleStatement.builder(userQuery).build();
            ResultSet resultSet = session.execute(statement);
            long endTime = System.nanoTime();

            long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            jsonResponse.put("query", userQuery);
            jsonResponse.put("executionTimeMs", executionTimeMs);
            jsonResponse.put("status", "Success");
            System.out.println("JSON : "+jsonResponse);
        } catch (Exception e) {
            jsonResponse.put("error", e.getMessage());
        }

        PrintWriter out = response.getWriter();
        out.write(jsonResponse.toString());
    }
    @Override
    public void destroy() {
        if (session != null) {
            session.close();
        }
    }
}
