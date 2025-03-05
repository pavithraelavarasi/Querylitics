package home;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GenerateMongoDBData {

    private static final String MOCKAROO_API_KEY = "e01c55e0";

    public static JSONObject generateData(String tableName, JSONArray columnsArray, int numRecords) {
        JSONObject jsonResponse = new JSONObject();
      

        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("dummy");
            MongoCollection<Document> collection = database.getCollection(tableName);

            // Clear existing data in MongoDB collection before inserting new data
            collection.deleteMany(new Document());

            // Fetch mock data
            String mockData = fetchMockDataFromMockaroo(columnsArray, Math.min(numRecords, 1000));
            JSONArray jsonArray = new JSONArray(mockData);

            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < numRecords; i++) {
                // Ensure we don't go out of bounds if numRecords exceeds available mock data
                JSONObject record = jsonArray.getJSONObject(i % jsonArray.length());
                Document doc = new Document();
                for (int j = 0; j < columnsArray.length(); j++) {
                    String columnName = columnsArray.getJSONObject(j).getString("name");
                    // Ensure the mock data contains the column name
                    if (record.has(columnName)) {
                        doc.append(columnName, record.get(columnName));
                    }
                }
                documents.add(doc);
            }

            long startTime = System.currentTimeMillis();
            collection.insertMany(documents);

            long endTime = System.currentTimeMillis();
            jsonResponse.put("tablename", tableName);
            jsonResponse.put("runtime", (endTime - startTime));
            jsonResponse.put("numberofrecords", numRecords);
            jsonResponse.put("message", "success");

        } catch (Exception e) {
            jsonResponse.put("message", "failure");
            jsonResponse.put("error", e.getMessage());
            e.printStackTrace(); // Log full stack trace for debugging
        }

        return jsonResponse;
    }

    private static String fetchMockDataFromMockaroo(JSONArray columnsArray, int numRecords) throws Exception {
        JSONArray fieldsArray = new JSONArray();
        for (int i = 0; i < columnsArray.length(); i++) {
            JSONObject column = columnsArray.getJSONObject(i);
            JSONObject field = new JSONObject();
            field.put("name", column.getString("name"));
            field.put("type", mapMockarooType(column.getString("type")));
            fieldsArray.put(field);
        }

        String apiUrl = "https://api.mockaroo.com/api/generate.json?key=" + MOCKAROO_API_KEY + "&count=" + numRecords;
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(fieldsArray.toString().getBytes("UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // Log the Mockaroo response for debugging
        System.out.println("Mockaroo Response: " + response.toString());

        return response.toString();
    }

    private static String mapMockarooType(String type) {
        switch (type.toLowerCase()) {
            case "int":
                return "Number";
            case "varchar":
                return "Full Name";
            case "boolean":
                return "Boolean";
            case "date":
                return "Datetime";
            case "float":
                return "Number";
            default:
                return "String";
        }
    }
}


