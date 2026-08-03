package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PostgreSQL MCP JSON-RPC Client
 * Communicates with pg-mcp-server via JSON-RPC 2.0 protocol over stdio
 */
@Component
public class DBHubMCPClient {

    private static final Logger logger = LoggerFactory.getLogger(DBHubMCPClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicInteger requestIdCounter = new AtomicInteger(1);

    private Process mcpProcess;
    private BufferedWriter writer;
    private BufferedReader reader;
    private String databaseUrl;

    public DBHubMCPClient() {
        // Read DATABASE_URL from environment
        this.databaseUrl = System.getenv("DATABASE_URL") != null
            ? System.getenv("DATABASE_URL")
            : "postgresql://arpit:1234@localhost:5432/hello_world_db";

        logger.info("DBHubMCPClient initialized - Database: {}", databaseUrl);
        initializeMCPServer();
    }

    /**
     * Initialize MCP server connection
     */
    private void initializeMCPServer() {
        try {
            logger.info("DBHubMCPClient: Starting pg-mcp-server process...");

            // Start pg-mcp-server with stdio transport (requires pg-mcp-server to be installed globally)
            // Install with: npm install -g pg-mcp-server
            ProcessBuilder pb = new ProcessBuilder(
                "pg-mcp-server",
                "--transport", "stdio"
            );

            // Inherit parent environment and add our settings
            Map<String, String> env = pb.environment();

            // HARDCODED FOR TESTING: Enable write operations
            env.clear();  // Clear all inherited env to start fresh
            env.put("DATABASE_URL", databaseUrl);
            env.put("DANGEROUSLY_ALLOW_WRITE_OPS", "true");
            env.put("PATH", System.getenv("PATH"));  // Keep PATH for command resolution
            env.put("HOME", System.getenv("HOME"));  // Keep HOME for node

            logger.info("DBHubMCPClient: HARDCODED WRITE OPS - Setting DANGEROUSLY_ALLOW_WRITE_OPS=true");
            logger.info("DBHubMCPClient: Environment - DATABASE_URL={}, DANGEROUSLY_ALLOW_WRITE_OPS={}",
                databaseUrl, env.get("DANGEROUSLY_ALLOW_WRITE_OPS"));

            // Redirect stderr to see errors from MCP server
            pb.redirectErrorStream(false);

            mcpProcess = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(mcpProcess.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(mcpProcess.getInputStream()));

            // Start thread to capture stderr from MCP server
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(mcpProcess.getErrorStream()));
            Thread stderrThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        logger.warn("PG-MCP SERVER STDERR: {}", line);
                    }
                } catch (IOException e) {
                    logger.debug("Error reading MCP server stderr: {}", e.getMessage());
                }
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            // Wait a bit for server to initialize
            Thread.sleep(500);

            // Check if process is still alive
            if (!mcpProcess.isAlive()) {
                throw new RuntimeException("pg-mcp-server process exited immediately. " +
                    "Make sure pg-mcp-server is installed: npm install -g pg-mcp-server");
            }

            logger.info("DBHubMCPClient: pg-mcp-server started successfully");

        } catch (IOException e) {
            logger.error("DBHubMCPClient: Failed to start MCP server: {}", e.getMessage());
            if (e.getMessage().contains("No such file")) {
                throw new RuntimeException("pg-mcp-server not found. Install with: npm install -g pg-mcp-server");
            }
            throw new RuntimeException("Failed to start pg-mcp-server: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.error("DBHubMCPClient: Interrupted while starting MCP server: {}", e.getMessage());
            throw new RuntimeException("Interrupted while starting MCP server: " + e.getMessage());
        }
    }

    /**
     * Execute INSERT/UPDATE/DELETE via MCP
     */
    public int execute(String sql, Object... params) {
        logger.info("DBHubMCPClient: Executing SQL via MCP: {}", sql);

        try {
            Map<String, Object> result = callMCPQuery(sql, params);
            // pg-mcp-server returns rowsAffected for INSERT/UPDATE/DELETE
            Object rowsAffectedObj = result.get("rowsAffected");
            int rowsAffected = rowsAffectedObj != null ? ((Number) rowsAffectedObj).intValue() : 0;
            logger.info("DBHubMCPClient: Execute completed - {} rows affected", rowsAffected);
            return rowsAffected;

        } catch (Exception e) {
            logger.error("DBHubMCPClient: Execute failed - {}", e.getMessage());
            throw new RuntimeException("MCP execute failed: " + e.getMessage());
        }
    }

    /**
     * Execute INSERT with RETURNING and get the ID
     */
    public Long executeAndGetId(String sql, Object... params) {
        logger.info("DBHubMCPClient: Executing SQL with RETURNING via MCP: {}", sql);

        try {
            Map<String, Object> result = callMCPQuery(sql, params);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");

            if (rows != null && !rows.isEmpty()) {
                Map<String, Object> firstRow = rows.get(0);
                Object idObj = firstRow.get("id");
                if (idObj != null) {
                    Long id = ((Number) idObj).longValue();
                    logger.info("DBHubMCPClient: INSERT RETURNING completed - ID: {}", id);
                    return id;
                }
            }
            logger.warn("DBHubMCPClient: No ID returned from RETURNING clause");
            return null;

        } catch (Exception e) {
            logger.error("DBHubMCPClient: Execute with RETURNING failed - {}", e.getMessage());
            throw new RuntimeException("MCP execute with RETURNING failed: " + e.getMessage());
        }
    }

    /**
     * Query single row via MCP
     */
    public Map<String, Object> queryOne(String sql, Object... params) {
        logger.info("DBHubMCPClient: Querying (single) via MCP: {}", sql);

        try {
            Map<String, Object> result = callMCPQuery(sql, params);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");

            if (rows != null && !rows.isEmpty()) {
                logger.info("DBHubMCPClient: Query returned 1 row");
                return rows.get(0);
            }
            logger.info("DBHubMCPClient: Query returned no rows");
            return null;

        } catch (Exception e) {
            logger.error("DBHubMCPClient: Query failed - {}", e.getMessage());
            throw new RuntimeException("MCP query failed: " + e.getMessage());
        }
    }

    /**
     * Query multiple rows via MCP
     */
    public List<Map<String, Object>> queryAll(String sql, Object... params) {
        logger.info("DBHubMCPClient: Querying (all) via MCP: {}", sql);

        try {
            Map<String, Object> result = callMCPQuery(sql, params);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");

            if (rows == null) {
                rows = new ArrayList<>();
            }
            logger.info("DBHubMCPClient: Query returned {} rows", rows.size());
            return rows;

        } catch (Exception e) {
            logger.error("DBHubMCPClient: Query failed - {}", e.getMessage());
            throw new RuntimeException("MCP query failed: " + e.getMessage());
        }
    }

    /**
     * Check if row exists via MCP
     */
    public boolean exists(String sql, Object... params) {
        Map<String, Object> result = queryOne(sql, params);
        return result != null;
    }

    /**
     * Build SQL with values (replace ? placeholders with actual values)
     * pg-mcp-server doesn't handle parameterized queries, so we build the full SQL
     */
    private String buildSqlWithValues(String sql, Object... params) {
        if (params == null || params.length == 0) {
            return sql;
        }

        String result = sql;
        for (Object param : params) {
            String value;
            if (param == null) {
                value = "NULL";
            } else if (param instanceof String) {
                // Escape single quotes in strings
                value = "'" + param.toString().replace("'", "''") + "'";
            } else if (param instanceof Number) {
                value = param.toString();
            } else if (param instanceof Boolean) {
                value = ((Boolean) param) ? "true" : "false";
            } else {
                value = "'" + param.toString().replace("'", "''") + "'";
            }

            // Replace first occurrence of ?
            result = result.replaceFirst("\\?", value);
        }

        logger.debug("DBHubMCPClient: Built SQL with values: {}", result);
        return result;
    }

    /**
     * Call pg-mcp-server "query" tool via JSON-RPC 2.0 protocol
     */
    private Map<String, Object> callMCPQuery(String sql, Object... params) throws Exception {
        int requestId = requestIdCounter.getAndIncrement();

        // Check if process is still alive
        if (!mcpProcess.isAlive()) {
            logger.error("DBHubMCPClient: MCP server process is dead");
            throw new RuntimeException("MCP server process is not alive");
        }

        // Build full SQL with values (pg-mcp-server doesn't handle parameterized queries with ?)
        String finalSql = buildSqlWithValues(sql, params);

        // Build JSON-RPC request for pg-mcp-server's "query" tool
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", "tools/call");

        Map<String, Object> toolParams = new HashMap<>();
        toolParams.put("name", "query");  // pg-mcp-server exposes "query" tool
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("sql", finalSql);
        toolParams.put("arguments", arguments);
        request.put("params", toolParams);

        logger.info("DBHubMCPClient: Sending JSON-RPC request ID {} - Tool: query", requestId);
        logger.info("DBHubMCPClient: Original SQL: {}", sql);
        logger.info("DBHubMCPClient: Final SQL with values: {}", finalSql);

        // Send request to MCP server
        String requestJson = objectMapper.writeValueAsString(request);
        logger.debug("DBHubMCPClient: Request JSON: {}", requestJson);

        try {
            writer.write(requestJson);
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            logger.error("DBHubMCPClient: Failed to send request to MCP server: {}", e.getMessage());
            throw new RuntimeException("Failed to send request to MCP server: " + e.getMessage());
        }

        // Read response from MCP server
        String responseLine;
        try {
            responseLine = reader.readLine();
        } catch (IOException e) {
            logger.error("DBHubMCPClient: Failed to read response from MCP server: {}", e.getMessage());
            throw new RuntimeException("Failed to read response from MCP server: " + e.getMessage());
        }

        if (responseLine == null) {
            logger.error("DBHubMCPClient: MCP server closed connection (null response)");
            throw new RuntimeException("MCP server closed connection");
        }

        logger.debug("DBHubMCPClient: Response JSON: {}", responseLine);

        JsonNode responseNode = objectMapper.readTree(responseLine);

        // Check for JSON-RPC errors
        if (responseNode.has("error")) {
            String errorMsg = responseNode.get("error").toString();
            logger.error("DBHubMCPClient: JSON-RPC error - {}", errorMsg);
            throw new RuntimeException("MCP error: " + errorMsg);
        }

        // Extract result from pg-mcp-server response
        JsonNode resultNode = responseNode.get("result");
        logger.info("DBHubMCPClient: Received result from MCP server");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.convertValue(resultNode, Map.class);
        return result;
    }

    /**
     * Close MCP connection
     */
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (mcpProcess != null) {
                mcpProcess.destroy();
                logger.info("DBHubMCPClient: MCP server stopped");
            }
        } catch (IOException e) {
            logger.error("DBHubMCPClient: Error closing connection: {}", e.getMessage());
        }
    }
}
