package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import pharma.model.BOMDetail;
import pharma.model.BOMHeader;
import pharma.repository.BOMRepository;
import pharma.service.DatabaseService;

public class BOMJdbcRepository implements BOMRepository {
    private final DatabaseService databaseService;
    private final JdbcSqlDialect sqlDialect;

    public BOMJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    public BOMJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public int createBOM(BOMHeader header, List<BOMDetail> details) throws SQLException, ClassNotFoundException {
        int bomId = -1;

        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String headerSql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.BOM_HEADER)
                        + " (material_code, version_number, is_active, effective_date, description) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(headerSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, header.getMaterialCode());
                    pstmt.setInt(2, header.getVersionNumber());
                    pstmt.setBoolean(3, header.isActive());
                    pstmt.setDate(4, java.sql.Date.valueOf(header.getEffectiveDate()));
                    pstmt.setString(5, header.getDescription());

                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            bomId = rs.getInt(1);
                        }
                    }
                }

                if (bomId > 0 && details != null && !details.isEmpty()) {
                    String detailSql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.BOM_DETAILS)
                            + " (bom_id, ingredient_material_code, required_qty, uom, sequence_number, notes) VALUES (?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
                        for (BOMDetail detail : details) {
                            pstmt.setInt(1, bomId);
                            pstmt.setString(2, detail.getIngredientMaterialCode());
                            pstmt.setDouble(3, detail.getRequiredQty());
                            pstmt.setString(4, detail.getUom());
                            pstmt.setInt(5, detail.getSequenceNumber());
                            pstmt.setString(6, detail.getNotes());
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }
                }

                conn.commit();
                return bomId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public BOMHeader getBOMById(int bomId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.BOM_HEADER) + " WHERE bom_id = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bomId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new BOMHeader(
                            rs.getInt("bom_id"),
                            rs.getString("material_code"),
                            rs.getInt("version_number"),
                            rs.getBoolean("is_active"),
                            rs.getDate("effective_date").toLocalDate(),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return null;
    }

    @Override
    public List<BOMDetail> getBOMIngredients(int bomId) throws SQLException, ClassNotFoundException {
        List<BOMDetail> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.BOM_DETAILS) + " WHERE bom_id = ? ORDER BY sequence_number";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bomId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new BOMDetail(
                            rs.getInt("bom_detail_id"),
                            rs.getInt("bom_id"),
                            rs.getString("ingredient_material_code"),
                            rs.getDouble("required_qty"),
                            rs.getString("uom"),
                            rs.getInt("sequence_number"),
                            rs.getString("notes")));
                }
            }
        }
        return ingredients;
    }

    @Override
    public List<BOMHeader> getActiveBOMsForMaterial(String materialCode) throws SQLException, ClassNotFoundException {
        List<BOMHeader> boms = new ArrayList<>();
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.BOM_HEADER) + " WHERE material_code = ? AND is_active = " + sqlDialect.trueLiteral() + " ORDER BY version_number DESC";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, materialCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    boms.add(new BOMHeader(
                            rs.getInt("bom_id"),
                            rs.getString("material_code"),
                            rs.getInt("version_number"),
                            rs.getBoolean("is_active"),
                            rs.getDate("effective_date").toLocalDate(),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime()));
                }
            }
        }
        return boms;
    }

    @Override
    public List<BOMHeader> getAllBOMs() throws SQLException, ClassNotFoundException {
        List<BOMHeader> boms = new ArrayList<>();
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.BOM_HEADER) + " ORDER BY bom_id DESC";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                boms.add(new BOMHeader(
                        rs.getInt("bom_id"),
                        rs.getString("material_code"),
                        rs.getInt("version_number"),
                        rs.getBoolean("is_active"),
                        rs.getDate("effective_date").toLocalDate(),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()));
            }
        }
        return boms;
    }

    @Override
    public boolean updateBOM(int bomId, BOMHeader header, List<BOMDetail> details) throws SQLException, ClassNotFoundException {
        String updateHeaderSql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.BOM_HEADER)
                + " SET version_number = ?, is_active = ?, effective_date = ?, description = ?, updated_at = " + sqlDialect.nowExpression()
                + " WHERE bom_id = ?";

        String deleteDetailsSql = "DELETE FROM " + sqlDialect.table(JdbcSqlDialect.Table.BOM_DETAILS) + " WHERE bom_id = ?";
        String insertDetailSql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.BOM_DETAILS)
                + " (bom_id, ingredient_material_code, required_qty, uom, sequence_number, notes) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updatedRows;
                try (PreparedStatement pstmt = conn.prepareStatement(updateHeaderSql)) {
                    pstmt.setInt(1, header.getVersionNumber());
                    pstmt.setBoolean(2, header.isActive());
                    pstmt.setDate(3, java.sql.Date.valueOf(header.getEffectiveDate()));
                    pstmt.setString(4, header.getDescription());
                    pstmt.setInt(5, bomId);
                    updatedRows = pstmt.executeUpdate();
                }

                if (updatedRows == 0) {
                    conn.rollback();
                    return false;
                }

                if (details != null) {
                    try (PreparedStatement delStmt = conn.prepareStatement(deleteDetailsSql)) {
                        delStmt.setInt(1, bomId);
                        delStmt.executeUpdate();
                    }

                    if (!details.isEmpty()) {
                        try (PreparedStatement insStmt = conn.prepareStatement(insertDetailSql)) {
                            for (BOMDetail detail : details) {
                                insStmt.setInt(1, bomId);
                                insStmt.setString(2, detail.getIngredientMaterialCode());
                                insStmt.setDouble(3, detail.getRequiredQty());
                                insStmt.setString(4, detail.getUom());
                                insStmt.setInt(5, detail.getSequenceNumber());
                                insStmt.setString(6, detail.getNotes());
                                insStmt.addBatch();
                            }
                            insStmt.executeBatch();
                        }
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean deleteBOM(int bomId) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.BOM_HEADER)
                + " SET is_active = FALSE, updated_at = " + sqlDialect.nowExpression()
                + " WHERE bom_id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bomId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
