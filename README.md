# SuperSOS

An Android safety app: add up to **3 trusted people**; when your phone loses
network coverage (or is unreachable), they automatically receive your **last
known GPS location**.

## How it works

```
                        ┌────────────────────────────────────────────┐
                        │         EmergencyAlertService              │
                        │            (foreground)                    │
                        │                                            │
   GPS (works offline)  │   LocationTracker  ──► latest fix          │
        ───────────────►│                                            │
                        │   ConnectivityMonitor ──► ONLINE | LOW_SIGNAL | OFFLINE
                        │                                            │
                        │   EmergencyMonitor (state machine)         │
                        │     • coverage lost?  start clock          │
                        │     • keep GPS fix fresh (store-and-forward)
                        │     • try SMS immediately                  │
                        │     • coverage back? flush alert           │
                        │              │                             │
                        └──────────────┼─────────────────────────────┘
                                       ▼
                          LocationNotifier
                             ├─ SMS  → the 3 contacts (needs only cell signal)
                             └─ HTTP → optional cloud backend (heartbeat)
```

### The core flow (`EmergencyMonitor.kt`)

1. **Reachable** → nothing happens.
2. **Coverage drops** → "unreachable clock" starts, and the app *immediately
   fires an SMS* with the current GPS fix. SMS only needs a cell signal, so it
   can get out even where mobile data is dead.
3. **Still unreachable** → GPS keeps fixing (it works with no network at all),
   so the queued alert is continuously refreshed with the newest position;
   SMS is re-attempted every `smsRetryIntervalMs`.
4. **Coverage returns** → the queued alert is flushed to all contacts
   (SMS + optional HTTP backend). If nothing got out, it is re-queued.

## Important honest limitation

A phone with **zero** signal physically cannot transmit anything. So:

- **Best case** (weak signal): SMS still reaches your contacts → they get your
  location in real time.
- **Worst case** (true dead zone): the alert is *stored* and delivered the
  moment the phone regains any coverage, with the timestamp and location from
  when you were lost.

If you need "alert contacts even when the phone is fully dark", the reliable
architecture is a **server-side heartbeat** (see `RemoteBackend.kt`): the app
posts its location to a server while it has coverage; if the server stops
hearing from the phone for X minutes, the *server* (which is online) pushes the
last known location to the contacts via SMS/push/call.

## Project structure

```
super sos/
├─ settings.gradle.kts / build.gradle.kts / gradle.properties
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/supersos/app/
│     │  ├─ data/        Contact, ContactsRepository (max-3), AppPrefs
│     │  ├─ location/    LocationTracker (GPS, works offline)
│     │  ├─ monitor/     ConnectivityMonitor, EmergencyMonitor,
│     │  │               EmergencyAlertService, BootReceiver, SmsStatusReceiver
│     │  ├─ notify/      PendingAlertStore, LocationNotifier, RemoteBackend
│     │  └─ ui/          MainActivity, AddContactActivity, ContactAdapter
│     └─ res/            layouts, strings, theme, launcher icon
└─ README.md
```

## Building the APK

Requires **Android Studio** (this repo has no wrapper JAR — open the folder in
Android Studio and it will generate `gradlew`, or run `gradle wrapper` first).

1. Open the project in Android Studio.
2. Build → `app` → `Build APK(s)`.
3. The APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

### First-run setup on the phone

1. Open SuperSOS, grant **Location**, **Notifications** and **SMS** permissions.
2. Add 3 contacts.
3. Toggle **Guard active** on (it restarts automatically after reboot).

## Tuning

Edit these in `EmergencyMonitor.kt`:

| Constant | Meaning | Default |
|---|---|---|
| `unreachableThresholdMs` | offline before it's an emergency | 2 min |
| `smsRetryIntervalMs` | SMS re-attempt interval while offline | 2 min |

## Notes / caveats

- **SMS sending**: works on sideloaded APKs with the runtime `SEND_SMS`
  permission. The Play Store restricts this permission; for a store release
  prefer the heartbeat backend + a fallback `intent:`-based SMS compose.
- `HttpRemoteBackend` is a stub — wire it to your own server for the
  fully-offline alert case described above.
- Min SDK 26, target SDK 34. Background location may need "Allow all the time"
  in Android settings for reliable tracking.
