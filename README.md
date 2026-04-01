# SmartFileManager

A complete Android file manager app built in Java with a white, minimalist UI.

---

## Setup & Push to GitHub

### Step 1: Create the GitHub repository

Go to https://github.com/new and create a **public** repository named `smart-file-manager-app`.  
Do **not** initialize it with a README or .gitignore.

### Step 2: Set your GitHub token

In your terminal (or wherever you're pushing from), set your token as an environment variable:

```bash
export GITHUB_TOKEN=your_personal_access_token_here
```

To create a token: GitHub → Settings → Developer settings → Personal access tokens → Generate new token  
Required scope: `repo`

### Step 3: Initialize and push the repo

```bash
cd smart-file-manager-app
git init
git add .
git commit -m "Initial commit: SmartFileManager"
git branch -M main
git remote add origin https://x-access-token:${GITHUB_TOKEN}@github.com/YOUR_USERNAME/smart-file-manager-app.git
git push -u origin main
```

Replace `YOUR_USERNAME` with your actual GitHub username.

---

## GitHub Actions — Automated APK Build

The workflow file `.github/workflows/android-build.yml` automatically:
- Triggers on every push to `main` or `master`
- Sets up JDK 17
- Sets up Android SDK
- Builds the debug APK via `./gradlew assembleDebug`
- Uploads the APK as a downloadable artifact

### How to download the APK

1. Go to your repo on GitHub
2. Click the **Actions** tab
3. Click the latest workflow run
4. Scroll down to **Artifacts**
5. Download `SmartFileManager-debug`
6. Extract the zip — the `.apk` file is inside
7. Transfer to your Android device and install (enable "Install from unknown sources" in Settings)

---

## Features

- Browse all files and folders
- Fast file search across entire storage
- Sort by name, size, or date
- File operations: copy, move, delete, rename
- Show/hide hidden files toggle
- Storage usage analytics with pie chart
- APK installer support
- File sharing via intent
- Recent files section
- Favorites system

## Tech Stack

- **Language**: Java
- **Min SDK**: 24 (Android 7.0)
- **Build**: Gradle 8.4
- **UI**: Material Components, RecyclerView, CardView
- **Chart**: MPAndroidChart (via JitPack)
- **Architecture**: MVVM with LiveData + ViewModel
