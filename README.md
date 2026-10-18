# SocialDesk Android — GitHub APK build

This Android app includes a local Page pool organizer, Facebook post-link queue, opening saved posts in Facebook/browser, status tracking, and local persistence.

**Scope:** This is a working local organizer, not a live Meta integration. It does not automate reactions, evade Meta safeguards, publish to Facebook, or manage inboxes. Those features require an approved Meta API integration and secure backend.

## Build APK on GitHub (no Android Studio required)
1. Extract this ZIP.
2. Create a new GitHub repository.
3. Upload the *contents* of the `SocialDeskAndroid` folder to the repository root. Make sure `.github/workflows/build-apk.yml` is included.
4. Commit to `main`. Open the **Actions** tab and select **Build SocialDesk Android APK**.
5. Wait for a green check. Open that run and find **Artifacts**.
6. Download `SocialDesk-debug-apk` and extract it. It contains `app-debug.apk`.
7. Transfer `app-debug.apk` to your Android phone and install it. If Android blocks installation, enable permission to install unknown apps for the app you used to open the APK, then install.

If GitHub's web uploader does not show `.github`, create the workflow manually using **Add file → Create new file** and name it `.github/workflows/build-apk.yml`, then paste in the included workflow.

## App use
- **Home:** summary and recent links.
- **Post links:** save a Facebook post URL, open it, and update the review status.
- **Page pool:** locally save Pages you manage.
- **Guide:** limitations and privacy notes.

No Facebook password or access token is required or collected. Data is stored on the phone. This starter release does not yet connect to Meta's API.
