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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.*;
import javafx.application.Platform;
import javafx.scene.Node;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    @FXML private GridPane virtualKeyboard;
    @FXML private Label totalWordsLabel;
    @FXML private Label correctWordsLabel;
    @FXML private Label errorRateLabel;
    @FXML private Label timePerWordLabel;
    @FXML private TextArea customTextArea;
    @FXML private VBox customTextSection;
    @FXML private ComboBox<PracticeFocus> practiceFocusComboBox;
    @FXML private VBox practiceFocusSection;
    @FXML private TextArea statisticsArea;

    private Timeline timer;
    private int secondsElapsed = 0;
    private String currentPrompt;
    private boolean testStarted = false;
    private String[] words;
    private int currentWordIndex = 0;
    private int totalWords = 0;
    private int totalCorrectChars = 0;
    private int totalTypedChars = 0;
    private int correctWords = 0;
    private Map<String, List<ScoreRecord>> highScores = new HashMap<>();
    private Map<String, String> textPrompts = new HashMap<>();
    private Map<String, Node> keyboardKeys = new HashMap<>();
    private List<Long> wordTimes = new ArrayList<>();
    private Map<Character, Integer> errorCounts = new HashMap<>();
    private LocalDateTime testStartTime;

    private enum TestMode {
        TIME_LIMIT("Time Limit"),
        WORD_COUNT("Word Count"),
        INFINITE("Infinite"),
        PRACTICE("Practice Mode"),
        CUSTOM("Custom Text");

        private final String displayName;

        TestMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private enum PracticeFocus {
        NUMBERS("Numbers", "1234567890"),
        SYMBOLS("Symbols", "!@#$%^&*()_+-=[]{}|;:,.<>?"),
        UPPERCASE("Uppercase", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        LOWERCASE("Lowercase", "abcdefghijklmnopqrstuvwxyz"),
        COMMON_WORDS("Common Words", "the be to of and a in that have I it for not on with he as you do at this but his by from they we say her she or an will my one all would there their what so up out if about who get which go me when make can like time no just him know take people into year your good some could them see other than then now look only come its over think also back after use two how our work first well way even new want because any these give day most us");

        private final String displayName;
        private final String practiceText;

        PracticeFocus(String displayName, String practiceText) {
            this.displayName = displayName;
            this.practiceText = practiceText;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class ScoreRecord implements Serializable {
        private final int wpm;
        private final double accuracy;
        private final String difficulty;
        private final LocalDateTime timestamp;
        private final TestMode mode;

        public ScoreRecord(int wpm, double accuracy, String difficulty, TestMode mode) {
            this.wpm = wpm;
            this.accuracy = accuracy;
            this.difficulty = difficulty;
            this.timestamp = LocalDateTime.now();
            this.mode = mode;
        }

        @Override
        public String toString() {
            return String.format("%s - WPM: %d, Accuracy: %.1f%%, Mode: %s, Time: %s",
                difficulty, wpm, accuracy, mode, timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }

    @FXML
    private void initialize() {
        setupPrompts();
        setupDifficultyLevels();
        setupTestModes();
        setupVirtualKeyboard();
        setupPracticeFocus();
        loadHighScores();
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
                updateVirtualKeyboard(newValue);
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

        // Add listener for practice focus changes
        practiceFocusComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && testModeComboBox.getValue() == TestMode.PRACTICE) {
                updatePracticeText(newVal);
            }
        });
    }

    private void setupVirtualKeyboard() {
        if (virtualKeyboard == null) {
            System.out.println("Warning: Virtual keyboard GridPane is not initialized");
            return;
        }

        String[][] keyLayout = {
            {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "="},
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]", "\\"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l", ";", "'"},
            {"z", "x", "c", "v", "b", "n", "m", ",", ".", "/"}
        };

        for (int row = 0; row < keyLayout.length; row++) {
            for (int col = 0; col < keyLayout[row].length; col++) {
                String key = keyLayout[row][col];
                Label keyLabel = new Label(key);
                keyLabel.getStyleClass().add("keyboard-key");
                virtualKeyboard.add(keyLabel, col, row);
                keyboardKeys.put(key, keyLabel);
            }
        }
    }

    private void updateVirtualKeyboard(String currentInput) {
        if (virtualKeyboard == null || keyboardKeys.isEmpty()) {
            return;
        }

        // Reset all keys
        keyboardKeys.values().forEach(key -> key.getStyleClass().removeAll("active", "correct", "error"));

        if (currentInput.isEmpty()) return;

        String targetWord = words[currentWordIndex];
        int minLength = Math.min(targetWord.length(), currentInput.length());

        for (int i = 0; i < minLength; i++) {
            String key = String.valueOf(currentInput.charAt(i)).toLowerCase();
            Node keyNode = keyboardKeys.get(key);
            if (keyNode != null) {
                if (currentInput.charAt(i) == targetWord.charAt(i)) {
                    keyNode.getStyleClass().add("correct");
                } else {
                    keyNode.getStyleClass().add("error");
                }
            }
        }
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
        
        updateTestModeUI(TestMode.TIME_LIMIT);
    }

    private void updateTestModeUI(TestMode mode) {
        switch (mode) {
            case TIME_LIMIT:
                limitSpinner.setVisible(true);
                limitLabel.setText("seconds");
                customTextSection.setVisible(false);
                practiceFocusSection.setVisible(false);
                break;
            case WORD_COUNT:
                limitSpinner.setVisible(true);
                limitLabel.setText("words");
                customTextSection.setVisible(false);
                practiceFocusSection.setVisible(false);
                break;
            case INFINITE:
                limitSpinner.setVisible(false);
                limitLabel.setVisible(false);
                customTextSection.setVisible(false);
                practiceFocusSection.setVisible(false);
                break;
            case PRACTICE:
                limitSpinner.setVisible(false);
                limitLabel.setVisible(false);
                customTextSection.setVisible(false);
                practiceFocusSection.setVisible(true);
                updatePracticeText(practiceFocusComboBox.getValue());
                break;
            case CUSTOM:
                limitSpinner.setVisible(false);
                limitLabel.setVisible(false);
                customTextSection.setVisible(true);
                practiceFocusSection.setVisible(false);
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
        int bestWpm = highScores.getOrDefault(difficulty + "_" + testModeComboBox.getValue(), new ArrayList<>())
            .stream()
            .mapToInt(r -> r.wpm)
            .max()
            .orElse(0); 
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
        
        // Count correct characters and track errors
        int minLength = Math.min(targetWord.length(), userInput.length());
        for (int i = 0; i < minLength; i++) {
            if (userInput.charAt(i) == targetWord.charAt(i)) {
                correctChars++;
            } else {
                errorCounts.merge(targetWord.charAt(i), 1, Integer::sum);
            }
        }
        
        // Add to totals
        totalCorrectChars += correctChars;
        totalTypedChars += totalChars;

        // Update word statistics
        if (correctChars == targetWord.length() && userInput.length() == targetWord.length()) {
            correctWords++;
            wordTimes.add(System.currentTimeMillis() - testStartTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
        }

        // Move to next word
        currentWordIndex++;
        if (currentWordIndex < words.length) {
            updatePromptDisplay();
        } else {
            finishTest();
        }
        
        // Update all statistics
        updateDetailedStats();
        calculateStats();
        updateProgress();
    }

    private void finishTest() {
        timer.stop();
        testStarted = false;
        
        // Calculate final statistics
        int currentWpm = Integer.parseInt(wpmLabel.getText());
        double accuracy = Double.parseDouble(accuracyLabel.getText().replace("%", ""));
        
        // Add to high scores
        ScoreRecord record = new ScoreRecord(currentWpm, accuracy, 
            difficultyComboBox.getValue(), testModeComboBox.getValue());
        addHighScore(record);
        
        // Update best WPM if current score is higher
        String difficulty = difficultyComboBox.getValue();
        int bestWpm = highScores.getOrDefault(difficulty + "_" + testModeComboBox.getValue(), new ArrayList<>())
            .stream()
            .mapToInt(r -> r.wpm)
            .max()
            .orElse(0);
            
        if (currentWpm > bestWpm) {
            updateBestWpm(difficulty);
            Platform.runLater(this::showCongratulations);
        }
        
        userInputArea.setDisable(true);
        difficultyComboBox.setDisable(false);
        testModeComboBox.setDisable(false);
        limitSpinner.setDisable(false);
        customTextArea.setDisable(false);
        practiceFocusComboBox.setDisable(false);
        
        updateStatistics();
    }

    private void showCongratulations() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Record!");
        alert.setHeaderText("Congratulations!");
        alert.setContentText("You've achieved a new best WPM score!");
        alert.show();
    }

    @FXML
    private void handleStartTest() {
        if (testModeComboBox.getValue() == TestMode.CUSTOM && customTextArea.getText().trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("No Custom Text");
            alert.setContentText("Please enter some text for the custom test mode.");
            alert.show();
            return;
        }

        userInputArea.setDisable(false);
        userInputArea.clear();
        userInputArea.requestFocus();
        currentWordIndex = 0;
        secondsElapsed = 0;
        totalCorrectChars = 0;
        totalTypedChars = 0;
        correctWords = 0;
        wordTimes.clear();
        errorCounts.clear();
        testStartTime = LocalDateTime.now();
        testStarted = true;
        updatePromptDisplay();
        progressBar.setProgress(0);
        timer.play();
        difficultyComboBox.setDisable(true);
        testModeComboBox.setDisable(true);
        limitSpinner.setDisable(true);
        customTextArea.setDisable(true);
        practiceFocusComboBox.setDisable(true);
        updateDetailedStats();
    }

    @FXML
    private void handleResetTest() {
        timer.stop();
        testStarted = false;
        secondsElapsed = 0;
        currentWordIndex = 0;
        totalCorrectChars = 0;
        totalTypedChars = 0;
        correctWords = 0;
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
        customTextArea.setDisable(false);
        practiceFocusComboBox.setDisable(false);
        updateDetailedStats();
        
        // Reset virtual keyboard
        keyboardKeys.values().forEach(key -> key.getStyleClass().removeAll("active", "correct", "error"));
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
                progress = (double) currentWordIndex / totalWords;
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
            Platform.runLater(this::finishTest);
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

    private void updateDetailedStats() {
        totalWordsLabel.setText(String.valueOf(currentWordIndex));
        correctWordsLabel.setText(String.valueOf(correctWords));
        
        double errorRate = totalTypedChars > 0 ? 
            ((double)(totalTypedChars - totalCorrectChars) / totalTypedChars) * 100 : 0;
        errorRateLabel.setText(String.format("%.1f%%", errorRate));
        
        double timePerWord = currentWordIndex > 0 ? 
            (double)secondsElapsed / currentWordIndex : 0;
        timePerWordLabel.setText(String.format("%.1fs", timePerWord));
    }

    private void setupPracticeFocus() {
        practiceFocusComboBox.getItems().addAll(PracticeFocus.values());
        practiceFocusComboBox.setValue(PracticeFocus.COMMON_WORDS);
        practiceFocusSection.setVisible(false);
    }

    private void updatePracticeText(PracticeFocus focus) {
        currentPrompt = focus.practiceText;
        words = currentPrompt.split("\\s+");
        totalWords = words.length;
        if (!testStarted) {
            currentWordIndex = 0;
            updatePromptDisplay();
        }
    }

    private void loadHighScores() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("highscores.dat"))) {
            highScores = (Map<String, List<ScoreRecord>>) ois.readObject();
        } catch (Exception e) {
            System.out.println("No high scores found. Starting fresh.");
            highScores = new HashMap<>();
        }
    }

    private void saveHighScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("highscores.dat"))) {
            oos.writeObject(highScores);
        } catch (IOException e) {
            System.err.println("Error saving high scores: " + e.getMessage());
        }
    }

    private void addHighScore(ScoreRecord record) {
        String key = record.difficulty + "_" + record.mode;
        highScores.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        highScores.get(key).sort((a, b) -> Integer.compare(b.wpm, a.wpm));
        if (highScores.get(key).size() > 10) {
            highScores.get(key).subList(10, highScores.get(key).size()).clear();
        }
        saveHighScores();
        updateStatistics();
    }

    private void updateStatistics() {
        StringBuilder stats = new StringBuilder("High Scores:\n\n");
        highScores.forEach((key, scores) -> {
            stats.append(key).append(":\n");
            scores.forEach(score -> stats.append(score.toString()).append("\n"));
            stats.append("\n");
        });

        // Add error analysis
        stats.append("\nError Analysis:\n");
        errorCounts.entrySet().stream()
            .sorted(Map.Entry.<Character, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> stats.append(String.format("'%c': %d errors\n", entry.getKey(), entry.getValue())));

        // Add timing analysis
        if (!wordTimes.isEmpty()) {
            double avgTime = wordTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            stats.append(String.format("\nAverage time per word: %.2f seconds\n", avgTime / 1000.0));
        }

        statisticsArea.setText(stats.toString());
    }
} 