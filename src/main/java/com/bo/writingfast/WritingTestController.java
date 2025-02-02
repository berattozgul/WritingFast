package com.bo.writingfast;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.scene.text.TextAlignment;
import javafx.scene.input.KeyEvent;
import java.util.*;
import javafx.application.Platform;

public class WritingTestController {
    @FXML private TextField userInputArea;
    @FXML private TextFlow promptTextFlow;
    @FXML private Label timerLabel;
    @FXML private Label wpmLabel;
    @FXML private Label accuracyLabel;
    @FXML private ProgressBar progressBar;
    @FXML private ComboBox<String> difficultyComboBox;
    @FXML private ComboBox<TestMode> testModeComboBox;
    @FXML private Spinner<Integer> limitSpinner;
    @FXML private Label limitLabel;
    @FXML private Label bestWpmLabel;

    private Timeline timer;
    private int secondsElapsed = 0;
    private String currentPrompt;
    private boolean testStarted = false;
    private String[] words;
    private int currentWordIndex = 0;
    private int totalWords = 0;
    private int totalCorrectChars = 0;
    private int totalTypedChars = 0;
    private Map<String, Integer> bestWpmScores = new HashMap<>();
    private Map<String, String> textPrompts = new HashMap<>();

    private enum TestMode {
        TIME_LIMIT("Time Limit"),
        WORD_COUNT("Word Count"),
        INFINITE("Infinite");

        private final String displayName;

        TestMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @FXML
    private void initialize() {
        setupPrompts();
        setupDifficultyLevels();
        setupTestModes();
        userInputArea.setDisable(true);
        setupTimer();
        
        // Add key event handler
        userInputArea.setOnKeyPressed(this::handleKeyPress);
        
        // Add listener for text changes
        userInputArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (testStarted) {
                if (newValue.startsWith(" ")) {
                    userInputArea.setText(newValue.trim());
                    return;
                }
                updateCurrentWordHighlighting(newValue);
            }
        });

        // Add listener for difficulty changes
        difficultyComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updatePromptForDifficulty(newVal);
                updateBestWpm(newVal);
            }
        });

        // Add listener for test mode changes
        testModeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateTestModeUI(newVal);
            }
        });
    }

    private void setupPrompts() {
        // Easy level texts - Simple sentences and common words
        textPrompts.put("Easy", String.join(" ",
            "The quick brown fox jumps over the lazy dog. Simple words make typing practice fun and easy.",
            "She sells seashells by the seashore. The sun shines bright in the blue sky.",
            "A happy family enjoys dinner together. Children play in the park after school.",
            "My favorite book tells an amazing story. The cat sleeps peacefully on the soft pillow.",
            "Fresh bread smells wonderful in the morning. Birds sing sweet songs in the garden."
        ));

        // Medium level texts - Mix of common and some challenging words
        textPrompts.put("Medium", String.join(" ",
            "The quick brown fox jumps over the lazy dog. This classic pangram contains every letter of the English alphabet at least once.",
            "Professional typists maintain excellent posture and finger positioning while working on their keyboards.",
            "The technology industry continues to evolve with innovative solutions and groundbreaking developments.",
            "Environmental scientists study the impact of climate change on various ecosystems around the world.",
            "Effective communication skills are essential for success in both personal and professional relationships."
        ));

        // Hard level texts - Complex sentences and challenging words
        textPrompts.put("Hard", String.join(" ",
            "Pack my box with five dozen liquor jugs! How vexingly quick daft zebras jump.",
            "Sphinx of black quartz, judge my vow! The five boxing wizards jump quickly.",
            "Amazingly few discotheques provide jukeboxes! Waltz, nymph, for quick jigs vex Bud.",
            "Jackdaws love my big sphinx of quartz. The job requires extra pluck and zeal from every young wage earner.",
            "Two driven jocks help fax my big quiz. Five quacking zephyrs jolt my wax bed. The quick onyx goblin jumps over the lazy dwarf."
        ));
    }

    private void setupDifficultyLevels() {
        difficultyComboBox.getItems().addAll("Easy", "Medium", "Hard");
        difficultyComboBox.setValue("Easy");
        updatePromptForDifficulty("Easy");
    }

    private void setupTestModes() {
        testModeComboBox.getItems().addAll(TestMode.values());
        testModeComboBox.setValue(TestMode.TIME_LIMIT);
        
        // Setup spinner with default values
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 60);
        limitSpinner.setValueFactory(valueFactory);
        limitSpinner.setEditable(true);
        
        // Add listener for mode changes to update spinner defaults
        testModeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == TestMode.TIME_LIMIT) {
                limitSpinner.getValueFactory().setValue(60); // Default 60 seconds
            } else if (newVal == TestMode.WORD_COUNT) {
                limitSpinner.getValueFactory().setValue(20); // Default 20 words
            }
        });
        
        updateTestModeUI(TestMode.TIME_LIMIT);
    }

    private void updateTestModeUI(TestMode mode) {
        switch (mode) {
            case TIME_LIMIT:
                limitSpinner.setVisible(true);
                limitLabel.setText("seconds");
                break;
            case WORD_COUNT:
                limitSpinner.setVisible(true);
                limitLabel.setText("words");
                break;
            case INFINITE:
                limitSpinner.setVisible(false);
                limitLabel.setVisible(false);
                break;
        }
    }

    private void updatePromptForDifficulty(String difficulty) {
        currentPrompt = textPrompts.get(difficulty);
        words = currentPrompt.split("\\s+");
        totalWords = words.length;
        if (!testStarted) {
            currentWordIndex = 0;
            updatePromptDisplay();
        }
    }

    private void updateBestWpm(String difficulty) {
        int bestWpm = bestWpmScores.getOrDefault(difficulty, 0);
        bestWpmLabel.setText("Best: " + bestWpm + " WPM");
    }

    private void handleKeyPress(KeyEvent event) {
        if (!testStarted) return;
        
        switch (event.getCode()) {
            case SPACE:
                event.consume();
                String currentInput = userInputArea.getText().trim();
                if (!currentInput.isEmpty()) {
                    checkWordAndMoveNext(currentInput);
                    Platform.runLater(() -> {
                        userInputArea.clear();
                        userInputArea.setText("");
                    });
                }
                break;
            case ENTER:
                event.consume();
                break;
            case BACK_SPACE:
                // If the input is empty and backspace is pressed, don't do anything special
                if (userInputArea.getText().isEmpty()) {
                    event.consume();
                }
                break;
        }
    }

    private void updatePromptDisplay() {
        promptTextFlow.getChildren().clear();
        
        // Show current word
        Text currentWord = new Text(words[currentWordIndex]);
        currentWord.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        promptTextFlow.getChildren().add(currentWord);
        
        // Show next word in gray if available
        if (currentWordIndex < words.length - 1) {
            Text nextWord = new Text(" " + words[currentWordIndex + 1]);
            nextWord.setStyle("-fx-font-size: 24px;");
            nextWord.setFill(Color.GRAY);
            promptTextFlow.getChildren().add(nextWord);
        }
        
        promptTextFlow.setTextAlignment(TextAlignment.CENTER);
    }

    private void checkWordAndMoveNext(String userInput) {
        String targetWord = words[currentWordIndex];
        int correctChars = 0;
        int totalChars = Math.max(targetWord.length(), userInput.length());
        
        // Count correct characters
        int minLength = Math.min(targetWord.length(), userInput.length());
        for (int i = 0; i < minLength; i++) {
            if (userInput.charAt(i) == targetWord.charAt(i)) {
                correctChars++;
            }
        }
        
        // Add to totals (including errors)
        totalCorrectChars += correctChars;
        totalTypedChars += totalChars;

        // Move to next word
        currentWordIndex++;
        
        // Check if we should continue based on the test mode
        TestMode currentMode = testModeComboBox.getValue();
        boolean shouldContinue = true;
        
        switch (currentMode) {
            case WORD_COUNT:
                if (currentWordIndex >= limitSpinner.getValue()) {
                    shouldContinue = false;
                }
                break;
            case INFINITE:
                if (currentWordIndex >= words.length) {
                    shouldContinue = false;
                }
                break;
            case TIME_LIMIT:
                if (currentWordIndex >= words.length) {
                    // In time limit mode, wrap around to the beginning if we reach the end
                    currentWordIndex = 0;
                }
                break;
        }
        
        if (!shouldContinue) {
            finishTest();
        } else {
            updatePromptDisplay();
            // Update stats and progress
            calculateStats();
            updateProgress();
        }
    }

    private void finishTest() {
        timer.stop();
        testStarted = false;
        
        // Update best WPM if current score is higher
        String difficulty = difficultyComboBox.getValue();
        int currentWpm = Integer.parseInt(wpmLabel.getText());
        int bestWpm = bestWpmScores.getOrDefault(difficulty, 0);
        if (currentWpm > bestWpm) {
            bestWpmScores.put(difficulty, currentWpm);
            updateBestWpm(difficulty);
            Platform.runLater(this::showCongratulations);
        }
        
        // Re-enable UI controls
        userInputArea.setDisable(true);
        difficultyComboBox.setDisable(false);
        testModeComboBox.setDisable(false);
        limitSpinner.setDisable(false);
    }

    private void showCongratulations() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Record!");
        alert.setHeaderText("Congratulations!");
        alert.setContentText(String.format("You've achieved a new best WPM score of %s!", wpmLabel.getText()));
        alert.show(); // Using show() instead of showAndWait()
    }

    @FXML
    private void handleStartTest() {
        userInputArea.setDisable(false);
        userInputArea.clear();
        userInputArea.requestFocus();
        currentWordIndex = 0;
        secondsElapsed = 0;
        totalCorrectChars = 0;
        totalTypedChars = 0;
        testStarted = true;
        updatePromptDisplay();
        progressBar.setProgress(0);
        timer.play();
        difficultyComboBox.setDisable(true);
        testModeComboBox.setDisable(true);
        limitSpinner.setDisable(true);
    }

    @FXML
    private void handleResetTest() {
        timer.stop();
        testStarted = false;
        secondsElapsed = 0;
        currentWordIndex = 0;
        totalCorrectChars = 0;
        totalTypedChars = 0;
        userInputArea.clear();
        userInputArea.setDisable(true);
        updateTimer();
        wpmLabel.setText("0");
        accuracyLabel.setText("0%");
        progressBar.setProgress(0);
        updatePromptDisplay();
        difficultyComboBox.setDisable(false);
        testModeComboBox.setDisable(false);
        limitSpinner.setDisable(false);
    }

    private void updateTimer() {
        int minutes = secondsElapsed / 60;
        int seconds = secondsElapsed % 60;
        timerLabel.setText(String.format("%d:%02d", minutes, seconds));
    }

    private void updateProgress() {
        double progress;
        TestMode currentMode = testModeComboBox.getValue();

        switch (currentMode) {
            case TIME_LIMIT:
                progress = (double) secondsElapsed / limitSpinner.getValue();
                break;
            case WORD_COUNT:
                progress = (double) currentWordIndex / limitSpinner.getValue();
                break;
            case INFINITE:
                progress = currentWordIndex >= words.length ? 1.0 : (double) currentWordIndex / words.length;
                break;
            default:
                progress = 0;
                break;
        }

        progressBar.setProgress(Math.min(1.0, progress));
    }

    private void calculateStats() {
        // Calculate WPM (Word Per Minute)
        double minutes = secondsElapsed / 60.0;
        int wpm = minutes > 0 ? (int)(currentWordIndex / minutes) : 0;
        wpmLabel.setText(String.valueOf(wpm));

        // Calculate accuracy
        if (totalTypedChars > 0) {
            double accuracy = ((double) totalCorrectChars / totalTypedChars) * 100;
            accuracyLabel.setText(String.format("%.0f%%", Math.min(accuracy, 100)));
        } else {
            accuracyLabel.setText("0%");
        }
    }

    private void updateCurrentWordHighlighting(String userInput) {
        String targetWord = words[currentWordIndex];
        promptTextFlow.getChildren().clear();
        
        // Count errors for current word
        int errors = 0;
        int minLength = Math.min(targetWord.length(), userInput.length());
        for (int i = 0; i < minLength; i++) {
            if (userInput.charAt(i) != targetWord.charAt(i)) {
                errors++;
            }
        }
        // Add extra characters as errors
        errors += Math.abs(targetWord.length() - userInput.length());
        
        // If the input exactly matches the target word, show all in green
        if (userInput.equals(targetWord)) {
            Text correctWord = new Text(targetWord);
            correctWord.setFill(Color.GREEN);
            correctWord.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            promptTextFlow.getChildren().add(correctWord);
            
            // Show next word in gray if available
            if (currentWordIndex < words.length - 1) {
                Text nextWord = new Text(" " + words[currentWordIndex + 1]);
                nextWord.setStyle("-fx-font-size: 24px;");
                nextWord.setFill(Color.GRAY);
                promptTextFlow.getChildren().add(nextWord);
            }
            return;
        }
        
        // Otherwise, do character by character comparison
        for (int i = 0; i < Math.max(targetWord.length(), userInput.length()); i++) {
            Text charText;
            if (i < targetWord.length()) {
                char targetChar = targetWord.charAt(i);
                if (i < userInput.length()) {
                    char userChar = userInput.charAt(i);
                    if (userChar == targetChar) {
                        charText = new Text(String.valueOf(targetChar));
                        charText.setFill(Color.GREEN);
                    } else {
                        charText = new Text(String.valueOf(targetChar));
                        charText.setFill(Color.RED);
                    }
                } else {
                    charText = new Text(String.valueOf(targetChar));
                    charText.setFill(Color.BLACK);
                }
            } else {
                charText = new Text("×");
                charText.setFill(Color.RED);
            }
            charText.setStyle("-fx-font-size: 24px;");
            promptTextFlow.getChildren().add(charText);
        }
        
        // Show next word in gray if available
        if (currentWordIndex < words.length - 1) {
            Text nextWord = new Text(" " + words[currentWordIndex + 1]);
            nextWord.setStyle("-fx-font-size: 24px;");
            nextWord.setFill(Color.GRAY);
            promptTextFlow.getChildren().add(nextWord);
        }
        
        promptTextFlow.setTextAlignment(TextAlignment.CENTER);
    }

    private void checkTestCompletion() {
        TestMode currentMode = testModeComboBox.getValue();
        boolean shouldFinish = false;

        switch (currentMode) {
            case TIME_LIMIT:
                if (secondsElapsed >= limitSpinner.getValue()) {
                    shouldFinish = true;
                }
                break;
            case WORD_COUNT:
                if (currentWordIndex >= limitSpinner.getValue()) {
                    shouldFinish = true;
                }
                break;
            case INFINITE:
                if (currentWordIndex >= words.length) {
                    shouldFinish = true;
                }
                break;
        }

        if (shouldFinish) {
            finishTest();
        }
    }

    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            updateTimer();
            if (testStarted) {
                calculateStats();
                checkTestCompletion();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
    }
} 