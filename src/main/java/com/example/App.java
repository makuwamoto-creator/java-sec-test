package com.example; // これを追加（名前は何でも良いですが、一般的になぞります）

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger; // 1. Loggerをインポート
import java.util.logging.Level;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        
        if (args.length > 0) {
            String userInput = args[0];
            
            // ✅ これが CWE-117 (ログ・インジェクション) のターゲットです
            // 外部からの入力（userInput）を、検証やサニタイズなしにログに書き込んでいます
            logger.log(Level.INFO, "User data: {0}", userInput);
            
            // こちらの書き方でも検知されるはずです
            logger.info("Processing input: " + userInput);
        }
        // 本来は引数などで受け取るユーザー入力（例: "google.com"）
        // 攻撃者が "google.com; cat /etc/passwd" と入力すると大変なことに！
        // String targetDomain = args.length > 0 ? args[0] : "localhost";

        try {
            // ❌ 脆弱なポイント：ユーザー入力をそのままシェルコマンドに渡している
            //ProcessBuilder pb = new ProcessBuilder("/usr/bin/ping", "-c", "3", targetDomain);
            //Process process = pb.start();
            //String command = "ping -c 3 " + targetDomain;
            //Process process = Runtime.getRuntime().exec(command);
            Process process = Runtime.getRuntime().exec(args[0]);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.log(Level.SEVERE, line);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "コマンドの実行に失敗しました。管理者に連絡してください。");
        }
    }
}