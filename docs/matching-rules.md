# Matching Rules — Module 2 (Ride Matching and Location-Based Dispatch)

> **Module:** Module 2 — Ride Matching and Location-Based Dispatch  
> **Requirement IDs:** FR2.1, FR2.2, FR2.3

---

## 1. Core Matching Concept

UMPooling matches ride **requests** (from passengers) to ride **offers** (from drivers) based on three dimensions:

1. **Origin zone overlap:** The passenger's saved residential zone or pickup location matches the driver's departure zone within the zone radius threshold.
2. **Destination match:** Both commuters are heading to the same destination faculty, college, or campus facility.
3. **Departure time proximity:** Departure times fall within a configurable time window ($\pm 15$ minutes).

A ride is defined as an eligible **match** when **all three** conditions are satisfied simultaneously.

---

## 2. Zone Definition & Spatial Structure

### 2.1 What is a Zone?
A **zone** is a designated geographic area defined by a **centroid point** $(\text{latitude}, \text{longitude})$ and a **radius** ($\text{default: } 500\text{ metres}$).

Two locations are classified within the **same zone** if their great-circle distance is less than or equal to the zone radius:
$$\text{haversine}(P_{\text{passenger}}, P_{\text{driver}}) \le R_{\text{zone}}$$

### 2.2 Why Radius-Based Centroids?
* **Simplicity & Performance:** Fast mathematical evaluation on both the Android client (via Haversine) and the database (via PostGIS `ST_DWithin`).
* **Appropriate for Campus Scale:** UM campus covers ~920 acres (~3.7 km²). A 500 m origin radius covers typical walking distances within off-campus condo clusters (e.g. Tiara/Astana Damansara, Vista Angkasa/KL Gateway), while a 300 m destination radius isolates specific faculty blocks.

### 2.3 Zone Dataset
All locations are maintained in [`docs/locations.json`](file:///Users/shaoern/Downloads/UMPooling/docs/locations.json) (73 locations aligned with the team's commuter survey options):
1. **On-Campus Residential Colleges (KK1 to KK13):** 13 colleges with official names and student acronyms.
2. **Faculties & Academies (18):** FSKTM, Engineering, Science, Medicine, Dentistry, Pharmacy, FBEc, Law, FASS, Languages, Built Environment, Education, Creative Arts, APIUM, APM, IPS, AEI, PASUM.
3. **Central Facilities & Landmarks (10):** Main Library, Za'ba Memorial Library, DTC, Examination Building, KPS, Pusat Sukan / UM Arena, UMMC (PPUM), UMSC, Masjid Ar-Rahman, Wisma R&D.
4. **Campus Gates (4):** KL Gate (Lembah Pantai), PJ Gate (Jalan Universiti), Damansara Gate (Lingkungan Budi), Pantai Sentral Gate.
5. **Transit Stations (4):** LRT KL Gateway-Universiti (KJ19), LRT Kerinchi (KJ18), MRT Phileo Damansara (KG12), LRT Abdullah Hukum (KJ17).
6. **Off-Campus Student Residential Zones (24):** Mapped to the 10 survey options (Tiara/Astana/Prima 16/Li Villas, Pacific Star/Ryan & Miho/Jaya One, Atwater/PJ Midtown/Centrestage, University Tower/Sec 11, Vista Angkasa/KL Gateway/Residensi Kerinchi, South View/South Link/Novum, Pantai Hillpark, Bangsar South, Other KL/Selangor).

> **Important Coordinate Convention:**
> * In **Android / Kotlin / Google Maps UI:** Coordinates are passed as `(latitude, longitude)`.
> * In **PostGIS / Supabase EWKT:** Coordinates are formatted as `SRID=4326;POINT(longitude latitude)` via `GeoPoint.ewkt(lng, lat)`. Longitude comes first in WKT.

---

## 3. The Three Matching Rules

### Rule 1: Origin Zone Overlap
```
match_origin(passenger, driver) =
    haversineKm(passenger.originLat, passenger.originLng, driver.originLat, driver.originLng) <= ZONE_RADIUS_KM
```
* **Default threshold:** `0.5 km` ($500\text{ m}$).
* Evaluates whether the passenger's pickup location and driver's departure point are within the same neighborhood or transit cluster.

### Rule 2: Destination Proximity
```
match_destination(passenger, driver) =
    haversineKm(passenger.destLat, passenger.destLng, driver.destLat, driver.destLng) <= DESTINATION_RADIUS_KM
```
* **Default threshold:** `0.3 km` ($300\text{ m}$).
* Destinations are specific faculty or facility buildings, requiring tighter spatial tolerance than the origin.

### Rule 3: Departure Time Proximity
```
match_time(passenger_time, driver_time) =
    abs(minutesBetween(passenger_time, driver_time)) <= TIME_WINDOW_MINUTES
```
* **Default threshold:** `15 minutes`.
* Time comparison operates on parsed ISO-8601 timestamps (`departure_time`) or local schedules (`preferred_time`).

### Combined Eligibility Condition
A ride offer is eligible to be recommended or matched with a request if and only if:
```
is_match(passenger, driver) =
    match_origin(passenger, driver)
    AND match_destination(passenger, driver)
    AND match_time(passenger, driver)
    AND driver.available_seats > 0
    AND driver.status == "open"
```

---

## 4. Architecture and Data Flow

### 4.1 FR2.1 — Saved Routes and Subscriptions
Commuters save frequent routes (e.g. Tiara Damansara $\rightarrow$ FSKTM at 08:00 AM) to receive targeted notifications when matching drivers post rides.

**Database Model (`public.saved_routes` in Supabase):**
```sql
create table public.saved_routes (
    route_id uuid primary key default gen_random_uuid(),
    user_id uuid references auth.users(id) on delete cascade not null,
    origin_geo geography(point, 4326) not null,
    destination_geo geography(point, 4326) not null,
    origin_zone text,
    destination_zone text,
    distance_km double precision,
    preferred_time time without time zone,
    created_at timestamptz default now()
);
```

### 4.2 FR2.2 — Matching & Driver Push Notifications
When a passenger posts a new ride request, matching drivers heading along overlapping routes are notified:

```
Passenger submits ride request (POST to public.ride_requests)
                           │
                           ▼
PostgreSQL trigger on ride_requests (public.notify_push)
                           │
                           ▼
Asynchronous HTTP POST to Supabase Edge Function (swift-worker)
                           │
                           ▼
Edge Function queries matching drivers with overlapping saved_routes:
  - ST_DWithin(saved_routes.origin_geo, request.pickup_geo, 500)
  - ST_DWithin(saved_routes.destination_geo, request.destination_geo, 300)
  - abs(preferred_time - request_time) <= 15 minutes
                           │
                           ▼
Edge Function reads fcm_token from public.users and dispatches FCM payload
```

**FCM Push Notification Payload:**
```json
{
  "title": "New Ride Request Near Your Route",
  "body": "Tiara Damansara → FSKTM at 8:15 AM (1 seat). Est. CO2 saved: 2.4 kg",
  "data": {
    "requestId": "b3e21820-...",
    "originZone": "tiara_damansara",
    "destinationZone": "fsktm",
    "departureTime": "2026-11-18T08:15:00Z",
    "seats": "1",
    "estimatedCo2Kg": "2.4"
  }
}
```

### Passenger match notifications

The target passenger flow checks new offers against saved recurring routes and pending schedule requests using pickup, destination, departure and sufficient seats where requested. Respect push preferences and include the offer ID plus the request ID when there is one. Opening a notification takes the passenger to RideMatchScreen, where current availability is rechecked before offer review. See the [user flow](design/User_Flow.md).

### 4.3 FR2.3 — Filterable Feed
The feed presents open, upcoming offers from other drivers. When the passenger submits criteria, apply pickup, destination, departure and requested-seat filters and sort the eligible offers by route proximity and departure time. Schedule results can also show compatible pickup groups. If the search returns no match, save a pending request only when the passenger chooses **Continue**.

* **Database Filter:** 
  ```kotlin
  SupabaseClientProvider.client.postgrest["trip_offers"]
      .select(Columns.ALL) {
          filter {
              eq("status", OfferStatus.OPEN)  // "open"
              gt("available_seats", 0)
          }
      }
  ```
* **Proximity Sorting:** For each fetched offer, the client computes distance from the user's current location or selected origin using `haversineKm()` and sorts ascending.
* **CO2 Presentation:** Each offer card displays the estimated avoided emissions calculated via Member 4's `estimateCo2(distanceKm, passengers, vehicleType, fuelCategory)` function.

---

## 5. Reference Implementation (Kotlin)

```kotlin
package com.fopgang.umpooling.util

import kotlin.math.*

object MatchingEngine {

    const val DEFAULT_ZONE_RADIUS_KM = 0.5        // 500 m
    const val DEFAULT_DESTINATION_RADIUS_KM = 0.3 // 300 m
    const val DEFAULT_TIME_WINDOW_MINUTES = 15

    /**
     * Calculates great-circle distance between two GPS coordinates using the Haversine formula.
     * Earth radius R = 6371.0 km.
     */
    fun haversineKm(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Checks if two coordinates are within a specified radius (in kilometers).
     */
    fun isWithinRadius(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double,
        radiusKm: Double
    ): Boolean {
        return haversineKm(lat1, lng1, lat2, lng2) <= radiusKm
    }

    /**
     * Checks if two departure times (in minutes from midnight) fall within the time window.
     */
    fun isWithinTimeWindow(
        timeMinutes1: Int,
        timeMinutes2: Int,
        windowMinutes: Int = DEFAULT_TIME_WINDOW_MINUTES
    ): Boolean {
        return abs(timeMinutes1 - timeMinutes2) <= windowMinutes
    }
}
```

---

## 6. Configurable System Parameters

| Parameter | Default Value | Description | Storage / Control |
|---|---|---|---|
| `ZONE_RADIUS_KM` | `0.5` ($500\text{ m}$) | Maximum pickup distance for origin zone overlap | App constants / Supabase config |
| `DESTINATION_RADIUS_KM` | `0.3` ($300\text{ m}$) | Maximum arrival distance for destination matching | App constants / Supabase config |
| `TIME_WINDOW_MINUTES` | `15` | Maximum allowable departure time difference | App constants / Supabase config |
| `FEED_PAGE_LIMIT` | `20` | Maximum offers loaded per page | Query parameter |
| `MAX_SAVED_ROUTES` | `5` | Maximum active saved routes per commuter | Client check & database limit |
