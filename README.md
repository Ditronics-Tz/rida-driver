# ride-driver-kotlin

Rida **Driver** app (Kotlin + Jetpack Compose). Built from the passenger app
[`ride-client-kotlin`](../ride-client-kotlin) — same stack, theme, map, auth and
API client — re-shaped around the driver's job.

## Features
| Area | What it does |
|---|---|
| Auth | Email sign-in, driver sign-up (`role = "driver"`), Google sign-in, token refresh — **live** against `rida-backend.ditronics.co.tz/api/v1`. |
| Home | Full-screen map, live GPS puck, **Go online / offline**, today's earnings + trip counters. |
| Ride offers | Incoming request sheet with fare, pickup distance, trip distance, passenger rating and a 15 s accept/decline countdown. |
| Active trip | `to pickup → arrived (wait timer) → in trip → complete`, OSRM road route on the map, call/SMS passenger, open turn-by-turn in Google Maps, cancel with reason. |
| Trip complete | Collect cash summary (fare, commission, net) and rate the passenger. |
| Earnings | Today / week / month totals, daily bar chart, commission + net breakdown, trips list. |
| Trips | History with filters, trip details (route map, fare breakdown). |
| Profile | Driver profile, rating, acceptance rate, vehicle details, document status (licence, insurance, etc.). |
| Settings / support | Navigation app, sounds, auto-accept, support contact, logout. |

## Backend status
Auth endpoints are documented by the passenger app and are used as-is. The
`/api/v1/driver/*` router exists on the backend (returns 401 without a token) but its
contract is **not** documented anywhere in this workspace. So:

* `data/driver/DriverRepository.kt` – the interface every screen talks to.
* `data/driver/MockDriverRepository.kt` – on-device simulator (default). Generates
  realistic Dar es Salaam ride offers while online so every flow is usable today.
* `data/driver/RemoteDriverRepository.kt` + `data/remote/DriverApi.kt` – best-guess
  REST implementation.

Switch with `USE_MOCK_DRIVER_API` in `app/build.gradle.kts` once the real driver routes
are confirmed (adjust `DriverApi.kt` paths/fields if they differ).

## Build
```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

> Google sign-in needs an Android OAuth client for `com.example.ridedriver` + your
> signing SHA-1 in the same Google Cloud project as `GoogleAuthManager.WEB_CLIENT_ID`.


## Driver authentication and profile configuration

Authentication uses the same API base URL and Google web client ID as the passenger app. Email registration creates `role=driver`; the driver app rejects sessions for other account roles. Google sign-in requests a one-use backend nonce and exchanges the signed ID token with `role=driver`. Existing passenger accounts cannot become driver accounts through Google sign-in. Deploy the accompanying backend changes before testing driver Google registration.

In Google Cloud, register an Android OAuth client for `com.example.ridedriver` and the SHA-1 of each signing certificate you use (debug and release) in the same project as the web client. The backend must have `GOOGLE_WEB_CLIENT_ID` set to that web client ID. No Google client secret belongs in the Android app. Device sign-in still requires this provider configuration; unit tests cannot validate it.

Driver registration and Personal information save name/phone/vehicle type through `PUT /driver/profile/me`, independently of `USE_MOCK_DRIVER_API`. Google accounts with incomplete details open the profile editor. Phone verification uses the existing request/verify OTP endpoints and requires the backend SMS provider. Refresh calls rotate both session tokens, serialize concurrent refreshes, preserve device headers and stop retry loops; logout revokes the server session.

Profile photos come from `GET /driver/profile/me`; the backend imports the picture from the verified Google identity only when no photo is already saved. In Profile → Edit profile & photo, choose or replace a JPG/PNG/WebP up to 5 MB through `POST /driver/profile/me/images`. Uploaded photos take precedence over Google photos. Home and Profile observe the saved account avatar, including photo replacement. Trip simulation remains controlled separately by `USE_MOCK_DRIVER_API`.

The backend does not currently expose passenger/driver password-reset endpoints. The driver reset screen therefore reports that recovery is unavailable instead of claiming a reset email was sent.


## Driver account pages

The driver pages use the supplied mint/green card design with Home, Earnings, Rides and Support tabs. Open your profile from the Home avatar. Earnings links to weekly breakdown and Wallet & payouts; ride receipts link to ride-specific support. History supports destination/date search and completed/cancelled filters.

Ride history and weekly charts read the selected DriverRepository (the simulator remains the default). Wallet balances, confirmed mobile-money payout requests, support cases/replies, data exports and deletion requests use authenticated backend endpoints. Payout and reply retries retain idempotency keys. Financial example values and support assignments from the reference images are not fabricated. Bank payouts, driver password changes and driver two-step enrollment are not supported by the current backend; their screens direct users to support. Location permission links open Android application settings. Availability preferences persist on the device.

Validation: assembleDebug and testDebugUnitTest cover the app and its auth, profile-photo, ride-filter and payout wire-contract tests. Google sign-in on a device still needs the driver OAuth package/SHA-1 configuration described above.
