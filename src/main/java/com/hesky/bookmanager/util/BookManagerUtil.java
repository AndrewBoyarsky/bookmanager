package com.hesky.bookmanager.util;

import com.hesky.bookmanager.model.*;
import org.slf4j.Logger;
import quickfix.FieldNotFound;
import quickfix.Group;

import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Util methods for BookManager
 */
public class BookManagerUtil {
    private static final Logger LOG = getLogger(BookManagerUtil.class);

    /**
     * Returns unique Orders from book1 and book2. Result contains all book1 and book2 orders excluding otders that are present in both books.
     * <h1>
     * Example:
     * </h1>
     * <div><b>book1:</b> Order(id = 0), Order(id = 1)</div>
     * <div><b>book2:</b> Order(id = 2), Order(id = 1)</div>
     * <div><b>result:  </b> Order(id = 0),  Order(id = 2)</div>
     *
     * @param book1 first Book of orders
     * @param book2 second Book of orders
     * @return list of Orders which are not present in book1 or book2
     */
    public static List<Order> getUniqueOrders(Book book1, Book book2) {
        List<Order> orders = new ArrayList<>();
        orders.addAll(getUniqueOrdersForBook(book1, book2));
        orders.addAll(getUniqueOrdersForBook(book2, book1));
        return orders;
    }

    /**
     * Returns unique orders from book1 that are not present in book2.
     * <h1>Example:
     * </h1>
     * <div><b>book1:</b> Order(id = 0), Order(id = 1)</div>
     * <div><b>book2:</b> Order(id = 2), Order(id = 1)</div>
     * <div><b>result:  </b> Order(id = 0)</div>
     *
     * @param book1 first book of orders
     * @param book2 second book of orders
     * @return list of Orders which are present in book1 and not in book2
     */
    public static List<Order> getUniqueOrdersForBook(Book book1, Book book2) {
        return book1.getAllOrders().stream().filter(o -> !book2.getAllMapOrders().containsKey(o.getId())).collect(Collectors.toList());
    }

    /**
     * Returns true or false depending on that: was price found in the list of orders or not
     *
     * @param orders - list of orders
     * @param price  - price, that should be found
     * @return true if price was found OR false if price was not found
     */
    public static boolean containsPrice(List<Order> orders, Double price) {
        return orders.stream().map(Order::getPrice).anyMatch(p -> p.equals(price));
    }

    /**
     * Returns order with such price as {@code updateCandidate} contains; therefore returned order is replacing received {@code updateCandidate} in currentSnapshot
     *
     * @param updateCandidate       order, which was removed from Book and may be an update candidate
     * @param currentSnapshotOrders map of all orders which are present in currentSnapshot
     * @param addedOrders           list of all orders, which were added to currentSnapshot
     * @return Order, which replaced {@code updateCandidate} having similar price and is present in addedOrders; OR return NULL if Order with such price was not updated, but simply removed or in other cases
     */
    private static Order tryUpdate(Order updateCandidate, Map<Long, Order> currentSnapshotOrders, Collection<Order> addedOrders) {
        Set<Double> prices = currentSnapshotOrders.values().stream().map(Order::getPrice).collect(Collectors.toSet());
        if (prices.contains(updateCandidate.getPrice())) {
            Order o = currentSnapshotOrders.get(updateCandidate.getId());
            if (!updateCandidate.equals(o)) {
                return addedOrders.stream().filter(or -> or.getPrice().equals(updateCandidate.getPrice())).findFirst().orElse(null);
            }
        }
        return null;
    }

    /**
     * Calculates Book changes occurred in prevBook by adding addedOrders and deleting removedOrders
     *
     * @param removedOrders Orders which were removed from prevBook
     * @param addedOrders   Orders which were added to prevBook
     * @param prevBook      Book of orders before modifying (adding/deleting orders)
     * @param currentBook   Book of orders after modifying (adding/deleting orders)
     * @param depth         number of Book levels which would be used for monitoring changes
     * @return list of prevBook changes
     */
    public static List<Delta.Entry> getBookChanges(Collection<Order> removedOrders, Collection<Order> addedOrders, Book prevBook, Book currentBook, int depth) {
        List<Order> deletedOrders = new ArrayList<>();
        List<Order> updatedOrders = new ArrayList<>();
        List<Order> newOrders = new ArrayList<>();
        Book currentSnapshot = currentBook.getFirstLevels(depth);
        Book prevSnapshot = prevBook.getFirstLevels(depth);
        //find all removed or updated orders which are present in removedOrders
        removedOrders.forEach(removed -> {
            Order matchedOrder = prevSnapshot.getAllOrders().stream()
                .filter(removed::equals)
                .findFirst()
                .orElse(null);
            if (matchedOrder != null) {
                Order updated = tryUpdate(matchedOrder, currentSnapshot.getAllMapOrders(), addedOrders);
                if (updated != null) {
                    updatedOrders.add(updated);
                    addedOrders.remove(updated);
                } else {
                    deletedOrders.add(matchedOrder);
                }
            }
        });
        //find all new orders among added orders
        addedOrders.forEach(order -> {
            Order matchedBid = currentSnapshot.getAllOrders().stream()
                .filter(order::equals)
                .findFirst()
                .orElse(null);
            if (matchedBid != null && isNew(matchedBid, prevSnapshot.getAllMapOrders())) {
                newOrders.add(matchedBid);
            }
        });

        newOrders.addAll(findNew(prevSnapshot, currentSnapshot, prevBook));

        deletedOrders.addAll(findDeleted(prevSnapshot, currentSnapshot, updatedOrders, deletedOrders));

        //making result
        List<Delta.Entry> result = new ArrayList<>();
        result.addAll(deletedOrders.stream().map(o -> new Delta.Entry(o, Action.DELETE)).collect(Collectors.toList()));
        result.addAll(updatedOrders.stream().map(o -> new Delta.Entry(o, Action.UPDATE)).collect(Collectors.toList()));
        result.addAll(newOrders.stream().map(o -> new Delta.Entry(o, Action.NEW)).collect(Collectors.toList()));

        return result;
    }

    /**
     * Returns all deleted orders which were not deleted by current message (not present in removedOrders), but disappeared from current snapshot by adding more profitable orders in current message
     *
     * @param prevSnapshot    Book with limited number(depth) of levels before modifying
     * @param currentSnapshot Book with limited number (depth) of levels after modifying
     * @param updatedOrders   List of orders, which were updated in currentSnapshot
     * @param deletedOrders   List of orders, which were deleted in currentSnapshot
     * @return List of Orders, which were disappeared in currentSnapshot, but were present in prevSnapshot
     */
    private static List<Order> findDeleted(Book prevSnapshot, Book currentSnapshot, List<Order> updatedOrders, List<Order> deletedOrders) {
        List<Order> uniqueOrdersForBook = getUniqueOrdersForBook(prevSnapshot, currentSnapshot);
        return uniqueOrdersForBook.stream().filter(order -> !deletedOrders.contains(order) && !containsPrice(updatedOrders, order.getPrice())).collect(Collectors.toList());
    }

    /**
     * @param order  - just order (bid or offer)
     * @param orders - list of orders
     * @return true if map of orders does not contain order and false if contains
     */
    public static boolean isNew(Order order, Map<Long, Order> orders) {
        return !orders.containsKey(order.getId());
    }

    /**
     * Return all new orders which were not added by current message (not present in addedOrders), but appeared in current snapshot from previous book's orders because were removed orders which were in prevSnapshot so that less profitable orders took their places in new snapshot
     *
     * @param prevSnapshot    Book with first N (depth) levels before modifying
     * @param currentSnapshot Book with first N (depth) levels after modifying
     * @param prevBook        Book with all orders (all levels) before modifying
     * @return list of new Orders which were appeared from prevBook and are present in currentSnapshot
     */
    private static List<Order> findNew(Book prevSnapshot, Book currentSnapshot, Book prevBook) {
        List<Order> uniqueOrders = getUniqueOrders(prevSnapshot, currentSnapshot);
        return currentSnapshot.getAllOrders().stream().filter(offer -> prevBook.contains(offer) && uniqueOrders.contains(offer)).collect(Collectors.toList());

    }

    /**
     * Check if delete orders exist in {@code groups}
     *
     * @param groups Order groups grom FIX message
     * @return true if Delete orders exist in groups OR false otherwise
     */
    public static boolean isDeleteExist(List<Group> groups) {
        return groups.stream().anyMatch(group -> {
            try {
                return group.getInt(279) == 2;
            }
            catch (FieldNotFound fieldNotFound) {
                LOG.error("Cannot find field 279", fieldNotFound);
                return false;
            }
        });
    }

    /**
     * @param group FIX message's group with order
     * @return new Order, that was found in {@code group}
     * @throws FieldNotFound if fields of order are not present in group
     */
    public static Order readOrder(Group group) throws FieldNotFound {
        Order order = new Order();
        int side = group.getInt(269);
        order.setSide(Side.values()[side]);
        order.setId(group.getDecimal(278).longValue());
        order.setPrice(group.getDouble(270));
        order.setSize(group.getDecimal(271).longValue());
        return order;
    }
}
