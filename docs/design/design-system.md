# UMPooling Design System

**Source:** [WIA2007 MAD Project — Figma](https://www.figma.com/design/ml2Idb1p9ZJIkOZW7HLhXo/WIA2007-MAD-PROJECT?node-id=0-1)  
**Platform:** Android, Kotlin, Jetpack Compose / Material 3  


## 1\. Visual direction

UMPooling uses a **warm peach and coral palette**, deep brown text, translucent white surfaces, soft gradients, shadows, and generously rounded cards and pill-shaped controls. The experience should feel approachable, contemporary, and campus-oriented. The source frames are predominantly **390 × 844 px**, with some vertically extended screens; treat this as a reference viewport, not a fixed device size.

## 2\. Color scheme

### 2.1 Core tokens

|Semantic token|Hex|Purpose|Source status|
|-|-|-|-|
|`background`|`#FDEBE3`|Main warm-peach page background|Observed on representative root frames|
|`backgroundAlt`|`#FDE3D7`|Secondary peach surface / accent background|Observed|
|`surface`|`#FFFFFF`|Cards, sheets, inputs; may be translucent|Observed|
|`textPrimary`|`#1F1A19`|Main text, headings, numbers|Observed|
|`textStrong`|`#2B2523`|Labels, prominent text|Observed|
|`textSecondary`|`#6B5C57`|Descriptions, helper text|Observed|
|`textMuted`|`#7C6C66`|Placeholder / muted information|Observed|
|`coral`|`#F08F76`|Brand accent and decorative elements|Observed|
|`coralLight`|`#F8C4B0`|Subtle coral accents|Observed|
|`accentDeep`|`#8A3524`|Strong warm accent, section labels|Observed|
|`accentLink`|`#B8482F`|E.g. forgot-password action|Observed|
|`error`|`#FF494C`|Error/destructive accent (verify context before using)|Observed|
|`onDark`|`#FFFFFF`|Text on sufficiently dark backgrounds|Observed|

Additional observed neutrals include `#3A2C28`, `#4A3B36`, `#F3E4DD`, and `#F0CFC2`.

**Brand-gradient guidance:** The design contains multiple `GRADIENT\_LINEAR` fills and radial background gradients. Preserve the *actual stops, opacity, angle, and positions* from each Figma node when implementing; do not assume a single gradient definition applies to every button or card. Many dark or coral buttons are gradient-backed rather than flat fills.

### 2.2 Semantic usage rules

* Use `background` for app screens; use the exact radial gradient overlays from Figma where shown.
* Use `textPrimary` for critical information and `textSecondary` for supporting copy.
* Reserve deep warm accents for actions and selected states; use coral as a highlight rather than as small, low-contrast text.
* Do not use color alone to distinguish pending, confirmed, cancelled, selected, or error states. Include text and/or icons.
* A color's appearance in Figma does **not** guarantee accessible contrast. Verify actual foreground/background pairs.

## 3\. Typography

**Primary family:** `Plus Jakarta Sans`, confirmed across the editable text nodes. The Figma file also contains a few `Inter` text nodes. **Playfair Display was not observed in the inspected design**; do not introduce it globally unless the design is deliberately revised.

|Role|Recommended size (sp)|Weight|Examples / evidence|
|-|-:|-|-|
|Hero headline|28–30|ExtraBold (800)|Welcome / login headlines|
|Page heading|22|Bold (700) or ExtraBold (800)|Home, post ride|
|Section heading|16–20|Bold/ExtraBold (700–800)|Impact and statistics|
|Body / field value|14–16|Regular–SemiBold (400–600)|Locations, descriptions, inputs|
|Button label|14–16|SemiBold/Bold (600–700)|Find a Carpool, Log in|
|Field label / metadata|12–13|Medium/Bold (500–700)|Origin, destination, ride metadata|
|Caption / micro-label|11–12|Medium/Bold (500–700)|Rides, rating, status|

These are **implementation role mappings**, derived from recurring source sizes, not an exact catalog of published Figma text styles. Source text also uses fractional sizes (e.g. 13.5 and 14.5 px); round to coherent Compose `sp` values only where visual comparison permits.

**Compose mapping:** load licensed/available Plus Jakarta Sans font files in `res/font/`, define `FontFamily` with 400/500/600/700/800 weights, and configure `MaterialTheme.typography`. Respect user font scaling; do not lock text into clipped fixed-height containers.

## 4\. Spacing, shape and elevation

### 4.1 Spacing scale (proposed implementation tokens)

|Token|dp|Use|
|-|-:|-|
|`space1`|4|Tight icon/label gaps|
|`space2`|8|Compact gaps|
|`space3`|12|Inner grouping|
|`space4`|16|Card contents / field spacing|
|`space5`|20|Screen-side margins and content|
|`space6`|24|Section spacing|
|`space7`|32|Large section separation|

These spacing tokens are a **proposed normalized scale**, not verified Figma variables.

### 4.2 Shape tokens

The Figma file contains many corner radii; most common include **28, 22, 18, 24, 14, and 30 px**. Suggested mappings:

|Token|Radius|Use|
|-|-:|-|
|`radiusSmall`|12 dp|Small controls / chips|
|`radiusMedium`|18 dp|Fields and compact cards|
|`radiusLarge`|24 dp|Main cards / sheets|
|`radiusXL`|28–30 dp|Floating hero panels|
|`radiusPill`|50% of control height|Primary CTA, status chips|

Use `RoundedCornerShape` or pill shapes in Compose. Prefer per-component measured Figma values over applying one radius everywhere.

### 4.3 Glassmorphism and shadows

Observed effects include **background blur, layer blur, inner shadow, drop shadow**, and a glass effect on the Offer a Ride screen.

* Use translucent light surfaces over the peach/gradient background.
* Add subtle elevation and shadow for floating cards; avoid excessive visual noise.
* Preserve Figma blur/shadow parameters where available; the document does not invent exact opacity, blur, or offsets.
* Keep text on an opaque or sufficiently contrasted backing when background blur/transparency reduces legibility.
* Compose `Modifier.blur` blurs the element itself, **not automatically the content behind it**; true backdrop blur may require a platform-specific implementation or visual fallback.

## 5\. Component library

The following are **reusable component candidates inferred from the 19 screens**. The Figma file contains mostly frames rather than a comprehensive published component set; only two named component nodes were detected in the page traversal. Create Compose components even where Figma layers are not formally components.

|Component|Variants / states|Usage|
|-|-|-|
|`PrimaryButton`|Default, pressed, disabled, loading|Log in, Get started, Find a Carpool, Post a ride|
|`SecondaryButton`|Outline/light, pressed, disabled|Register, alternate actions|
|`AppTextField`|Empty, focused, filled, error, disabled|UM email, password, locations|
|`LocationInput`|Origin, destination, swap|Search and post ride|
|`DateTimePickerField`|Unselected, selected, error|Booking departure|
|`SeatSelector`|Available count, selected count, disabled|Booking and ride offers|
|`RideCard`|Available, matched, saved, confirmed|Search and recommendations|
|`RideDetailsCard`|Driver, route, time, seats, price|Trip details|
|`StatusChip`|Confirmed, pending, verified, error|Booking and identity|
|`Avatar`|Photo, initials, placeholder|Driver and passenger identity|
|`BottomNavigation`|Selected, unselected, badge|Main app destinations|
|`NotificationRow`|Unread/read, type, enabled/disabled|Notification center and preferences|
|`ToggleRow`|On/off, disabled|Notification settings|
|`TripTicket`|Confirmed, cancelled/completed|Confirmed trip|
|`MetricCard`|CO₂ saved, shared rides, ratings|Sustainability dashboard and profile|
|`ProgressIndicator`|Determinate / indeterminate|Sustainability progress and loading|
|`ChatBubble`|Incoming/outgoing, timestamp|Chat|
|`SOSAction`|Idle, confirm, triggered|Safety flow|

### Component anatomy

**Ride card:** route origin/destination, date/time, available seats, suggested contribution, driver details/rating, status, and primary action as applicable.

**Form field:** persistent label, current value or hint, optional leading/trailing icon, validation/error text, focus indicator, and supporting instructions.

**Navigation:** consistent icons, selected-state label, clear current destination, and back behavior matching Figma's prototype intent.

**Trip ticket:** departure, pickup, destination, driver/passenger details, seat allocation, contribution, verification and booking reference.

### State rules

* Define loading, empty, success, error, disabled, and offline states for data-dependent components.
* Keep navigation and form state in appropriate Compose state holders/ViewModels.
* Avoid making decorative Figma elements clickable unless they represent a genuine action.
* Use consistent icon sizing, semantic labels, and state transitions.

## 6\. Accessibility rules

The following are **acceptance criteria**, not claims that the current Figma file already passes accessibility testing.

1. **Contrast:** Target WCAG AA: minimum **4.5:1** for normal text, **3:1** for large text, and **3:1** for essential UI boundaries and graphical controls. Evaluate gradients/translucent backgrounds at their worst-case location.
2. **Touch targets:** Interactive controls should have a target of at least **48 × 48 dp** (Android recommendation), even when the visible icon is smaller.
3. **Text scaling:** Support system font scaling; avoid clipped labels, fixed text boxes, or truncation of critical trip information.
4. **Screen readers:** Give actionable icons meaningful `contentDescription` or Compose semantics. Hide purely decorative icons from accessibility services.
5. **Keyboard/focus:** Ensure logical traversal order, visible focus indication, correct input types, IME actions, and accessible error messages.
6. **Color independence:** Pair colored statuses with explicit labels such as “Confirmed”, “Pending”, or “Cancelled”.
7. **Motion:** Keep animations subtle and nonessential; avoid conveying required information through motion alone.
8. **Safety actions:** Clearly label SOS controls, avoid accidental activation, and provide explicit feedback after an action.
9. **Forms:** Never rely on placeholder text as the only field label. Associate errors with the relevant field.
10. **Dynamic content:** Announce meaningful updates (e.g. seat confirmed, ride match found) without excessive interruptions.
11. **Localization:** Keep times, dates, RM currency values, and CO₂ units understandable and locale-aware.
12. **Privacy:** Avoid exposing private driver/passenger details unnecessarily in notifications or screenshots.

## 7\. Android Jetpack Compose implementation guidance

Suggested structure:

```text
ui/
  theme/
    Color.kt
    Type.kt
    Shape.kt
    Theme.kt
    Spacing.kt
  components/
    AppButton.kt
    AppTextField.kt
    RideCard.kt
    StatusChip.kt
    AppBottomNavigation.kt
    MetricCard.kt
  screens/
    WelcomeScreen.kt
    LoginScreen.kt
    HomeScreen.kt
    OfferRideScreen.kt
    FindRideScreen.kt
    TripDetailsScreen.kt
    TicketScreen.kt
    NotificationsScreen.kt
    ProfileScreen.kt
```

**Implementation principles:**

* Reuse semantic tokens through `MaterialTheme` and custom design-system definitions rather than scattering hex codes across screens.
* Use `Column`, `Row`, `Box`, `LazyColumn`, `Scaffold`, and responsive constraints instead of absolute screenshot coordinates.
* Prefer reusable components with state parameters and callbacks; separate business logic from composables.
* Apply safe-area/inset handling for status and navigation bars.
* Verify screenshots at the source reference width (\~390 dp) and on narrower/wider devices.
* Do not use the Figma screenshot as a replacement for real composables.
* Download source assets and avoid temporary Figma URLs in production code.

## 8\. Screen inventory reviewed

1. Welcome
2. Log in
3. Register
4. UM email verification
5. Home
6. Offer a ride
7. Voice Request
8. Find a Ride
9. Recommended rides for advance booking
10. Recommended rides for current booking
11. Confirmed trip ticket
12. SOS
13. Rate \& Review
14. Chat
15. Sustainability Dashboard
16. Green Commuter Tier
17. Saved Routes
18. Notifications
19. Profile

## 9\. Design quality checklist

* \[ ] All visible foreground/background color pairs pass the appropriate contrast check.
* \[ ] Fonts and weights match the Figma design.
* \[ ] All actionable controls have accessible touch targets and labels.
* \[ ] Buttons, fields, and status chips have defined interaction states.
* \[ ] All screens work with increased font sizes and small devices.
* \[ ] Glass/gradient effects do not reduce readability.
* \[ ] Actual icons and image assets match the Figma source.
* \[ ] No hardcoded absolute layout positions for normal content.
* \[ ] Navigation, form errors, and empty/loading states are testable.
* \[ ] Visual comparisons have been completed against the Figma frames.

\---

**Evidence note:** Colors, typography family/weights, frame dimensions, screen names, effects, and radius frequency were inspected directly from the editable Figma file. Component taxonomy, semantic naming, spacing scale, accessibility criteria, and implementation architecture are recommendations for the Android app.

