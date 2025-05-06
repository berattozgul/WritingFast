# Writing Speed Test Application

A JavaFX application to test and improve your typing speed and accuracy.

## Features

### Multiple Test Modes
- **Time Limit Mode**: Test your typing speed for a specific duration
- **Word Count Mode**: Practice typing a specific number of words
- **Infinite Mode**: Type continuously until you complete the text

### Difficulty Levels
- **Easy**: Simple sentences for beginners
- **Medium**: Standard pangrams and common phrases
- **Hard**: Complex pangrams with challenging words

### Real-time Statistics
- Words Per Minute (WPM)
- Accuracy Percentage
- Time Elapsed
- Progress Bar
- Best WPM Score Tracking

### Interactive Features
- Real-time character highlighting
  - Green: Correct characters
  - Red: Incorrect characters
  - Black: Characters not yet typed
- Next word preview
- Progress tracking
- Best score tracking per difficulty level

## How to Use

### Getting Started
1. Launch the application
2. Select your preferred difficulty level (Easy, Medium, Hard)
3. Choose a test mode
4. Click "Start Test" to begin

### Test Modes

#### Time Limit Mode
1. Select "Time Limit" from the mode dropdown
2. Use the spinner to set your desired duration in seconds
3. Click "Start Test"
4. Type until the time runs out

#### Word Count Mode
1. Select "Word Count" from the mode dropdown
2. Set the number of words you want to type
3. Click "Start Test"
4. Type until you reach the word count goal

#### Infinite Mode
1. Select "Infinite" from the mode dropdown
2. Click "Start Test"
3. Type until you complete all the text

### Typing Rules
- Type the text exactly as shown
- Press SPACE after each word to proceed
- Incorrect characters will be highlighted in red
- Correct characters will be highlighted in green
- The next word will be shown in gray

### Controls
- **Start Test**: Begin the typing test
- **Reset**: Clear current progress and start over
- **Backspace**: Delete previous character
- **Space**: Submit current word and move to next

## Requirements
- Java 21 or higher
- JavaFX 21

## Installation

### Using Maven
1. Clone the repository
```bash
git clone [repository-url]
```

2. Navigate to the project directory
```bash
cd WritingFast
```

3. Run the application using Maven
```bash
mvn clean javafx:run
```

## Development

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── bo/
│   │           └── writingfast/
│   │               ├── WritingFastApplication.java
│   │               └── WritingTestController.java
│   └── resources/
│       └── com/
│           └── bo/
│               └── writingfast/
│                   ├── writing-test-view.fxml
│                   └── styles.css
```

### Key Components
- `WritingFastApplication.java`: Main application class
- `WritingTestController.java`: Main controller handling test logic
- `writing-test-view.fxml`: UI layout definition
- `styles.css`: Application styling

## Tips for Improving Your Score
1. Focus on accuracy first, speed will come naturally
2. Start with Easy difficulty and progress gradually
3. Take regular breaks to avoid fatigue
4. Practice regularly with different test modes
5. Pay attention to your error patterns

## Contributing
Feel free to submit issues and enhancement requests! 