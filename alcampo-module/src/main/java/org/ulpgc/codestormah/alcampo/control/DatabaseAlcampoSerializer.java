package org.ulpgc.codestormah.alcampo.control;

import org.ulpgc.codestormah.alcampo.model.Product;

import java.sql.*;
import java.util.List;

public class DatabaseAlcampoSerializer implements AlcampoSerializer {

    private static final String DB_PATH = "jdbc:sqlite:alcampo.db";

    @Override
    public void serialize(List<Product> productos) {
        try (Connection conn = DriverManager.getConnection(DB_PATH)) {
            crearTablas(conn);
            insertarProductos(productos, conn);
            insertarPrecios(productos, conn);
            System.out.println("Datos guardados correctamente en la base de datos.");
        } catch (SQLException e) {
            System.err.println("Error al guardar en la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void crearTablas(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS alcampo_products (" +
                            "id TEXT PRIMARY KEY," +
                            "nombre TEXT," +
                            "nombre_normalizado TEXT," +
                            "marca TEXT," +
                            "categoria TEXT," +
                            "cantidad REAL," +
                            "unidad TEXT)"
            );

            st.execute(
                    "CREATE TABLE IF NOT EXISTS alcampo_prices (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "product_id TEXT," +
                            "precio_unidad REAL," +
                            "en_oferta BOOLEAN," +
                            "fecha TEXT DEFAULT (datetime('now'))," + // Marca temporal requerida por el profesor
                            "FOREIGN KEY(product_id) REFERENCES alcampo_products(id))"
            );
        }
    }

    private void insertarProductos(List<Product> productos, Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO alcampo_products VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product p : productos) {
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

    private void insertarPrecios(List<Product> productos, Connection conn) throws SQLException {
        String sql = "INSERT INTO alcampo_prices (product_id, precio_unidad, en_oferta, fecha) " +
                "VALUES (?, ?, ?, datetime('now'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Product p : productos) {
                ps.setString(1, p.getId());
                ps.setDouble(2, p.getUnitPrice());
                ps.setBoolean(3, p.isOnOffer());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
