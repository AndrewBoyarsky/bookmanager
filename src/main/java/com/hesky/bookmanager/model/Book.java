package com.hesky.bookmanager.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents storage of {@link com.hesky.bookmanager.model.Order} market offers and bids
 */
public class Book {
    //trading symbol
    private String symbol;
    //list of bids (key=id of bid)
    private Map<Long, Order> bids = new HashMap<>();
    //list of offers (key=id of offer)
    private Map<Long, Order> offers = new HashMap<>();

    public Book() {
    }

    public Book(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return Objects.equals(getSymbol(), book.getSymbol()) &&
                Objects.equals(getBids(), book.getBids()) &&
                Objects.equals(getOffers(), book.getOffers());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getSymbol(), getBids(), getOffers());
    }

    /**
     * Copy book without deep copying of each bid and each offer (only references)
     * @return new Book with orders that are present in this book
     */
    public Book copy() {
        Book book = new Book(this.getSymbol());
        book.getOffers().putAll(this.getOffers());
        book.getBids().putAll(this.getBids());
        return book;
    }

    /**
     * Returns true if Book contains {@code order}, otherwise return false
     * @param order Order, that should be found
     * @return true if {@code order} was found and false otherwise
     */
    public boolean contains(Order order) {
        if (order.getSide() == Side.BID) {
            return bids.containsKey(order.getId());
        } else if (order.getSide() == Side.ASK) {
            return offers.containsKey(order.getId());
        }
        return false;
    }

    /**
     * Put Collection of bids to map of bids in book
     * @param bids Collection of bids
     */
    public void putBids(Collection<Order> bids) {
        this.bids.putAll(bids.stream().collect(Collectors.toMap(Order::getId, order -> order)));
    }

    /**
     * @return List of all orders (bids+offers)
     */
    public List<Order> getAllOrders() {
        return Stream.concat(bids.values().stream().sorted(Comparator.comparing(Order::getPrice)),offers.values().stream().sorted(Comparator.comparing(Order::getPrice))).collect(Collectors.toList());
    }

    /**
     * @return Map of all orders(bids+offers)
     */
    public Map<Long, Order> getAllMapOrders() {
        Map<Long, Order> result = new LinkedHashMap<>();
        result.putAll(bids);
        result.putAll(offers);
        return result;
    }

    /**
     * @param offers Collection of offers
     */
    public void putOffers(Collection<Order> offers) {
        this.offers.putAll(offers.stream().collect(Collectors.toMap(Order::getId, order -> order)));
    }

    /**
     * Deletes order by id
     * @param orderId Order id
     * @return removed Order or null if not exist
     */
    public Order delete(Long orderId) {
        if (bids.containsKey(orderId)) {
            return bids.remove(orderId);
        } else if (offers.containsKey(orderId)) {
            return offers.remove(orderId);
        }
        return null;
    }

    /**
     * Clears all orders
     */
    public void reset() {
        bids.clear();
        offers.clear();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Map<Long, Order> getBids() {

        return bids;
    }

    public void setBids(Map<Long, Order> bids) {
        this.bids = bids;
    }

    public Map<Long, Order> getOffers() {
        return offers;
    }

    public void setOffers(Map<Long, Order> offers) {
        this.offers = offers;
    }

    /**
     * Creates new book(snapshot) of this book by limiting number of Orders (Bids and Orders)
     * @param depth quantity of levels which should be present in new book
     * @return new Book with limited by {@code depth} number of offers and bids
     */
    public Book getFirstLevels(int depth) {
        Book book = new Book();
        book.setBids(bids.values().stream()
                .sorted(Comparator.comparing(Order::getPrice).reversed())
                .limit(depth)
                .collect(Collectors.toMap(Order::getId, Order::new)));
        book.setOffers(offers.values().stream()
                .sorted(Comparator.comparing(Order::getPrice))
                .limit(depth)
                .collect(Collectors.toMap(Order::getId, Order::new)));
        book.setSymbol(this.getSymbol());
        return book;
    }

    /**
     * Add new order to book
     * @param order Order that should be added to book
     */
    public void add(Order order) {
        if (order.getSide() == Side.BID) {
            bids.put(order.getId(), order);
        } else if (order.getSide() == Side.ASK) {
            offers.put(order.getId(), order);
        } else {
            throw new IllegalArgumentException("Order has no side! " + order);
        }
    }
}
