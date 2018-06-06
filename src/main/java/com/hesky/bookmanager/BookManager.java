package com.hesky.bookmanager;


import com.hesky.bookmanager.controller.InputData;
import com.hesky.bookmanager.model.*;
import com.hesky.bookmanager.util.FixParser;
import j2html.tags.ContainerTag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import quickfix.*;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;

import static com.hesky.bookmanager.util.BookManagerUtil.getBookChanges;
import static com.hesky.bookmanager.util.BookManagerUtil.isDeleteExist;
import static com.hesky.bookmanager.util.BookManagerUtil.readOrder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provide services to Parse logFile, manage book and build report
 */
public class BookManager {
    private static final Logger LOG = getLogger(BookManager.class);

    private InputData data;

    public BookManager(InputData data) {
        this.data = data;
    }

    public BookManager() {

    }

    public void setData(InputData data) {
        this.data = data;
    }

    /**
     * Read logFile and parse and filter fix messages
     *
     * @return List of FIX messages which were found in file and filtered by user inputData
     * @throws Exception if cannot read file or cannot parse FIX message
     */
    public List<Message> parseLogs() throws Exception {
        LOG.debug("Parse log file: " + data.getLogFile());
        List<Message> messages = new ArrayList<>();
        LineIterator it = FileUtils.lineIterator(data.getLogFile().toFile(), "UTF-8");
        try {
            while (it.hasNext()) {
                String line = it.nextLine();
                //ignore heartbeat, login and other 35=v and 35=x messages that dont refers to chosen symbol
                if (!line.contains(data.getSymbol())) {
                    continue;
                }
                //cut the fixmessage from line of logs
                String fixMessageString = line.substring(line.indexOf(" : ") + 3,
                        line.lastIndexOf(" "));
                Message fixMessage = FixParser.parse(fixMessageString);
                //skip when message exceeds time bounds
                LocalDateTime messageTime = fixMessage.getHeader().getField(new UtcTimeStampField(52)).getObject();
                if (messageTime.isBefore(data.getStartTime())
                        || messageTime.isAfter(data.getEndTime()))
                    continue;
                messages.add(fixMessage);
            }
        }
        finally {
            LineIterator.closeQuietly(it);
        }
        LOG.debug("Found: {} messages", messages.size());
        return messages;
    }

    /**
     * Perform Book managing for each message in {@code messages}
     *
     * @param messages List of FIX messages, that should be used for managing book
     * @return List of Delta that should be displayed in report
     * @throws Exception if FIX messages are not valid
     */
    public List<Delta> manageBook(List<Message> messages) throws Exception {
        LOG.debug("Managing book");
        Book currentBook = new Book(data.getSymbol());
        Message marketDataRequest = null;
        boolean isBookReset = false;
        List<Delta> deltas = new ArrayList<>(128);
        for (Message message : messages) {
            //receive new market data request
            if (message.getHeader().getField(new StringField(35)).getValue().equalsIgnoreCase("v")) {
                //type = subscribe + update (reset book)
                if (message.getInt(263) == 1) {
                    marketDataRequest = message;
                    isBookReset = true;
                }
                currentBook.reset();
                continue;
            }
            Delta delta = new Delta();
            //Request is a market data incremental update (35=x)
            if (message.getHeader().getField(new StringField(35)).getValue().equalsIgnoreCase("x")) {
                //groups of orders
                List<Group> groups = message.getGroups(268);
                Book prevBook = currentBook.copy();
                //book with limited by depth number of bids and asks
                Book prevSnapshot = prevBook.getFirstLevels(data.getDepth());
                //all added orders in current message
                List<Order> addedOrders = new ArrayList<>();
                for (Group group : groups) {
                    //parse message and create new order
                    if (group.getInt(279) == 0) {
                        Order order = readOrder(group);
                        //add new order to the book
                        currentBook.add(order);
                        //add new Order to delta input message
                        delta.getInputOrders().add(new Delta.Entry(order, Action.NEW));
                        //keep order of new orders
                        addedOrders.add(order);
                    }
                }
                //check for presence of delete requests in fix message
                boolean isDeleteExist = isDeleteExist(groups);
                //order of removed orders is important
                //all removed orders in current message
                Map<Double, Order> removedOrders = new LinkedHashMap<>();
                if (isDeleteExist) {
                    for (Group group : groups) {
                        //check for group with 279 = 2 (delete request)
                        if (group.getInt(279) == 2) {
                            Long id = group.getDecimal(278).longValue();
                            //remove from book
                            Order removedOrder = currentBook.delete(id);
                            //add order to removed orders to keep order
                            removedOrders.put(removedOrder.getPrice(), removedOrder);
                            //add delete request to input message in delta
                            delta.getInputOrders().add(new Delta.Entry(removedOrder.getId(), 0.0, 0L, Action.DELETE));
                        }
                    }
                }
                //new book with limited by depth number of bids and asks(offers)
                Book newSnapshot = currentBook.getFirstLevels(data.getDepth());
                //if changes in first book levels occurred
                if (!prevSnapshot.equals(newSnapshot)) {
                    //add to delta book changes
                    delta.setBookChanges(getBookChanges(removedOrders.values(), addedOrders, prevBook, currentBook, data.getDepth()));
                } else {
                    //add to delta empty list of book changes to avoid NullPointerException
                    delta.setBookChanges(Collections.emptyList());
                }
            }
            //compose delta
            delta.setStartDateTime(Objects.requireNonNull(marketDataRequest).getHeader().getUtcTimeStamp(52));
            delta.setEndDateTime(message.getHeader().getUtcTimeStamp(52));
            delta.setBook(currentBook.getFirstLevels(data.getDepth()));
            delta.setNumber(message.getHeader().getInt(34));
            delta.setBookRefresh(isBookReset);
            isBookReset = false;
            //add delta to result list
            deltas.add(delta);
        }
        LOG.debug("Created {} deltas", deltas.size());
        return deltas;
    }

    /**
     * Creates and writes report to specified file using deltas for each iteration
     *
     * @param deltas Independent entries of report
     */
    public void createAndWriteReport(List<Delta> deltas) {
        LOG.debug("Making report");
        Report report = new Report(deltas, data);
        ContainerTag html = report.makeReport();
        try {
            PrintWriter writer = new PrintWriter(data.getReportFile().toFile());
            html.render(writer);
            writer.close();
        }
        catch (Exception e) {
            LOG.error("Cannot write report", e);
        }
    }


    /**
     * Build report for input data
     *
     * @throws Exception if any error occurred during building report
     */
    public void buildReport() throws Exception {
        this.createAndWriteReport(manageBook(parseLogs()));
    }
}
