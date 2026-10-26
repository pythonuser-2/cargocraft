package com.cargocraft.db;

import com.cargocraft.model.CargoItem;
import com.cargocraft.model.Material;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CargoDatabaseManager {
    private String jdbcUrl = "jdbc:mysql://localhost:3306/cargocraft_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private String username = "root";
    private String password = "mysql@321*"; // <-- Put your MySQL root password here

    public CargoDatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found in classpath: " + e.getMessage());
        }
    }

    public boolean saveCargoLog(CargoItem item) {
        String sql = "INSERT INTO cargo_items (item_name, length, width, height, weight, geometry_profile, density_class) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, item.getItemName());
            stmt.setDouble(2, item.getLength());
            stmt.setDouble(3, item.getWidth());
            stmt.setDouble(4, item.getHeight());
            stmt.setDouble(5, item.getWeight());
            stmt.setString(6, item.getGeometryProfile());
            stmt.setString(7, item.getDensityClass());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
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
        List<CargoItem> logs = new ArrayList<>();
        String sql = "SELECT item_id, item_name, length, width, height, weight, geometry_profile, density_class FROM cargo_items ORDER BY item_id ASC";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("item_id");
                String name = rs.getString("item_name");
                double l = rs.getDouble("length");
                double w = rs.getDouble("width");
                double h = rs.getDouble("height");
                double wt = rs.getDouble("weight");
                String geom = rs.getString("geometry_profile");
                String density = rs.getString("density_class");
                Material mat = Material.fromName(name);

                logs.add(new CargoItem(id, name, l, w, h, wt, geom, density, mat));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching cargo logs: " + e.getMessage());
        }
        return logs;
    }

    public boolean deleteCargoLog(int id) {
        String sql = "DELETE FROM cargo_items WHERE item_id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting cargo log: " + e.getMessage());
        }
        return false;
    }

    public void clearAllLogs() {
        String sql = "TRUNCATE TABLE cargo_items";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error clearing cargo logs: " + e.getMessage());
        }
    }
}