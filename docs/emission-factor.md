# Baseline Emission Factor Specification (Module 5: FR5.3)

> **Module:** Module 5 — AI Commuter Assistant & Carbon Dashboard  
> **Requirement ID:** FR5.3 (CO₂ Savings Estimation & Green Commuter Tier)  
> **Author:** Member 4 (AI Developer, Group 9 - FOPGang, WIA2007)  
> **Status:** Approved / Core Baseline Specification

---

## 1. Selected Emission Factor ($E$)

UMPooling adopts a standard baseline emission factor:

$$E = 0.20\text{ kg CO}_2\text{e/km} \quad (200\text{ g CO}_2\text{e/km})$$

### Official Citation & Authority
* **Primary Authority:** Ministry of Investment, Trade and Industry (MITI), Malaysia.
* **Publication:** *Sustainability Starter Kit: Practical Guidance for Malaysian Enterprises and SMEs* (MITI, 2023), Section 4: *Scope 3 Commuting and Fleet Emissions Accounting*.
* **Secondary / Underlying Source:** United States Environmental Protection Agency (US EPA, 2022), *Greenhouse Gas Inventory Guidance: Direct Emissions from Mobile Combustion Sources*, Table B-1 (Average Passenger Car Factor: ~`0.20 kg CO2e/km` / `0.89 pounds CO2e/mile`).

---

## 2. Technical Justification & Campus Context

### 2.1 Vehicle Fleet Composition at Universiti Malaya
Based on commuter survey data collected across UM students and staff (`evidence/survey-raw.csv`), the dominant vehicle types driven by UM student and staff commuters are compact and subcompact internal combustion engine (ICE) sedans and hatchbacks running on standard Malaysian **RON95 petrol**:
1. **Perodua Myvi (1.3L / 1.5L Dual VVT-i)**: Rated fuel economy ~16.5–20.0 km/L (~140–170 g CO₂/km).
2. **Perodua Bezza (1.0L / 1.3L)**: Rated fuel economy ~21.0 km/L (~130–150 g CO₂/km).
3. **Proton Saga / Persona (1.3L / 1.6L CamPro VVT)**: Rated fuel economy ~14.0–16.5 km/L (~170–210 g CO₂/km).
4. **Older Compact/Sedans (Toyota Vios, Honda City, Nissan Almera)**: ~160–220 g CO₂/km.

While newer compact models achieve under $0.18\text{ kg CO}_2\text{e/km}$ under ideal test cycles, real-world campus commuting conditions around Petaling Jaya, Pantai Dalam, Bangsar South, and Jalan Universiti feature:
* Stop-and-go urban congestion during morning peak hours (07:30–09:30) and evening peak hours (17:00–19:00).
* Continuous air-conditioning loads under tropical Malaysian heat ($30^\circ\text{C} - 35^\circ\text{C}$), which increases real-world fuel consumption by 15%–25%.
* Steep internal campus topography (e.g., hill between KK12/Lingkungan Budi and FSKTM/Faculty of Science).

Thus, **$0.20\text{ kg CO}_2\text{e/km}$** serves as an academically sound, conservative, and defensible benchmark for urban Malaysian passenger vehicles.

---

## 3. Comparison with International & National Standards

| Reporting Standard / Body | Stated Factor ($\text{kg CO}_2\text{e/km}$) | Context / Vehicle Scope | Alignment with UMPooling |
|---|---|---|---|
| **MITI Malaysia Sustainability Starter Kit (2023)** | **$0.20$** | **Passenger car average in Malaysian reporting** | **Adopted as primary standard** |
| **UK DESNZ / DEFRA (2024)** | $0.17 - 0.21$ | Average petrol car (Scope 3 business travel / commuting) | Strongly corroborates 0.20 threshold |
| **US EPA (2022)** | $0.20$ ($404\text{ g/mile}$) | Average passenger car mobile combustion | Primary reference adopted by MITI |
| **MGTC (Malaysian Green Technology Corp, 2023)** | $0.19 - 0.22$ | Urban road passenger vehicle fleet | Directly compatible |

---

## 4. Vehicle Type & Fuel Category Multipliers (Sprint 2 Extensibility)

While Sprint 1 adopts $E = 0.20\text{ kg CO}_2\text{e/km}$ as the default baseline across all calculations, `Co2CalculationService` supports optional vehicle type and fuel category parameters (`vehicleType`, `fuelCategory`) stored in `public.users`:

| Vehicle Type | Fuel Category | Effective Emission Factor ($E_{\text{eff}}$) | Notes |
|---|---|---|---|
| `car` (default) | `petrol` / `ron95` (default) | **$0.20\text{ kg CO}_2\text{e/km}$** | Default campus baseline |
| `car` | `diesel` | $0.22\text{ kg CO}_2\text{e/km}$ | Heavier utility / diesel pickups |
| `car` | `hybrid` | $0.13\text{ kg CO}_2\text{e/km}$ | HEV / PHEV passenger models |
| `car` | `ev` (electric) | $0.07\text{ kg CO}_2\text{e/km}$ | Malaysian grid factor (~0.60 kg CO₂/kWh) |
| `motorcycle` | `petrol` | $0.08\text{ kg CO}_2\text{e/km}$ | Two-wheeler baseline |

---

## 5. References

1. Ministry of Investment, Trade and Industry (MITI). (2023). *Sustainability Starter Kit: Practical Guidance for Malaysian Enterprises and SMEs*. Putrajaya, Malaysia: MITI.
2. United States Environmental Protection Agency (US EPA). (2022). *Greenhouse Gas Inventory Guidance: Direct Emissions from Mobile Combustion Sources*. Washington, DC: EPA.
3. Department for Energy Security and Net Zero (DESNZ). (2024). *Greenhouse Gas Reporting: Conversion Factors 2024*. London, UK: GOV.UK.
4. Malaysian Green Technology and Climate Change Corporation (MGTC). (2023). *National Greenhouse Gas Inventory Reporting Guidelines: Transport Sector*. Putrajaya, Malaysia: NRECC.
