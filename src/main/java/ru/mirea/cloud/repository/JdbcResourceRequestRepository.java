package ru.mirea.cloud.repository;

import ru.mirea.cloud.exception.*;
import ru.mirea.cloud.model.*;
import ru.mirea.cloud.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public final class JdbcResourceRequestRepository implements ResourceRequestRepository {
    private final DatabaseManager database;
    public JdbcResourceRequestRepository(DatabaseManager database) { this.database = database; }

    @Override public ResourceRequest create(ResourceRequest request) {
        String sql = """
                INSERT INTO resource_requests(client_id,title,description,resource_type,region,cpu,ram_gb,storage_gb,status)
                VALUES (?,?,?,?,?,?,?,?,?) RETURNING *
                """;
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, request);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return map(rows); }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public List<ResourceRequest> findAll() {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM resource_requests ORDER BY id"); ResultSet rows = statement.executeQuery()) {
            List<ResourceRequest> result = new ArrayList<>();
            while (rows.next()) result.add(map(rows));
            return result;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public Optional<ResourceRequest> findById(long id) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM resource_requests WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public void update(ResourceRequest request) {
        String sql = """
                UPDATE resource_requests SET client_id=?,title=?,description=?,resource_type=?,region=?,
                cpu=?,ram_gb=?,storage_gb=?,status=?,version=version+1 WHERE id=? AND version=?
                """;
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, request);
            statement.setLong(10, request.getId());
            statement.setInt(11, request.getVersion());
            checkVersion(statement.executeUpdate());
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public void delete(long id, int version) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM resource_requests WHERE id=? AND version=?")) {
            statement.setLong(1, id);
            statement.setInt(2, version);
            checkVersion(statement.executeUpdate());
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    @Override public boolean existsByClient(long clientId) {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM resource_requests WHERE client_id=? LIMIT 1")) {
            statement.setLong(1, clientId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private static void checkVersion(int count) {
        if (count == 0) throw new BusinessException("Заявка изменена или удалена другим пользователем. Повторите операцию.");
    }

    private static void bind(PreparedStatement statement, ResourceRequest request) throws SQLException {
        statement.setLong(1, request.getClientId());
        statement.setString(2, request.getTitle());
        statement.setString(3, request.getDescription());
        statement.setString(4, request.getResourceType().name());
        statement.setString(5, request.getRegion().name());
        statement.setInt(6, request.getCpu());
        statement.setInt(7, request.getRamGb());
        statement.setInt(8, request.getStorageGb());
        statement.setString(9, request.getStatus().name());
    }

    private static ResourceRequest map(ResultSet row) throws SQLException {
        return new ResourceRequest(row.getLong("id"), row.getLong("client_id"), row.getString("title"),
                row.getString("description"), ResourceType.valueOf(row.getString("resource_type")),
                Region.valueOf(row.getString("region")), row.getInt("cpu"), row.getInt("ram_gb"),
                row.getInt("storage_gb"), RequestStatus.valueOf(row.getString("status")),
                row.getTimestamp("created_at").toLocalDateTime(), row.getInt("version"));
    }
}
