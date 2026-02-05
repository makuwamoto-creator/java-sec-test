import java.sql.*;

public class App {
    public void getUser(String userId) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");
        
        String key = "xoxb-123456789012-1234567890123-4567890abcdefghijklmnopqrstuv";

        // 1. GitHub Personal Access Token (一番検知されやすい)
        // 文字列を分割せず、そのまま1つのリテラルとして書くのがコツです
        String github_token = "ghp_n0tReAlK3yJuStF0rTeStingPuRp0s3sOnLy12345";
             
        // 2. AWS Access Key (AKIAで始まり20文字)
        String aws_access_key = "AKIAV7XXXXXXXXXXXXXX"; 
        
        // 3. AWS Secret Key (40文字のランダムっぽい文字列)
        String aws_secret_key = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

        Statement stmt = conn.createStatement();
        // ❌ 脆弱性テスト用：SQLインジェクションが起きる書き方
        // String query = "SELECT * FROM users WHERE id = " + "`" + userId + "'";
        String query = "SELECT * FROM users WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, userId);
        
        ResultSet rs = stmt.executeQuery(query);



    }
}
