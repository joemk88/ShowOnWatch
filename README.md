# Show on watch

View photos and videos from your e-ink Android phone in full colour on your Wear OS watch.

Built for e-ink phones like the Bigme HiBreak, where pictures are hard to make out, but it
works with any Android phone. Long-press an image or share a video, pick **Show on watch**,
and it opens on your wrist.

- **Photos:** pinch or turn the crown to zoom (up to 8×), drag to pan, double-tap to toggle zoom.
- **Videos:** play muted on a loop; tap for sound. Zoom and pan work the same way (up to 4×).
- Swipe right to close. Reopening the watch app shows the last thing you sent.

## Download

Get both APKs from the [latest release](https://github.com/joemk88/ShowOnWatch/releases/latest):

| File | Install on |
|---|---|
| `ShowOnWatch-phone-<version>.apk` | your phone |
| `ShowOnWatch-watch-<version>.apk` | your watch |

**You need both**, from the **same release**. They're signed with the same key, which is how
the phone and watch apps recognise each other.

### Requirements
- An Android 9+ phone with Google Play services
- A Wear OS 3+ watch paired to that phone (Pixel Watch, Galaxy Watch 4 or later, TicWatch, OnePlus Watch, etc.)

Not supported: iPhones, and watches that don't run Wear OS (Garmin, Fitbit, Huawei, Tizen Galaxy Watches).

## Install on your phone

1. Download the phone APK on your phone and open it.
2. If asked, allow your browser or file manager to **install unknown apps**, then tap **Install**.
3. Open **Show on watch** once. Allow notifications if asked (used for progress while a video is sent).

## Install on your watch

Wear OS can't open APK files directly, so the watch APK has to be sideloaded over Wi-Fi.
Pick one of the two methods below. Either way, first set up the watch:

1. **Turn on Developer options:** on the watch, go to Settings → System → About and tap
   **Build number** seven times. (On Pixel Watch it's under About → Versions; on Galaxy Watch,
   About watch → Software information → Software version.)
2. Make sure the watch is on the **same Wi-Fi network** as your phone or PC.
3. In Settings → **Developer options**, turn on **ADB debugging** and **Wireless debugging**.
4. Tap **Wireless debugging → Pair new device**. Keep this screen open: it shows a pairing code,
   an IP address and a port.

### Option A: from your phone (no computer)

Use a sideloading app such as **Wear Installer 2** or **Bugjaeger** from the Play Store.

1. Download the watch APK on your phone.
2. In the sideloading app, enter the watch's IP address, port and pairing code from step 4 above.
3. Choose the watch APK and install it.

The exact screens differ between apps; each has its own short guide.

### Option B: from a computer with ADB

Install [Android platform-tools](https://developer.android.com/tools/releases/platform-tools), then run:

```
adb pair <watch-ip>:<pairing-port>        # enter the pairing code when asked
adb connect <watch-ip>:<port>             # the port shown on the main Wireless debugging screen
adb install ShowOnWatch-watch-<version>.apk
```

The pairing port and the connection port are different numbers; both are shown on the watch.

**Afterwards**, turn Wireless debugging off again to save battery.

## Using it

- **Browser:** long-press an image → **Share image** (or Share) → **Show on watch**.
- **Gallery / files:** open a photo or video → Share → **Show on watch**, or use **Open with**.
- **Phone app:** open Show on watch and tap **Pick a photo or video to send**.

Videos are shrunk on the phone before sending (first 60 seconds, up to 640 px, about 1 Mbps),
so a full minute is around 7–8 MB and takes a little while over Bluetooth. Videos embedded in web
pages usually share as a link, not a file, so download them first.

## Troubleshooting

| Message on the phone | What to do |
|---|---|
| **Watch not connected** | Check the watch is paired and connected in your watch companion app (Pixel Watch, Galaxy Wearable or Wear OS app). |
| **Sent – open Show on watch on the watch** | The watch app isn't installed, or it's from a different release. Install the watch APK from the same release as the phone APK. |
| **Couldn't send: …** | The file format wasn't readable. Try saving the image or video first and sharing it from your gallery. |

**Updating:** install both new APKs over the old ones. If you previously built the app yourself,
uninstall that version from the phone and the watch first, since it's signed with a different key.

## Changelog
- **1.2** – pinch, crown and double-tap zoom for videos.
- **1.1** – video support. Sending runs in the background with a progress notification.
- **1.0** – images.

## Building from source

1. Open this folder in Android Studio and let Gradle sync.
2. Run the **mobile** configuration on your phone.
3. Pair the watch for wireless debugging (*Device Manager → Pair devices using Wi-Fi*), then run
   the **wear** configuration on it.

Both modules share the applicationId `io.github.joemk88.showonwatch` and must be signed with the
same key. Debug builds from one computer already are; for releases, sign both with the same keystore.

### How it works
- `mobile/` – a share target that hands off to `SendService`.
  - Images: downscaled to ≤1280 px JPEG and sent as an Asset through the Wear Data Layer.
  - Videos: converted with Media3 Transformer (`VideoShrinker`), then streamed over
    `ChannelClient` on path `/video/<id>/<bytes>`.
  - Either way the viewer is opened on the watch with `RemoteActivityHelper`.
- `wear/` – `ImageListenerService` saves the incoming item (videos are checked against the
  expected byte count). `ViewerActivity` shows photos with `ZoomImageView` and plays videos with
  `ZoomVideoView` (MediaPlayer + TextureView). Both share their gestures through `ZoomController`.
