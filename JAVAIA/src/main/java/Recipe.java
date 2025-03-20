
// Recipe class to store individual recipe information
public class Recipe {
    // Recipe properties
    private String id;
    private String name;
    private String ingredients;
    private String instructions;
    
    // Constructor to initialize a new recipe
    public Recipe(String id, String name, String ingredients, String instructions) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients;
        this.instructions = instructions;
    }
    
    // Getter methods
    public String getId() { return id; }
    public String getName() { return name; }
    public String getIngredients() { return ingredients; }
    public String getInstructions() { return instructions; }
    
    // Convert recipe to JSON format
    public String toJson() {
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"ingredients\":\"%s\",\"instructions\":\"%s\"}", 
            id, name, ingredients.replace("\"", "\\\""), instructions.replace("\"", "\\\""));
    }
}
