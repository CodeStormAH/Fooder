package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseAlcampoStore implements AlcampoStore {

    private static final Logger logger = Logger.getLogger(DatabaseAlcampoStore.class.getName());
    private static final String CREATE_PRODUCTS_TABLE = "CREATE TABLE IF NOT EXISTS products (id TEXT PRIMARY KEY, name TEXT, normalized_name TEXT, brand TEXT, category TEXT, quantity REAL, unit TEXT)";
    private static final String CREATE_PRICES_TABLE = "CREATE TABLE IF NOT EXISTS prices (id INTEGER PRIMARY KEY AUTOINCREMENT, product_id TEXT, unit_price REAL, is_on_sale BOOLEAN, date TEXT DEFAULT (datetime('now')), FOREIGN KEY(product_id) REFERENCES products(id))";
    private static final String INSERT_PRODUCT = "INSERT OR REPLACE INTO products (id, name, normalized_name, brand, category, quantity, unit) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_PRICE = "INSERT INTO prices (product_id, unit_price, is_on_sale) VALUES (?, ?, ?)";
    private final String jdbcUrl;
    public DatabaseAlcampoStore(File databaseFile) {
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    @Override
    public void store(List<Product> products) {
        try {
            executeStoreTransaction(products);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error almacenando datos: " + e.getMessage(), e);
        }
    }

    private void executeStoreTransaction(List<Product> products) throws SQLException {
        try (Connection connection = DriverManager.getConnection(this.jdbcUrl)) {
            connection.setAutoCommit(false);
            createSchema(connection);
            saveProducts(products, connection);
            savePrices(products, connection);
            connection.commit();
            logger.info("Almacenados " + products.size() + " productos y precios en: " + this.jdbcUrl);
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_PRODUCTS_TABLE);
            statement.execute(CREATE_PRICES_TABLE);
        }
    }

    private void saveProducts(List<Product> products, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PRODUCT)) {
            for (Product product : products) {
                bindProductStatement(statement, product);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void bindProductStatement(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getId());
        statement.setString(2, product.getName());
        statement.setString(3, product.getNormalizedName());
        statement.setString(4, product.getBrand());
        statement.setString(5, product.getCategory());
        statement.setDouble(6, product.getQuantity());
        statement.setString(7, product.getUnit());
    }

    private void savePrices(List<Product> products, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PRICE)) {
            for (Product product : products) {
                bindPriceStatement(statement, product);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void bindPriceStatement(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getId());
        statement.setDouble(2, product.getUnitPrice());
        statement.setBoolean(3, product.isOnSale());
    }
}