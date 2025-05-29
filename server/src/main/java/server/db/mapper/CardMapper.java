package server.db.mapper;

import common.model.Card;
import common.model.Tier;
import common.model.User;
import common.model.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Mapper for converting query results to Card objects
 */
public class CardMapper {
    /**
     * Converts query result to Card object
     * @param rs query result
     * @return Card object
     * @throws SQLException if error occurs while getting data
     */
    public static Card map(ResultSet rs) throws SQLException {
        Card card = new Card();
        card.setId(rs.getLong("id"));
        card.setCardNumber(rs.getString("number"));
        card.setPoints(rs.getInt("points"));
        
        // Set user ID
        try {
            card.setUserId(rs.getLong("user_id"));
        } catch (SQLException e) {
            // Column not found, ignore
        }
        
        // Handle tier information
        try {
            int tierId = rs.getInt("tier_id");
            card.setLevel(tierId);
        } catch (SQLException e) {
            // Column not found, set default
            card.setLevel(1);
        }
        
        // Handle dates
        try {
            if (rs.getTimestamp("issue_date") != null) {
                card.setIssueDate(rs.getTimestamp("issue_date").toLocalDateTime());
            }
        } catch (SQLException e) {
            // Column not found, ignore
        }
        
        // Handle last used date if available
        try {
            if (rs.getTimestamp("last_used") != null) {
                card.setLastUsed(rs.getTimestamp("last_used").toLocalDateTime());
            }
        } catch (SQLException e) {
            // Column not found, ignore
        }
        
        // Handle status
        try {
            boolean isActive = rs.getBoolean("is_active");
            card.setStatus(isActive ? "ACTIVE" : "INACTIVE");
        } catch (SQLException e) {
            // Column not found, set default
            card.setStatus("ACTIVE");
        }
        
        return card;
    }
    
    /**
     * Checks if the query result contains the specified column
     * @param rs query result
     * @param columnName column name
     * @return true if column exists
     * @throws SQLException if error occurs while getting metadata
     */
    private static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columns = metaData.getColumnCount();
        for (int i = 1; i <= columns; i++) {
            if (columnName.equals(metaData.getColumnName(i))) {
                return true;
            }
            // Some JDBC drivers return column labels instead of names
            if (columnName.equals(metaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
} 