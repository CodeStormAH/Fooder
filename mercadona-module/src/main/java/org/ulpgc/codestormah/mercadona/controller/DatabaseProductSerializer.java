package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;

import java.sql.*;
import java.util.List;

public class DatabaseProductSerializer implements ProductSerializer {

    private static final String DB_PATH = "jdbc:sqlite:mercadona.db";

    @Override
    public void save(List<Product> products) {
        try (Connection conn = DriverManager.getConnection(DB_PATH)) {
            createTables(conn);
            insertProducts(products, conn);
            insertPrices(products, conn);
            System.out.println("Saved " + products.size() + " products.");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS products (" +
                            "id TEXT PRIMARY KEY," +
                            "name TEXT," +
                            "normalized_name TEXT," +
                            "brand TEXT," +
                            "category TEXT," +
                            "amount REAL," +
                            "unit TEXT);"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS prices (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "product_id TEXT," +
                            "unit_price REAL," +
                            "on_offer BOOLEAN," +
                            "date TEXT," +
                            "FOREIGN KEY(product_id) REFERENCES products(id));"
            );
        }
    }

    private void insertProducts(List<Product> products, Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO products " +
                "(id, name, normalized_name, brand, category, amount, unit) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product p : products) {
                ps.setString(1, p.getId());
                ps.setString(2, p.getName());
                ps.setString(3, p.getNormalizedName());
                ps.setString(4, p.getBrand());
                ps.setString(5, p.getCategory());
                ps.setDouble(6, p.getAmount());
                ps.setString(7, p.getUnit());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertPrices(List<Product> products, Connection conn) throws SQLException {
        String sql = "INSERT INTO prices " +
                "(product_id, unit_price, on_offer, date) " +
                "VALUES (?, ?, ?, datetime('now'))";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product p : products) {
                ps.setString(1, p.getId());
                ps.setDouble(2, p.getUnitPrice());
                ps.setBoolean(3, p.isOnOffer());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
