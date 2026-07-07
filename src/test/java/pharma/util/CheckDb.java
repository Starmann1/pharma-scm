package pharma.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CheckDb {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/pharma_scm", "postgres", "postgres");
            Statement stmt = conn.createStatement();
            
            System.out.println("Executing V010 patch...");
            String sql = Files.readString(Paths.get("db/postgresql/seed/V010__seed_roles_permissions.sql"));
            // JDBC driver doesn't natively handle multiple statements separated by semicolon well in all cases.
            // Split by ; and execute individually
            String[] statements = sql.split(";");
            for (String s : statements) {
                if (!s.trim().isEmpty()) {
                    stmt.execute(s.trim());
                }
            }
            
            System.out.println("Done executing patch. Verifying...");
            java.sql.ResultSet rs2 = stmt.executeQuery("SELECT p.permission_name FROM role_permission rp JOIN role_master r ON rp.role_id = r.role_id JOIN permission_master p ON rp.permission_id = p.permission_id WHERE r.role_name = 'Admin'");
            int count = 0;
            while(rs2.next()) {
                System.out.println("Admin has: " + rs2.getString(1));
                count++;
            }
            System.out.println("Total permissions: " + count);
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
