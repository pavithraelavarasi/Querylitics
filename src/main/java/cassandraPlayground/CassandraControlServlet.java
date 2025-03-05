package cassandraPlayground;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.*;
import org.json.JSONObject;

@WebServlet("/cassandraproxy")
public class CassandraControlServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
	private String cassandraPath = "/home/tamiz-zstch1544/Downloads/apache-cassandra-4.1.7/bin/";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Read the request body to get the JSON input
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            requestBody.append(line);
        }
        
        // Parse the JSON request
        JSONObject jsonRequest = new JSONObject(requestBody.toString());
        
        String status;
        try {
            // Use getString() instead of optString()
            status = jsonRequest.getString("status");
        } catch (Exception e) {
            // Handle the case where "status" is missing or null
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("message", "Missing 'status' parameter").toString());
            return;
        }

        // Set the response content type
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        // Handle the 'status' parameter
        if ("on".equalsIgnoreCase(status)) {
            String startMessage = startCassandra();
            out.write(new JSONObject().put("status", "successfully on").put("message", startMessage).toString());
        } else if ("off".equalsIgnoreCase(status)) {
            String stopMessage = stopCassandra();
            out.write(new JSONObject().put("status", "successfully off").put("message", stopMessage).toString());
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new JSONObject().put("message", "Invalid status, must be 'on' or 'off'").toString());
        }
        
        out.flush();
    }

    private String startCassandra() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cassandraPath + "cassandra");
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
            return "Cassandra has been started";
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error starting Cassandra: " + e.getMessage();
        }
    }

    private String stopCassandra() {
        try {
            // Run the stop-server script with the '-l' option to stop the 'cassandra' process by name
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", cassandraPath + "stop-server -l");
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
            
            // Check if the process exited successfully
            if (process.exitValue() == 0) {
                return "Cassandra has been stopped successfully";
            } else {
                return "Error stopping Cassandra: non-zero exit code";
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error stopping Cassandra: " + e.getMessage();
        }
    }

}

