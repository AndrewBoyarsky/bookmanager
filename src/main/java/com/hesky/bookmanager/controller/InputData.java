package com.hesky.bookmanager.controller;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * User input data from GUI
 */
public class InputData {
    private Path logFile;
    private Path reportFile;
    private String symbol;
    private int depth;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Path getLogFile() {
        return logFile;
    }

    public Path getReportFile() {
        return reportFile;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDepth() {
        return depth;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public InputData(Path logFile, Path reportFile, String symbol, int depth, LocalDateTime startTime, LocalDateTime endTime) {

        this.logFile = logFile;
        this.reportFile = reportFile;
        this.symbol = symbol;
        this.depth = depth;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "InputData{" +
                "logFile=" + logFile +
                ", reportFile=" + reportFile +
                ", symbol='" + symbol + '\'' +
                ", depth=" + depth +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}