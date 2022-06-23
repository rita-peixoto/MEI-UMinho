package DAO;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface StatementCall<K> {
    void setParameter(PreparedStatement pst, int id, K value) throws SQLException;
}
