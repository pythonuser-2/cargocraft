package com.cargocraft.db;

import com.cargocraft.model.CargoItem;
import com.cargocraft.model.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CargoDatabaseManager {
    // URL without database name initially so we can create it automatically if missing
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/cargocraft_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root"; // Update with your MySQL user if needed
    private static final String PASS = "mysql@321*";

    public CargoDatabaseManager() {
        initializeDatabase();
    }

    // Automatically bootstraps database and tables on startup for seamless new-device setup
    public void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(SERVER_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS cargocraft_db;");
            stmt.executeUpdate("USE cargocraft_db;");
            
            String createTableSQL = "CREATE TABLE IF NOT EXISTS cargo_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "item_name VARCHAR(255) NOT NULL, " +
                    "length DOUBLE NOT NULL, " +
                    "width DOUBLE NOT NULL, " +
                    "height DOUBLE NOT NULL, " +
                    "weight DOUBLE NOT NULL, " +
                    "geometry_profile VARCHAR(100), " +
                    "density_class VARCHAR(100), " +
                    "material VARCHAR(100)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
            
            stmt.executeUpdate(createTableSQL);
            System.out.println("✅ MySQL Database & Tables verified/initialized successfully.");

        } catch (SQLException e) {
            System.err.println("⚠️ Database initialization warning: " + e.getMessage());
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public boolean saveCargoLog(CargoItem item) {
        String sql = "INSERT INTO cargo_logs (item_name, length, width, height, weight, geometry_profile, density_class, material) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, item.getItemName());
            pstmt.setDouble(2, item.getLength());
            pstmt.setDouble(3, item.getWidth());
            pstmt.setDouble(4, item.getHeight());
            pstmt.setDouble(5, item.getWeight());
            pstmt.setString(6, item.getGeometryProfile());
            pstmt.setString(7, item.getDensityClass());
            pstmt.setString(8, item.getMaterial().getMaterialName());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setItemId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving cargo log: " + e.getMessage());
        }
        return false;
    }

    public List<CargoItem> fetchCargoLogs() {
        List<CargoItem> items = new ArrayList<>();
        String sql = "SELECT id, item_name, length, width, height, weight, geometry_profile, density_class, material FROM cargo_logs";
        
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("item_name");
                double l = rs.getDouble("length");
                double w = rs.getDouble("width");
                double h = rs.getDouble("height");
                double wt = rs.getDouble("weight");
                String geom = rs.getString("geometry_profile");
                String density = rs.getString("density_class");
                String matName = rs.getString("material");
                
                CargoItem item = new CargoItem(id, name, l, w, h, wt, geom, density, Material.fromName(matName));
                items.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching cargo logs: " + e.getMessage());
        }
        return items;
    }

    public boolean deleteCargoLog(int id) {
        String sql = "DELETE FROM cargo_logs WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting cargo log ID " + id + ": " + e.getMessage());
            return false;
        }
    }

    public boolean clearAllLogs() {
        String sql = "TRUNCATE TABLE cargo_logs";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.err.println("Error clearing cargo logs: " + e.getMessage());
            return false;
        }
    }
}