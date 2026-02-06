import java.io.BufferedReader;
import java.io.InputStreamReader;

public class App {
    public static void main(String[] args) {
        // 本来は引数などで受け取るユーザー入力（例: "google.com"）
        // 攻撃者が "google.com; cat /etc/passwd" と入力すると大変なことに！
        String targetDomain = args.length > 0 ? args[0] : "localhost";

        try {
            // ❌ 脆弱なポイント：ユーザー入力をそのままシェルコマンドに渡している
            //String command = "ping -c 3 " + targetDomain;
            //Process process = Runtime.getRuntime().exec(command);
            ProcessBuilder pb = new ProcessBuilder("ping", "-c", "3", targetDomain);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}