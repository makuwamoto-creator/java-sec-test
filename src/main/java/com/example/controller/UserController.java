package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.Optional;

@RestController
@Validated
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. 許可するファイル名のリスト（ホワイトリスト）を定義
    private static final Set<String> ALLOWED_FILES = Set.of(
        "readme.txt",
        "manual.pdf",
        "logo.png"
    );

    // 🚨 脆弱性 1: SQL Injection
    // ユーザー入力をそのまま SQL クエリに結合している
    @GetMapping("/users/search")
    public List searchUsers(@RequestParam String name) {
        String sql = "SELECT * FROM users WHERE name = ? ";
        
        return jdbcTemplate.queryForList(sql, name);
    }

    // 🚨 脆弱性 2: Reflected Cross-Site Scripting (XSS)
    // ユーザー入力をサニタイズせずにそのままレスポンス（HTML/Text）として返している
    @GetMapping("/hello")
    public String sayHello(@RequestParam String name) {
        return "<h1>Hello, " + name + "!</h1>";
    }

    // 🚨 脆弱性 3: Path Traversal
    // 外部からの入力を使ってサーバー上のファイルを直接読み込んでいる
    @GetMapping("/view-file")
    public String viewFile(
        @RequestParam 
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "不正なファイル形式です") String fileName
    ) throws Exception {
        // 1. リストの中から一致するものを探す（ここで外部入力との直接の繋がりを断つ）
        Optional<String> safeFileName = ALLOWED_FILES.stream()
            .filter(f -> f.equals(fileName))
            .findFirst();

        // 2. リストに含まれているかチェック
        if (safeFileName.isEmpty()) {
            throw new IllegalArgumentException("アクセスが許可されていないファイルです。");
        }

        String finalSafeName = new String(safeFileName.get().toCharArray());

        File file = new File("src/main/resources/static/" + finalSafeName);
        return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
    }

    // ❌ 危険：OS Command Injection の脆弱性があるコード
    // 入力したIPアドレスにpingを打つ機能（のつもり）
    @GetMapping("/ping")
    public String ping(@RequestParam String ip) throws Exception {
        
        // 1. 入力バリデーション（IPアドレスとして妥当な文字以外は即拒否）
        // 数字とドット以外が含まれていたらエラーにする
        if (ip == null || !ip.matches("^[0-9.]+$")) {
            throw new IllegalArgumentException("無効なIPアドレス形式です");
        }

        // これにより、たとえ ";" が含まれていても、OSはそれを一つの引数（文字列）として扱います
        String[] command = {"ping", "-c", "1", ip};
        // ユーザーの入力をそのままコマンドとして実行してしまう
        ProcessBuilder ps = new ProcessBuilder(command);
        
        Process process = ps.start();

        // 実行結果を読み取って返す
        return new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        /*
        // Java標準機能でPingに相当する処理をする例
        boolean reachable = java.net.InetAddress.getByName(ip).isReachable(3000);
        return "Reachable: " + reachable; 
        */

    }


    @GetMapping("/deserialize")
    public String deserialize(@RequestParam String data) throws Exception {
        // 1. Base64デコード
        byte[] bytes = java.util.Base64.getDecoder().decode(data);

        // 2. 危険なデシリアライゼーション
        // ObjectInputStream は、中身が何かを確認せずに復元（インスタンス化）しようとします
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes));
        
        // ここで攻撃者が用意した特殊なオブジェクトが読み込まれると、
        // readObject() が呼ばれた瞬間に任意のコードが実行されます
        Object obj = ois.readObject();
        ois.close();

        return "Object deserialized: " + obj.toString();
    }
    
}