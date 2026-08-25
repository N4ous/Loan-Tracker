# Loan Tracker — Phone-only build

This is a native Android Loan Tracker starter app.

## What is included
- Offline local storage
- Add loans
- Record payments
- Automatic paid/remaining calculation
- Loan detail/history screen
- Dashboard totals
- Delete loan

## Build the APK using only your phone

### Method: GitHub + GitHub Actions

1. On your phone, open GitHub in Chrome and create a new repository named `LoanTracker`.
2. Upload all files/folders from this project.
3. The included workflow will build the debug APK automatically.
4. Open the repository's **Actions** tab.
5. Open the latest successful workflow run.
6. Download the `loan-tracker-apk` artifact.
7. Extract the ZIP and install `app-debug.apk` on your Android phone.

If Android asks, allow installation from the browser/file manager you used.

## Important
This first build is Version 1. It intentionally keeps the database simple so it is easy to build from a phone. Before using it as your only financial record, add a backup/export feature. Local app data can be lost if the app is uninstalled or the phone is reset.

## Future upgrades
- PIN / biometric lock
- CSV/Excel backup
- PDF loan statement
- Automatic installment schedule
- Payment reminders
- Principal vs interest breakdown
- Attach receipt photos
- Search/filter
- Backup/restore
- Dark mode
