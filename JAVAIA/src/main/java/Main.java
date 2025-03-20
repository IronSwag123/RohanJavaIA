
// Main class that sets up the HTTP server and handles API endpoints
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws Exception {
        // Set up server on port 8000
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        
        // Handler for serving the main HTML page
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String html = readResourceFile("main.html");
                
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, html.getBytes().length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(html.getBytes());
                }
            }
        });
        
        // Handler for recipe API endpoints - GET all recipes and POST new recipe
        server.createContext("/api/recipes", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("GET".equals(exchange.getRequestMethod())) {
                    // Return all recipes as JSON
                    String responseJson = RecipeManager.getRecipesJson();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseJson.getBytes().length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseJson.getBytes());
                    }
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    // Read and parse the request body
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder requestBody = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        requestBody.append(line);
                    }
                    
                    // Parse form data into a map
                    Map<String, String> formData = parseFormData(requestBody.toString());
                    String name = formData.get("name");
                    String ingredients = formData.get("ingredients");
                    String instructions = formData.get("instructions");
                    
                    // Add the new recipe
                    String id = RecipeManager.addRecipe(name, ingredients, instructions);
                    
                    // Send success response
                    String response = "{\"success\":true,\"id\":\"" + id + "\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    // Handle invalid HTTP methods
                    String response = "{\"error\":\"Method not allowed\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            }
        });
        
        // Handler for individual recipe operations - GET and DELETE by ID
        server.createContext("/api/recipes/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                String id = path.substring("/api/recipes/".length());
                
                if ("GET".equals(exchange.getRequestMethod())) {
                    // Get single recipe by ID
                    Recipe recipe = RecipeManager.getRecipe(id);
                    String response;
                    int statusCode;
                    
                    if (recipe != null) {
                        response = recipe.toJson();
                        statusCode = 200;
                    } else {
                        response = "{\"error\":\"Recipe not found\"}";
                        statusCode = 404;
                    }
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(statusCode, response.getBytes().length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else if ("DELETE".equals(exchange.getRequestMethod())) {
                    // Delete recipe by ID
                    boolean deleted = RecipeManager.deleteRecipe(id);
                    String response;
                    int statusCode;
                    
                    if (deleted) {
                        response = "{\"success\":true}";
                        statusCode = 200;
                    } else {
                        response = "{\"error\":\"Recipe not found\"}";
                        statusCode = 404;
                    }
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(statusCode, response.getBytes().length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } else {
                    // Handle invalid HTTP methods
                    String response = "{\"error\":\"Method not allowed\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            }
        });
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Server is running on http://0.0.0.0:" + port);
        System.out.println("Use the Replit URL to access the website");
    }
    
    // Helper method to read HTML file content
    private static String readResourceFile(String fileName) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("main.html");
            return new String(java.nio.file.Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<h1>Error: Could not load HTML file</h1><p>" + e.getMessage() + "</p>";
        }
    }
    
    // Helper method to parse form data from request body
    private static Map<String, String> parseFormData(String formData) {
        Map<String, String> result = new HashMap<>();
        
        if (formData.startsWith("{")) {
            // Parse JSON format
            try {
                formData = formData.substring(1, formData.length() - 1);
                String[] pairs = formData.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim().replace("\"", "");
                        result.put(key, value);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing JSON: " + e.getMessage());
            }
        } else {
            // Parse URL encoded format
            try {
                String[] pairs = formData.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1).replace("+", " ");
                        value = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                        result.put(key, value);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing form data: " + e.getMessage());
            }
        }
        
        return result;
    }
}
