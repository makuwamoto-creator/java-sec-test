package com.example; // これを追加（名前は何でも良いですが、一般的になぞります）

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger; // 1. Loggerをインポート

import javax.crypto.NoSuchPaddingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());



    public static void main(String[] args) {
        
        Scanner scanner = new Scanner(System.in, "UTF_8");
        System.out.print("Enter your name: ");
        
        // 🚨 SonarQube が「汚染源（Taint Source）」として認識しやすい入力方法
        String input = "User logged in:" + scanner.nextLine(); 
        // 🚨 汚染されたデータをそのままログに流す
        logger.info(input.replaceAll("[\r\n]", "_")); 
        
        scanner.close();

        String hardcodedPassword = "password12345";
        logger.info(hardcodedPassword);

        // 🚨 2. 暗号化の問題 (S2257 / CWE-327)
        // インスタンスを作るだけで検知される強力なルール
        try {
            javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            logger.info("exception");
            // e.printStackTrace(); // これは Code Smell なので書かない（笑）
        }

        // 🚨 3. ログ・インジェクション (CWE-117)
        if (args.length > 0) {
            String logtext = "User input: " + args[0]; 
            logger.info(logtext.replaceAll("[\r\n]", ""));
        }
        
        // 本来は引数などで受け取るユーザー入力（例: "google.com"）
        // 攻撃者が "google.com; cat /etc/passwd" と入力すると大変なことに！
        // String targetDomain = args.length > 0 ? args[0] : "localhost";

        try {
            // ❌ 脆弱なポイント：ユーザー入力をそのままシェルコマンドに渡している
            //ProcessBuilder pb = new ProcessBuilder("/usr/bin/ping", "-c", "3", targetDomain);
            //Process process = pb.start();

            //Process process = Runtime.getRuntime().exec(command);
            if (args.length == 0) return;
            //String safeCommand = args[0].replaceAll("[\r\n]", "");
            String command = "ping -c 3 " + args[0];

            Process process = Runtime.getRuntime().exec(command.replaceAll("[\r\n]", "").replaceAll("([&|;><`!\\\\'\"\\{\\}\\[\\]\\(\\)\\^~])", "\\\\$1"));
            //ProcessBuilder pb = new ProcessBuilder("/usr/bin/ping", "-c", "3", safeCommand.replaceAll("([&|;><`!\\\\'\"\\{\\}\\[\\]\\(\\)\\^~])", "\\\\$1"));
            //Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF_8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String cleanLine = line.replace('\n', '_').replace('\r', '_');
                logger.log(Level.SEVERE, cleanLine);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "コマンドの実行に失敗しました。管理者に連絡してください。");
        }
    }
}