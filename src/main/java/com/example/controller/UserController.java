package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

@RestController
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    public String viewFile(@RequestParam String fileName) throws Exception {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        String saniFileNeme = (new File(fileName)).getName();
        File file = new File("src/main/resources/static/" + saniFileNeme);
        return new String(Files.readAllBytes(file.toPath()));
    }
}