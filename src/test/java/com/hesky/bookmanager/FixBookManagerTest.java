package com.hesky.bookmanager;

import com.hesky.bookmanager.controller.InputData;
import com.hesky.bookmanager.model.Action;
import com.hesky.bookmanager.model.Book;
import com.hesky.bookmanager.model.Delta;
import com.hesky.bookmanager.model.Order;
import com.hesky.bookmanager.util.BookManagerUtil;
import com.hesky.bookmanager.util.FixParser;
import org.junit.Assert;
import org.junit.Test;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.StringField;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hesky.bookmanager.model.Side.ASK;
import static com.hesky.bookmanager.model.Side.BID;
import static com.hesky.bookmanager.util.BookManagerUtil.isNew;

public class FixBookManagerTest {
    private InputData data = new InputData(new File(Objects.requireNonNull(getClass().getClassLoader().getResource("data.summary")).getFile()).toPath(), Paths.get("report.html"), "EUR/USD", 2, LocalDateTime.of(2015, 3, 3, 0, 0), LocalDateTime.of(2015, 3, 3, 23, 59));
    private BookManager bookManager = new BookManager(data);

    @Test
    public void testParse() {
        String message = "8=FIX.4.4\u00019=107\u000135=A\u000134=1\u000149=1001083\u000152=20150303-22:10:02.873\u000156=77MARKETS\u000157=PRICE\u000198=0\u0001108=30\u0001141=Y\u0001553=1001083\u0001554=1234\u000110=047\u0001";
        Message actual = FixParser.parse(message);
        Assert.assertEquals(message, actual.toString());
    }

    @Test
    public void testReadFile() throws Exception {
        List<Message> messages = bookManager.parseLogs();
        Assert.assertEquals(messages.stream().filter(m -> {
            try {
                return m.getHeader().getField(new StringField(35)).getValue().equalsIgnoreCase("v");
            }
            catch (FieldNotFound fieldNotFound) {
                fieldNotFound.printStackTrace();
            }
            return false;
        }).count(), 6);
    }

    @Test
    public void testManageBook() throws Exception {
        List<Message> messages = bookManager.parseLogs();
        bookManager.manageBook(messages);
    }

    @Test
    public void testMakeReport() throws Exception {
        List<Delta> deltas = bookManager.manageBook(bookManager.parseLogs());
        bookManager.createAndWriteReport(deltas);
    }

    @Test
    public void testGetUniqueOrders() {
        Order bid1 = new Order(1L, BID, 20d, 10L);
        Order bid2 = new Order(2L, BID, 21d, 12L);
        Order ask1 = new Order(3L, ASK, 22d, 5L);
        Order ask2 = new Order(4L, ASK, 23d, 10L);
        Order ask3 = new Order(5L, ASK, 24d, 20L);
        Book book = new Book("EUR/USD");
        book.getBids().put(bid1.getId(), bid1);
        book.getBids().put(bid2.getId(), bid2);
        book.getOffers().put(ask1.getId(), ask1);
        book.getOffers().put(ask2.getId(), ask2);
        Book book2 = new Book("EUR/USD");
        book2.getBids().put(bid1.getId(), bid1);
        book2.getBids().put(bid2.getId(), bid2);
        book2.getOffers().put(ask1.getId(), ask1);
        book2.getOffers().put(ask3.getId(), ask3);
        List<Order> uniqueOrders = BookManagerUtil.getUniqueOrders(book, book2);
        Assert.assertEquals(2, uniqueOrders.size());
        Assert.assertEquals(Arrays.asList(ask2, ask3), uniqueOrders);
    }

    @Test
    public void testBookChanges() {
        //start from 268
        List<Order> bids = new ArrayList<>();
        bids.addAll(
                Arrays.asList(
                        new Order(8390570142L, BID, 1.11821, 1000000L),
                        new Order(8390592531L, BID, 1.11819, 6250000L),
                        new Order(8390592532L, BID, 1.11818, 1000000L),
                        new Order(8390592533L, BID, 1.11817, 2500000L)
                )
        );
        List<Order> offers = new ArrayList<>(Arrays.asList(
                new Order(8390573953L, ASK, 1.11823, 1000000L),
                new Order(8390592542L, ASK, 1.11824, 500000L),
                new Order(8390592543L, ASK, 1.11826, 1000000L),
                new Order(8390592544L, ASK, 1.11827, 1750000L)));
        Book currentBook = new Book("EUR/USD");
        Book prevBook = currentBook.copy();
        currentBook.putBids(bids);
        currentBook.putOffers(offers);
        List<Delta.Entry> entries = BookManagerUtil.getBookChanges(Collections.emptyList(), Stream.concat(bids.stream(), offers.stream()).collect(Collectors.toList()), prevBook, currentBook, 2);
        List<Delta.Entry> expected = new ArrayList<>(Arrays.asList(
                new Delta.Entry(bids.get(0), Action.NEW),
                new Delta.Entry(bids.get(1), Action.NEW),
                new Delta.Entry(offers.get(0), Action.NEW),
                new Delta.Entry(offers.get(1), Action.NEW)
        ));
        Assert.assertEquals(expected, entries);

        offers.clear();
        bids.clear();
        bids.addAll(
                Arrays.asList(
                        new Order(8390598790L, BID, 1.11819, 6750000L),
                        new Order(8390598791L, BID, 1.11818, 500000L)
                )
        );
        offers.addAll(
                Arrays.asList(
                        new Order(8390598803L, ASK, 1.11827, 1250000L),
                        new Order(8390598804L, ASK, 1.11828, 4500000L)
                )
        );
        prevBook = currentBook.copy();
        currentBook.putBids(bids);
        currentBook.putOffers(offers);
        List<Order> removedOrders = Arrays.asList(
                currentBook.getBids().remove(8390592531L),
                currentBook.getBids().remove(8390592532L),
                currentBook.getOffers().remove(8390592544L)
        );
        entries = BookManagerUtil.getBookChanges(removedOrders, Stream.concat(bids.stream(), offers.stream()).collect(Collectors.toList()), prevBook, currentBook, 2);
        expected = Collections.singletonList(
                new Delta.Entry(bids.get(0), Action.UPDATE)
        );
        Assert.assertEquals(expected, entries);


        offers.clear();
        bids.clear();
        bids.addAll(
                Arrays.asList(
                        new Order(8390599555L, BID, 1.1182, 500000L),
                        new Order(8390599556L, BID, 1.11819, 6250000L),
                        new Order(8390599557L, BID, 1.11818, 2500000L),
                        new Order(8390599558L, BID, 1.11817, 3500000L)
                )
        );
        offers.addAll(
                Collections.singletonList(
                        new Order(8390599567L, ASK, 1.11823, 400000L)
                )
        );
        prevBook = currentBook.copy();

        removedOrders = Arrays.asList(
                currentBook.getBids().remove(8390598790L),
                currentBook.getBids().remove(8390598791L),
                currentBook.getBids().remove(8390592533L),
                currentBook.getOffers().remove(8390573953L)
        );
        currentBook.putBids(bids);
        currentBook.putOffers(offers);
        entries = BookManagerUtil.getBookChanges(removedOrders, Stream.concat(bids.stream(), offers.stream()).collect(Collectors.toList()), prevBook, currentBook, 2);
        expected = Arrays.asList(
                new Delta.Entry(removedOrders.get(0), Action.DELETE),
                new Delta.Entry(offers.get(0), Action.UPDATE),
                new Delta.Entry(bids.get(0), Action.NEW)
        );
        Assert.assertEquals(expected, entries);
    }

    @Test
    public void testChanges() {
        Order bid1 = new Order(1L, BID, 20d, 10L);
        Order bid2 = new Order(2L, BID, 19d, 12L);
        Order bid3 = new Order(3L, BID, 18d, 17L);
        Order bid4 = new Order(4L, BID, 17d, 18L);
        Order ask1 = new Order(5L, ASK, 22d, 5L);
        Order ask2 = new Order(6L, ASK, 23d, 10L);
        Order ask3 = new Order(7L, ASK, 24d, 25L);
        Order ask4 = new Order(8L, ASK, 25d, 22L);
        Book book = new Book("EUR/USD");
        book.putOffers(Arrays.asList(ask1, ask2, ask3, ask4));
        book.putBids(Arrays.asList(bid1, bid2, bid3, bid4));
        Book prevBook = book.copy();
        List<Order> offers = new ArrayList<>();
        List<Order> bids = new ArrayList<>();
        offers.clear();
        bids.clear();
        offers.addAll(
                Collections.singletonList(
                        new Order(9L, ASK, 23d, 12L)
                )
        );
        prevBook = book.copy();

        List<Order> removedOrders = Arrays.asList(
                book.getBids().remove(1L),
                book.getOffers().remove(6L),
                book.getOffers().remove(5L)
        );
        book.putBids(bids);
        book.putOffers(offers);
        List<Delta.Entry> entries = BookManagerUtil.getBookChanges(removedOrders, Stream.concat(bids.stream(), offers.stream()).collect(Collectors.toList()), prevBook, book, 2);
        //order is undefined
        List<Delta.Entry> expected = Arrays.asList(
                new Delta.Entry(removedOrders.get(0), Action.DELETE),
                new Delta.Entry(removedOrders.get(2), Action.DELETE),
                new Delta.Entry(offers.get(0), Action.UPDATE),
                new Delta.Entry(bid3, Action.NEW),
                new Delta.Entry(ask3, Action.NEW)
        );
        Assert.assertEquals(expected, entries);

        //test drop orders by adding more profitable
        prevBook = book.copy();
        offers.clear();
        bids.clear();
        offers.addAll(
                Arrays.asList(
                        new Order(10L, ASK, 20d, 12L),
                        new Order(11L, ASK, 21d, 12L)
                )
        );

        removedOrders = Collections.emptyList();
        book.putBids(bids);
        book.putOffers(offers);
        entries = BookManagerUtil.getBookChanges(removedOrders, offers, prevBook, book, 2);
        //order is undefined
        expected = Arrays.asList(
                new Delta.Entry(new Order(9L, ASK, 23d, 12L), Action.DELETE),
                new Delta.Entry(ask3, Action.DELETE),
                new Delta.Entry(offers.get(0), Action.NEW),
                new Delta.Entry(offers.get(1), Action.NEW)
        );
        Assert.assertEquals(expected, entries);
    }

    /**
     * use case
     * if Book.isEmpty() then NEW,NEW,NEW,NEW
     * Book deleted and not added -> DELETE + NEW
     * Book added best offers,bids NEW + DELETED old
     * Book added in middle NEW + Delete for last worse
     * Book remove old and place new with same Price => UPDATE (replace NEW + DELETE for same price
     */
    @Test
    public void testIsNew() {
        Order ask1 = new Order(1L, ASK, 22d, 5L);
        Order ask2 = new Order(2L, ASK, 23d, 10L);
        Order ask3 = new Order(3L, ASK, 24d, 25L);
        Order ask4 = new Order(4L, ASK, 25d, 22L);
        Book book = new Book("EUR/USD");
        book.putOffers(Arrays.asList(ask1, ask2, ask3, ask4));
        Assert.assertFalse(isNew(ask3, book.getOffers()));
        Assert.assertTrue(isNew(new Order(5L, ASK,123d, 45L), book.getOffers()));
    }
}
