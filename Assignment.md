🎯 Assignment Title:
"AR Placement App for Android"
📌 Objective
Build a minimal AR app that lets the user:
1. Select a drill from a list on the UI
2. Tap on the ground (detected plane) to place the drill marker (object) in AR
   🧱 Core Features to Implement
1. Basic UI – Drill Selector
   ● A simple page or overlay with:
   ○ Dropdown or list of 2–3 “drills” (e.g., Drill 1, Drill 2, Drill 3)
   ○ Open Drill specific page, with Dummy Data with display image, description and
   tips sections
   ○ Button: Start AR Drill
2. AR Scene – Tap to Place Drill Object
   ● Once drill is selected and AR starts:
   ○ Detect horizontal plane (floor)
   ○ On tap, place a 3D object (can be a colored cube or cone) representing the drill
   ○ Only allow one object placed at a time (optional)
   🖼 UI Flow Overview
   Page 1 – Drill Selection
---------------------------------------
| Select Drill: [Dropdown] |
| |
| [Start AR Drill] (button) |
---------------------------------------
Page 2 – AR View
[Live camera feed with plane detection]
Instructions:
"Tap on ground to place drill marker"
(When tapped → place a small cone or cube on tap location)
💡 Implementation Tips
● Use ARCore (Android)
● 3D object can be a simple colored cube or cone
● Mock 2-3 drill names for selection (you don’t need real models)
📁 Expected Output
● Working mobile app or APK file
● Code (shared via GitHub or ZIP)
● Short README on how to run