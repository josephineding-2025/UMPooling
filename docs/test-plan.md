# UMPooling — Test Plan

## 1. Introduction

### 1.1 Purpose

This document defines the testing strategy and acceptance criteria for UMPooling, a mobile carpooling application developed for the Universiti Malaya community.

The purpose of testing is to verify that the application satisfies its functional and non-functional requirements, operates reliably, and protects users' personal and trip-related information.

### 1.2 Scope

The test plan covers the following modules:

- Module 1: Authentication and Green Commuter Profile
- Module 2: Ride Matching and Location-Based Dispatch
- Module 3: Booking, Cost-Sharing and Coordination
- Module 4: Commuter Safety and Emergency
- Module 5: AI Commuting Assistant and Climate Impact Analytics

The plan also covers the two non-functional requirements:

- NFR1: Performance
- NFR2: Security and Privacy

### 1.3 References

- `docs/UMPooling.md` — Functional and non-functional requirements
- `docs/schema.md` — Database schema, RLS policies and trip lifecycle
- `docs/Task_Division.md` — Team responsibilities

## 2. Testing Strategy

The following testing methods are planned. Tools and implementation details will be confirmed against the team's development environment.

### 2.1 Unit Testing

Test individual functions and components independently, including input validation, calculations and business rules.

### 2.2 Integration Testing

Verify interactions between the Android application, Supabase authentication, database operations and backend functions.

### 2.3 User Interface Testing

Verify that users can complete the intended workflows through the application interface.

### 2.4 Security Testing

Verify authentication, authorisation, database Row Level Security policies and protection of trip-related information.

### 2.5 User Acceptance Testing

Invite representative UM users to complete selected application tasks and provide feedback on functionality, usability and safety.

## 3. Functional Requirements Testing

Acceptance criteria will be documented using the Given–When–Then format.


### 3.1 Module 1 — Authentication and Green Commuter Profile

#### FR1.1 — UM Email Registration and Login

**TC1.1-01: Registration with an eligible UM email**

Given a new user provides a valid email address ending in
@siswa.um.edu.my or @um.edu.my,
When the user submits the registration form,
Then the system shall accept the email domain for registration,
subject to the remaining registration requirements.

Status: Planned.

**TC1.1-02: Registration with an ineligible email**

Given a new user provides an email address outside the two
permitted UM domains,
When the user submits the registration form,
Then the system shall reject the registration.

Status: Planned.

**TC1.1-03: Login restriction**

Given an account does not meet the permitted institutional
email requirement,
When login is attempted,
Then the system shall not grant access to protected features.

Status: Planned.

#### FR1.2 — Institutional Email Verification

**TC1.2-01: Verification email**

Given a user has submitted a valid institutional email
during registration,
When registration is processed,
Then the system shall send an email verification link
to that institutional email address.

Status: Planned.

**TC1.2-02: Unverified account access**

Given a registered user has not verified their email address,
When the user attempts to access protected application features,
Then the system shall deny access until verification is completed.

Status: Planned.

**TC1.2-03: Verified account access**

Given a registered user has successfully verified their
institutional email address,
When the user signs in with valid credentials,
Then the system shall permit access subject to applicable
authorisation rules.

Status: Planned.

#### FR1.3 — Commuter Profile Management

**TC1.3-01: Create and update commuter profile**

Given a verified user is signed in,
When the user enters or updates their faculty or department,
residential area and commuter role,
Then the system shall save the profile information and
display the updated values.

Status: Planned.

**TC1.3-02: Driver vehicle information**

Given a verified user selects the driver role,
When the user enters vehicle type, registration number
and fuel-efficiency category,
Then the system shall save the vehicle information
to the user's commuter profile.

Status: Planned.

**TC1.3-03: Verification badge and Green Commuter Tier**

Given a verified user has a commuter profile,
When the user views the profile,
Then the system shall display the verification badge
and cumulative Green Commuter Tier according to
the project's defined calculation rules.

Status: Pending confirmation of Green Commuter Tier rules.

**TC1.3-04: Student ID image minimisation**

Given a user is registering or editing their profile,
When the user provides the required profile information,
Then the system shall not require or collect
a student ID image.

Status: Planned.


### 3.2 Module 2 — Ride Matching and Location-Based Dispatch

Pending acceptance criteria for FR2.1–FR2.3.

### 3.3 Module 3 — Booking, Cost-Sharing and Coordination

Pending acceptance criteria for FR3.1–FR3.3.

### 3.4 Module 4 — Commuter Safety and Emergency

#### FR4.1 — Trip Records

**TC4.1-01: Authorised trip record access**

Given a user is a participant in a trip,
When the user requests the trip record,
Then the system shall permit access to the record.

**TC4.1-02: Unauthorised trip record access**

Given a user is not a participant in a trip,
When the user attempts to access that trip record,
Then the system shall deny access unless the user has an authorised administrative role.


#### FR4.2 — Emergency SOS

**TC4.2-01: SOS activation during an active trip**

Given an authenticated user is participating in a trip with status `in_progress`,
When the user presses and holds the SOS button for three seconds,
Then the application shall activate the SOS workflow and present the available emergency messaging options.

Status: Planned.

**TC4.2-02: Early release cancels SOS activation**

Given an eligible user is viewing an active trip,
When the user releases the SOS button before three seconds,
Then the SOS workflow shall not activate and no emergency message composer shall open.

Status: Planned.

**TC4.2-03: SMS emergency message**

Given an eligible user has activated SOS and selects SMS,
When the application launches the SMS composer,
Then the composer shall contain a pre-filled emergency message with available trip details and location information, and the user shall remain responsible for sending it.

Status: Planned. Recipient pending team confirmation.

**TC4.2-04: WhatsApp emergency message**

Given an eligible user has activated SOS and selects WhatsApp,
When the application launches the supported WhatsApp messaging flow,
Then the application shall provide a pre-filled emergency message for the user to review and send, subject to the selected integration's capabilities.

Status: Planned. Recipient and routing method pending team confirmation.

**TC4.2-05: Location permission denied**

Given an eligible user has activated SOS but location permission is denied,
When the emergency message is prepared,
Then the application shall allow the messaging workflow to continue without live location and shall not represent unavailable location data as current.

Status: Planned.

**TC4.2-06: Current location unavailable**

Given an eligible user has activated SOS and current location cannot be obtained,
When the emergency message is prepared,
Then the application shall indicate that current location is unavailable and allow the messaging workflow to continue.

Status: Planned.

**TC4.2-07: Messaging application unavailable**

Given an eligible user has activated SOS,
When the selected messaging application cannot be opened,
Then the application shall inform the user and offer an available supported alternative, without claiming that an emergency message was sent.

Status: Planned. Fallback behaviour pending team confirmation.

**TC4.2-08: SOS database logging failure**

Given an eligible user has activated SOS and the Supabase alert-recording operation fails,
When the user proceeds with emergency messaging,
Then the application shall not block the external messaging handoff solely because SOS logging failed.

Status: Planned. SOS logging requirement pending team confirmation.

**TC4.2-09: Unauthorised SOS database insertion**

Given an authenticated user is not a participant in a specified trip,
When the user attempts to insert an SOS alert referencing that trip,
Then the backend shall reject the unauthorised insertion.

Status: Planned. RLS policy amendment pending Member 2 review.

**TC4.2-10: SOS insertion for an inactive trip**

Given a user participated in a trip that is completed or cancelled,
When the user attempts to create an SOS alert for that trip,
Then the backend shall reject the insertion under the proposed active-trip-only rule.

Status: Planned. RLS policy amendment pending Member 2 review.

**TC4.2-11: No false message-delivery confirmation**

Given the application has opened an SMS or WhatsApp composer,
When the user returns to UMPooling without independently verified delivery confirmation,
Then the application shall not display a message stating that the emergency message was successfully sent.

Status: Planned.

**Pending team decisions**

- Final emergency recipient(s) and routing rules.
- Whether SOS logging in Supabase is mandatory.
- SOS alert read, update, resolution and deletion permissions.
- Fallback when SMS and WhatsApp cannot be opened.
- Final SOS interface and accessibility behaviour.
  

#### FR4.3 — Ratings and Reviews

**TC4.3-01: Post-trip rating**

Given a user has completed a trip with another participant,
When the user submits a valid rating and review,
Then the system shall record the rating for the relevant trip and participant.

**TC4.3-02: Unauthorised rating**

Given a user did not participate in a trip,
When the user attempts to submit a rating for that trip,
Then the system shall reject the submission.

Note: The existing ratings INSERT policy requires review.

### 3.5 Module 5 — AI Commuting Assistant and Climate Impact Analytics

Pending acceptance criteria for FR5.1–FR5.3.

## 4. Non-Functional Requirements Testing

### 4.1 NFR1 — Performance

The project specifies the following targets:

- Ride-request notification delivery within 5 seconds
- Screen loading within 3 seconds
- Performance with up to 100 concurrent users

The test environment, measurement tools, sample sizes and procedures remain to be confirmed.

### 4.2 NFR2 — Security and Privacy

Planned tests include:

- Rejecting unauthorised account access
- Restricting protected features to verified UM users
- Preventing access to another user's private trip records
- Testing Supabase Row Level Security policies
- Verifying restrictions on live location sharing

## 5. Test Environment and Test Data

To be confirmed against the team's actual implementation.

## 6. Defect Tracking

Defects will be recorded with a test ID, description, expected result, actual result, severity and resolution status.

## 7. Open Decisions

| Item | Status |
|---|---|
| Final trip lifecycle approval | Pending Member 2 confirmation |
| Lifecycle functions found in live database; migration history and runtime tests pending |
| SOS participants and recipient rules | Pending team confirmation |
| SOS location-permission handling | Pending team confirmation |
| Trip-record retention period | Pending team confirmation |
| Ratings authorisation policy | Requires review |
| Test tools and environments | Pending confirmation |

## 8. Test Results

Not yet executed. Actual results and supporting evidence will be recorded during the testing phases.
