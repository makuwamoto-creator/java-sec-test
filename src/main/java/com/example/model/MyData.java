package com.example.model;

// 1. データの受け皿となるクラス（インナークラスでOK）
public class MyData {
    private String name;
    private String message;

    // Getter/Setterやデフォルトコンストラクタが必要ですが、
    // Jacksonは public フィールドもそのまま扱えます
    // Getter と Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

}