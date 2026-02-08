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
        //String saniFileNeme = (new File(fileName)).getName();

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
}