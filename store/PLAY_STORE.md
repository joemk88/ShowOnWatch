# Publishing Show on Watch on Google Play

Everything to copy into the Play Console, in the order you'll need it.
Graphics are in this folder: `play-icon-512.png` and `feature-graphic-1024x500.png`.

---

## 1. Decide on the app signing key (before your first upload)

Phone and watch apps must be signed with the same key. Play manages this for you, but on the
first upload it asks which key to use:

- **Recommended: use your existing release key.** In the release page, choose
  *Change app signing key → Export and upload a key from Java keystore* (or tick
  *Export encrypted key for enrolling published apps in Google Play App Signing* in Android
  Studio's Generate Signed Bundle dialog, which produces a `.pepk` file to upload).
  Play-installed and GitHub-installed copies then stay compatible: people who sideloaded can
  update from Play without uninstalling, and you can keep posting APKs on GitHub.
- **Or let Google generate a new key.** Simpler, but Play copies and GitHub APKs will no longer
  talk to each other or update over one another, so you'd stop publishing APKs on GitHub.

This can't be changed after the first upload, so pick before step 3.

---

## 2. Create the app

Play Console → **Create app**

| Field | Value |
|---|---|
| App name | `Show on Watch` |
| Default language | English (Australia) – en-AU |
| App or game | App |
| Free or paid | Free |

Package name is set by your upload: `io.github.joemk88.showonwatch` (permanent).

---

## 3. Build the release bundles

Play needs **Android App Bundles (.aab)**, not APKs.

Android Studio → **Build → Generate Signed App Bundle or APK → Android App Bundle**

1. Module **mobile**, your keystore, build variant **release** → `mobile/release/mobile-release.aab`
2. Repeat for module **wear** → `wear/release/wear-release.aab`

Version codes are already set up for Play: phone `4`, watch `1004`. For each future release,
bump both (e.g. phone `5`, watch `1005`) and both version names.

---

## 4. Store listing

**Main store listing**

**App name** (max 30)
```
Show on Watch
```

**Short description** (max 80)
```
See photos and videos from your e-ink phone in full colour on your Wear OS watch
```

**Full description** (max 4000)
```
Love your e-ink phone but miss seeing photos in colour? Show on Watch sends any picture or video from your phone to your Wear OS watch, where it opens full-screen in vivid colour.

Long-press an image in your browser, or share a photo or video from your gallery, and pick Show on Watch. Your watch wakes up and shows it straight away.

PERFECT FOR SHOWING PEOPLE
Double-tap the watch to turn the picture upside down, then simply turn your wrist towards the person you're talking to. The watch buzzes to confirm.

PHOTOS
• Opens instantly, full-screen and in colour
• Pinch or turn the crown to zoom up to 8×, drag to look around

VIDEOS
• Plays on a loop, muted until you tap for sound
• Zoom and pan just like photos
• Shrunk on your phone before sending, so it arrives quickly (first 60 seconds)

PRIVATE BY DESIGN
• No accounts, no ads, no analytics, no tracking
• Pictures go directly from your phone to your own watch, never to a server
• The watch keeps only the most recent item

REQUIREMENTS
• An Android phone with Google Play services
• A Wear OS 3 or newer watch paired to that phone
• Install Show on Watch on both the phone and the watch

Built for e-ink phones, where photos are hard to make out, but it works with any Android phone.

Free and open source: github.com/joemk88/ShowOnWatch
```

**Graphics**
- App icon: `play-icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots: 2 to 8, portrait. From the HiBreak: the share sheet showing
  "Show on watch", the app's main screen, and the "Sending to watch…" notification.
- Wear OS screenshots: at least 1, square (1:1), no device frame, no round mask.
  Take them on the watch (press the crown and side button together, after turning on
  Settings → Gestures → Screenshots), or in Android Studio's Running Devices window with
  **Take screenshot → Play Store Compatible**. Good ones: a photo, a zoomed-in photo, a video,
  and the upside-down view.

**Store settings**
- Category: **Tools** (or Photography)
- Tags: Tools, Photography
- Contact email: the public email for your developer account
- Website: `https://github.com/joemk88/ShowOnWatch`

---

## 5. App content (Policy → App content)

**Privacy policy URL**
```
https://github.com/joemk88/ShowOnWatch/blob/master/PRIVACY.md
```
(Push `PRIVACY.md` first so the link works.)

**Ads:** No, the app does not contain ads.

**App access:** All functionality is available without special access.
Add this note:
```
No login is needed. The app requires a Wear OS watch paired to the phone. Install the phone app and the Wear OS app, then share any image or video to "Show on watch" from the share menu, or open the phone app and tap "Pick a photo or video to send".
```

**Content rating:** Category *All other app types*. Answer **No** to every question
(no violence, sexuality, user interaction, location sharing, purchases, etc.). Expected rating:
Everyone / PEGI 3.

**Target audience:** 18 and over. (Avoids the extra Families-policy requirements. Choosing
13+ also works.) Not designed for children.

**News app:** No. **Health apps:** No. **Financial features:** None. **Government app:** No.

**Data safety**
- Does your app collect or share any of the required user data types? **No.**
- Reason: the app sends nothing to the developer or any server. Items go directly between the
  user's own phone and watch through Google Play services, and the watch stores only the latest
  item locally.

**Foreground service permissions** (the Console asks because the phone app targets Android 14+)
- Type: **Data sync**
- Use case: **Network transfer: upload or download** (closest match; the user starts every
  transfer)
- Description:
```
When the user shares a photo or video to "Show on watch", the app shrinks it and sends it to the user's paired Wear OS watch. A video can take up to a minute to convert and send, so this runs in a short foreground service with a progress notification. It starts only from the user's share action and stops as soon as the item has been sent.
```
- Impact if deferred or interrupted:
```
The photo or video the user just chose would not reach the watch, and the user would have to share it again.
```
- Video link: a short screen recording (Settings → Screen recorder on the phone, or film the
  phone and watch together) showing: share a video → pick Show on watch → "Sending…"
  notification → it plays on the watch. Upload as an **unlisted** YouTube video and paste the link.

---

## 6. Add Wear OS

1. **Test and release → Advanced settings → Form factors → Add form factor → Wear OS.**
2. Add the Wear OS screenshots to the listing.
3. Wear OS gets its own release track. Upload `wear-release.aab` to the **Wear OS** closed
   testing track (same testers as the phone).
4. Back in **Advanced settings → Form factors → Wear OS**, opt in and accept the Wear OS review
   policy.

---

## 7. Closed test (required before going public)

Because the account is a new personal account, Play requires a closed test with **at least 12
testers who stay opted in for 14 days in a row** before you can apply for production.

1. **Test and release → Testing → Closed testing → Create track** (or use the default
   *Alpha*).
2. Testers: create an email list, or point it at a Google Group so people can join themselves.
3. Upload `mobile-release.aab` to this track, and `wear-release.aab` to the Wear OS closed track.
4. Send testers the **opt-in link**. They join, then install from the Play Store on their phone.
   The watch app then appears in the Play Store on their watch under *Apps on your phone*.
5. After 14 days with 12+ testers, go to **Dashboard → Apply for production** and answer the
   questions about your test. Approval usually takes a few days.

Recruiting testers: a post on r/Bigme asking for 12 people with a Wear OS watch works well.
Ask them to stay opted in for the full 14 days. Uninstalling is fine; opting out restarts the clock.

---

## 8. Go live

Production → Create release → add both bundles (phone, and Wear OS on its Wear track) → roll
out. Then update the README's Download section to link to the Play Store.
