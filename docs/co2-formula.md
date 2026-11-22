# CO₂ Calculation Methodology & Green Commuter Tiers (FR5.3)

> **Module:** Module 5 — AI Commuter Assistant & Carbon Dashboard  
> **Requirement ID:** FR5.3 (CO₂ Savings Estimation, Personal Impact, and Leaderboards)  
> **Author:** Member 4 (AI Developer, Group 9 - FOPGang, WIA2007)  
> **Collaborators:** Member 1 (Profile & Tiers), Member 2 (Schema & Triggers), Member 3 (UI Display), Member 5 (Route Distance & Feed)  
> **Status:** Final Architectural Specification (Sprint 1)

---

## 1. Mathematical Formulas

### 1.1 Total Trip CO₂e Avoided
When a driver pools $N$ passengers in a single vehicle, $N$ separate single-occupancy vehicle (SOV) trips are eliminated from the road network. The gross greenhouse gas emissions avoided by the carpool trip are:

$$\text{CO}_2\text{e}_{\text{trip}} = N \times D \times E$$

Where:
* $N \in \mathbb{N}_{\ge 1}$: The number of **confirmed passengers** who completed the trip (excluding the driver).
* $D \in \mathbb{R}_{> 0}$: The actual road distance of the trip in kilometres ($\text{km}$).
  - Primary source: Google Routes API / OSRM navigation route.
  - Fallback source: Great-circle Haversine straight-line distance multiplied by a road circuity factor of **$1.3$**:
    $$D_{\text{est}} = \text{haversine}(P_{\text{origin}}, P_{\text{destination}}) \times 1.3$$
* $E$: Baseline emission factor = **$0.20\text{ kg CO}_2\text{e/km}$** (MITI Malaysia Sustainability Starter Kit / US EPA 2022).

### 1.2 Personal CO₂e Share per Occupant
To credit each participant equitably and ensure that campus-wide community aggregations sum up to physical trips without inflation:

$$\text{CO}_2\text{e}_{\text{personal}} = \frac{\text{CO}_2\text{e}_{\text{trip}}}{N + 1} = \frac{N \times D \times E}{N + 1}$$

Where:
* $N + 1$: Total vehicle occupancy (1 driver + $N$ confirmed passengers).
* Both the driver and each confirmed passenger receive an identical personal credit share $\text{CO}_2\text{e}_{\text{personal}}$.
* **Community Integrity Property:** Summing the personal shares across all occupants yields exactly the total trip avoided emissions:
  $$(N + 1) \times \left(\frac{N \times D \times E}{N + 1}\right) = N \times D \times E$$

#### Example Calculation:
* Route: Tiara Damansara $\to$ FSKTM ($D = 10.0\text{ km}$)
* Passengers: $N = 2$ confirmed passengers (total occupancy = 3)
* Baseline factor: $E = 0.20\text{ kg CO}_2\text{e/km}$
* **Total Trip CO₂e Avoided:** $2 \times 10.0 \times 0.20 = \mathbf{4.00\text{ kg CO}_2\text{e}}$
* **Personal Share per Person:** $\frac{4.00}{3} = \mathbf{1.333\text{ kg CO}_2\text{e}}$ (driver and both passengers each receive $1.33\text{ kg}$).

---

## 2. Counting & Anti-Gaming Rules

To ensure academic and regulatory integrity (preventing artificial score inflation or ghost rides):

1. **Dual Confirmation Required:**
   - A trip is counted if and only if its database status is `completed` in `public.trips`.
   - Trip completion requires:
     1. Driver calls `complete_trip(trip_id)`.
     2. Passenger confirms trip conclusion via `confirm_trip_end(trip_id)`.
2. **Passenger Count Floor:**
   - Trips with $N < 1$ confirmed passengers award **$0.0\text{ kg}$** (a solo drive yields no carpooling offset).
3. **Distance Floor:**
   - Minimum trip distance: $D \ge 1.0\text{ km}$. Rides below 1.0 km (e.g. within the same residential college parking lot) are deemed walking-accessible and ineligible for CO₂ savings credit.
4. **Excluded Trip States:**
   - Cancelled (`cancelled`), no-show (`no_show`), open/scheduled, or expired offers award **$0.0\text{ kg}$**.
5. **Daily Commuter Cap:**
   - A maximum of **2 completed trips per user per calendar day** (00:00 to 23:59 MYT, UTC+8) count toward cumulative CO₂ savings and Green Commuter Tier advancement (reflecting standard morning and evening commutes). Additional trips within the same day are recorded for audit but excluded from leaderboard/tier totals.
6. **Equal Participant Credit:**
   - The driver and all confirmed passengers earn identical personal credits per completed trip.

---

## 3. Green Commuter Tier Thresholds

The Green Commuter Tier gamification system incentivises continuous carpooling among UM students and staff.

### Tier Rules:
* **Cumulative:** Tiers are earned through cumulative career statistics recorded in `public.user_impact_stats`.
* **Permanent / Non-Downgrading:** Once achieved, a badge is retained permanently.
* **Dual Threshold Condition:** A user holds the highest tier where **BOTH** the completed trips threshold **AND** the cumulative $\text{CO}_2\text{e}$ savings threshold are satisfied:
  $$\text{tier} = \max \{ L \mid \text{trips} \ge T_L \land \text{co2\_kg} \ge C_L \}$$

### Tier Table:

| Level | Badge Name | Minimum Completed Trips ($T_L$) | Minimum Cumulative Avoided CO₂e ($C_L$) | Meaning / Milestone |
|:---:|:---:|:---:|:---:|:---|
| **0** | *(No badge)* | 0 | 0.0 kg | New or unverified commuter |
| **1** | **Seedling** | 1 trip | 0.5 kg | First successful carpool commute |
| **2** | **Sprout** | 5 trips | 5.0 kg | Regular weekly commuter (~1 week) |
| **3** | **Sapling** | 15 trips | 20.0 kg | Established monthly carpooler (~1 month) |
| **4** | **Tree** | 40 trips | 60.0 kg | Dedicated semester commuter |
| **5** | **Forest Guardian** | 80 trips | 120.0 kg | Campus sustainability champion (~1 academic year) |

#### Boundary Edge Cases:
* **Case A:** User has 18 trips but only 19.5 kg CO₂ $\to$ **Sprout (Level 2)** (meets trips for Sapling, but fails CO₂ threshold of 20.0 kg).
* **Case B:** User has 12 trips and 25.0 kg CO₂ $\to$ **Sprout (Level 2)** (meets CO₂ for Sapling, but has only 12 of the required 15 trips).
* **Case C:** User has 15 trips and exactly 20.0 kg CO₂ $\to$ **Sapling (Level 3)** (meets both thresholds).

---

## 4. Shared Technical Interfaces

### 4.1 Kotlin Service Interface (`FR5.3` / Shared Domain)
Exposed in package `com.fopgang.umpooling.ai` for use by Member 1 (Profile & Tiers), Member 2 (Trip Completion), Member 3 (UI Displays), and Member 5 (Ride Offers Feed):

```kotlin
interface ICo2CalculationService {
    /**
     * Estimates CO2 avoided for a trip and the personal share per occupant.
     * Guaranteed deterministic and non-throwing.
     */
    fun estimateCo2(
        distanceKm: Double,
        passengers: Int,
        vehicleType: String = "car",
        fuelCategory: String = "petrol"
    ): Co2EstimateResult

    /**
     * Computes great-circle distance between two WGS84 points and applies
     * road circuity factor (1.3) when navigation route distance is unavailable.
     */
    fun estimateRoadDistanceKm(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Double

    /**
     * Validates whether a trip record meets the anti-gaming and counting criteria.
     */
    fun isTripEligibleForCo2(
        status: String,
        confirmedPassengers: Int,
        distanceKm: Double,
        dailyCountedTrips: Int = 0
    ): Boolean

    /**
     * Determines the user's Green Commuter Tier from cumulative metrics.
     */
    fun computeGreenCommuterTier(
        completedTrips: Int,
        cumulativeCo2Kg: Double
    ): GreenCommuterTier
}
```

#### Output Data Structures:
```kotlin
data class Co2EstimateResult(
    val totalTripCo2Kg: Double,
    val personalShareCo2Kg: Double,
    val emissionFactorUsed: Double,
    val distanceKm: Double,
    val passengerCount: Int
)

enum class GreenCommuterTier(
    val level: Int,
    val badgeName: String,
    val minTrips: Int,
    val minCo2Kg: Double
) {
    NO_BADGE(0, "No Badge", 0, 0.0),
    SEEDLING(1, "Seedling", 1, 0.5),
    SPROUT(2, "Sprout", 5, 5.0),
    SAPLING(3, "Sapling", 15, 20.0),
    TREE(4, "Tree", 40, 60.0),
    FOREST_GUARDIAN(5, "Forest Guardian", 80, 120.0);
}
```

---

## 5. Database Schema Specifications

### 5.1 Personal Impact Table (`public.user_impact_stats`)
Maintains live personal metrics. Writable solely via backend triggers upon trip completion; readable by the individual user.

```sql
create table if not exists public.user_impact_stats (
    user_id uuid primary key references auth.users(id) on delete cascade,
    total_trips integer not null default 0 check (total_trips >= 0),
    total_distance_km double precision not null default 0.0 check (total_distance_km >= 0),
    total_co2_saved_kg double precision not null default 0.0 check (total_co2_saved_kg >= 0),
    current_tier_level integer not null default 0 check (current_tier_level between 0 and 5),
    current_tier_name text not null default 'No Badge',
    updated_at timestamptz not null default now()
);

-- Row Level Security: Owner reads only
alter table public.user_impact_stats enable row level security;
create policy "Users read own impact stats"
    on public.user_impact_stats for select
    using (auth.uid() = user_id);
```

### 5.2 Aggregated Analytics Table (`public.impact_aggregates`)
Maintains institutional metrics for faculty and residential college leaderboards and the Sustainability Centre dashboard.

```sql
create table if not exists public.impact_aggregates (
    aggregate_id uuid primary key default gen_random_uuid(),
    week_start date not null,
    faculty text,
    residential_area text,
    residential_college text,
    active_user_count integer not null default 0 check (active_user_count >= 0),
    trip_count integer not null default 0 check (trip_count >= 0),
    total_distance_km double precision not null default 0.0,
    total_co2_saved_kg double precision not null default 0.0,
    created_at timestamptz not null default now(),
    unique(week_start, faculty, residential_area, residential_college)
);
```

### 5.3 Anonymisation & Privacy Constraint ($k \ge 5$)
To protect student and staff privacy under the Malaysian **Personal Data Protection Act 2010 (PDPA)**:
* Aggregate queries surfaced to the Staff / Sustainability Analyst role **MUST** suppress or mask any grouping cell where:
  $$\text{active\_user\_count} < 5$$
* Cells with fewer than 5 active commuters return aggregated totals masked as `NULL` or combined under `"Other / Small Cohorts"` to prevent reverse-identification of individual commuting patterns.
