package com.hesky.bookmanager;

import com.hesky.bookmanager.controller.InputData;
import com.hesky.bookmanager.model.Delta;
import com.hesky.bookmanager.model.Order;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

import java.util.List;

import static com.hesky.bookmanager.Align.*;
import static j2html.TagCreator.*;

/**
 * Represents HTML report of managing Order Book from FIX messages in log file
 */
public class Report {
    private static final int MAX_WIDTH = 100;
    private static final int HALF_WIDTH = 50;
    private static final int THIRD_WIDTH = 33;
    private List<Delta> deltas;
    private InputData data;

    public Report(List<Delta> deltas, InputData data) {
        this.deltas = deltas;
        this.data = data;
    }

    private static ContainerTag tableWith(DomContent... dc) {
        return tableWith(MAX_WIDTH, dc);
    }

    private static ContainerTag tableWith(int width, DomContent... dc) {
        return table(dc).withClass("table table-bordered table-condensed").attr("width", percentage(width));
    }

    private static String percentage(int width) {
        return width + "%";
    }

    private static ContainerTag tdWith(int width, DomContent dc) {
        return td(dc).attr("width", percentage(width));
    }

    private static DomContent tdWith(Object content, Align align) {
        return td(content.toString()).attr("align", align.toString());
    }

    private static DomContent thWith(Object content, int width, int colspan) {
        return
            th(content.toString())
                .attr("width", percentage(width))
                .attr("colspan", colspan);
    }

    private static DomContent thWith(Object content, int width) {
        return thWith(content, width, 1);
    }

    private static DomContent trWith(String name, Object value) {
        return
            tr(
                td(name),
                td(value.toString())
            );
    }

    private static DomContent tdWrapper(DomContent dc) {
        return tdWrapper(THIRD_WIDTH, dc);
    }

    private static DomContent tdWrapper(int width, DomContent dc) {
        return tdWith(width, dc);
    }

    private static ContainerTag tableWrapper(DomContent header, DomContent rows) {
        return
            tableWith(
                thead(header),
                tbody(rows)
            );
    }

    private static ContainerTag tableWrapper(DomContent rows) {
        return tableWrapper(null, rows);
    }

    /**
     * Makes report using {@code deltas} and {@code data} for each record in report
     *
     * @return HTML report
     */
    public ContainerTag makeReport() {
        return
            html(
                head(
                    styles(),
                    scripts()
                ),
                body(
                    bodyHeader(),
                    records()
                )
            );
    }

    /**
     * @return javascript src for report
     */
    private DomContent scripts() {
        return
            script().withSrc("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js");
    }

    /**
     * @return css styles for report including bootstrap
     */
    private DomContent styles() {
        return
            join(
                styleLink("/css/main.css"),
                styleLink("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css"),
                styleLink("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap-theme.min.css")
            );
    }

    /**
     * @param ref reference to style
     * @return html link dom element to stylesheet with {@code ref}
     */
    private DomContent styleLink(String ref) {
        return link().withRel("stylesheet").withHref(ref);
    }

    /**
     * @return main html header of document, based on user's {@link InputData}
     */
    private DomContent bodyHeader() {
        return
            tableWith(
                tbody(
                    trWith("Log file", data.getLogFile().toAbsolutePath()),
                    trWith("Symbol name", data.getSymbol()),
                    trWith("Book depth", data.getDepth()),
                    trWith("Start time", data.getStartTime()),
                    trWith("End time", data.getEndTime())
                )
            );
    }

    /**
     * @return book management records of report
     */
    private DomContent records() {
        return
            each(deltas, delta ->
                tableWrapper(
                    bookRefreshHeader(delta.isBookRefresh()),
                    record(delta)
                )
            );
    }

    /**
     * @param delta data for one record
     * @return one record of book management
     */
    private DomContent record(Delta delta) {
        return
            tr(
                tdWrapper(MAX_WIDTH, recordTable(delta))
            );
    }

    /**
     * @param delta data for one record
     * @return html table, that wraps record header and record data
     */
    private DomContent recordTable(Delta delta) {
        return
            tableWrapper(
                recordHeader(delta),
                recordData(delta)
            );
    }

    /**
     * @param isBookRefresh boolean flag that indicates existing of book refresh (new market data request)
     * @return header for record if {@code isBookRefresh} = true and nothing if false
     */
    private DomContent bookRefreshHeader(boolean isBookRefresh) {
        return h2(isBookRefresh ? "FULL BOOK REFRESH" : "");
    }

    /**
     * @param delta data for one record
     * @return wrapped by main table row - record data
     */
    private DomContent recordData(Delta delta) {
        return
            tr(
                td(
                    tableWrapper(
                        fullRecordData(delta)
                    )
                )
            );
    }

    /**
     * @param delta data for one record
     * @return input orders, book orders and bookChanges for this delta
     */
    private DomContent fullRecordData(Delta delta) {
        return
            tr(
                inputOrders(delta.getInputOrders()),
                bookOrders(delta),
                bookChanges(delta.getBookChanges())

            );
    }

    /**
     * @param bookChanges list of book changes
     * @return table of changes wrapped by table cell
     */
    private DomContent bookChanges(List<Delta.Entry> bookChanges) {
        return tdWrapper(bookChangesTable(bookChanges));

    }

    /**
     * @param bookChanges list of book changes
     * @return table of book changes
     */
    private ContainerTag bookChangesTable(List<Delta.Entry> bookChanges) {
        return
            tableWrapper(
                bookChangesHeader(),
                bookChangesRecords(bookChanges)
            );
    }

    /**
     * @param delta data for one record
     * @return wrapped by table cell table of current book orders
     */
    private DomContent bookOrders(Delta delta) {
        return tdWrapper(bookOrdersTable(delta));

    }

    /**
     * @param delta record data, that contains book offers and bids
     * @return table of current book offers and bids
     */
    private ContainerTag bookOrdersTable(Delta delta) {
        return
            tableWrapper(
                bookOrdersHeader(),
                bookOrdersRecords(delta)
            );
    }

    /**
     * @param inputOrders list of input orders, that contains current Fix message
     * @return wrapped by table cell table of input orders
     */
    private DomContent inputOrders(List<Delta.Entry> inputOrders) {
        return tdWrapper(inputOrdersTable(inputOrders));
    }

    /**
     * @param inputOrders list of input orders, that contains current Fix message
     * @return html table of input orders
     */
    private ContainerTag inputOrdersTable(List<Delta.Entry> inputOrders) {
        return
            tableWrapper(
                inputOrdersHeader(),
                inputOrdersRecords(inputOrders)
            );
    }

    /**
     * @param bookChanges list of book changes, that occurred in Book after input orders
     * @return html list of table rows of book changes
     */
    private DomContent bookChangesRecords(List<Delta.Entry> bookChanges) {
        return
            each(bookChanges, this::bookChangesRow);
    }

    /**
     *
     * @param order affected order in book (new,delete,update)
     * @return html table row of one book change
     */
    private DomContent bookChangesRow(Delta.Entry order) {
        return
            tr(
                td(order.getAction().toString()),
                td(order.getSide().toString()),
                tdWith(order.getPrice(), ALIGN_RIGHT),
                tdWith(order.getSize(), ALIGN_RIGHT)
            );
    }

    /**
     * @return table header of book changes table
     */
    private DomContent bookChangesHeader() {
        return
            tr(
                thWith("Action", 20),
                thWith("Side", 20),
                thWith("Price", 30),
                thWith("Size", 30)
            );
    }

    /**
     * @param delta record data, that contains current bids and asks in book
     * @return list of current market orders (offers -> bids), sorted by price
     */
    private DomContent bookOrdersRecords(Delta delta) {
        return
            join(
                offers(delta.getOffers()),
                bids(delta.getBids())
            );
    }

    /**
     * @param bids list of bids in current book
     * @return html list of table rows, which includes each bid
     */
    private DomContent bids(List<Order> bids) {
        return
            each(bids, bid ->
                bidRow(bid.getPrice(), bid.getSize())
            );
    }

    /**
     * @param price bid price
     * @param size bid size
     * @return html table row that represent bid price and size
     */
    private DomContent bidRow(Double price, Long size) {
        return
            tr(
                tdWith(price, ALIGN_RIGHT),
                tdWith(size, ALIGN_RIGHT),
                td(),
                td()
            );
    }

    /**
     * @param offers list of offers in current book
     * @return html list of table rows, which includes each offer
     */
    private DomContent offers(List<Order> offers) {
        return
            each(offers, offer ->
                offerRow(offer.getPrice(), offer.getSize())
            );
    }


    /**
     * @param price offer price
     * @param size offer size
     * @return html table row that represent offer price and size
     */
    private DomContent offerRow(Double price, Long size) {
        return
            tr(
                td(),
                td(),
                tdWith(price, ALIGN_RIGHT),
                tdWith(size, ALIGN_RIGHT)
            );
    }

    /**
     * @return header of current book orders (for current bids and offers
     */
    private DomContent bookOrdersHeader() {
        return
            join(
                tr(
                    thWith("BID", HALF_WIDTH, 2),
                    thWith("ASK", HALF_WIDTH, 2)
                ),
                tr(
                    thWith("Price", 25),
                    thWith("Size", 25),
                    thWith("Price", 25),
                    thWith("Size", 25)
                ));
    }

    /**
     * @param inputOrders list of input orders
     * @return html list of table rows of each input order
     */
    private DomContent inputOrdersRecords(List<Delta.Entry> inputOrders) {
        return each(inputOrders, entry ->
            tr(
                tdWith(entry.getId(), ALIGN_CENTER),
                tdWith(entry.getAction(), ALIGN_LEFT),
                tdWith(entry.getSide() == null ? "" : entry.getSide(), ALIGN_CENTER),
                tdWith(entry.getPrice(), ALIGN_RIGHT),
                tdWith(entry.getSize(), ALIGN_RIGHT)
            )
        );
    }

    /**
     * @return header row of input orders table
     */
    private DomContent inputOrdersHeader() {
        return
            tr(
                thWith("ID", 30),
                thWith("Action", 10),
                thWith("Side", 10),
                thWith("Price", 25),
                thWith("Size", 25)
            );
    }

    /**
     * @param delta data for record header
     * @return html row that represents header of new record
     */
    private DomContent recordHeader(Delta delta) {
        return
            tr(
                tdWrapper(MAX_WIDTH,
                    tableWrapper(
                        recordHeaderData(delta)
                    )
                )
            );
    }

    /**
     * @param delta data for one new record
     * @return html record header row
     */
    private DomContent recordHeaderData(Delta delta) {
        return
            tr(
                tdWrapper(HALF_WIDTH, h1(delta.getNumber().toString())),
                tdWrapper(HALF_WIDTH,
                    recordHeaderTimeData(delta)
                )
            );
    }


    /**
     * @param delta data for header
     * @return header's rows with request and response time
     */
    private DomContent recordHeaderTimeData(Delta delta) {
        return
            tableWrapper(
                join(
                    trWith("Sending Time", delta.getStartDateTime()),
                    trWith("Receiving time", delta.getEndDateTime()),
                    trWith("Difference", delta.getDifference())
                )
            );
    }
}
