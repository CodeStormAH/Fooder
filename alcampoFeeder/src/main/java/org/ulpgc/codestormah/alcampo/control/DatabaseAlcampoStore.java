package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;
import java.io.File;
import java.sql.*;
import java.util.List;

public class DatabaseAlcampoStore implements AlcampoStore {

    private final String jdbcUrl;

    public DatabaseAlcampoStore(File dbFile) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    @Override
    public void store(List<Product> products) {
        try (Connection conn = DriverManager.getConnection(this.jdbcUrl)) {
            conn.setAutoCommit(false);
            createSchema(conn);
            saveProducts(products, conn);
            savePrices(products, conn);
            conn.commit();
            System.out.println("Almacenados " + products.size() + " productos y precios en: " + this.jdbcUrl);
        } catch (SQLException e) {
            System.err.println("Error almacenando datos: " + e.getMessage());
        }
    }

    private void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id TEXT PRIMARY KEY, name TEXT, normalized_name TEXT, " +
                    "brand TEXT, category TEXT, quantity REAL, unit TEXT)");

            st.execute("CREATE TABLE IF NOT EXISTS prices (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, product_id TEXT, " +
                    "price REAL, unit_price REAL, is_on_sale BOOLEAN, date TEXT DEFAULT (datetime('now')), " +
                    "FOREIGN KEY(product_id) REFERENCES products(id))");
        }
    }

    private void saveProducts(List<Product> products, Connection conn) throws SQLException {
        String sql = "INSERT OR REPLACE INTO products (id, name, normalized_name, brand, category, quantity, unit) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product p : products) {
                ps.setString(1, p.getId());
                ps.setString(2, p.getName());
                ps.setString(3, p.getNormalizedName());
                ps.setString(4, p.getBrand());
                ps.setString(5, p.getCategory());
                ps.setDouble(6, p.getQuantity());
                ps.setString(7, p.getUnit());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void savePrices(List<Product> products, Connection conn) throws SQLException {
        String sql = "INSERT INTO prices (product_id, price, unit_price, is_on_sale) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product p : products) {
                ps.setString(1, p.getId());
                ps.setDouble(2, p.getUnitPrice());
                ps.setDouble(3, p.getPricePerUnit());
                ps.setBoolean(4, p.isOnSale());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}