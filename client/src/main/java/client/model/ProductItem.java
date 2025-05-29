package client.model;

/**
 * Модель товара для продажи
 */
public class ProductItem {
    private final Long id;
    private final String name;
    private final String category;
    private final Double price;
    private final Integer stock;
    
    public ProductItem(Long id, String name, String category, Double price, Integer stock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public Integer getStock() {
        return stock;
    }
    
    @Override
    public String toString() {
        return name + " - " + price + " ₽";
    }
} 