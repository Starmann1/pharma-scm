package pharma.repository;

import java.sql.SQLException;
import java.util.List;
import pharma.model.BOMDetail;
import pharma.model.BOMHeader;

public interface BOMRepository {
    int createBOM(BOMHeader header, List<BOMDetail> details) throws SQLException, ClassNotFoundException;
    BOMHeader getBOMById(int bomId) throws SQLException, ClassNotFoundException;
    List<BOMDetail> getBOMIngredients(int bomId) throws SQLException, ClassNotFoundException;
    List<BOMHeader> getActiveBOMsForMaterial(String materialCode) throws SQLException, ClassNotFoundException;
}
