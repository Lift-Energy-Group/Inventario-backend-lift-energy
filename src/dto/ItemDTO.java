package dto;

public class ItemDTO {
    private Long id;
    private String name;
    private String category;
    private Integer quantity;

    public ItemDTO(Long id, String name, String category, Integer quantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Integer getQuantity() { return quantity; }
}