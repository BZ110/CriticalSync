package org.criticalking.criticalSync;

import org.bukkit.Bukkit;
import java.sql.*;

public class SQLInstance {

    private Connection conn;
    private String url;
    private final CriticalSync main;

    public SQLInstance(String jdbc, CriticalSync main){
        this.url = jdbc;
        this.main = main;
    }

    public void init(){
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if(conn != null){
            main.getLogger().info("Database successfully connected!");
        } else {
            main.getLogger().severe("Unable to connect to the database. Disabling...");
            Bukkit.getPluginManager().disablePlugin(main);
        }
    }

    // Ensures that the connection is valid. If not, it attempts to reconnect.
    private void ensureConnection() {
        try {
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                main.getLogger().info("Database connection lost. Attempting to reconnect...");
                conn = DriverManager.getConnection(url);
                if (conn != null && !conn.isClosed() && conn.isValid(2)) {
                    main.getLogger().info("Database reconnected successfully!");
                } else {
                    main.getLogger().severe("Failed to reconnect to the database.");
                }
            }
        } catch (SQLException e) {
            main.getLogger().severe("Error checking/reconnecting database connection: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void close(){
        try {
            main.getLogger().info("Closing connection!");
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            main.getLogger().severe("We were unable to close the connection! If you just turned on this plugin for the first time, ignore this error.");
            throw new RuntimeException(e);
        }
    }

    public void addTable(Table table) {
        ensureConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(table.getInitCommand());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeUpdate(String query) {
        ensureConnection();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean rowExists(String tableName, String whereClause) {
        ensureConnection();
        String query = "SELECT COUNT(*) FROM " + tableName + " " + whereClause;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if(rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    public Connection getConnection() {
        return conn;
    }
}
