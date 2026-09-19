package org.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import redis.clients.jedis.Jedis;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Scanner;

public class sitebanner {

    static final String BANNER_KEY = "app:banner";
    static Jedis jedis = new Jedis("localhost", 6379);
    static String DB_URL = "jdbc:postgresql://localhost:5432/bannerDB";
    static String DB_USER = "sumanthgowda"; // fixed: no "postgres" role exists on Homebrew Postgres
    static String DB_PASS = "";             // fixed: trust auth, no password by default

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);

        server.createContext("/banner", exchange -> {
            try {
                String method = exchange.getRequestMethod();

                if (method.equals("GET")) {
                    String cached = jedis.get(BANNER_KEY);
                    String message;
                    if (cached != null) {
                        message = cached;
                    } else {
                        message = fetchFromDb();
                        if (message != null) {
                            jedis.set(BANNER_KEY, message);
                        } else {
                            respond(exchange, 404, "{\"error\": \"no banner set\"}");
                            return;
                        }
                    }
                    respond(exchange, 200, "{\"message\": \"" + message + "\"}");

                } else if (method.equals("POST")) {
                    String body = readBody(exchange);
                    String message = extractMessage(body);
                    boolean saved = saveToDb(message);
                    if (saved) {
                        jedis.set(BANNER_KEY, message);
                        respond(exchange, 200, "{\"success\": true}");
                    } else {
                        respond(exchange, 500, "{\"success\": false, \"error\": \"db write failed\"}");
                    }

                } else if (method.equals("DELETE")) {
                    boolean deleted = deleteFromDb();
                    if (deleted) {
                        jedis.del(BANNER_KEY);
                        respond(exchange, 200, "{\"success\": true}");
                    } else {
                        respond(exchange, 500, "{\"success\": false, \"error\": \"db delete failed\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });

        server.createContext("/banner/exists", exchange -> {
            try {
                boolean exists = jedis.exists(BANNER_KEY);
                respond(exchange, 200, "{\"exists\": " + exists + "}");
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port 3000");
    }

    static String fetchFromDb() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT message FROM banner LIMIT 1")) {
            if (rs.next()) return rs.getString("message");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static boolean saveToDb(String message) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.createStatement().executeUpdate("DELETE FROM banner");
            PreparedStatement ps = conn.prepareStatement("INSERT INTO banner(message) VALUES (?)");
            ps.setString(1, message);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static boolean deleteFromDb() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.createStatement().executeUpdate("DELETE FROM banner");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static String readBody(HttpExchange exchange) {
        Scanner s = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static String extractMessage(String json) {
        // crude: {"message": "hello"} -> hello
        int start = json.indexOf(":") + 1;
        return json.substring(start).replaceAll("[{}\"]", "").trim();
    }

    static void respond(HttpExchange exchange, int status, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}