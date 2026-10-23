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
