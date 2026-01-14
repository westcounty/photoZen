# PicZen V1.1 PRD: Flow & Achievement

| Document Info | Details |
| :--- | :--- |
| **Version** | V1.1.0 (Flow & Achievement) |
| **Status** | **Ready for Development** |
| **Base Version** | V1.0 (MVP) - Basic Swiping & Virtual Crop implemented |
| **Primary Goal** | Transform the utility tool into a **Gamified Workflow** with Visual Feedback. |

---

## 1. Core Concept: The "Flow" Tunnel

**Problem:** V1.0 features (Swipe, Compare, Tag) are disconnected.
**Solution:** Create a linear, game-like tunnel that guides the user from start to finish without distraction.

### 1.1 The New Home Dashboard
*   **UI Change:** Replace the simple Grid List with a **"Task Board"**.
*   **Logic:**
    *   Scan all albums.
    *   If an album has > 0 photos with `status == UNSORTED`, display it as a "Mission Card".
    *   **Card UI:** Show Album Cover, Name, and a Progress Bar (e.g., "50/200 sorted").
    *   **Action:** Big "Start Flow" button on the card.

### 1.2 The Tunnel Sequence
When "Start Flow" is clicked, enter **Immersive Mode** (Hide Bottom Navigation):
1.  **Stage 1: Swipe (The Filter)** -> User swipes all unsorted photos.
2.  **Stage 2: Light Table (The Decision)** -> Auto-navigate here if `Maybe` list > 0.
3.  **Stage 3: Tagging (The Organization)** -> Auto-navigate here to tag `Keep` photos.
4.  **Stage 4: Victory (The Reward)** -> Show the Result Screen.

---

## 2. Feature: Gamification & Juice (The "Feel")

**Goal:** Make the boring task of sorting addictive.

### 2.1 Combo System (In-Swipe)
*   **Logic:**
    *   Track the time interval between swipes.
    *   If interval < 1.5s, increment `comboCounter`.
    *   If interval > 1.5s, reset `comboCounter` to 0.
*   **Visuals:**
    *   Display a dynamic text overlay (e.g., "x5", "x10") near the card.
    *   Scale the text size slightly with higher combos.
    *   Color shift: White (x1-x9) -> Orange (x10-x19) -> Red/Fire (x20+).
*   **Haptics:**
    *   Use `VibrationEffect.createOneShot`.
    *   Increase duration/amplitude slightly as combo increases (Max 50ms).

### 2.2 Victory Screen (Result)
*   **UI:** A full-screen celebration page.
*   **Content:**
    *   **Stats:** "Sorted 200 photos", "Time: 10m", "Max Combo: x50".
    *   **Visual Reward:** An animation of a Polaroid photo developing, showing one of the "Keep" photos.
    *   **Action:** "Back to Home" or "Share Report".

---

## 3. Feature: Smart Hierarchy & Bubble Graph

**Goal:** A fun, intuitive way to view categories (Tags) instead of a boring list.

### 3.1 Data Structure Update
*   **Entity:** Update `TagEntity` to include `parentId` (Long, nullable).
*   **Logic:**
    *   **Parent Tag:** e.g., "2026 Yunnan Trip".
    *   **Child Tag:** e.g., "Food", "Scenery", "People".

### 3.2 The Bubble Graph UI (Physics Layout)
*   **Tech:** Custom Jetpack Compose Layout using `Canvas` or `Layout`.
*   **Physics Simulation (Simplified):**
    *   **Center Node:** The Parent Tag (Fixed in center).
    *   **Child Nodes:** Floating around the center.
    *   **Spring Force:** Child nodes are pulled towards the center.
    *   **Repulsion Force:** Child nodes push each other away to prevent overlap.
*   **Interaction:**
    *   **Drag:** User can drag a bubble; on release, it springs back.
    *   **Click:** Clicking a bubble navigates to the Photo Grid for that tag.
    *   **Drop Zone:** (Advanced) Drag a photo from a bottom strip onto a bubble to tag it.

---

## 4. Feature: Trajectory Map (Osmdroid)

**Goal:** Visualize the trip path for a specific Collection.

### 4.1 Tech Stack
*   **Library:** `org.osmdroid:osmdroid-android`.
*   **Source:** OpenStreetMap (No API Key required).

### 4.2 Logic
1.  **Exif Extraction:**
    *   Create a `WorkManager` job: `LocationScannerWorker`.
    *   Run in background.
    *   Read `ExifInterface.TAG_GPS_LATITUDE` / `LONGITUDE`.
    *   Update `PhotoEntity` columns: `lat`, `lng`.
2.  **Map Rendering:**
    *   Filter photos by the current Tag/Collection.
    *   Sort by `dateTaken`.
    *   **Polyline:** Draw a line connecting the coordinates in time order (Color: Accent Color).
    *   **Markers:**
        *   Do not show ALL markers (too crowded).
        *   Show a marker for every Nth photo or significant distance change (> 500m).
        *   **Icon:** A small circular thumbnail of the photo.

---

## 5. Technical Implementation Guidelines (For AI)

### 5.1 Architecture
*   **ViewModel:**
    *   `WorkflowViewModel`: Manages the state machine of the Tunnel (Swipe -> Compare -> Result).
    *   `MapViewModel`: Handles async loading of GPS data and Polyline generation.
*   **Canvas Physics:**
    *   Create a class `BubblePhysicsState`.
    *   Use `Animatable` for position updates.
    *   Run the physics loop in a `LaunchedEffect` but throttle it to 60fps.

### 5.2 Performance Constraints
*   **Map:** Osmdroid can be memory heavy. Ensure `MapView` lifecycle methods (`onResume`, `onPause`) are called correctly in Compose `DisposableEffect`.
*   **Bubble Graph:** Do not use complex physics engines (like JBox2D) if not necessary. A simple Hooke's Law (Spring) implementation is sufficient and lighter.

### 5.3 Code Style
*   Use **Material 3** components.
*   Use **Kotlin Flow** for all data observation.
*   Keep the `PhotoEntity` migration safe (provide default nulls for new fields).

---

## 6. Implementation Phases (Summary)

1.  **Data Layer Upgrade:** Add `lat/lng` to DB, implement Exif Scanner.
2.  **Home & Workflow:** Refactor Home to Task Board, build the Tunnel logic.
3.  **Gamification:** Add Combo counter and Haptics.
4.  **Map Module:** Integrate Osmdroid and Polyline logic.
5.  **Bubble UI:** Implement the physics canvas.