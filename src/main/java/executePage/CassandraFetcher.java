package executePage;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;

public class CassandraFetcher {

    // Fetch Data from Table (SELECT with LIMIT 10)
    public static JSONObject fetchData(String keyspace, String tableName) {
        JSONObject jsonResponse = new JSONObject();

        try (CqlSession session = CqlSession.builder().build()) {

            String query = "SELECT * FROM " + keyspace + "." + tableName + " LIMIT 5";
            PreparedStatement preparedStatement = session.prepare(query);
            BoundStatement boundStatement = preparedStatement.bind();

            long startTime = System.nanoTime();
            ResultSet resultSet = session.execute(boundStatement);
            long endTime = System.nanoTime();

            // Get column metadata
            Optional<List<ColumnMetadata>> columnMetadataOpt = session.getMetadata()
                    .getKeyspace(keyspace)
                    .flatMap(ks -> ks.getTable(tableName))
                    .map(tbl -> tbl.getColumns().values().stream().toList());

            JSONArray columnNames = new JSONArray();
            JSONArray data = new JSONArray();

            if (columnMetadataOpt.isPresent()) {
                List<ColumnMetadata> columns = columnMetadataOpt.get();
                for (ColumnMetadata col : columns) {
                    columnNames.put(col.getName().asCql(true));
                }

                for (Row row : resultSet) {
                    JSONArray rowData = new JSONArray();
                    for (ColumnMetadata col : columns) {
                        rowData.put(row.getObject(col.getName().asCql(true)));
                    }
                    data.put(rowData);
                }
            }

            double executionTimeMs = (endTime - startTime) / 1_000_000.0;
            jsonResponse.put("columns", columnNames);
            jsonResponse.put("data", data);
            jsonResponse.put("execution_time_ms", executionTimeMs);

        } catch (Exception e) {
            jsonResponse.put("error", e.getMessage());
        }

        return jsonResponse;
    }

    // Execute Query (INSERT, UPDATE, DELETE, or Custom Query)
    public static JSONObject executeQuery(String keyspace, String query) {
        JSONObject jsonResponse = new JSONObject();

        try (CqlSession session = CqlSession.builder().build()) {
        	 session.execute("USE "+keyspace);
            PreparedStatement preparedStatement = session.prepare(query);
            BoundStatement boundStatement = preparedStatement.bind();

            long startTime = System.nanoTime();
            session.execute(boundStatement);
            long endTime = System.nanoTime();

            double executionTimeMs = (endTime - startTime) / 1_000_000.0;
            jsonResponse.put("execution_time_ms", executionTimeMs);
            jsonResponse.put("message", "Query executed successfully");

        } catch (Exception e) {
            jsonResponse.put("error", e.getMessage());
        }

        return jsonResponse;
    }
}
