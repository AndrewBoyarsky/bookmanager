package com.hesky.bookmanager.controller;


import com.hesky.bookmanager.BookManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Controller of main window of application (printer scene)
 */
public class MainController implements Initializable {
    private static final Logger LOG = getLogger(MainController.class);

    @FXML
    private TextField logFileField;
    @FXML
    private TextField reportFileField;
    @FXML
    private TextField symbolNameField;
    @FXML
    private ChoiceBox<String> bookDepthChoiceBox;
    @FXML
    private DatePicker dateField;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private Button startButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button browseLogFileButton;
    @FXML
    private Button browseReportFileButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    private BookManager bookManager = new BookManager();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.debug("Initialize main controller...");
        initInputFields();
        initButtonsClickHandlers();
    }

    /**
     * initialize OnClick Event handlers on each ui button
     */
    private void initButtonsClickHandlers() {
        cancelButton.setOnAction(e -> shutdown());

        browseLogFileButton.setOnAction(e -> {
            File file = getFileFromOpenDialog();
            if (file != null) {
                logFileField.setText(file.getAbsolutePath());
            }
        });
        browseReportFileButton.setOnAction(e -> {
            File file = getFileFromSaveDialog();
            if (file != null) {
                reportFileField.setText(file.getAbsolutePath());
            }
        });
        startButton.setOnAction(e -> {
            try {
                InputData data = readInputData();
                Objects.requireNonNull(data);
                LOG.debug("Got input data from user: {}", data);
                bookManager.setData(data);
                LOG.info("Start creating report");
                executeBuildReportTask();
            }
            catch (Exception ex) {
                unblockInputWithError();
                LOG.error("Error has occurred while creating report!");
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
            }
        });
    }

    /**
     * Enables input buttons and shows Fail error
     */
    private void unblockInputWithError() {
        unblockInput();
        showFailLabel("Error while creating report!");
    }


    /**
     * Filling input fields with some default data
     */
    private void initInputFields() {
        symbolNameField.setText("EUR/USD");
        bookDepthChoiceBox.getItems().addAll(Arrays.asList("Top of Book", "2", "3"));
        bookDepthChoiceBox.getSelectionModel().select(1);
        logFileField.setText("E:\\axon-test\\market_data\\data.summary");
        reportFileField.setText("E:\\axon-test\\report2.html");
        dateField.setValue(LocalDate.of(2015, 3, 3));
    }

    /**
     * Build report in new Thread to avoid ui freezing
     */
    private void executeBuildReportTask() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<Void> task = createBuildReportTask(executor);
        executor.execute(task);
    }

    /**
     * Creates new task for building report
     *
     * @param executor - Pool of threads which should be terminated after that report was created
     * @return Task for building report, which is ready to executing
     */
    private FutureTask<Void> createBuildReportTask(ExecutorService executor) {
        return new FutureTask<>(
                () -> {
                    blockInput();
                    bookManager.buildReport();
                    Platform.runLater(this::unblockInputWithSuccess);
                    System.gc();
                    executor.shutdown();
                    LOG.info("Report was successfully created");
                    return null;
                });
    }

    /**
     * Disable buttons to avoid input errors; shows that work is in progress
     */
    private void blockInput() {
        statusLabel.setVisible(false);
        progressIndicator.setVisible(true);
        startButton.setDisable(true);
        cancelButton.setDisable(true);
    }

    /**
     * Enable buttons and hiding progress indicator
     */
    private void unblockInput() {
        progressIndicator.setVisible(false);
        startButton.setDisable(false);
        cancelButton.setDisable(false);

    }

    /**
     * Unblock input buttons and show success label
     */
    private void unblockInputWithSuccess() {
        unblockInput();
        showSuccessLabel("Report was created");
    }

    /**
     * Shows success label
     *
     * @param text - text, that should be displayed
     */
    private void showSuccessLabel(String text) {
        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setText(text);
        statusLabel.setVisible(true);
    }

    /**
     * Shows fail label
     *
     * @param text - text, that should be displayed
     */
    private void showFailLabel(String text) {
        statusLabel.setTextFill(Color.RED);
        statusLabel.setText(text);
        statusLabel.setVisible(true);
    }

    /**
     * Read and parse input data from input fields
     *
     * @return InputData object if all data from input fields was successfully parsed and collected into InputData
     */
    private InputData readInputData() {
        Path logFile = Paths.get(logFileField.getText());
        if (!Files.exists(logFile))
            throw new RuntimeException("File: \'" + logFile.toString() + "\' was not found");
        Path reportFile = Paths.get(reportFileField.getText());
        String symbol = symbolNameField.getText();
        int depth = bookDepthChoiceBox.getItems().indexOf(bookDepthChoiceBox.getValue()) + 1;
        LocalDate date = dateField.getValue();
        LocalTime startTime = LocalTime.parse(startTimeField.getText(), DateTimeFormatter.ofPattern("HH:mm:ss"));
        LocalTime endTime = LocalTime.parse(endTimeField.getText(), DateTimeFormatter.ofPattern("HH:mm:ss"));
        return new InputData(logFile, reportFile, symbol, depth, LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime));
    }

    /**
     * Fully shutdowns application
     */
    private void shutdown() {
        LOG.info("Shutdown app");
        Platform.exit();
        System.exit(0);
    }

    /**
     * Shows open dialog to select file and returns selected file or null if no file was selected
     *
     * @return file, which was selected by user
     */
    private File getFileFromOpenDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select file");
        File file = chooser.showOpenDialog(null);
        if (file != null && file.exists()) {
            return file;
        }
        return null;
    }

    /**
     * Shows save dialog and returns selected file or null if no file was selected
     *
     * @return file, which was selected by user
     */
    private File getFileFromSaveDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select report file");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("HTML", "*.html");
        chooser.getExtensionFilters().add(filter);
        chooser.setSelectedExtensionFilter(filter);
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            return file;
        }
        return null;
    }
}
