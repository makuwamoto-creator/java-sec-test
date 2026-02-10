package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// ...

@RestController
@Validated
public class LogController {
    private static final Logger log4j = LogManager.getLogger("Log4ShellDemo");

    @GetMapping("/log4shell")
    public String log4shell(@RequestParam String userInput) {
        // ❌ 致命的な脆弱性
        // Log4j 2.14.1 以前では、ログの中に ${jndi:ldap://...} という文字列があると、
        // 外部サーバーへ接続してプログラムを実行しようとしてしまいます。
        log4j.error("User said: " + userInput); 
        
        return "Logged with Log4j 2.14.1";
    }
}