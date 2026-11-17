package com.example.warehouse.db;

import com.example.warehouse.model.Movement;
import com.example.warehouse.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarehouseRepository {

    // ---------------------------------------------------------------- products

    public List<Product> findAll(String filter) {
        boolean hasFilter = filter != null && !filter.isBlank();
        String sql = "SELECT id, sku, name, location, quantity, min_stock, price FROM products"
                + (hasFilter ? " WHERE sku LIKE ? OR name LIKE ? OR location LIKE ?" : "")
                + " ORDER BY sku";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (hasFilter) {
                String like = "%" + filter.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            List<Product> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Product(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getInt(5), rs.getInt(6), rs.getDouble(7)));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load products: " + e.getMessage(), e);
        }
    }

    public void insert(Product p) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO products (sku, name, location, quantity, min_stock, price) VALUES (?,?,?,?,?,?)")) {
                    ps.setString(1, p.getSku());
                    ps.setString(2, p.getName());
                    ps.setString(3, p.getLocation());
                    ps.setInt(4, p.getQuantity());
                    ps.setInt(5, p.getMinStock());
                    ps.setDouble(6, p.getPrice());
                    ps.executeUpdate();
                }
                if (p.getQuantity() > 0) {
                    addMovement(c, p.getSku(), p.getQuantity(), "IN", "Initial stock");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new DataAccessException("A product with SKU " + p.getSku() + " already exists.", e);
            }
            throw new DataAccessException("Could not save product: " + e.getMessage(), e);
        }
    }

    /** Updates master data only; quantity is changed through {@link #adjustStock}. */
    public void update(Product p) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE products SET sku=?, name=?, location=?, min_stock=?, price=? WHERE id=?")) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setString(3, p.getLocation());
            ps.setInt(4, p.getMinStock());
            ps.setDouble(5, p.getPrice());
            ps.setLong(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new DataAccessException("A product with SKU " + p.getSku() + " already exists.", e);
            }
            throw new DataAccessException("Could not update product: " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete product: " + e.getMessage(), e);
        }
    }

    /** Changes the quantity by {@code delta} and logs the movement, in one transaction. */
    public void adjustStock(String sku, int delta, String type, String note) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                int current;
                try (PreparedStatement ps = c.prepareStatement("SELECT quantity FROM products WHERE sku=?")) {
                    ps.setString(1, sku);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new DataAccessException("Unknown SKU: " + sku);
                        }
                        current = rs.getInt(1);
                    }
                }
                if (current + delta < 0) {
                    throw new DataAccessException("Not enough stock for " + sku + " (available: " + current + ")");
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE products SET quantity = quantity + ? WHERE sku=?")) {
                    ps.setInt(1, delta);
                    ps.setString(2, sku);
                    ps.executeUpdate();
                }
                addMovement(c, sku, delta, type, note);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not book stock: " + e.getMessage(), e);
        }
    }

    private void addMovement(Connection c, String sku, int delta, String type, String note) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO movements (sku, delta, type, note) VALUES (?,?,?,?)")) {
            ps.setString(1, sku);
            ps.setInt(2, delta);
            ps.setString(3, type);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    public List<Movement> recentMovements(int limit) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, sku, delta, type, note, created_at FROM movements ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            List<Movement> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Movement(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                            rs.getString(5), rs.getString(6)));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load movements: " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------- label templates

    public List<String> templateNames() {
        try (Connection c = Database.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM label_templates ORDER BY name")) {
            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
            return names;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load templates: " + e.getMessage(), e);
        }
    }

    public Optional<String> loadTemplate(String name) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT data FROM label_templates WHERE name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load template: " + e.getMessage(), e);
        }
    }

    public void saveTemplate(String name, String data) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO label_templates (name, data) VALUES (?,?)")) {
            ps.setString(1, name);
            ps.setString(2, data);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save template: " + e.getMessage(), e);
        }
    }

    public void deleteTemplate(String name) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM label_templates WHERE name=?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete template: " + e.getMessage(), e);
        }
    }
}
