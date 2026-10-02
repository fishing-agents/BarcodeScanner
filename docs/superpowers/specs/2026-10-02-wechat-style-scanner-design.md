# WeChat-style scanner on BarcodeScanner — Design

Date: 2026-10-02
Status: approved in chat, pending written-spec review

## 1. Intent

A FOSS (GPLv3, F-Droid, no Google Play Services) fork of `references/BarcodeScanner` (Atharok, `com.atharok.barcodescanner`) that scans **all QR codes and barcodes** with the feel of the WeChat/Alipay in-app scanners:

- fast full-frame scanning, several codes per frame, tap-to-pick
- auto-zoom toward codes too small/far to decode
- auto-torch in dark scenes
- AI-assisted decoding (OpenCV WeChatQRCode CNN detector + super-resolution) for hard QR codes
- WeChat Pay / Alipay codes recognized and handed off to the payment app

Ordinary QR codes and barcodes are the primary path. Payment codes are one additional result type; every existing result type (URL, Wi-Fi, contact, agenda, SMS, geo, product lookup, UPI) keeps its current behavior.

### Decisions (from brainstorming)

| Topic | Decision |
|---|---|
| Base | Fork of BarcodeScanner (Kotlin, Koin, Room, CameraX 1.6.1, minSdk 23, target 36) |
| Distribution | FOSS, GPLv3, no GMS, no ML Kit. Primary channel: an alternative F-Droid-compatible store without the strict reproducible-binary-blob policy (so committed WeChatCV model assets are acceptable there without a post-install download step) |
| Payment UX | Payment card + explicit hand-off button (no auto-launch) |
| Scan UX | WeChat-style: full frame, no ROI box, freeze + tap-to-pick on 2+ codes |
| Engine | Tiered: zxing-cpp every frame, WeChatQRCode fallback for hard QR |

### Rejected

- `references/Privacy-QR-And-Barcode-Scanner`: Java, a single 573-line `ScanFragment.java` that does everything, full-frame decode with up to 5 rotate+decode passes per frame, no inverted-code support, shared non-thread-safe `MultiFormatReader`, no tests. Only its tap-to-focus (`FocusMeteringAction`) idea is reused.
- ML Kit (needs GMS; unavailable to F-Droid builds and mainland-China users).
- WeChatQRCode-only (QR only, slowest per frame).

## 2. Verified external facts

- zxing-cpp Android: `io.github.zxing-cpp:android:3.1.1` on Maven Central, Apache-2.0. `zxingcpp.BarcodeReader.read(ImageProxy)` reads the Y plane `ByteBuffer` with `rowStride`, so there is no copy. `Options` include `formats`, `tryHarder`, `tryRotate`, `tryInvert`, `tryDownscale`, `maxNumberOfSymbols`, `returnErrors`. `Result` has `text`, `bytes`, `format`, `position` (4 points), `error`.
- OpenCV official AAR `org.opencv:opencv:4.14.0` (Maven Central, 123 MB) contains **no** `wechat_qrcode` classes (confirmed: downloaded the AAR, grepped `classes.jar`, 0 matches). A custom contrib build is required.
- `cv::wechat_qrcode::WeChatQRCode::detectAndDecode` (opencv_contrib `modules/wechat_qrcode/src/wechat_qrcode.cpp`, `4.x` branch, read directly) is the **only public entry point**. Internally it calls private `Impl::detect` (CNN candidate boxes via `SSDDetector::forward`) then private `Impl::decode`, and only returns `points` for boxes that reached a successful decode — undecodable candidates are dropped before they reach the caller. `SSDDetector` lives at `modules/wechat_qrcode/src/detector/ssd_detector.hpp`, a module-private header (`src/`, not the installed `include/opencv2/`), not an unexported symbol issue — since OpenCV+contrib are built from source in this project, our own JNI translation unit can add an include path into that `src/` directory and call `SSDDetector::forward` directly to get detected-but-undecoded candidate quads. No upstream patch needed, but this is non-standard API use that must be pinned to the exact contrib tag we build against (signature/behavior can change between releases).
- With empty model paths, `WeChatQRCode`'s fallback is not a useful auto-zoom source: `Impl::detect` without a CNN detector returns one box covering the *entire frame*, and `Impl::decode` skips the super-resolution step. Degraded mode (no models) therefore loses WeChatQRCode's candidate-quad signal entirely; auto-zoom in that mode relies only on zxing-cpp `returnErrors` positions.
- **Confirmed**: ZXing release notes (github.com/zxing/zxing/releases/tag/zxing-3.4.0): "Requires Java 8+. Android apps using this version must target API 24 or higher." BarcodeScanner's `minSdk 23` is below that floor. **Decision: stay on ZXing core 3.3.3** (generation, `ResultParser`, Java fallback only — zxing-cpp 3.1.1 carries all live decoding, so this costs nothing). Do not bump past 3.3.3 without also raising `minSdk` to 24, which is out of scope for this change.

## 3. Architecture

Paths below are relative to `app/src/main/java/com/atharok/barcodescanner/`.

### 3.1 Decode layer — `domain/library/scan/` (pure Kotlin + JNI, no UI)

| Unit | Responsibility |
|---|---|
| `ScanOutcome(codes: List<Result>, candidates: List<Array<ResultPoint>>)` | Per-frame result, reusing `com.google.zxing.Result`/`ResultPoint`/`BarcodeFormat` directly (**DRY correction over the original design**: no parallel `DecodedCode`/`Quad`/`LumaFrame` wrapper types — both decode libraries already accept `ImageProxy`/`Bitmap` natively, and the whole existing UI/history/intent pipeline already consumes `Result`/`BarcodeFormat`, so reusing them needs zero changes to that pipeline). `candidates` = detected but undecoded symbols |
| `interface ScanEngine { fun scan(image: ImageProxy): ScanOutcome; fun scan(bitmap: Bitmap): ScanOutcome }` | Engine contract |
| `ZxingCppEngine` | `maxNumberOfSymbols = 8`, `tryInvert = true`, `tryRotate = true`, `returnErrors = true`. **Formats restricted at the reader level** to `ZxingCppFormatMapper.SUPPORTED` — the subset of zxing-cpp's `Format` that has a `BarcodeFormat` counterpart (ZXing core 3.3.3 has no `MICRO_QR_CODE`/`RMQR_CODE`/GS1 DataBar family/etc). This is a decoder-option choice, not a post-hoc filter, so zxing-cpp never spends a decode pass on a format this app can't represent downstream (`Barcode`/Room/`BarcodeType` all key off `BarcodeFormat`). Results with `error != null` become `candidates` |
| `WeChatQrEngine` | JNI wrapper. Calls `WeChatQRCode::detectAndDecode` for decoded QR text+points, **and** separately calls `SSDDetector::forward` (via the module-internal header, §2) to get detected-but-undecoded candidate quads for auto-zoom — `detectAndDecode` alone drops those. QR only. Models load lazily on a background thread from assets copied to `noBackupFilesDir`. Without models, candidate quads degrade to a single full-frame box (not useful for zoom, §2) — treat as "no candidates" in that mode |
| `TieredScanEngine(primary, fallback)` | Runs zxing-cpp. If nothing decoded, runs WeChatQRCode **synchronously on the caller's thread**: for camera frames at most once every 2 missed frames while a candidate is visible (every 10 otherwise); for bitmaps (gallery/share) always. Synchronous because the camera `ImageProxy` is closed as soon as `analyze()` returns (no handing it to another thread) and the bitmap path has no "next scan" to receive a deferred result. `STRATEGY_KEEP_ONLY_LATEST` drops stale frames while the fallback runs. If the fallback is unavailable, it degrades to primary-only |

`meanLuma` and candidate quads are reported every frame via a single callback, independent of whether a code decoded — `AutoTorchController`/`AutoZoomController` need continuous ambient/candidate data, not just on "no code found" frames.

### 3.2 Camera control — `domain/library/camera/`

| Unit | Responsibility |
|---|---|
| `CameraBarcodeAnalyzer` (rewrite) | Full frame (remove the centered `ScanOverlay.RATIO` crop at L58-71). `STRATEGY_KEEP_ONLY_LATEST`. Calls `ScanEngine.scan(image: ImageProxy)` (zxing-cpp/WeChatQRCode read `ImageProxy`/`Bitmap` directly — no `LumaFrame`/`Quad` wrapper type, per the DRY correction in §2) and reports every frame via an extended `BarcodeDetector` interface (`onBarcodeFound`, `onMultipleBarcodesFound`, `onFrameAnalyzed(candidates, meanLuma, width, height)`, `onError`). Exceptions are caught per frame and logged; they no longer call `onError` / hide the camera |
| `ScanController` | Pure state machine (`com.google.zxing.Result` in, `ScanUiState` out: `Searching`/`Approaching`/`Picking`/`Found`). Requires the same decoded text on 2 consecutive frames before `Found`; ≥ 2 codes → `Picking` |
| `AutoTorchController(hasFlash)` | Pure decision function on `meanLuma`, decoupled from `CameraControl` — caller applies `enableTorch`. `< 40` for 1.5 s → request torch on, `> 110` for 1.5 s → request off. A manual torch toggle disables auto behavior for the session. No-op without a flash unit |
| `AutoZoomController(minZoomRatio, maxZoomRatio)` | Pure decision function on the candidate list + current `zoomRatio` — caller applies `setZoomRatio`. Candidate area < 10% of frame → step toward the ratio where it covers ~25%. Steps are ≥ 300 ms apart, capped at `maxZoomRatio`. Resets to `minZoomRatio` after 5 s without candidates. Pauses while the user is pinching |
| `CameraZoomGestureDetector` (unchanged zoom path) | Pinch / double-tap / slider keep calling `setLinearZoom`, unmodified — CameraX keeps `linearZoom` and `zoomRatio` in sync on one shared `ZoomState`, so `AutoZoomController`'s `setZoomRatio` calls coexist without a conversion. One addition: a single-tap-to-focus hook (`onSingleTapUp` → `startFocusAndMetering`), ported from Privacy-QR's `FocusMeteringAction` idea, since both gestures share the view's one `OnTouchListener` |

### 3.3 UI — `presentation/`

- `ScanOverlay`: remove the fixed centered box/corner painting (no ROI). On ≥ 2 codes, freeze the current preview frame and draw a tappable marker (dot/ring) at each code's centroid. Tapping one → `Found`. Back/cancel → resume `Searching`. Centroids are mapped from image-sensor coordinates to view coordinates via CameraX's own `ImageProxyTransformFactory`/`PreviewView.outputTransform` → `CoordinateTransform` (no hand-rolled projection math).
- `MainCameraXScannerFragment`: wire `ScanController`. Keep the existing format whitelist, ZXing SCAN-intent support, vibration/sound, history.

### 3.4 Result handling

- Single code: require the same text on 2 consecutive frames (or one WeChatQRCode hit), then the existing `onSuccessfulScanFromCamera` path.
- `PaymentCodeClassifier` (pure function, `domain/library/payment/`): `classify(uri: String): PaymentCode?`. Called from `BarcodeMatrixUriFragment`/`UrlActionsFragment` (§3.4 hook path below), not from `Modules.kt` — `BarcodeType` stays `URL` for all these codes, unchanged. Matching is case-insensitive on scheme and host:
  - `wxp://…` → `WeChatPay`
  - `weixin://…`, `https?://u.wechat.com/…`, `https?://weixin.qq.com/…` → `WeChatLink`
  - `https?://qr.alipay.com/…`, `alipays://…`, `alipay://…` → `Alipay`
  - anything else → `null` (existing flow unchanged)
- Hook path (confirmed against the real precedent, `BarcodeMatrixUriFragment.kt:52-68` and `UrlActionsFragment.kt:29-48`, `AbstractActionsFragment.kt:228-233,298-306` — **not** a new `BarcodeType`; UPI is itself handled as a sub-case of `BarcodeType.URL`, and this follows the same shape):
  1. `BarcodeMatrixUriFragment.start()`: alongside the existing `uri.startsWith("upi")` branch, add `PaymentCodeClassifier.classify(uri)`; on a match, show a new sibling display-only fragment `BarcodeMatrixPaymentParsedFragment` (modeled 1:1 on `BarcodeMatrixUpiParsedFragment`) in the same `fragmentBarcodeMatrixUriParsedLayout` container
  2. `UrlActionsFragment.configureActionItems()`: when `PaymentCodeClassifier.classify(uri)` matches, relabel the existing single `configureUrlActionItem(uri)` call ("Pay with Alipay" / "Open WeChat", brand icon) instead of adding a parallel action
- No new `BarcodeType`, no `Modules.kt` mapping change, no new `IntentCreator` functions, no new Actions-fragment class, no `<queries>` (below). `openUrl(…)` itself is unmodified — still `createSearchUrlIntent(…)` → `ACTION_VIEW` with `Uri.normalizeScheme()`, and `mStartActivity` still catches `ActivityNotFoundException` → toast. **Correction**: it is *not* called with the raw `uri` for Alipay — `https://qr.alipay.com/…` is a plain web link with no guaranteed App Link verification for that host, so a bare `ACTION_VIEW` on it opens a browser, not Alipay. `PaymentCodeClassifier.handoffUri(uri, code)` wraps it into `alipays://platformapi/startapp?saId=10000007&qrcode=<urlencoded>` first; `wxp://`/`weixin://`/already-`alipays://` codes pass through unchanged since they already deep-link directly
- Payment content screen (`BarcodeMatrixPaymentParsedFragment`): brand name + icon, raw content, reuses the existing `isPossiblyMaliciousURI` warning already shown by the parent `BarcodeMatrixUriFragment`. No buttons here (matches the UPI precedent — actions live on the Actions screen, not the content screen)
- No `AndroidManifest.xml` `<queries>` needed: explicit-scheme `ACTION_VIEW` with no target package resolves via standard implicit-intent resolution, not package-visibility introspection — confirmed by the existing code already doing this for `upi://`, `geo:`, `tel:`, `mailto:` with zero `<queries>` entries in the manifest today
- Gallery/share/shortcut (`BarcodeBitmapAnalyser`) uses `TieredScanEngine.scan(bitmap)` for all formats. Multiple codes in an image → same picker.

## 4. Error handling

| Failure | Behavior |
|---|---|
| `wechatqr` native lib or models fail to load | Log once; `TieredScanEngine` runs zxing-cpp only; no user-visible error |
| zxing-cpp native load fails | Fatal at startup in debug; in release fall back to ZXing core `MultiFormatReader` (existing code path kept as `ZxingJavaEngine`) |
| Analyzer exception on a frame | Catch, log, drop the frame, continue |
| Torch/zoom `CameraControl` future fails (camera closed) | Ignore `OperationCanceledException`; log others |
| Payment app missing / `ActivityNotFoundException` | Toast only — existing `mStartActivity` catch in `AbstractActionsFragment.kt:298-306`, unchanged |

## 5. Build & distribution

- New module `:wechatqr` (Android library, NDK, CMake):
  - git submodules `third_party/opencv` and `third_party/opencv_contrib`, pinned to the same release tag (4.14.0)
  - CMake builds static OpenCV with `BUILD_LIST=core,imgproc,dnn,wechat_qrcode`, `BUILD_JAVA=OFF`, `BUILD_SHARED_LIBS=OFF`, and with tests/apps/videoio/highgui disabled
  - our own JNI `wechatqr_jni.cpp`: `nativeInit(modelDir)`, `nativeDetectAndDecode(yBuffer, width, height, rowStride, rotation) → Array<String> + FloatArray quads`. Compiled with an include path into `opencv_contrib/modules/wechat_qrcode/src/detector/` so it can call `SSDDetector::forward` directly for candidate quads (§2); pin the exact contrib commit this relies on
  - ABIs `arm64-v8a`, `armeabi-v7a`. APK splits per ABI
- Models: `detect.prototxt`, `detect.caffemodel`, `sr.prototxt`, `sr.caffemodel` from `WeChatCV/opencv_3rdparty` committed as assets in `app/src/main/assets/wechat_qrcode/`, SHA-256 pinned in a Gradle verification task. Primary distribution channel (an alternative F-Droid-compatible store, §1) does not enforce a reproducible-build/no-binary-blob policy, so bundling is acceptable there. **Model license must still be confirmed before release.** Runtime graceful-degrade (empty model paths, §2/§3.1) is kept as a resilience fallback, not a policy requirement.
- `app`: add `io.github.zxing-cpp:android:3.1.1`. ZXing core stays at 3.3.3 (§2 decision): still used for generation, `ResultParser`, and the Java fallback only.
- F-Droid metadata: `submodules: true`, `ndk:` pinned version. Keep the existing fastlane metadata.
- APK size budget: measure after the first build. The target is ≤ +8 MB per ABI.

## 6. Testing

JVM unit tests (`app/src/test`):
- `PaymentCodeClassifierTest`:
  - real-format samples for each type; case variants
  - look-alikes that must **not** match (`https://qr.alipay.com.evil.example/`, `https://example.com/?u=wxp://x`)
  - ordinary URLs/text → `null`
- `AutoTorchControllerTest`: hysteresis (no flicker at thresholds), 1.5 s debounce, manual override sticks, no-flash no-op.
- `AutoZoomControllerTest`: target ratio from quad area, cap at `min(max, 6.0)`, step spacing, reset after timeout.
- `ScanControllerTest`: transitions for 0/1/2+ codes, 2-frame stability, pick/back.

Instrumented test (`app/src/androidTest`), with fixture images in `androidTest/assets/scan_fixtures/` run through `TieredScanEngine.scan(bitmap)`:
- normal QR, EAN-13, DataMatrix, inverted QR, two QR codes in one image → all decoded by zxing-cpp
- small/blurred QR that zxing-cpp misses → decoded via WeChatQRCode
- WeChat Pay and Alipay sample QR → classified correctly end-to-end

Manual device checklist: auto-torch in a dark room, auto-zoom from ~1.5 m, multi-code picker, Alipay hand-off, WeChat open, behavior with neither app installed.

## 7. Out of scope

- Generating WeChat/Alipay codes
- Payment processing or any network call for payments
- iOS
- Non-QR AI recovery (1D barcodes rely on zxing-cpp `tryHarder`/`tryRotate`/`tryInvert`)

## 8. Risks

| Risk | Mitigation |
|---|---|
| OpenCV contrib NDK build is fragile/slow on F-Droid | Minimal `BUILD_LIST`; pinned tags; CI job that builds `:wechatqr` from clean |
| WeChatQRCode model license unclear | Verify before release; distribution channel (§1/§5) doesn't force a fallback, but the engine stays optional at runtime so it can ship without models if needed |
| WeChat refuses external hand-off | Design already shows a "scan inside WeChat" hint; verify on device |
| Auto-zoom oscillation | Step spacing, cap, pause on user gesture, unit-tested math |
| GPLv3 copyleft | Accepted: the fork stays GPLv3 |
| `SSDDetector::forward` is a module-internal, unstable API (§2) | Pin exact opencv_contrib commit in the submodule; instrumented tests (§6) catch a signature/behavior break on contrib bump |
| ZXing core bump would raise the effective Android floor to API 24 (§2, confirmed via 3.4.0 release notes) | Resolved: stay on 3.3.3; revisit only alongside a `minSdk` bump, out of scope here |
