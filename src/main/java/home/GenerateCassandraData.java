package home;
import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.management.ManagementFactory;

public class GenerateCassandraData {
    private static final String MOCKAROO_API_KEY = "e01c55e0";
    private static final int BATCH_SIZE = 500;

    public static JSONObject generateData(String dbName, String tableName, JSONArray columnsArray, int numRecords, String replicationFactor, String classType) {
        JSONObject jsonResponse = new JSONObject();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); // Memory before
        
        List<InetSocketAddress> ipAddresses = new ArrayList<>();
        ipAddresses.add(new InetSocketAddress("10.51.25.163", 9042));

        try (CqlSession session = CqlSession.builder().build()) {
        
            createKeyspace(session, dbName, replicationFactor, classType);
            createTable(session, dbName, tableName, columnsArray);

            // Fetch mock data
            String mockData = fetchMockDataFromMockaroo(columnsArray, Math.min(numRecords, 1000));
            
            long startTime = System.currentTimeMillis();

            // Batch insert data
            batchInsertData(session, dbName, tableName, columnsArray, mockData, numRecords);

            long endTime = System.currentTimeMillis();
            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); // Memory after

            // Get system details
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String osArch = System.getProperty("os.arch");

            // Add to response
            jsonResponse.put("dbname", dbName);
            jsonResponse.put("tablename", tableName);
            jsonResponse.put("runtime", (endTime - startTime));
            jsonResponse.put("numberofrecords", numRecords);
            jsonResponse.put("message", "success");
            jsonResponse.put("memoryUsed", (memoryAfter - memoryBefore)); // Memory used
            jsonResponse.put("osName", osName); // OS name
            jsonResponse.put("osVersion", osVersion); // OS version
            jsonResponse.put("osArch", osArch); // OS architecture

        } catch (Exception e) {
            jsonResponse.put("message", "fail");
            jsonResponse.put("error", e.getMessage());
            e.printStackTrace();
            
        }
        return jsonResponse;
    }
    
//    public static JSONObject generateData(String tableName, JSONArray columnsArray, int numRecords, String replicationFactor, String classType) {
////    	String dbName = "dummy";
//        JSONObject jsonResponse = new JSONObject();
//        long startTime = System.currentTimeMillis();
//        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); // Memory before
//
//        try (CqlSession session = CqlSession.builder().build()) {
//            createKeyspace(session, dbName, replicationFactor, classType);
//            createTable(session, dbName, tableName, columnsArray);
//
//            // Fetch mock data
//            String mockData = fetchMockDataFromMockaroo(columnsArray, Math.min(numRecords, 1000));
//
//            // Batch insert data
//            batchInsertData(session, dbName, tableName, columnsArray, mockData, numRecords);
//
//            long endTime = System.currentTimeMillis();
//            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); // Memory after
//
//            // Get system details
//            String osName = System.getProperty("os.name");
//            String osVersion = System.getProperty("os.version");
//            String osArch = System.getProperty("os.arch");
//
//            // Add to response
//            jsonResponse.put("dbname", dbName);
//            jsonResponse.put("tablename", tableName);
//            jsonResponse.put("runtime", (endTime - startTime) + " ms");
//            jsonResponse.put("numberofrecords", numRecords);
//            jsonResponse.put("message", "success");
//            jsonResponse.put("memoryUsed", (memoryAfter - memoryBefore) + " bytes"); // Memory used
//            jsonResponse.put("osName", osName); // OS name
//            jsonResponse.put("osVersion", osVersion); // OS version
//            jsonResponse.put("osArch", osArch); // OS architecture
//
//        } catch (Exception e) {
//            jsonResponse.put("message", "fail");
//            jsonResponse.put("error", e.getMessage());
//            e.printStackTrace();
//        }
//        return jsonResponse;
//    }

    private static void createKeyspace(CqlSession session, String dbName, String replicationFactor, String classType) {
        String query = "CREATE KEYSPACE IF NOT EXISTS " + dbName +
                " WITH replication = {'class':'" + classType + "', 'replication_factor':" + replicationFactor + "}";
        System.out.println(query);
        session.execute(query);
    }

    private static void createTable(CqlSession session, String dbName, String tableName, JSONArray columnsArray) {
    	System.out.println(tableName);
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + dbName + "." + tableName + " (uuid_col UUID PRIMARY KEY, ");
        for (int i = 0; i < columnsArray.length(); i++) {
            JSONObject column = columnsArray.getJSONObject(i);
            query.append(column.getString("name")).append(" ").append(mapCassandraType(column.getString("type"))).append(", ");
        }
        query.setLength(query.length() - 2);
        query.append(");");
        System.out.println(query.toString());
        session.execute(query.toString());
    }

    private static String fetchMockDataFromMockaroo(JSONArray columnsArray, int numRecords) throws IOException {
        JSONArray fieldsArray = new JSONArray();
        for (int i = 0; i < columnsArray.length(); i++) {
            JSONObject column = columnsArray.getJSONObject(i);
//            System.out.println(column.toString());
            JSONObject field = new JSONObject();
//            System.out.println(column.getString("type"));
            field.put("name", column.getString("name"));
//            System.out.println(mapMockarooType(column.getString("type")));
            field.put("type", mapMockarooType(column.getString("type")));
            fieldsArray.put(field);
        }

        String apiUrl = "https://api.mockaroo.com/api/generate.json?key=" + MOCKAROO_API_KEY + "&count=" + numRecords;
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
        	System.out.println(fieldsArray.toString());
            os.write(fieldsArray.toString().getBytes("UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
//        System.out.println(response.toString());
        return response.toString();
    }

    private static void batchInsertData(CqlSession session, String dbName, String tableName, JSONArray columnsArray, String mockData, int numRecords) {
        JSONArray jsonArray = new JSONArray(mockData);
        int originalDataCount = jsonArray.length();

        for (int i = 0; i < numRecords; i += BATCH_SIZE) {
            StringBuilder batchQuery = new StringBuilder("BEGIN BATCH ");

            for (int j = i; j < Math.min(i + BATCH_SIZE, numRecords); j++) {
                JSONObject record = jsonArray.getJSONObject(j % originalDataCount);
                StringBuilder insertQuery = new StringBuilder("INSERT INTO " + dbName + "." + tableName + " (uuid_col, ");
                UUID newUuid = UUID.randomUUID();
                StringBuilder valuesPart = new StringBuilder("VALUES (" + newUuid + ", ");

                for (int k = 0; k < columnsArray.length(); k++) {
                    JSONObject column = columnsArray.getJSONObject(k);
//                    System.out.println(column);
                    String columnName = column.getString("name");
//                    System.out.println(columnName);
                    String columnType = column.getString("type");
//                    System.out.println(columnType);
                    insertQuery.append(columnName).append(", ");
                    valuesPart.append(formatValue(record, columnName, columnType)).append(", ");
                }

                insertQuery.setLength(insertQuery.length() - 2);
                valuesPart.setLength(valuesPart.length() - 2);
                insertQuery.append(") ").append(valuesPart).append("); ");

                batchQuery.append(insertQuery);
            }

            batchQuery.append(" APPLY BATCH;");
            session.executeAsync(batchQuery.toString());
        }
    }

    private static String formatValue(JSONObject record, String columnName, String columnType) {
        switch (columnType.toLowerCase()) {
            case "int":
                return String.valueOf(record.getInt(columnName));
            case "varchar":
                return "'" + record.getString(columnName).replace("'", "''") + "'";
            case "boolean":
                return String.valueOf(record.getBoolean(columnName));
            case "date":
                return "'" + record.getString(columnName) + "'";
            case "double":
                return String.valueOf(record.getDouble(columnName));
            case "float":
                return String.valueOf(record.getDouble(columnName));
            default:
                return "'" + record.getString(columnName).replace("'", "''") + "'";
        }
    }

    private static String mapCassandraType(String type) {
        switch (type.toLowerCase()) {
            case "int":
                return "int";
            case "varchar":
                return "text";
            case "float":
                return "double";  // Cassandra does not support float, so use double
            case "date":
                return "timestamp";
            case "double":
                return "double";
            case "boolean":
                return "boolean";
            default:
                return "text";
        }
    }

    private static String mapMockarooType(String type) {
        switch (type.toLowerCase()) {
            case "int":
                return "Number";
            case "varchar":
                return "Full Name";
            case "float":
                return "Number";  // Mockaroo supports "Decimal" for float-like values
            case "date":
                return "Datetime";
            case "double":
                return "Decimal";
            case "boolean":
                return "Boolean";
            default:
                return "String";
        }
    }
}