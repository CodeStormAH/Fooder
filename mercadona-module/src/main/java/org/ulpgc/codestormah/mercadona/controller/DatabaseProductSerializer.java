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
        try (Connection connection = openConnection()) {
            initializeDatabase(connection);
            saveProductsAndPrices(products, connection);
            logSavedProducts(products);
        } catch (SQLException exception) {
            handleDatabaseException(exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DB_PATH);
    }

    private void initializeDatabase(Connection connection) throws SQLException {
        createDatabaseTables(connection);
    }

    private void saveProductsAndPrices(List<Product> products, Connection connection) throws SQLException {
        insertProducts(products, connection);
        insertPrices(products, connection);
    }

    private void logSavedProducts(List<Product> products) {
        System.out.println("Saved " + products.size() + " products.");
    }

    private void handleDatabaseException(SQLException exception) {
        exception.printStackTrace();
    }

    private void createDatabaseTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_PRODUCTS_TABLE_SQL);
            statement.execute(CREATE_PRICES_TABLE_SQL);
        }
    }

    private void insertProducts(List<Product> products, Connection connection) throws SQLException {
        try (PreparedStatement productInsertStatement = connection.prepareStatement(INSERT_PRODUCTS_SQL)) {
            for (Product product : products) {
                fillProduct(productInsertStatement, product);
                productInsertStatement.addBatch();
            }
            productInsertStatement.executeBatch();
        }
    }

    private void fillProduct(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.id());
        statement.setString(2, product.name());
        statement.setString(3, product.normalizedName());
        statement.setString(4, product.brand());
        statement.setString(5, product.category());
        statement.setDouble(6, product.amount());
        statement.setString(7, product.unit());
    }

    private void insertPrices(List<Product> products, Connection connection) throws SQLException {
        try (PreparedStatement priceInsertStatement = connection.prepareStatement(INSERT_PRICES_SQL)) {
            for (Product product : products) {
                fillPrice(priceInsertStatement, product);
                priceInsertStatement.addBatch();
            }
            priceInsertStatement.executeBatch();
        }
    }

    private void fillPrice(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.id());
        statement.setDouble(2, product.unitPrice());
        statement.setBoolean(3, product.onOffer());
    }
}