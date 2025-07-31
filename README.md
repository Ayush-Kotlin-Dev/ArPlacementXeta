# AR Placement App for Android

An Augmented Reality app that allows users to select drills and place them in AR space using ARCore.

## Features

- **Drill Selection**: Browse and select from different types of drills
- **Drill Details**: View detailed information about each drill including images, descriptions, and
  tips
- **AR Placement**: Use your device camera to detect horizontal surfaces and place drill markers in
  3D space
- **Interactive AR**: Tap to place colored cylinder objects representing different drill types

## How to Run

### Prerequisites

- Android device with ARCore support
- Android 7.0 (API level 24) or higher
- Camera permission granted

### Installation

1. Clone or download the project
2. Open in Android Studio
3. Build and run on an ARCore-supported device:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

### Using the App

1. **Drill Selection Screen**:
    - Browse the list of available drills
    - Tap on any drill to view its details

2. **Drill Detail Screen**:
    - View drill specifications, description, and tips
    - Tap "Start AR Placement" to enter AR mode

3. **AR Placement Screen**:
    - Point your camera at a flat horizontal surface (floor, table, etc.)
    - Wait for surface detection (white dots will appear)
    - Tap on the detected surface to place a colored cylinder representing the drill
    - Different drill types are represented by different colors:
        - Drill 1: Blue
        - Drill 2: Red
        - Drill 3: Green
    - Tap elsewhere to move the drill marker to a new location

## Technical Implementation

- **UI Framework**: Jetpack Compose with Material Design 3
- **AR Framework**: ARCore with SceneView library
- **3D Rendering**: Google Filament engine
- **Architecture**: MVVM pattern with Compose Navigation
- **Language**: Kotlin with Coroutines and Flow

## Project Structure

```
app/src/main/java/com/xeta/arplacement/
├── data/                 # Data models
├── ui/
│   ├── screens/         # Compose screens
│   └── theme/           # Material Design theme
└── MainActivity.kt      # Main entry point
```

## Key Components

- **ARScreen.kt**: Handles AR camera, plane detection, and 3D object placement
- **DrillSelectionScreen.kt**: Lists available drills with navigation
- **DrillDetailScreen.kt**: Shows drill details and launches AR mode
- **Drill.kt**: Data model for drill information

## Dependencies

- ARCore SDK
- SceneView library for AR rendering
- Jetpack Compose for UI
- Navigation Compose for screen navigation
- Material Design 3 components

## Troubleshooting

- **App crashes on launch**: Ensure device supports ARCore
- **Camera not working**: Check camera permissions in device settings
- **No plane detection**: Try pointing camera at a well-lit, textured flat surface
- **Objects not placing**: Ensure surface is detected (look for white dots) before tapping