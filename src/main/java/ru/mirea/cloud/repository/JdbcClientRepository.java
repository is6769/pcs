package ru.mirea.cloud.repository;

import ru.mirea.cloud.exception.*;
import ru.mirea.cloud.model.Client;
import ru.mirea.cloud.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public final class JdbcClientRepository implements ClientRepository {
    private final DatabaseManager database;
    public JdbcClientRepository(DatabaseManager database) { this.database = database; }

    @Override public Client create(Client client) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO clients(name,email,company) VALUES (?,?,?) RETURNING *")) {
            bind(statement, client);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return map(rows); }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public List<Client> findAll() {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM clients ORDER BY id"); ResultSet rows = statement.executeQuery()) {
            List<Client> result = new ArrayList<>();
            while (rows.next()) result.add(map(rows));
            return result;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public Optional<Client> findById(long id) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM clients WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public boolean emailExists(String email, long excludingId) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM clients WHERE email = ? AND id <> ?")) {
            statement.setString(1, email);
            statement.setLong(2, excludingId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public void update(Client client) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE clients SET name=?,email=?,company=? WHERE id=?")) {
            bind(statement, client);
            statement.setLong(4, client.getId());
            if (statement.executeUpdate() == 0) throw new EntityNotFoundException("Клиент", client.getId());
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public void delete(long id) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM clients WHERE id=?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) throw new EntityNotFoundException("Клиент", id);
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private static void bind(PreparedStatement statement, Client client) throws SQLException {
        statement.setString(1, client.getName());
        statement.setString(2, client.getEmail());
        statement.setString(3, client.getCompany());
    }

    private static Client map(ResultSet row) throws SQLException {
        return new Client(row.getLong("id"), row.getString("name"), row.getString("email"), row.getString("company"));
    }
}
