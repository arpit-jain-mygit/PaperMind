package com.example.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBHub MCP Integration - Handles all database operations
 * Uses PostgreSQL connection pool to query/insert data
 */
@Component
public class DBHubMCP {

    private static final Logger logger = LoggerFactory.getLogger(DBHubMCP.class);

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DBHubMCP() {
        // Read from environment variables (same as Spring datasource)
        this.dbUrl = System.getenv("SPRING_DATASOURCE_URL") != null
            ? System.getenv("SPRING_DATASOURCE_URL")
            : "jdbc:postgresql://localhost:5432/hello_world_db";

        this.dbUser = System.getenv("SPRING_DATASOURCE_USERNAME") != null
            ? System.getenv("SPRING_DATASOURCE_USERNAME")
            : "arpit";

        this.dbPassword = System.getenv("SPRING_DATASOURCE_PASSWORD") != null
            ? System.getenv("SPRING_DATASOURCE_PASSWORD")
            : "1234";

        logger.info("DBHubMCP initialized - URL: {}, User: {}", dbUrl, dbUser);
    }

    /**
     * Execute INSERT/UPDATE/DELETE query
     */
    public int execute(String sql, Object... params) {
        logger.info("DBHubMCP: Executing SQL: {}", sql);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set parameters
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            int result = stmt.executeUpdate();
            logger.info("DBHubMCP: Execute completed - {} rows affected", result);
            return result;

        } catch (Exception e) {
            logger.error("DBHubMCP: Execute failed - {}", e.getMessage());
            throw new RuntimeException("Database execute failed: " + e.getMessage());
        }
    }

    /**
     * Query single row and return as Map
     */
    public Map<String, Object> queryOne(String sql, Object... params) {
        logger.info("DBHubMCP: Querying (single): {}", sql);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                }
                logger.info("DBHubMCP: Query returned 1 row");
                return row;
            }

            logger.info("DBHubMCP: Query returned no rows");
            return null;

        } catch (Exception e) {
            logger.error("DBHubMCP: Query failed - {}", e.getMessage());
            throw new RuntimeException("Database query failed: " + e.getMessage());
        }
    }

    /**
     * Query multiple rows and return as List of Maps
     */
    public List<Map<String, Object>> queryAll(String sql, Object... params) {
        logger.info("DBHubMCP: Querying (all): {}", sql);

        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }

            logger.info("DBHubMCP: Query returned {} rows", results.size());
            return results;

        } catch (Exception e) {
            logger.error("DBHubMCP: Query failed - {}", e.getMessage());
            throw new RuntimeException("Database query failed: " + e.getMessage());
        }
    }

    /**
     * Check if row exists
     */
    public boolean exists(String sql, Object... params) {
        Map<String, Object> result = queryOne(sql, params);
        return result != null;
    }

    /**
     * Get database connection
     */
    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
