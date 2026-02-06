import java.sql.*;

public class App {
    public void getUser(String userId) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement pstmt = null;
        try {
            String dbPassword = System.getenv("DB_PASSWORD");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", dbPassword);

            stmt = conn.createStatement();
            // ❌ 脆弱性テスト用：SQLインジェクションが起きる書き方
            // String query = "SELECT * FROM users WHERE id = " + "`" + userId + "'";
            String query = "SELECT * FROM users WHERE id = ?";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery(query);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (stmt != null) {
                stmt.close();
            }

            if (conn != null) {
                conn.close();
            }
        }

    }
}
