package com.hesky.bookmanager.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Piece of report, that includes all data needed for creating record in report for one message
 */
public class Delta {
    /**
     * Message sequence number
     */
    private Integer number;

    /**
     * Received new or deleted Orders in fix message
     */
    private List<Entry> inputOrders = new ArrayList<>();
    /**
     * Snapshot of Order's book (after adding and removing all input orders), that should be displayed
     */
    private Book book;
    /**
     * List of changes occurred in book in comparison to book in previous Delta
     */
    private List<Entry> bookChanges = new ArrayList<>();
    /**
     * Time of receiving market data request 35=v
     */
    private LocalDateTime startDateTime;
    /**
     * Time of sending response (market data incremental update 35=x) to current market data request
     */
    private LocalDateTime endDateTime;

    /**
     * Indicating appearing of new market data request 35=v, that forces to reset current book
     */
    private boolean isBookRefresh;

    public Delta() {
    }

    public Delta(List<Entry> inputOrders, Book book, List<Entry> bookChanges, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        this.inputOrders = inputOrders;
        this.book = book;
        this.bookChanges = bookChanges;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public boolean isBookRefresh() {
        return isBookRefresh;
    }

    public void setBookRefresh(boolean bookRefresh) {
        isBookRefresh = bookRefresh;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    /**
     * @return list of all bids sorted by price desc
     */
    public List<Order> getBids() {
        return book.getBids().values().stream().sorted(Comparator.comparing(Order::getPrice)).collect(Collectors.toList());
    }

    /**
     * @return list of all offers sorted by price asc
     */
    public List<Order> getOffers() {
        return book.getOffers().values().stream().sorted(Comparator.comparing(Order::getPrice).reversed()).collect(Collectors.toList());
    }

    public int getDifference() {
        return (int) Duration.between(startDateTime, endDateTime).toMillis();
    }

    public List<Entry> getInputOrders() {
        return inputOrders;
    }

    public void setInputOrders(List<Entry> inputOrders) {
        this.inputOrders = inputOrders;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public List<Entry> getBookChanges() {
        return bookChanges;
    }

    public void setBookChanges(List<Entry> bookChanges) {
        this.bookChanges = bookChanges;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    /**
     * Represents order with action( NEW ,DELETE ,UPDATE) to show in report
     */
    public static class Entry {
        private Order order;
        private Action action;

        public Entry(Long id, Double price, Long size, Action action) {
            this.order = new Order(id, price, size);
            this.action = action;
        }

        public Entry() {
        }

        public Entry(Order order, Action action) {
            this.order = order;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry entry = (Entry) o;
            return Objects.equals(order, entry.order) &&
                    getAction() == entry.getAction();
        }

        @Override
        public int hashCode() {

            return Objects.hash(order, getAction());
        }

        public Long getId() {return order.getId();}

        public void setId(Long id) {order.setId(id);}

        public Double getPrice() {return order.getPrice();}

        public void setPrice(Double price) {order.setPrice(price);}

        public Long getSize() {return order.getSize();}

        public void setSize(Long size) {order.setSize(size);}

        public Side getSide() {return order.getSide();}

        public void setSide(Side side) {order.setSide(side);}

        @Override
        public String toString() {
            return "Entry{" +
                    "order=" + order +
                    ", action=" + action +
                    '}';
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }
    }
}