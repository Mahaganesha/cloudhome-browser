# CloudHome Browser

A minimal Android file manager for the **WD My Cloud Home**, written in Kotlin
with Jetpack Compose. It talks to the device over SMB — the same protocol Windows
and macOS use — so nothing needs to be installed or hacked on the NAS itself.

## Features

- Connect with the device's IP, your WD account credentials and a share name
- Browse folders (tap to enter, back button/up arrow to go up)
- **Sort** by name / size / date (folders always shown first)
- **Read/download** a file: tap it — saved to the app's Downloads folder
  (`Android/data/com.example.cloudhome/files/Download`)
- **Write/upload**: tap the + (upload) button and pick any file on the phone
- **Delete**: long-press a file or folder, confirm
- Credentials are remembered between launches

## Requirements

- Android Studio (Hedgehog 2023.1.1 or newer)
- JDK 17 (bundled with Android Studio)
- Phone with Android 8.0+ on the **same Wi-Fi** as the My Cloud Home

## How to run

1. Unzip this folder and open it in Android Studio (`File > Open`).
2. Let Gradle sync (it downloads dependencies on first open — takes a few minutes).
3. Connect your phone with USB debugging enabled (or use an emulator, but then
   the emulator must reach your LAN — a real phone is easier).
4. Press **Run**.

## What to enter on the connect screen

| Field | Value |
|---|---|
| Device IP | Your My Cloud Home's LAN IP, e.g. `192.168.1.50`. Find it in your router's device list, or check `\MYCLOUD-XXXX` on a Windows PC. |
| Username | Your WD account email (the one used to set up the device) |
| Password | Your WD account password |
| Share name | `Public` for the public share, or the name of a private share/user |

## Notes & ideas for next steps

- Passwords are stored in plain SharedPreferences for simplicity — switch to
  `EncryptedSharedPreferences` before sharing the app with anyone.
- SMBJ logs via SLF4J; add `org.slf4j:slf4j-android` if you want its logs in logcat.
- Possible extensions: rename/move, search, image thumbnails, multi-select,
  mDNS auto-discovery of `MYCLOUD-XXXX.local`, downloads via
  `DownloadManager` to shared storage, WorkManager for resumable transfers.

## How it works

```
UI (Compose)  ->  FileBrowserViewModel  ->  SmbRepository (SMBJ)  ->  My Cloud Home (SMB/Samba, port 445)
```

All network calls run on `Dispatchers.IO`. No storage permissions are needed:
downloads go to the app-specific directory, and uploads use the system file picker.
