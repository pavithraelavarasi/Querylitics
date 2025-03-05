package home;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class GenerateMysqlData {

    private static final String MOCKAROO_API_KEY = "e01c55e0";
    private static final int BATCH_SIZE = 500;

    public static JSONObject generateData(String tableName, JSONArray columnsArray, int numRecords) {

//        System.out.println("inside generate data method.....");
        JSONObject jsonResponse = new JSONObject();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        Connection conn = null;
        Statement stmt = null;
        try {
        	Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "pavi", "Pavi@2001");
            stmt = conn.createStatement();

            // Create the database if it doesn't exist
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + "dummy");
            stmt.executeUpdate("USE " + "dummy");

            // conn.commit();

            // Create the table if it doesn't exist
            String createTableQuery = buildCreateTableQuery(tableName, columnsArray);
//            System.out.println("create table query : "+ createTableQuery);
            stmt.executeUpdate(createTableQuery);

            // Fetch mock data
            String mockData = fetchMockDataFromMockaroo(columnsArray, Math.min(numRecords, 1000));
            
            long startTime = System.currentTimeMillis();

            // Insert data into the table
            insertDataIntoTable(conn, tableName, columnsArray, mockData, numRecords);
            
            long endTime = System.currentTimeMillis();
            
            double size = getTableSizeMB(conn,"dummy",tableName);

            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // Success response
            // jsonResponse.put("dbname", dbName);
            jsonResponse.put("tablename", tableName);
            jsonResponse.put("runtime", (endTime - startTime));
            jsonResponse.put("numberofrecords", numRecords);
            jsonResponse.put("message", "success");
            jsonResponse.put("memoryUsed", size);

        } catch (Exception e) {
            jsonResponse.put("message", "failure");
            jsonResponse.put("error", e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                jsonResponse.put("error", "Error closing resources: " + e.getMessage());
            }
        }

        return jsonResponse;
    }

    private static String buildCreateTableQuery(String tableName, JSONArray columnsArray) {
//        System.out.println("inside build query method......");
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");
        for (int i = 0; i < columnsArray.length(); i++) {
            JSONObject column = columnsArray.getJSONObject(i);
            query.append(column.getString("name")).append(" ").append(mapSQLType(column.getString("type"))).append(", ");
//            System.out.println("qasdddddddddddddd====="+query);
        }
        query.setLength(query.length() - 2); // Remove last comma
        query.append(")");
//        System.out.println("Full Query-------------------"+query.toString());
        return query.toString();
    }

    private static String fetchMockDataFromMockaroo(JSONArray columnsArray, int numRecords) throws IOException {
        JSONArray fieldsArray = new JSONArray();
        for (int i = 0; i < columnsArray.length(); i++) {
            JSONObject column = columnsArray.getJSONObject(i);
            JSONObject field = new JSONObject();
            field.put("name", column.getString("name"));
//            System.out.println(column.getString("type"));
//            System.out.print(mapMockarooType(column.getString("type")));
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
        return response.toString();
    }

    private static void insertDataIntoTable(Connection conn, String tableName, JSONArray columnsArray, String mockData, int numRecords) throws SQLException {
        JSONArray jsonArray = new JSONArray(mockData);
        int originalDataCount = jsonArray.length();

        String[] columnDefs = new String[columnsArray.length()];
        for (int i = 0; i < columnsArray.length(); i++) {
            columnDefs[i] = columnsArray.getJSONObject(i).getString("name");
        }

        // Building multi-row insert query
        StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tableName + " (");
        for (String col : columnDefs) {
            insertQuery.append(col).append(", ");
        }
        insertQuery.setLength(insertQuery.length() - 2); // Remove last comma
        insertQuery.append(") VALUES ");

        // Disable auto-commit for better performance
        conn.setAutoCommit(false);

        try (Statement stmt = conn.createStatement()) {
            int count = 0;
            StringBuilder valuesPart = new StringBuilder();

            for (int i = 0; i < numRecords; i++) {
                JSONObject record = jsonArray.getJSONObject(i % originalDataCount);
                valuesPart.append("(");
                for (int j = 0; j < columnsArray.length(); j++) {
                    String columnName = columnsArray.getJSONObject(j).getString("name");
                    Object value = record.get(columnName);
					if (value instanceof Boolean) {
					    valuesPart.append(((Boolean) value) ? "1" : "0").append(", ");
					} else {
					    valuesPart.append("'").append(value.toString().replace("'", "''")).append("', ");
					}
                }
                valuesPart.setLength(valuesPart.length() - 2); // Remove last comma
                valuesPart.append("), ");

                count++;

                if (count % BATCH_SIZE == 0 || i == numRecords - 1) {
                    valuesPart.setLength(valuesPart.length() - 2); // Remove last comma
                    stmt.executeUpdate(insertQuery.toString() + valuesPart.toString());
                    valuesPart.setLength(0); // Reset valuesPart
                }
            }

            // Commit the transaction
            conn.commit();
        } catch (SQLException e) {
            conn.rollback(); // Rollback in case of failure
            throw e;
        } finally {
            conn.setAutoCommit(true); // Re-enable auto-commit
        }
    }
    
    private static double getTableSizeMB(Connection conn, String databaseName, String tableName) throws SQLException {
        String query = "SELECT ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb " +
                       "FROM information_schema.tables " +
                       "WHERE table_schema = ? AND table_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, databaseName);
            pstmt.setString(2, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("size_mb");
                }
            }
        }
        return 0.0;
    }

    private static String mapSQLType(String type) {
        switch (type.toLowerCase()) {
            case "int":
                return "INT";
            case "string":
                return "VARCHAR(255)";
            case "boolean":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "float":
                return "DOUBLE";
            default:
                return "VARCHAR(255)";
        }
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
