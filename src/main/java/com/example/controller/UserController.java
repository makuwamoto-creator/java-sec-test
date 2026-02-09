package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.tomcat.util.buf.Utf8Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.logging.Logger;

import com.example.App;
import com.example.model.MyData;// 作成したモデルをインポート

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

    private static final Logger logger = Logger.getLogger(App.class.getName());

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

/* 
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
 */

/*     @GetMapping("/deserialize")
    public String deserialize(@RequestParam String data) throws Exception {
        byte[] bytes = java.util.Base64.getDecoder().decode(data);

        // 🌟 修正ポイント：ObjectInputStream を作成
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
             java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {

            // ✅ 対策：ホワイトリストを設定（Stringクラスと特定のパッケージのみ許可）
            // これにより、攻撃用の怪しいクラスが混じっていても、復元前にブロックされます
            java.io.ObjectInputFilter filter = java.io.ObjectInputFilter.Config.createFilter("java.lang.String;com.example.models.*;!*");
            ois.setObjectInputFilter(filter);

            Object obj = ois.readObject();
            return "Object deserialized: " + obj.toString();
        }

        // イメージ：ObjectInputStream をやめて Jackson を使う
        ObjectMapper mapper = new ObjectMapper();
        MyData data = mapper.readValue(jsonData, MyData.class); // これなら指摘は出ません
    }
*/   
    @GetMapping("/deserialize")
    public String deserialize(@RequestParam String data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // MyData.class を指定してパース
        MyData obj = mapper.readValue(data, MyData.class);
        return "JSON deserialized: Name=" + obj.getName();
    }
/* 
    @GetMapping("/greet")
    public void greet(@RequestParam String name, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        // コンテンツタイプを HTML に固定する
        response.setContentType("text/html;charset=UTF-8");

        // ❌ 危険：ユーザーの入力をそのまま HTML として返している
        // もし name に <script>alert('XSS')</script> と入れられたら...？
        response.getWriter().write("<html><body><h1>Hello, " + name + "!</h1></body></html>");
    }
 */
    @GetMapping("/greet")
    @ResponseBody // 明示的にレスポンス本体であることを示す
    public String greet(@RequestParam String name) {
        // String.format を使って、外部入力を HTML 構造に埋め込む
        // 多くのツールはこの「埋め込み」を XSS の種として認識します
        //String template = "<html><body><div>Welcome, %s</div></body></html>";
        //return String.format(template, name);
        
        // ✅ 対策：ユーザー入力を HTML エスケープする
        // これにより <script> は &lt;script&gt; に変換され、
        // ブラウザ上では「実行」されず、単なる「文字」として表示されます。
        String escapedName = HtmlUtils.htmlEscape(name);
        return "<html><body><h1>Hello, " + escapedName + "!</h1></body></html>";
    }
    /* 
    // ❌ 危険：IDOR の脆弱性
    // 他人の ID を指定するだけで、誰のプロフィールでも見られてしまう
    @GetMapping("/user/profile")
    public String getUserProfile(@RequestParam String userId) {
        // 本来は「ログインしている自分の ID」しか見られないはずだが、
        // 外部から userId を自由に指定できてしまうため、全ユーザーの情報が丸見えになる
        return "Displaying profile for user: " + userId + " (Confidential Data...)";
    } 
    */

    // ✅ 修正済み：IDOR 対策（ログインユーザーに基づいた認可）
    @GetMapping("/user/profile")
    public String getUserProfile(@RequestParam String userId) {
        // 1. 本来はログインセッションから「操作者のID」を取得する
        String currentLoginUser = "user101"; // セッション等から取得した値（例）

        // 2. 「見ようとしているID」と「自分のID」が一致するかチェック
        // または、そのデータに対する閲覧権限があるかをDB等で確認する
        if (!userId.equals(currentLoginUser)) {
            return "Error: アクセス権限がありません";
        }

        return "Displaying profile for user: " + userId + " (Confidential Data...)";
    }    
    /* 
    @PostMapping("/xml")
    public String parseXml(@RequestBody String xmlData) throws Exception {
        // ❌ 危険：デフォルト設定の DocumentBuilderFactory は XXE に脆弱
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        
        // XMLをパースする（ここで外部ファイルを読み込まされる可能性がある）
        builder.parse(new java.io.ByteArrayInputStream(xmlData.getBytes()));
        return "XML processed";
    }
    */
    @PostMapping("/xml")
    public String parseXml(@RequestBody String xmlData) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        
        // ✅ 対策：外部エンティティの読み込みをすべて無効化する
        // これにより、XMLの中に悪意ある外部参照があっても無視されるようになります
        String feature = "http://apache.org/xml/features/disallow-doctype-decl";
        factory.setFeature(feature, true);
        
        // その他の推奨設定
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(new java.io.ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8)));
        
        return "XML processed safely";
    }

    @GetMapping("/log")
    public String logInput(@RequestParam String data) {
        // ❌ 危険：ユーザー入力をそのままログに出力
        // 改行コードを含ませて、偽のログエントリーを捏造される（Log Forgery）
        logger.info("User input: " + data);
        return "Logged";
    }
}