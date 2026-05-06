package com.lockerkorea.ui.utils;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Database connector for verification and rollback operations
 */
public class DBConnector {
    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    private String url;
    private String username;
    private String password;

    public DBConnector() {
        this.url = ConfigReader.getString("db.url");
        this.username = ConfigReader.getString("db.username");
        this.password = ConfigReader.getString("db.password");
    }

    /**
     * Establish database connection
     */
    public void connect() throws SQLException {
        if (connectionHolder.get() == null || connectionHolder.get().isClosed()) {
            try {
                Class.forName(ConfigReader.getString("db.driver"));
            } catch (ClassNotFoundException e) {
                throw new SQLException("Database driver not found: " + ConfigReader.getString("db.driver"), e);
            }
            Connection conn = DriverManager.getConnection(url, username, password);
            connectionHolder.set(conn);
        }
    }

    /**
     * Close database connection
     */
    public void disconnect() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            } finally {
                connectionHolder.remove();
            }
        }
    }

    /**
     * Execute SQL query (for DML/DDL statements)
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    /**
     * Execute SQL query and return single result
     */
    public Object executeQuerySingle(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
                return null;
            }
        }
    }

    /**
     * Execute SQL query and return result set as list of maps
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    /**
     * Execute SQL script from file (multiple statements)
     */
    public void executeScript(String scriptPath) throws IOException, SQLException {
        String fullPath = ConfigReader.getString("sql." + scriptPath.replace(".sql", ""));
        if (fullPath == null) {
            fullPath = scriptPath;
        }

        InputStream input = getClass().getClassLoader().getResourceAsStream(fullPath);
        if (input == null) {
            throw new FileNotFoundException("SQL script not found: " + fullPath);
        }

        String script = new String(input.readAllBytes());
        // Split by semicolon and execute each statement
        String[] statements = script.split(";");
        for (String stmt : statements) {
            stmt = stmt.trim();
            if (!stmt.isEmpty()) {
                executeUpdate(stmt);
            }
        }
    }

    /**
     * Run cleanup script to reset database state
     */
    public void cleanupDatabase() throws IOException, SQLException {
        System.out.println("Running database cleanup...");
        executeScript("cleanup.sql");
        System.out.println("Cleanup completed");
    }

    /**
     * Verify record exists in database
     */
    public boolean recordExists(String table, String whereClause, Object... params) throws SQLException {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s", table, whereClause);
        Long count = (Long) executeQuerySingle(sql, params);
        return count != null && count > 0;
    }

    /**
     * Get record count
     */
    public long getRecordCount(String table, String whereClause, Object... params) throws SQLException {
        String sql = String.format("SELECT COUNT(*) FROM %s", table);
        if (whereClause != null && !whereClause.isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        Object result = executeQuerySingle(sql, params);
        return result != null ? ((Number) result).longValue() : 0;
    }

    /**
     * Get column value from record
     */
    public Object getColumnValue(String table, String column, String whereClause, Object... params) throws SQLException {
        String sql = String.format("SELECT %s FROM %s WHERE %s LIMIT 1", column, table, whereClause);
        return executeQuerySingle(sql, params);
    }

    /**
     * Begin transaction (auto-commit false)
     */
    public void beginTransaction() throws SQLException {
        Connection conn = getConnection();
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
        }
    }

    /**
     * Commit transaction
     */
    public void commit() throws SQLException {
        Connection conn = getConnection();
        if (!conn.getAutoCommit()) {
            conn.commit();
        }
    }

    /**
     * Rollback transaction
     */
    public void rollback() throws SQLException {
        Connection conn = getConnection();
        if (!conn.getAutoCommit()) {
            conn.rollback();
        }
    }

    /**
     * Set parameters for prepared statement
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    /**
     * Get current connection
     */
    private Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn == null || conn.isClosed()) {
            connect();
            conn = connectionHolder.get();
        }
        return conn;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        Connection conn = connectionHolder.get();
        return conn != null && !isConnectionClosed(conn);
    }

    private boolean isConnectionClosed(Connection conn) {
        try {
            return conn.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }
}
