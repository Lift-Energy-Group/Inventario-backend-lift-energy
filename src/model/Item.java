package model;

public class Item {
    private Long id;
    private String name;
    private String category;
    private Integer quantity;

    // Constructor para fabricar un material nuevo
    public Item(Long id, String name, String category, Integer quantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
    }

    // Getters y Setters (Necesarios para que el sistema lea y modifique los datos)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}