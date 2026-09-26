# PartyPortrait

A fullscreen **party photo booth** tool: it takes pictures from a camera attached to the computer (triggered by a USB foot switch) and shows the photos in a random, ever-changing collage — perfect for parties and events.

## Features

- 📷 **Take photos** via `gphoto2` from any camera supported by it
- 🦶 **Foot switch trigger** — a USB pedal (any device emitting a key press) takes the photo
- 🖼️ **Live photo screensaver** — idle photos are displayed in a shuffled 3×3 collage that fills cell by cell
- 🎲 **Random full-screen photo** — occasionally a photo is shown full screen instead of the collage
- 🎉 **Fun prompts** — shows a "smile!" message while the photo is being taken
- 🔁 **Auto-restart** — the `start` script relaunches the app if it exits unexpectedly
- 🚪 **Easy exit** — click anywhere with the mouse or press `e`

## How It Works

Single Java Swing `JFrame` running fullscreen with three display states:

| State | Description |
|-------|-------------|
| `MESSAGE_DISPLAY` | Shows a message (e.g. *"Lächle!"* while the photo is taken, or an error) |
| `FULL_PHOTO` | Shows a single photo scaled to the screen width |
| `RANDOM_COLLAGE` | Idle screensaver: fills a 3×3 grid with randomly picked, randomly positioned photos |

**Photo flow:** any key press (foot pedal) stops the screensaver → `gphoto2 --capture-image-and-download` saves the image to `photos/` → the new photo is shown fullscreen → the screensaver resumes with the updated collection.

Photos are stored as `photos/yyyy_MM_dd_HHmmss.jpg`.

## Requirements

- **JDK 1.8 or later** (works with newer JDKs too)

  ```sh
  apt install openjdk-25-jdk
  ```

- **gphoto2** — for camera control

  ```sh
  apt install gphoto2
  ```

- A camera supported by gphoto2, connected via USB (my setup: old Nikon D60)
- A USB foot switch (my setup: FS1-P) configured to emit a key press
- An X11/display environment (Java Swing fullscreen)

## Build

```sh
./compile
```

Compiles `src/PartyPictures.java` into `bin/`.

## Run

```sh
./start
```

Starts the app in a loop, restarting it after 5 seconds if it exits.

## Usage

1. Connect the camera via USB and the USB foot switch
2. Run `./start`
3. Step on the pedal to take a photo — watch for the *"Lächle!"* prompt
4. Photos appear in `photos/` and in the ongoing collage

**Controls**

| Action | Effect |
|--------|--------|
| Any key press (foot pedal) | Take a photo |
| Mouse click | Quit |
| `e` key | Quit |

## Configuration

Hard-coded in `src/PartyPictures.java`:

| Setting | Default | Description |
|---------|---------|-------------|
| `FULLSCREEN` | `true` | Fullscreen mode; `false` shows a 600×400 window |
| `fileNamePatter` | `yyyy_MM_dd_HHmmss` | Photo filename timestamp pattern |
| `fileExt` | `.jpg` | Photo file extension |
| `M_LAUGH` / `M_WAIT` | German | Photo prompt messages (Italian and Croatian variants commented in the source) |
| `saver` dimensions | 3×3 | Collage grid size |
| `saverTimer` interval | 2000 ms | Collage update speed |

## Project Layout

```
PartyPortrait/
├── src/PartyPictures.java   # Single-file application source
├── bin/                     # Compiled classes
├── photos/                  # Captured photos (runtime output)
├── compile                  # Build script
├── start                    # Run script (auto-restart loop)
└── README.md
```