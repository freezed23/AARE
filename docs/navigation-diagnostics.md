# Optional AndroidX navigation diagnostics (debug builds)

This developer-only recorder observes **AndroidX Car App Library** navigation events at the AARE host boundary. It is not screen/OCR capture, a map Surface scraper, a third-party APK hook, or an Android Auto wire-protocol extension. Recording is **off by default** and has no effect on release builds.

## Enable

1. Install your own **debug** AARE build.
2. Open **Advanced → Navigation diagnostics (debug)** and turn **Record navigation events** on. It remains off until explicitly enabled.
3. Run a navigation app in AARE phone Preview or on a head unit.
4. Turn the toggle off when finished. The existing private file persists until the user deletes app data or deletes the file.

This is a diagnostic feature, **not** an external navigation API.

## Events

Records are UTF-8 JSONL with `schema=aare-tbt/1` and:

- `event=started` / `ended`: the app's navigation lifecycle callbacks.
- `event=trip`: the actual `INavigationHost.updateTrip` callback, first two steps, displayed distance **and unit**, maneuver type, lane shapes / recommendation flags when supplied, icon/image *presence* (not bitmap bytes) and destination estimate.
- `event=routing_template`: the latest **accepted** `NavigationTemplate.navigationInfo as? RoutingInfo` that AARE will use for its UI. Its current/next step, displayed distance **and unit**, lane flags, junction-image presence and destination estimate can differ from the independent Trip feed.

Use `seq`, `elapsedRealtimeMs`, and `wallMs` in each event for correlation. `Trip` and `RoutingInfo` may update at different rates or even disagree across an imminent turn; never splice their fields into an “atomic” sample. The AndroidX maneuver integers and distance units are **not** native TMAP/KNSDK/vendor/vehicle-CAN codes.

The private file is limited to **1 MiB** and rotated once to `aare-tbt-probe.prev.jsonl`. Recent events stay in `aare-tbt-probe.jsonl`. Compact, throttled summaries appear in **Advanced → Connection log**, but **complete JSONL is deliberately not sent to Logcat**.

## Extract privately

Use Windows **cmd.exe** for raw-byte redirection (Windows PowerShell 5.1 may transcode stdout):

```bat
adb exec-out run-as net.prototux.aare cat files/aare-tbt-probe.jsonl > aare-tbt-probe.jsonl
adb exec-out run-as net.prototux.aare cat files/aare-tbt-probe.prev.jsonl > aare-tbt-probe.prev.jsonl
```

The rotated previous file may not yet exist; check the command's exit status. To delete local records without uninstalling, use `run-as` to remove only the two named probe files, after backing up any material you wish to retain. No network upload or sharing feature is provided.

**Privacy:** Road names, verbal cues, timestamps, and package names **can reveal journeys and locations**. These files stay in AARE's app-private storage. Redact them before sharing reports or screenshots; do not attach raw route logs, Bluetooth MACs, certificates, Wi-Fi identifiers, or credentials to public issues or pull requests.

## Current combined build (2026-11-02)

This port restores the opt-in Trip + RoutingInfo recorder from diagnostic branch
`716b71219fa329915e4dd1675ed91ee9c40f0c5a` onto the connection-tested LAN retry
source `593132c174811ff12e5caed1eb8f95fc415f930e` (PR #2). The two callback streams
remain independent; this patch does not merge template lanes into Trip or change
AA NavigationState output. The recorder does not depend on the AA version override
or receipt of wire messages 0x8006/0x8007. It also works in phone Preview.

Each step records up to eight lanes and five directions per lane, retaining the
original shape integer and isRecommended boolean. An empty lane list means that
snapshot provided no structured lanes; an image-presence flag alone cannot provide
structured recommendations.

Historical owner evidence: the old Advanced summary reportedly showed lanes=2/3,
but the submitted 2026-10-26 Kakao JSONL contained 59 Trip + 59 RoutingInfo events
and zero lanes in all 236 step objects. Recommended-lane content was therefore
not validated by that file. This combined revision requires a new runtime sample.

Build status is recorded separately in the PR and exact-source Owner Build result.
Compilation/signature/test success is not lane runtime, DHU or real-car proof.
