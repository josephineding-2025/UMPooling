# UMPooling 

A UM-only campus carpooling app that helps students and staff share rides, cut single-occupant car trips, and see the CO2 they save.

Status: idea proposal v2 (fixes applied). Items marked [CONFIRM] or [VERIFY] must be checked before they go into the report.

## 1. Introduction and Problem Context (SDG 13: Climate Action)

**Selected SDG and target:** SDG 13 Climate Action, Target 13.3: "Improve education, awareness-raising and human and institutional capacity on climate change mitigation, adaptation, impact reduction and early warning". 

**How the app supports 13.3:** UMPooling raises climate awareness and builds capacity for mitigation at campus level:
- **Awareness and education (individual):** users see the CO2 they save at the point of choosing a ride, right after each trip, and in a weekly summary with short low-carbon commuting tips.
- **Mitigation in practice:** matching and cost-sharing turn that awareness into action by filling empty seats and reducing single-occupant car trips.
- **Institutional capacity (UM):** verified, anonymised CO2 and commuting data gives UM staff a way to measure and plan campus mitigation.

**Problem statement:** Universiti Malaya (UM) serves tens of thousands of students and staff daily. Many students live in internal residential colleges (Kolej Kediaman) or in nearby off-campus areas such as Tiara Damansara, Seksyen 17, Pacific Star and Pantai Hillpark, and commute to faculties such as FSKTM, the Faculty of Engineering and the Main Library.

**Current pain points (to be validated by survey, observation and interviews):**
- **Public transport reliability:** bus delays, crowding or missed schedules at peak morning hours. [VALIDATE: survey Q3, Q4.]
- **Congestion, parking and emissions:** many individual cars and e-hailing rides enter campus each morning, causing gate congestion, parking shortages at faculties such as FSKTM, and avoidable emissions. [VALIDATE: gate occupancy counts, parking observation.]
- **Cost and trust barriers:** e-hailing fares fluctuate, while informal "sapu" ride sharing through chat groups has no verification, no safety log and no reliable coordination. [VALIDATE: survey Q6 to Q8, sapu group admin interview.]

## 2. System Overview (the solution)

UMPooling is a peer-to-peer carpooling mobile app built only for the UM community. Registration is restricted to valid @siswa.um.edu.my and @um.edu.my email addresses and requires email verification.

Drivers post trips and set a suggested **cost-share contribution** per seat (inspired by the UM "sapu" culture). The contribution is capped at a fuel-sharing level displayed in the app. **There is no in-app payment, and the app does not facilitate commercial transport.** Passengers save favourite routes, get notified when a matching driver is heading their way, chat to agree on a pickup spot, and can trigger an SOS message during a trip.

Roles: passenger, driver, or both (a user can switch). Admin/analyst view (UM sustainability or security staff, or the project team during the pilot) sees aggregated, anonymised statistics only.

## 3. AI Integration and Justification

AI is used only where it removes a real problem. All AI calls go through a backend function so API keys are never inside the app, and the user can always correct AI output.

1. **Natural-language ride request parser (NLP).**
   - Problem: typing origin, building code (e.g. FSKTM Block B), time and seats on a small screen while rushing is slow.
   - Implementation: a cloud LLM (e.g. Gemini via a Firebase Cloud Function) turns a sentence such as "Need a ride from Tiara to FSKTM tomorrow by 8:30am" into structured JSON (origin, destination, time, seats). The app pre-fills the form and the user confirms.
   - Evaluation: 100+ test sentences (English, Bahasa Melayu, mixed) covering field-level accuracy, handling of ambiguous or invalid input, and a fallback to manual entry.
   - Note: a custom fine-tuned on-device model is NOT claimed. It is out of scope and hard to evidence.
2. **Route and demand matching (recommendation).**
   - Problem: with many students travelling at once, manual searching for a matching driver or passenger is inefficient.
   - Implementation: group requests by origin zone and departure window (rule-based scoring plus simple clustering such as k-means or DBSCAN on saved routes and time) and push targeted notifications to drivers on overlapping routes, with a suggested time window for the group.
   - Cold start: early on there is little history, so evaluate with survey-derived and seeded data and state this limitation openly.
3. **Carbon impact calculation (not machine learning).**
   - CO2 saved per trip = passenger count x trip distance x emission factor for the driver's vehicle type, assuming each passenger would otherwise have travelled alone by car. Emission factors come from a cited source [VERIFY: choose and cite a recognised factor, e.g. DEFRA or a Malaysian source].
   - The weekly personal summary and the aggregated campus dashboard use this formula. Describe it honestly as a calculation, not as AI.

## 4. System Modules and Functional Requirements (5 modules x 3 FRs)

**Module 1: Authentication and Green Commuter Profile**
- 13.3 link: supporting. Stores the vehicle data used for CO2 estimates and shows the user's Green Commuter Tier, which recognises sustainable commuting.
- FR1.1: The system shall allow registration and login only with @siswa.um.edu.my or @um.edu.my email addresses.
- FR1.2: The system shall send a verification link to the institutional email and block access until the user verifies it.
- FR1.3: The user shall be able to manage a commuter profile (faculty or department, residential area, role as driver, passenger or both; for drivers, vehicle type, registration number and fuel-efficiency category). The profile shall show a verification badge and the user's cumulative Green Commuter Tier. Student ID images are NOT collected, to limit sensitive data.

**Module 2: Ride Matching and Location-Based Dispatch**
- 13.3 link: awareness at the point of decision (CO2 saved is shown on notifications and offers) and mitigation by filling empty seats.
- FR2.1: The user shall be able to save frequent routes and residential zones (for example Tiara Damansara, Seksyen 17, or a UM residential college) and receive a notification when a matching ride is posted.
- FR2.2: The system shall notify drivers with overlapping saved routes when a passenger submits a ride request. The notification shall show departure time, destination, available seats and the estimated CO2 saved.
- FR2.3: The user shall be able to browse a filterable feed of ride offers and requests, sorted by route proximity, departure time and destination, with the estimated CO2 saved shown on each offer.

**Module 3: Booking, Cost-Sharing and Coordination**
- 13.3 link: lowers the cost barrier to carpooling (behaviour change) and gives trip-end CO2 feedback based on verified occupancy.
- FR3.1: The driver shall be able to suggest a cost-share per passenger, limited by a community cap based on shared fuel cost. Payment is settled outside the app.
- FR3.2: The passenger shall be able to accept an offer and reserve a seat, producing a confirmed trip ticket. The system shall update the remaining seats so the same seat cannot be booked twice. When the trip ends, the system shall record the confirmed passenger count and distance and show the CO2 saved to the driver and passengers.
- FR3.3: The matched driver and passenger shall be able to chat in the app to agree on the pickup location and departure time.

**Module 4: Commuter Safety and Emergency**
- 13.3 link: enabling only. Trust and safety are needed for people to share rides. Not claimed as a direct contribution.
- FR4.1: The system shall store a trip record (trip ID, vehicle registration number, origin and destination zones, driver and passenger IDs, start and end time) for safety and accountability, accessible only to the trip participants and authorised admins.
- FR4.2: The user shall be able to trigger an SOS during an active trip that opens a pre-filled message (trip details and current location) in SMS or WhatsApp, addressed to the user's emergency contacts or UM Security.
- FR4.3: The user shall be able to rate and review the other party after a trip, and the ratings shall form the community trust rating.

**Module 5: AI Commuting Assistant and Climate Impact Analytics (AI module)**
- 13.3 link: direct. Builds individual awareness through CO2 insights and builds UM's capacity through aggregated data.
- FR5.1: The system shall use a cloud language model to turn a natural-language request into origin, destination, departure time and seats, and pre-fill the booking form for the user to confirm.
- FR5.2: The system shall recommend pickup groupings and departure windows for passengers from the same residential area, to increase the number of passengers per vehicle.
- FR5.3: The system shall provide a sustainability dashboard that shows: (a) CO2 avoided per trip and the user's cumulative total, with personalised insights and short low-carbon commuting tips; (b) community totals and leaderboards by faculty or residential college (never by individual); and (c) an anonymised aggregate view by faculty, college and week for authorised UM staff.

### SDG 13.3 mapping by module

| Module | Relationship to 13.3 | How it contributes |
|---|---|---|
| M5 AI and impact | Direct | Personal CO2 insights and tips (education and awareness); leaderboards (social awareness); campus dashboard and anonymised staff view (institutional capacity) |
| M2 Ride matching | Awareness and mitigation | CO2 saved shown on notifications and offers at the moment of decision; fills empty seats and reduces single-occupant trips |
| M3 Booking and cost-sharing | Behaviour change | Fuel-share cap makes carpooling cheaper than e-hailing; trip-end CO2 feedback; confirmed passenger count and distance create verified data |
| M1 Authentication and profile | Supporting | Vehicle and fuel-efficiency data set the emission factor; Green Commuter Tier gives recognition; faculty/college fields allow aggregated reporting |
| M4 Safety | Enabling | Builds the trust that carpool adoption depends on. Not claimed as a direct contribution |

Primary claim: M5, M2 and M3 (three modules), with M1 as supporting and M4 as enabling. Do not over-claim M1 and M4 in the report.

Data dependency: the Green Commuter Tier (M1) and the dashboard (M5) depend on verified trip-end data from M3. Members 1, 2 and 4 should agree on that data early.

## 5. Non-Functional Requirements (maximum 2)

1. **Performance:** a ride-request notification shall reach matching drivers within 5 seconds, and each screen shall load within 3 seconds with up to 100 concurrent users. Justification: morning peak demand is concentrated in a short window, and slow matching defeats the purpose.
2. **Security and privacy:** access is limited to verified UM accounts, data access is controlled by backend security rules, live location is shared only during an active trip, and only the minimum personal data is stored (consistent with Malaysia's PDPA 2010 [VERIFY]). Justification: users share trips with strangers, and trust determines adoption.

## 6. Draft Architecture (to be confirmed by the team)

- Client: Android, Kotlin, Jetpack Compose, MVVM. [CONFIRM stack with lecturer and team.]
- Backend: Firebase Authentication (email verification), Firestore, Cloud Functions (AI call, matching, notifications), Firebase Cloud Messaging.
- Maps: Google Maps or OpenStreetMap for pickup selection and distance.
- AI: LLM API behind Cloud Functions; matching logic in Cloud Functions or on the device.
- Core collections: users, savedLocations, tripOffers, rideRequests, trips (with ticket and seat data), messages, ratings, safetyLogs, impactStats.

## 7. Validation Plan (summary)

- Survey: one Google Form, 30+ responses collected online from friends across faculties (a convenience sample, stated as a limitation), handled by one team member. Fields: commute mode and cost, bus reliability, sapu experience and problems, willingness to use features.
- UM engagement: one written enquiry to the UM Security / traffic and parking / transport unit (through the lecturer) for their view on how the project can support SDG 13.3 (climate awareness and capacity) and on safety and cost-sharing rules.
- Details: see Survey_Interview_Kit.md.
- User testing: 8 to 10 users, task timings and SUS score.

## 8. Concerns and Risks

| # | Concern | Why it matters | Mitigation |
|---|---|---|---|
| 1 | SDG fit (13.3 is about education and awareness, not only cutting emissions) | Markers check alignment with the target | Keep awareness features prominent (CO2 on offers, trip-end feedback, weekly tips, campus dashboard) and measure their effect: add a user-test question on whether seeing the CO2 changes willingness to carpool |
| 2 | Legality of paid rides in Malaysia | Carrying passengers for reward may be regulated (e-hailing is licensed) | Cap at fuel-cost sharing, no in-app payment, disclaimer, ask lecturer and UM Security, state in limitations |
| 3 | Safety of riding with strangers | Biggest trust barrier | UM-verified accounts, SOS, trip log, ratings, share trip with a contact |
| 4 | Privacy (location, trip logs) | PDPA and ethics | Minimum data, no ID card upload, location only during trips, retention limit |
| 5 | Cold start | Empty app has no value, matching cannot be evaluated | Pilot with one condo group and one faculty, seeded demo data, say so openly |
| 6 | AI claims too heavy | Marks and honesty (AI policy) | Cloud LLM with human confirmation, measured accuracy, carbon is a formula |
| 7 | Unverified facts in the draft | Credibility | Verify campus size, population, emission factor, legal position |
| 8 | Scope for 6 people in about 10 weeks | Real-time features are costly | Mandatory: M1 to M4 and the core of M5 (parser, CO2 totals). First to simplify if time is short: leaderboards, staff aggregate view, advanced clustering |
| 9 | AI usage policy | Penalties for undocumented AI use | Keep an AI Usage Log from day 1, own the design decisions |
