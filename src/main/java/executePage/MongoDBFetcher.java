package executePage;
import com.mongodb.client.*;
import com.mongodb.client.model.Updates;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;

public class MongoDBFetcher {
  private static final String DB_HOST = "localhost";

  private static final int DB_PORT = 27017;

  public static JSONObject fetchData(
      String databaseName, String collectionName) {
    JSONObject jsonResponse = new JSONObject();

    if (databaseName == null || databaseName.trim().isEmpty()) {
      databaseName = "dummy";
    }

    if (collectionName == null || collectionName.trim().isEmpty()) {
      jsonResponse.put("error", "Collection name must be provided.");
      return jsonResponse;
    }

    String connectionString = "mongodb://" + DB_HOST + ":" + DB_PORT;

    try (MongoClient mongoClient = MongoClients.create(connectionString)) {
      MongoDatabase database = mongoClient.getDatabase(databaseName);
      MongoCollection<Document> collection =
          database.getCollection(collectionName);

      long startTime = System.nanoTime();
      List<Document> documents =
          collection.find().limit(5).into(new ArrayList<>());
      long endTime = System.nanoTime();

      JSONArray data = new JSONArray();
      JSONArray columnNames = new JSONArray();

      // Fetch column names from the first document (if available)
      Document firstDoc = collection.find().first();
      if (firstDoc != null) {
        Set<String> keys = firstDoc.keySet(); // Get the keys directly as a Set
        for (String key : keys) {
          columnNames.put(key);
        }
      }

      // Extract only values from each document
      for (Document doc : documents) {
        JSONArray rowValues = new JSONArray();
        for (int i = 0; i < columnNames.length(); i++) {
          String key = columnNames.getString(i);
          rowValues.put(doc.get(key)); // Add only values
        }
        data.put(rowValues);
      }

      double executionTimeMs = (endTime - startTime) / 1_000_000.0;
      jsonResponse.put("columns", columnNames);
      jsonResponse.put("data", data);
      jsonResponse.put("execution_time_ms", executionTimeMs);

    } catch (Exception e) {
      jsonResponse.put("error", "MongoDB fetchData failed: " + e.getMessage());
    }

    return jsonResponse;
  }

  public static JSONObject executeMongoShell(
      String databaseName, String query) {
    JSONObject response = new JSONObject();

    long startTime = System.nanoTime();

    try {
      // Use single quotes to properly wrap JSON in echo

      String fullCommand = "echo '" + query + "' | mongosh " + databaseName;

      ProcessBuilder processBuilder =
          new ProcessBuilder("/bin/sh", "-c", fullCommand);

      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      // Capture output from mongosh

      BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));

      StringBuilder output = new StringBuilder();

      String line;

      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }

      int exitCode = process.waitFor();

      long endTime = System.nanoTime();

      double executionTime =
          (endTime - startTime) / 1e6; // Convert to milliseconds

      response.put("status", exitCode == 0 ? "success" : "error");

      response.put("output", output.toString().trim());

      response.put("execution_time_ms", executionTime);

    } catch (Exception e) {
      response.put("status", "error");

      response.put("message", "Execution failed: " + e.getMessage());
    }

    return response;
  }

  public static JSONObject executeQuery(String databaseName, String query) {
    return executeMongoShell(databaseName, query);
  }
}
