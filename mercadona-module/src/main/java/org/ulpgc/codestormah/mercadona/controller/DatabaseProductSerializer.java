package org.ulpgc.codestormah.mercadona.controller;

import org.ulpgc.codestormah.mercadona.model.Product;

import java.sql.*;
import java.util.List;

public class DatabaseProductSerializer implements ProductSerializer {

    private static final String DB_PATH = "jdbc:sqlite:mercadona.db";

    private static final String CREATE_PRODUCTS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS products (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "normalized_name TEXT," +
                    "brand TEXT," +
                    "category TEXT," +
                    "amount REAL," +
                    "unit TEXT);";

    private static final String CREATE_PRICES_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS prices (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_id TEXT," +
                    "unit_price REAL," +
                    "on_offer BOOLEAN," +
                    "date TEXT," +
                    "FOREIGN KEY(product_id) REFERENCES products(id));";

    private static final String INSERT_PRODUCTS_SQL =
            "INSERT OR IGNORE INTO products " +
                    "(id, name, normalized_name, brand, category, amount, unit) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_PRICES_SQL =
            "INSERT INTO prices " +
                    "(product_id, unit_price, on_offer, date) " +
                    "VALUES (?, ?, ?, datetime('now'))";

    @Override
    public void save(List<Product> products) {
        try (Connection conn = connect()) {
            initDatabase(conn);
            persist(products, conn);
            log(products);
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_PATH);
    }

    private void initDatabase(Connection conn) throws SQLException {
        createTables(conn);
    }

    private void persist(List<Product> products, Connection conn) throws SQLException {
        insertProducts(products, conn);
        insertPrices(products, conn);
    }

    private void log(List<Product> products) {
        System.out.println("Saved " + products.size() + " products.");
    }

    private void handleError(SQLException e) {
        e.printStackTrace();
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_PRODUCTS_TABLE_SQL);
            stmt.execute(CREATE_PRICES_TABLE_SQL);
        }
    }

    private void insertProducts(List<Product> products, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PRODUCTS_SQL)) {
            for (Product p : products) {
                fillProduct(ps, p);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void fillProduct(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.id());
        ps.setString(2, p.name());
        ps.setString(3, p.normalizedName());
        ps.setString(4, p.brand());
        ps.setString(5, p.category());
        ps.setDouble(6, p.amount());
        ps.setString(7, p.unit());
    }

    private void insertPrices(List<Product> products, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PRICES_SQL)) {
            for (Product p : products) {
                fillPrice(ps, p);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void fillPrice(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.id());
        ps.setDouble(2, p.unitPrice());
        ps.setBoolean(3, p.onOffer());
    }
}