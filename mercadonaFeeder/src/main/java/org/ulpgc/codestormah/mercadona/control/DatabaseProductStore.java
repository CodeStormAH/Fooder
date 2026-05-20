package org.ulpgc.codestormah.mercadona.control;

import org.ulpgc.codestormah.mercadona.model.Product;
import java.sql.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseProductStore implements ProductStore {

    private static final Logger logger = Logger.getLogger(DatabaseProductStore.class.getName());
    private static final String CREATE_PRODUCTS_SQL = "CREATE TABLE IF NOT EXISTS products (id TEXT PRIMARY KEY, name TEXT, normalized_name TEXT, brand TEXT, category TEXT, amount REAL, unit TEXT);";
    private static final String CREATE_PRICES_SQL = "CREATE TABLE IF NOT EXISTS prices (id INTEGER PRIMARY KEY AUTOINCREMENT, product_id TEXT, unit_price REAL, on_offer BOOLEAN, date TEXT, FOREIGN KEY(product_id) REFERENCES products(id));";
    private static final String INSERT_PRODUCTS_SQL = "INSERT OR IGNORE INTO products (id, name, normalized_name, brand, category, amount, unit) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_PRICES_SQL = "INSERT INTO prices (product_id, unit_price, on_offer, date) VALUES (?, ?, ?, datetime('now'))";

    private final String dbPath;

    public DatabaseProductStore(String dbPath) {
        this.dbPath = dbPath;
    }

    @Override
    public void save(List<Product> products) {
        try (Connection c = DriverManager.getConnection(dbPath)) {
            processSave(products, c);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error", e);
        }
    }

    private void processSave(List<Product> p, Connection c) throws SQLException {
        createDatabaseTables(c);
        insertProducts(p, c);
        insertPrices(p, c);
    }

    private void createDatabaseTables(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(CREATE_PRODUCTS_SQL);
            s.execute(CREATE_PRICES_SQL);
        }
    }

    private void insertProducts(List<Product> p, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(INSERT_PRODUCTS_SQL)) {
            fillAndBatchProducts(p, s);
        }
    }

    private void fillAndBatchProducts(List<Product> p, PreparedStatement s) throws SQLException {
        for (Product prod : p) addProductToBatch(prod, s);
        s.executeBatch();
    }

    private void addProductToBatch(Product p, PreparedStatement s) throws SQLException {
        setProductStrings(s, p);
        s.setDouble(6, p.quantity());
        s.setString(7, p.unit());
        s.addBatch();
    }

    private void setProductStrings(PreparedStatement s, Product p) throws SQLException {
        s.setString(1, p.id());
        s.setString(2, p.name());
        s.setString(3, p.normalizedName());
        s.setString(4, p.brand());
        s.setString(5, p.category());
    }

    private void insertPrices(List<Product> p, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(INSERT_PRICES_SQL)) {
            fillAndBatchPrices(p, s);
        }
    }

    private void fillAndBatchPrices(List<Product> p, PreparedStatement s) throws SQLException {
        for (Product prod : p) addPriceToBatch(prod, s);
        s.executeBatch();
    }

    private void addPriceToBatch(Product p, PreparedStatement s) throws SQLException {
        s.setString(1, p.id());
        s.setDouble(2, p.unitPrice());
        s.setBoolean(3, p.isOnSale());
        s.addBatch();
    }
}