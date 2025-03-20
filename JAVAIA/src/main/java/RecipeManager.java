The method handle(HttpExchange) of type new HttpHandler(){} must override a superclass method// Handler for serving the main HTML page
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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

// Manager class to handle recipe operations
public class RecipeManager {
    // Store recipes in memory using a HashMap
    private static Map<String, Recipe> recipes = new HashMap<>();
    private static int nextId = 1;

    // Add a new recipe and return its ID
    public static String addRecipe(String name, String ingredients, String instructions) {
        String id = String.valueOf(nextId++);
        Recipe recipe = new Recipe(id, name, ingredients, instructions);
        recipes.put(id, recipe);
        return id;
    }

    // Get a recipe by its ID
    public static Recipe getRecipe(String id) {
        return recipes.get(id);
    }

    // Delete a recipe by its ID
    public static boolean deleteRecipe(String id) {
        if (recipes.containsKey(id)) {
            recipes.remove(id);
            return true;
        }
        return false;
    }

    // Convert all recipes to JSON format
    public static String getRecipesJson() {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Recipe recipe : recipes.values()) {
            if (!first) {
                json.append(",");
            }
            json.append(recipe.toJson());
            first = false;
        }

        json.append("]");
        return json.toString();
    }
}
