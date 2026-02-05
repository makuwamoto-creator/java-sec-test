import java.sql.*;

public class App {
    public void getUser(String userId) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");
        Statement stmt = conn.createStatement();
        // ❌ 脆弱性テスト用：SQLインジェクションが起きる書き方
        // String query = "SELECT * FROM users WHERE id = " + "`" + userId + "'";
        String query = "SELECT * FROM users WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, userId);
        
        ResultSet rs = stmt.executeQuery(query);

        // GitHubが検知しやすいように、一般的な形式を模したダミーキーです
        String gcp_key = "AIzaSyA1234567890BCDEFGHIJKLMNOPQRSTUV";
    }
}
