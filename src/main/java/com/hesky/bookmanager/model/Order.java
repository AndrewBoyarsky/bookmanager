package com.hesky.bookmanager.model;

import java.util.Objects;

/**
 * Represents market offer to buy or sell
 */
public class Order {
    private Long id;
    private Double price;
    private Long size;
    private Side side;

    public Order() {
    }

    public Order(Order order) {
        this(order.getId(), order.getPrice(), order.getSize());
        this.side = order.getSide();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", price=" + price +
                ", size=" + size +
                ", side=" + side +
                '}';
    }

    public Order(Long id, Side side, Double price, Long size) {
        this.id = id;
        this.price = price;
        this.size = size;
        this.side = side;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return Objects.equals(getId(), order.getId()) &&
                Objects.equals(getPrice(), order.getPrice()) &&
                Objects.equals(getSize(), order.getSize()) &&
                getSide() == order.getSide();
    }

    @Override
    public int hashCode() {

        return Objects.hash(getId(), getPrice(), getSize(), getSide());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public Order(Long id, Double price, Long size) {
        this.id = id;
        this.price = price;
        this.size = size;
    }
}
