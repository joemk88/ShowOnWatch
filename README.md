# Show on watch

Share any image or video from your phone (e.g. a Bigme HiBreak) to your Pixel Watch
(or any Wear OS 3+ watch), where it opens full-screen in colour.

**Phone:** long-press an image (or share a video) → Share → **Show on watch**
**Watch:** wakes and opens the picture. Pinch or turn the crown to zoom, drag to pan,
double-tap to toggle zoom, swipe right (at the left edge) to close.
Videos play muted on a loop; tap for sound, and zoom/pan them the same way as photos (up to 4×). Reopening the watch app shows the last item sent.

## Changelog
- **1.2** – pinch, crown and double-tap zoom for videos (TextureView, GPU-scaled); zoom
  gestures shared between photos and videos (`ZoomController`).
- **1.1** – video support: shrunk on the phone to ≤640 px, ≤30 fps, ~1 Mbps H.264, first 60 s,
  then streamed over a Wear channel. Sending now runs in a short foreground service with a
  progress notification, so the phone stays usable.
- **1.0** – images.

## How it works
- `mobile/` – a share target that hands off to `SendService`.
  - Images: downscaled to ≤1280 px JPEG and sent as an Asset through the Wear Data Layer.
  - Videos: converted with Media3 Transformer (`VideoShrinker`), then streamed over
    `ChannelClient` on path `/video/<id>/<bytes>`.
  - Either way the viewer is opened on the watch with `RemoteActivityHelper`.
- `wear/` – `ImageListenerService` saves the incoming item (videos are checked against the
  expected byte count); `ViewerActivity` shows images with pinch/crown zoom and plays videos
  with `ZoomVideoView` (MediaPlayer + TextureView), so the watch app has no extra libraries.

Both apps share the applicationId `io.github.joemk88.showonwatch` and must be signed
with the same key (debug builds from one PC already are).

## Install
1. Open this folder in Android Studio and let Gradle sync.
2. Phone: plug in (or wireless-debug) the phone, pick the **mobile** run config, Run.
3. Watch: on the Pixel Watch enable Developer options → Wireless debugging.
   In Android Studio use *Device Manager → Pair devices using Wi-Fi* (or
   `adb pair <ip>:<port>` then `adb connect <ip>:<port>`).
   Pick the **wear** run config, select the watch, Run.
