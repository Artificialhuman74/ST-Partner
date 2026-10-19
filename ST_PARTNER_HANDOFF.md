# ST Partner — Delivery App: Full Context Handoff

> **For the next Claude session.** This file is the complete context from a discovery session held on **2026-10-19**. Every item under "Decisions" was confirmed by the user in a structured Q&A, so don't ask about them again. Items marked **(proposal)** are my engineering suggestions and are not confirmed. Items under "Open items" still need the user. Read the whole file before touching code.

---

## 1. The goal (what the user asked for)

Build a **native Android app called "ST Partner"** for the people who deliver Sanathana Tattva orders, both **existing traders** and **new outside delivery riders**. Requirements in the user's words, condensed:

- An entire onboarding flow, with animations designed via **/design** (the user calls this "Claude's design shortcut"; it was not available in the session that wrote this file, and the user is updating Claude Code to get it). Animations named explicitly:
  - **Splash screen**
  - **Logged in** (success moment)
  - **Not logged in → login screen with an animated background** right after the splash
  - **Delivery done** (celebration)
- **/impeccable** (installed skill) should craft the UI/UX using its Android guidance, in the spirit of how **Swiggy / Zomato / Blinkit** rider apps work.
- **"I don't need you to replicate the web UI."** It must be a completely different, intuitive UI built for **low-tech users**.
- Delivery people **must get notified when an order comes in**, and the app must **refuse to work unless notifications are enabled**.
- Delivery people must see **exactly what the customer ordered, in a nice card**.
- A **profile** where they can enter and save addresses and other details.
- **People outside the traders must be able to deliver.** I checked: the website does NOT support this today (see §3).

---

## 2. Repos, environment, tools

| Thing | Value |
|---|---|
| Android project | `/Users/chiranthr/AndroidStudioProjects/STDeliveryApp` (not a git repo yet) |
| Android state | **Blank template.** Views/AppCompat + Material 1.10, AGP 9.2.1, compileSdk 36, minSdk 24, targetSdk 36, Java 11, package `com.example.st_delivery_app`, rootProject `ST-Delivery-App`. No Compose, no Kotlin source files. |
| Website repo | `/Users/chiranthr/Desktop/st/SanathanaTattava` (git, branch `main`, GitHub `Artificialhuman74/SanathanaTattva`) |
| Website stack | React 18 + TS + Vite + Tailwind (frontend, Netlify → `sanathanatattva.shop`); Node/Express + Socket.IO + SQLite `better-sqlite3` (backend); JWT auth; Razorpay; firebase-admin (Google sign-in only) |
| Backend URL (testing) | `https://sanathanatattava-testing.up.railway.app` |
| Backend URL (prod) | **Unknown, different from testing.** Use it for the release build. See Open items. |
| Website docs | `CLAUDE.md` (its deployment section about a laptop + cloudflared tunnel is **outdated**; the backend now runs on Railway), `PRODUCT.md` (consumer-facing brand brief), several `*_FEATURE_SPEC.md` |
| Skills | `impeccable` v4.5.2 at `~/.claude/skills/impeccable` (also vendored in the website repo `.claude/skills/impeccable`). It has `reference/android.md`, `onboard.md`, `animate.md`, `delight.md`, `layout.md`, `shape.md`, `craft-floor.md`. Setup: run `~/.claude/skills/impeccable/scripts/impeccable context` once per session from the project dir. |
| Tests in website | backend Jest (`backend/tests`), Playwright e2e (`e2e/`), load tests (`loadtests/`), CI in `.github/workflows/ci.yml` |

**User working style observed:** wants to be asked lots of questions and answers them all; types quickly and informally. Re-ask a question **with an explanation** when an option's purpose isn't obvious (the user explicitly asked for this once). They want screens to look **crafted, not generic lists**.

---

## 3. What exists today in the website (findings, with file refs)

All paths are relative to `SanathanaTattava/`.

1. **No outside-delivery support.**
   - Every `/api/delivery/*` route uses `requireTraderOrAdmin` (`backend/src/routes/delivery.js:23`, `backend/src/middleware/auth.js`).
   - The web delivery login rejects anyone who isn't a trader or admin (`frontend/src/pages/delivery/Login.tsx`).
   - Auto-assignment `findNearestDealer` only selects `role='trader' AND status='active' AND delivery_enabled=1 AND will_deliver=1 AND availability_status='available' AND pan_verified=1` (`backend/src/services/deliveryAssignment.js:143-185`).
   - **There is no rider role.**
2. **Assignment happens at order creation** (`backend/src/routes/consumer.js:660-734`). Current priority:
   1. H3 nearest available trader
   2. Referral chain (linked trader, then tier-1 parent)
   3. Relaxed fallback: any delivery-capable trader, ordered by pincode match
   4. **Admin** as last resort

   It is assigned outright: the driver can only "accept". There is **no reject, no timer, no cascade**. H3 is resolution 7 with MAX_K_RING 10 (`backend/src/services/h3Service.js:17-18`).
3. **Delivery state machine** (`consumer_orders.delivery_status`): `pending → accepted → packed → out_for_delivery → delivered | failed`.
   - Accept: `POST /delivery/orders/:id/accept`.
   - **Packed deducts inventory from the delivering trader's own stock** via `deductOrderInventory(order.id, delivery_dealer_id || linked_dealer_id)` (`delivery.js:398-465`).
   - Start delivery generates a 6-digit OTP for the customer (in-app notification + email).
   - `verify-otp` marks the order delivered.
   - `fail` needs a reason.
   - `resend-otp` exists.
   - Admin `takeover` exists (`delivery.js:186`).
   - Order lines are tagged `refill | new | standard` by `classifyLine` (`delivery.js:34`).
4. **Notifications** are Socket.IO rooms plus a DB `notifications` table (`user_type` = `dealer|admin|consumer`) plus browser Web Notifications. Rooms exist only for `admin`, `trader:{id}`, `consumer:{id}`, `order:{id}` (`backend/src/websocket/socketServer.js:60-71`). **There is no FCM / mobile push.** `backend/src/services/firebaseAdmin.js` only verifies Google ID tokens; reuse its `ensureInit()` for `admin.messaging()`.
5. **Where an order becomes "paid":** `backend/src/routes/payments.js`, three places: `~L122` (client verify), `~L346`, `~L399` (webhook paths). The post-payment notification block (`~L195-240`) calls `notifyDealerDeliveryAssigned`. **Dispatch must start after payment confirmation** (delivery queries filter `payment_status='paid'`). Put it in one shared function called from all three paths.
6. **Background jobs** are plain `setInterval` in `backend/src/index.js`: the sweeper every 10 min (`~L336-342`), the payout scheduler (`~L328`). Add the dispatch ticker the same way.
7. **Auth:**
   - `POST /api/auth/login` returns `{ token, user }`, and **403 "Account is suspended" for any `status !== 'active'`** (`auth.js:112-133`).
   - `authenticate` middleware also rejects non-active users.
   - Partner Google sign-in uses `POST /api/auth/partner/google` (Firebase ID token). It returns `{exists:false, email, name}` for unknown users and doesn't auto-create; registration goes through `POST /api/auth/partner/google/register`.
   - Trader email registration: `POST /api/auth/register`.
   - **The SMS service is disabled** (`backend/src/services/smsService.js` exports `{}`).
8. **Money:**
   - Only **prepaid** orders reach delivery (no COD).
   - Commission is a % of the order (default `commission_rate` 10) for the **linked** trader (`type='direct'`), plus a tier-1 `override`, inserted at payment (`payments.js:150-163`).
   - Admin-delivered orders reroute the commission to the founder via `commissionRoutingService.js` (keyed on email `barathichiru@gmail.com`). Don't break this.
   - **Two payout paths:**
     - Weekly manual payouts (`payoutService.js`): admin pays and logs a UTR with `method: bank|upi`.
     - **Razorpay Route** linked accounts (`payments.js /onboard`), which require `bank_account_number`, `bank_ifsc`, `bank_account_name` and `pan`. Transfers happen **after the fact** per commission via `razorpay.payments.transfer(payment_id, …)` (`payments.js:895-930`). This makes a post-delivery rider-fee transfer feasible.
9. **Uploads:** `backend/src/middleware/uploadProof.js` writes WebP files to `${DATA_DIR}/uploads/<subdir>/<uuid>.webp`, **served publicly** as static files at `/uploads` (`index.js:138-155`). **Rider KYC documents (selfie, licence, PAN) must NOT use this public path.** They need an authenticated route. (Side note: `GET /api/__uploads-debug` is also public.)
10. **Container pickups** (deposit refunds): `GET /delivery/container-pickups`, `POST /delivery/container-pickups/:id/resolve` (photo + `refunded|forfeited`).
11. **Brand tokens** (`frontend/tailwind.config.js`):
    - brand green 600 `#1a6b2e` (primary), 700 `#14532d`, 800 `#0f3d1a`, 900 `#092510`
    - cream surfaces `#fffbf2` / `#fdfaf5` / `#f5ede0` / `#e8dcc8`
    - web font Inter
    - Logo: `frontend/public/logo.webp`, `logo-192.png`
12. The current web delivery portal (`frontend/src/pages/delivery/*`, `APP_MODE === 'delivery'`) **must keep working for traders** during and after this change.

---

## 4. Decisions (confirmed by the user)

### 4.1 Who uses the app
- **One app for both traders and outside riders.**
- **Existing traders log in seamlessly with no new approval.** Outside riders **sign up in the app and wait for admin approval**.
- Login for now is **email + password** and **Google sign-in**. **Phone-number login comes later.** Design the login so it can be added without rework.

### 4.2 Auth and onboarding flow
```
Splash (animated)
  └─ first run → Language (English / ಕನ್ನಡ) → 3-step intro (animated, narrated, skippable)
  └─ Login screen (animated background)
        ├─ email+password OR Google
        ├─ HAS account (trader or approved rider) → "logged in" animation → Home
        │     └─ Google login but profile incomplete (e.g. no address) → onboarding prompts for missing info
        ├─ HAS account, rider pending review → Under-review screen
        └─ NO account → signup flow (address + all required info) → submit → Under-review screen
Under-review screen: "Your profile is being checked by our team, we'll get back to you."
  → approved: celebration → Home
  → rejected: show reason, let them fix and resubmit (proposal)
```
- The user asked for **/impeccable delight + craft** on the under-review screen.

### 4.3 Rider signup data (outside riders)
- **Selfie / profile photo**, via live camera. It's shown to traders at pickup and to customers.
- **Driving licence**, for motorised vehicles.
- **Vehicle type + number plate.** Big icons: Walking / Cycle / Scooter / Bike. The number plate is shown to the trader at handover.
- **Emergency contact, required:** name, relation, phone.
- **Payout via Razorpay Route, like traders:** **bank account number + IFSC + account holder name + PAN**, all required. The user chose Route over weekly-UPI after hearing that Route needs bank + PAN.
- **Saved base addresses** with map pin (see 4.9).
- The user asked for a **proper document-upload UI with neat animations** (use /design + /impeccable).

### 4.4 Dispatch: how an order reaches people
1. **Owner trader.** If the customer is linked to a trader (signed up or ordered with that trader's code), **every order from that customer goes to that trader.** Direct customers go to **the trader covering that area** (H3 nearest). ⚠️ This **reverses today's priority**, where H3 nearest comes before the referral chain.
2. **Trader opted out of delivery** (`will_deliver=0`): **rider search starts at once.** The trader only gets "Pack this order, a rider is coming." They can switch to delivering any time in their profile.
3. **Outside the trader's delivery hours:** the order **waits for the trader's hours**. The 20-minute clock starts when their hours open. The customer sees something like "Will be delivered from 8 AM".
4. **20-minute trader window**, counted from order placement (or hours opening).
   - **First alert:** full-screen call-style ring plus a **spoken announcement** of what the order is and who it's from.
   - **Then a distinct, attention-grabbing alert sound every 2 minutes** until they respond.
   - The trader can **Accept** or tap **"I can't deliver"**.
5. **Accept** → the trader delivers it themselves (4.7).
6. **"I can't deliver" or the 20 minutes run out** → **outside-rider search starts immediately.** The trader is told to **pack it and keep it ready** in the app. The trader sees **who is picking up: name, photo, phone, vehicle, live location**.
7. **Rider search:** offered to the **nearest eligible rider first** (ringing card with countdown, Accept / Skip). On skip or timeout it goes to the **next nearest**. The same 2-minute re-alert behaviour applies.
8. **No rider accepts:** keep offering as riders come online. **After 30 minutes, send admin an urgent alert.** Admin can take over (the existing takeover endpoint) or push the trader to deliver.
9. **One active order per rider** for v1. No batching.
10. **3 missed or skipped offers in a row → rider auto-set Offline**, with a gentle message: "You missed 3 orders, so we've set you Offline. Tap Go Online when you're ready."
11. **Traders as riders:** a trader may **opt in to taking nearby rider jobs** (other traders' escalated orders). It **must be impossible to switch on by accident**; put a deliberate confirmation step in front of it (proposal: explanation sheet + hold-to-confirm).

### 4.5 Alerts and notifications
- **Full-screen, call-style ringing alert**, like an incoming phone call: looping ringtone + vibration until they act. It must arrive **when the app is in the background or swiped away, and when the phone is locked**.
- The **first** alert for an order includes a **voice announcement** in the chosen language: what the order is and who it's from.
  - For traders: products + customer name/area.
  - For riders: products + pickup trader/area.
- Later reminders use a **proper alert sound** ("the one which actually catches their ear", not the default), **every 2 minutes** until accepted.
- What I told the user about Android behaviour (keep this promise accurate):
  - **Locked / screen off:** the ringing screen shows over the lock screen.
  - **Phone unlocked and in use:** Android shows a large ringing heads-up banner, not a takeover. This is system behaviour; WhatsApp calls work the same way.
  - Failure modes, all covered by the permission gate:
    - Android 14+ on Play: full-screen intent isn't granted by default.
    - OEM battery killers.
    - Force-stop blocks all pushes until the app is reopened.
    - Do Not Disturb.
    - No internet: the alert arrives on reconnect.

### 4.6 Permission gate (hard block)
**"Go Online" stays locked until every item is green.** It's a checklist with big "Turn on" buttons:
1. Notifications (POST_NOTIFICATIONS, Android 13+)
2. Location (precise)
3. Battery: "Don't restrict / Unrestricted", plus OEM **Autostart** on Xiaomi/Redmi/POCO, Oppo/Realme, Vivo, OnePlus, Samsung (deep links where possible, picture guides where autostart can't be detected)
4. **Full-screen alerts** (Android 14+: `NotificationManager.canUseFullScreenIntent()` → `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`)
5. **Allow alerts during Do Not Disturb**, **mandatory** (notification policy access + channel `setBypassDnd(true)`)

Re-check on every app resume and before Go Online. Show a persistent warning if anything gets turned off later.

### 4.7 Flows

**A. Trader delivers it themselves**
- Accept → **Pack** (stock deducts from the trader, as today) → **Start delivery** (customer gets OTP) → Navigate (Google Maps link) → **Customer OTP + mandatory photo of the package at the door** → Delivered celebration.
- Keep compatibility with the existing endpoints and web portal.

**B. Trader packs, rider delivers** (escalated or non-delivering trader)
- **Trader side:** "Pack this order" card → mark packed (stock deducts from the trader) → "Ravi is coming" screen (photo, vehicle + plate, phone, live map/ETA) → **rider shows a pickup code on their phone, trader types it in**, seeing the rider's photo + plate on the same screen before typing → handed over.
- **Rider side:** offer → Accept → **Google Maps link to the trader** → arrive → show pickup code → picked up → Maps link to customer → **Call customer** button → **customer OTP + door photo** → Delivered celebration showing "+₹fee".

**C. Delivery fails** (customer not home, phone off, wrong address…)
- Big reason buttons → **return the oil to the trader** with the same code-based handover (rider shows code, trader types) → **order re-offered the next day**.
- (proposal) The next day it re-enters dispatch at the trader's hours, trader-first like a new order.
- No inventory change on return, because the stock is physically back with the trader.

### 4.8 The order card
Show all of the following, but **lay it out so the most useful thing catches the eye first: what the order actually is** (user: "use /impeccable layout here").
1. **Product photos, size, quantity**, e.g. big bottle photo with "2 × 1 L Groundnut Oil"
2. **Refill vs New container tag** per line ("REFILL: collect the empty bottle" / "NEW bottle"). The backend already provides `line_type`.
3. **Customer name, area, distance**, e.g. "Lakshmi · Jayanagar 4th Block · 3.2 km". (proposal) Full address only after accepting.
4. **Earnings + order value**: riders see "You earn ₹X"; traders see their commission.

### 4.9 Online / Offline and location
- **One big "Go Online" switch.** Offers only arrive while Online. It maps onto the existing `availability_status`.
- Riders: **the Online switch is enough**, with no weekly schedule. (proposal) Auto-offline after delivery hours end or after long idle.
- **Saved base locations** ("Home", "Shop", …) with a map pin. When going Online, they **pick where they're starting from**. **Also check live GPS every so often:** if they're at the chosen base, match from the base; if not, **use live GPS for matching**.
- Live location is shared with the trader (incoming rider) and the customer (see 4.13) during an active order.

### 4.10 Delivery hours (traders)
- **Each trader sets which hours they deliver on each day** in their profile.
- **Admin can change these hours too.**
- The user wants this to be a **beautiful, crafted UI, "not the generic list ones"** (/impeccable craft).

### 4.11 Money
- **Prepaid only.** Riders never handle cash.
- **Riders get a fixed amount per delivery.**
- On rider-delivered orders, **the trader gets a reduced commission.**
- **The admin sets both numbers in admin settings**: "Rider fee per delivery (₹)" and "Trader's share on rider-delivered orders (%)". Use placeholder defaults.
- Rider payout uses **Razorpay Route**, like traders. (proposal) On delivery, create a `commissions` row for the rider (new `type='delivery_fee'`, `trader_id` = rider's user id); admin transfers it from the order's captured payment with the existing `/payments/transfer` flow. Reduce the linked trader's pending `direct` commission to the configured share. **Don't touch founder routing** (`commissionRoutingService.js`).

### 4.12 Privacy
- **Customer phone: a "Call customer" button only during the delivery.** The digits are never written on screen, and the button disappears once delivered. No masked-calling provider.

### 4.13 Customer website changes
- On order tracking, once a rider is on the way: **rider name, photo, vehicle, call button and a live map**, Swiggy-style.

### 4.14 Admin website changes (in scope)
- **Rider approval queue**: view selfie / licence / PAN / bank, approve or reject with a reason.
- **Settings** for rider fee and trader share.
- **Edit any trader's delivery hours.**
- **Live fleet view.** One already exists at `/delivery/fleet/orders`; extend it with riders and dispatch stage.
- **Urgent alert** when no rider is found after 30 minutes.

### 4.15 Container pickups
- **In the app for v1, traders only.** Riders don't handle refunds yet.

### 4.16 Language and voice
- **English + Kannada.** The language picker is the **first onboarding screen**. Text and the voice announcement both follow the chosen language.

### 4.17 Distribution, naming, stack
- **Google Play Store.**
- App name under the icon: **"ST Partner"**.
- **Kotlin + Jetpack Compose + Material 3.** Rename the package from `com.example.*`. (proposal) `com.sanathanatattva.partner`. The applicationId is permanent once on Play, so confirm before the first upload.
- **Build variants:** debug → `https://sanathanatattava-testing.up.railway.app`; release → prod URL (TBD).
- **Scope: Android app + backend changes + admin pages**, done on a **new git branch** in the website repo.

---

## 5. Design direction

- **Brand:** Sanathana Tattva **colours + logo**, but its **own work-app feel**: a tool, not the website's storytelling. Huge type, high contrast that's readable in sunlight, big touch targets (≥48dp; bigger for primary actions), minimal text, icon + word together, one clear next action per screen. Built for **low-tech users**, many reading Kannada.
- **Motion:** **calm everywhere** (smooth, viscous ease-out, no bounce, matching the brand's "pour like oil" principle), **with a real celebration at wins**: Delivered, You're approved, Logged in. Honour the system "remove animations" setting.
- **Anti-goals:** don't replicate the web UI; no generic settings-style lists for the schedule/profile; don't put an iOS look on Android (Material 3 components and the system Back gesture are the rulebook; see impeccable `reference/android.md`).
- **Tooling the user wants:**
  - **/design** (Claude's design shortcut) for the animations: splash, animated login background, logged-in, delivery done, under-review, document upload.
  - **/impeccable** for UI/UX craft. Specifically called out:
    - under-review screen: **delight + craft**
    - order card: **layout**, so what's ordered stands out first
    - document-upload flow: **neat animations**
    - trader delivery-hours screen: **craft**
- (proposal) The Android project has no `PRODUCT.md`. The website's `PRODUCT.md` describes **consumers**, not delivery people. Run `impeccable init` in the Android project to write an app-specific PRODUCT.md from §1, §4 and §5 so impeccable's context loader picks up the right audience.

### Screen inventory
1. Splash (animated)
2. Language picker (first run)
3. 3-step narrated intro: phone rings with an order → collect/pack → deliver with code + photo
4. Login (animated background): email/password, Continue with Google, "New here? Join as a delivery partner". Leave room for phone login later.
5. Logged-in success moment
6. Rider signup, multi-step: basics → base address(es) with map pin → selfie → vehicle → driving licence → PAN + bank → emergency contact → review & submit
7. Missing-info onboarding for Google users (e.g. address)
8. Under review / approved celebration / rejected → fix & resubmit
9. Permission gate checklist
10. Home: Go Online switch, chosen base, today's deliveries + earnings, active task card
11. Incoming order: full-screen ring (trader: Accept / I can't deliver, with time left of 20 min; rider: Accept / Skip, with countdown)
12. Trader self-delivery steps (pack → start → navigate → OTP + door photo → done)
13. Trader pack-for-rider: pack → rider-coming live screen → enter rider's code → handed over
14. Rider active order: to trader → show code → to customer → call → OTP + photo → done (+₹)
15. Failed delivery: reason buttons → return-to-trader handover
16. History and earnings (weekly)
17. Profile: photo/details, saved base locations, **delivery hours (traders)**, **"Also take rider jobs" (traders, guarded)**, payout details, language, permission status, call support/admin, logout
18. Container pickups (traders)
19. Auto-offline after 3 missed offers

---

## 6. Technical plan (proposal, none of it built yet)

### 6.1 Android
- Kotlin, Compose, Material 3, Navigation Compose, ViewModel + StateFlow, DataStore (token, language, onboarding flags), Retrofit/OkHttp + kotlinx.serialization (or Ktor), Coil (product images), CameraX (selfie/documents), Play Services Location, Firebase Messaging, Credential Manager + Firebase Auth (Google → Firebase ID token → `POST /api/auth/partner/google`), Android TextToSpeech (`kn-IN` / `en-IN`), MapLibre Native on OpenStreetMap tiles with OSRM routes, styled like Google Maps, for every in-app map and its live updates (see §10). Turn-by-turn navigation is handed off to the Google Maps app via intents.
- Socket.IO client (optional) for live updates while the app is open; **FCM is the source of truth for alerts**.
- **Ringing implementation:**
  - High-priority **FCM data message** → `FirebaseMessagingService` posts a notification on a dedicated channel (custom ringtone, `IMPORTANCE_HIGH`, `setBypassDnd`).
  - Use `FLAG_INSISTENT` so the sound loops until acted on, and `setFullScreenIntent` for a lock-screen `Activity` (`showWhenLocked`, `turnScreenOn`).
  - The voice announcement plays from the Activity when it launches, or from the messaging service when only a heads-up is shown.
  - The 2-minute re-alerts come from the **server** (new pushes), so they survive process death.
  - **Verify on real Xiaomi + Samsung devices.**
- **While Online:** a `location`-type foreground service with an ongoing "You're online" notification. It provides live location without `ACCESS_BACKGROUND_LOCATION` and makes OEM kills less likely. Play needs the FGS-type declaration.
- Kannada TTS voice data may be missing on some phones. Check `isLanguageAvailable`; fall back to pre-recorded Kannada clips or English.

### 6.2 Backend (website repo, new branch)
- **Schema** (via `backend/src/database/migrations.js`, keeping its `hasColumn` style):
  - `users.role` gains **`'rider'`**.
  - Rider profile fields: selfie, licence, PAN, vehicle type/plate, emergency contact; bank fields already exist.
  - `review_status` (`pending_review|approved|rejected`), `review_note`, `reviewed_by`, `reviewed_at`.
  - Trader flag `takes_rider_jobs`.
  - `missed_offer_streak`, live location columns.
  - New tables:
    - `device_tokens` (user_id, fcm_token, app_version, updated_at)
    - `delivery_hours` (user_id, weekday, start, end, enabled, updated_by)
    - `user_addresses` (saved bases: label, address, lat, lng, h3, is_default)
    - `dispatch_offers` (order_id, user_id, kind trader|rider, offered_at, expires_at, last_alert_at, alert_count, response accepted|declined|skipped|expired)
  - `consumer_orders` additions: `dispatch_stage` (`awaiting_hours|trader_offer|rider_search|rider_assigned|picked_up|returning|…`), `trader_offer_expires_at`, `rider_id`, `pickup_code`, `rider_fee`, `escalated_at`, `escalation_reason` (`timeout|declined|non_delivering_trader`), `admin_alerted_at`, `door_photo_url`, `attempt_count`, `retry_on`.
- **Auth:**
  - Let pending/rejected riders log in and read **their own review status**. Today both `/login` and `authenticate` reject non-active users.
  - Add rider register (email/password and Google).
  - Add `requireApprovedDeliveryActor` (trader | approved rider | admin).
  - Leave existing trader logins untouched.
- **Dispatch service + ticker** (`setInterval` in `index.js`, ~every 30 s), driving everything in §4.4:
  - open trader windows when hours start
  - re-alert every 2 min
  - escalate at 20 min or on decline
  - cascade rider offers nearest-first (distance from the **trader**)
  - auto-offline after 3 misses
  - admin alert at 30 min
  - next-day retry

  Start dispatch from the single post-payment hook (§3.5). **Reorder assignment priority in `consumer.js:660-734`** to linked trader first, and replace the immediate "admin last resort" with the escalation path.
- **Push service:** `admin.messaging()` via `firebaseAdmin.js`'s `ensureInit`. Store and refresh FCM tokens, prune invalid ones.
- **Delivery routes:**
  - Extend `delivery.js` for riders: offers accept/skip, trader decline, pack-for-rider, pickup-code verify, door-photo upload (with OTP verify), fail → return-handover.
  - Keep the existing endpoints and responses working for the web portal.
- **Sockets:** add a `rider:{id}` room, and the `'rider'` user_type in `notificationService.js`.
- **Private KYC storage:** an authenticated admin-only route for selfie/licence/PAN images, not public `/uploads`.
- **Money:** settings keys for rider fee and trader share; a rider `delivery_fee` commission row; adjust the trader's pending commission on rider-delivered orders; extend Route onboarding (`/payments/onboard`) to riders.
- **Admin frontend:** rider approvals, settings, hours editor, fleet view with dispatch stage and riders, urgent no-rider alerts.
- **Consumer frontend:** tracking card with rider info and live map (`frontend/src/pages/consumer/Orders.tsx`).
- **Tests:** add Jest tests for the dispatch state machine (timers with fake clock), handover codes, commission split. Run the existing Jest + Playwright suites; the web delivery portal must stay green.

### 6.3 Suggested build order
1. Backend foundations: rider role + review, device tokens + FCM push, settings.
2. Android shell: Compose/M3 setup, package rename, variants, API client, auth (email + Google), language, splash/login/onboarding, under-review.
3. Permission gate + Online switch + location FGS + ringing alert pipeline (prove it on real devices **early**; it's the riskiest part).
4. Dispatch engine (trader window → rider cascade → admin alert) + trader flows A/B.
5. Rider flow, handover codes, door photo, failure/return.
6. Profile: bases, delivery hours, rider-jobs opt-in, payouts; container pickups.
7. Admin pages + customer tracking.
8. Design passes: /design animations + /impeccable craft/polish/audit (native), then Play submission prep (FGS + full-screen-intent declarations, data safety form).

---

## 7. Constraints and don't-break list
- The web delivery portal for traders keeps working.
- **Bengaluru-only** orders (recent commit) stay as they are.
- Founder commission routing (`commissionRoutingService.js`) and existing commission/payout history stay intact; only `pending` rows may change.
- Prepaid-only delivery queries (`payment_status='paid'`) stay.
- Accessibility: WCAG AA contrast, large touch targets, honour reduced motion, readable for older users and second-language readers.
- Never show the customer's raw phone digits to riders.

## 8. Open items (need the user)
1. **Production backend URL** for the release build.
2. **Firebase:** add an Android app to the existing Firebase project (the one used for Google sign-in), download `google-services.json`, and register the debug + release **SHA-1/SHA-256**. Confirm FCM is enabled.
3. **Default numbers** for rider fee (₹) and trader share (%). Admin can edit them; placeholders are fine until then.
4. **Rider offer countdown length** per rider before moving to the next (proposal: 60–120 s). How it interacts with the 2-minute re-alert needs one decision.
5. **Vehicle capacity rule** (e.g. no 15 L tins far away on a cycle): mentioned in Q&A as a reason to collect vehicle type, but never defined.
6. **Alert sound + ringtone assets** (licensed or custom), and whether to use TTS or **recorded Kannada voice prompts**.
7. **Razorpay Route for riders:** confirm Route KYC accepts individual riders and that post-delivery transfers from the order payment are allowed in your account. (`payments.transfer` from captured payments is already used for traders.)
8. Final **applicationId** (proposal: `com.sanathanatattva.partner`) and the Play Console account; Play policy declarations for full-screen intent, foreground-service location, and the data-safety form (KYC images).
9. Existing traders who are **not PAN-verified**: they can log in, but today's rule stops them getting delivery offers until verified. Keep that? (proposal: yes)
10. A high-res/vector **logo** for the splash and adaptive icon.

---

## 9. Q&A log (user's answers, close to verbatim)
| Topic | Answer |
|---|---|
| Joining | Sign up in app + admin approves. "I need the already approved traders to have a seamless login without having to be approved again. Also I need a screen that tells them 'your profile is being checked by our team, we'll get back to you'. /impeccable delight and craft this UI." |
| Users | Both traders and outside riders, one app. |
| Login | "Phone number login should be there, it'll come later, but for now make it email + password. They can even have Google login, but if logged in through Google they should be prompted in the onboarding UI to enter their address. No account → prompted to enter address etc., all required info → put under review." |
| Pickup | Nearest trader packs, rider picks up. |
| Dispatch | "A trader handles delivery for an area… whoever places an order in that area or with their trader code. If a customer is linked to trader B, all that customer's orders are delivered by trader B. Keep a 20 min timer: if the linked trader doesn't accept within 20 mins of the order being placed, search for an external one and the trader is asked to pack the order and keep it ready in their delivery app. The trader hands over the packed order with an OTP-based transaction. They can see who is picking up, their phone, current location etc. Delivery guys get a Google Maps link to the trader. In any case, send an alert notification with a proper ear-catching sound, not the usual one; one voice that says what the order is and who it came from for the first notification; subsequent ones are proper alert sounds, every 2 mins if they don't accept." |
| Alert style | Full-screen ringing like a phone call. |
| Permissions | Hard block: can't go online until all are on. |
| Duty | One big Go Online switch. |
| Trader decline | Rider search starts right away. |
| Rider offer | Nearest rider first, then next. |
| Rider pay | Fixed amount per delivery. |
| Trader commission on escalated orders | Reduced commission. |
| Order card | Product photos/size/qty; refill vs new tag; customer name/area/distance; earnings + order value. "Use /impeccable layout here to make the useful stuff catch their attention first, like what the order actually is." |
| Proof | Code + photo of package at door. |
| Failed | Return to trader, retry tomorrow. |
| Payment | Prepaid only. |
| Addresses | Base locations, pick one when going online, "but also use live GPS every now and then to see if they are in the designated base location… if not then use live GPS for assigning orders." |
| ID docs | Selfie + driving licence, "with a proper UI here with neat animations, /design to figure it out using /impeccable." |
| Vehicle | Type + number plate. |
| Emergency contact | Required. |
| Payout | Asked me to check whether Razorpay needs bank details. After learning Route needs bank + IFSC + PAN and weekly manual supports UPI → chose **Razorpay Route like traders**. |
| Language | English + Kannada. |
| Review method | "/design is Claude's design shortcut. I need to update Claude Code, so once you're done with all the questions, make one full context transfer md file so I can continue with another Claude." (→ this file) |
| Brand | Brand colours + logo, own work-app feel. |
| Motion | Calm everywhere, celebration at wins. |
| Scope | App + backend + admin pages. |
| Install | Google Play Store. |
| Backend | Railway: `https://sanathanatattava-testing.up.railway.app` (testing; prod is different). |
| Stack | Kotlin + Compose + Material 3. |
| No rider | Keep trying, alert admin after 30 min. |
| Hours | "Let the trader decide when they want to deliver each day, set in their profile page. Beautiful UI, not the generic list ones, /impeccable craft. Admin can alter these timings also." |
| Batching | One at a time. |
| Containers | Traders only in v1. |
| Off-hours order | Wait for the trader's hours. |
| Rider hours | Online switch is enough. |
| Numbers | Admin sets both in settings. |
| Customer view | Rider name, photo, call + live map. |
| Non-delivering traders | They only pack; rider search starts at once. |
| Trader-as-rider | Yes if opted in, "but make sure they won't click this by accident." |
| Missed offers | Auto-offline after 3 in a row. |
| Intro | 3 illustrated steps with voice. |
| App name | ST Partner. |
| Handover | Rider shows code, trader types it. |
| Background alerts | Asked whether the call alert works when the app isn't open → explained (§4.5). |
| Phone privacy | Call button only during the delivery. |
| DND | Mandatory. |

---

## 10. Design session decisions (2026-10-19, confirmed by the user)

Designs live on the Design canvas **https://claude.ai/artifact/Tza9m8CmgdNBhcMciRqwjP** (onboarding, signup, review, permission gate, home, ringing alerts, order card, Delivered).

- **Look:** cream daylight ground `#fffbf2`, deep green `#1a6b2e` actions, ink `#0f2417`. Gold accents use the **solid oil gold `#e2a72e` with dark text**, never a pale tint (user asked for gold "darker, like the slider and the oil"). Ringing screens use a deep green full-bleed `#0f3d1a` with gold-bordered buttons.
- **Font:** Anek Kannada (covers Latin + Kannada). **Motion motif:** golden oil drop (falls, ripples, fills).
- **Language:** English screens + Kannada twins of key screens. The language picker animates a mini phone whose labels pour from one language to the other, plus "You can change this anytime in Settings". (Open: the handoff puts language under Profile; confirm the word "Settings".)
- **Accept gesture:** **slide to accept** on the full-screen ring (prevents pocket accepts).
- **Rider offer window (resolves §8 item 4):** riders get the **same 20 minutes as traders**, with the 2-minute re-alert, until they tap Skip. Only then (or at 20 min) does it move to the next rider. Note: a rider who ignores an offer holds it for up to 20 min; the 30-min admin alert and 3-miss auto-offline still apply.
- **Refill vs new can must be unmissable:** every order line shows a loud banner — **"REFILL ONLY · Don't take/pack new cans"** (solid gold, crossed-out can icon) or **"TAKE/PACK A NEW CAN · They paid for a new can"** (solid green, can-plus icon). The same appears as chips in the heads-up notification and on the pack checklist. (Proposal: the voice announcement says it too.) Products are steel cans, 2.8 L / 5 L (`container_type`), so designs show cans, not bottles. (Open: does a refill swap a full can for the empty one, or is oil poured into the customer's can? This decides the banner's second line.)
- **Rejection:** each rejected item gets its own card, reason, fix button and animation, with an "x of n fixed" counter; "Send again" unlocks only when all are fixed. **Implication:** the admin rejection UI (§4.14) must let admin flag individual items with a reason each, not just one reason.
- **Emergency contact copy:** framed as care ("We look out for you"), not as an accident warning.
- **Order card / ring:** what's ordered is the largest element; riders also see the total load ("3 cans · 12.8 L") for vehicle fit, pickup → drop route and "You earn ₹X"; traders see commission + "Paid online".
- Sample values on the canvas (₹45 rider fee, ₹1,240 order, ₹124 commission, names, addresses) are placeholders.
- **Real maps in production (user requirement, revised 2026-10-19):** every in-app map (trader "On the way", trader "Rider is coming", rider "Go to the shop" and "To the customer") is a **live OpenStreetMap map with OSRM routing**, **styled to look like Google Maps** (Google-like palette, road hierarchy, pins and blue location dot). It shows the real road route (OSRM polyline), the user's live location, the destination pin, and on "Rider is coming" the rider's live position, so **all live updates happen in the OSRM map**. **Turn-by-turn navigation is handed off to the Google Maps app** (navigation intent) for pickup and for delivery; the app never draws its own turn-by-turn. No Google Maps SDK / Routes API / billing key needed. (proposal) Android: MapLibre Native with a custom Google-like vector style; tiles from a hosted OSM vector-tile provider or self-hosted; **self-host OSRM** with the Karnataka/India extract, because the public OSRM demo server is not allowed for production traffic. Show the required "© OpenStreetMap contributors" attribution. (proposal) The website's customer tracking map uses the same OSM/OSRM stack (MapLibre GL JS) so both look alike. The canvas maps are stand-ins only.
- **Accents are owned by the user:** the user edits accent colours directly on the canvas. Don't re-tint existing screens without being asked. App screens use `#14532d` as the default green; website/admin pages keep the website's own tokens (Inter, brand-600 `#1a6b2e`, parchment, `.card`/`.badge` styles).
- **Trader's share on rider orders** is shown in admin as "% of the trader's usual commission" (e.g. 70% of ₹124 = ₹87); rider fee is a flat ₹ per delivery. Both placeholders until §8 item 3 is answered.
- **Admin rider review is per item:** selfie, licence front/back, PAN, bank, vehicle, emergency contact, start place each get "Looks good / Problem + reason". Approve is blocked while anything is flagged; "Send back with N reasons" feeds the app's multi-issue Rejected screen.
- **Container pickups in the app** mirror the web portal's rules: refund via store credit (instant), UPI from the trader's phone (screenshot required, admin reimburses) or bank transfer by admin; a damaged can forfeits the deposit with a damage photo and a 48-hour customer dispute window.
- **Trader "also take rider jobs"** is turned on with a 2-second press-and-hold (a tap does nothing), after a plain-language explanation.
- **Customer tracking (website)** shows the 6-digit delivery code with the site's existing wording ("share it only when the agent is at your door with the order in hand"), the rider's first name, photo, vehicle + plate, a Call button and the live map.
- **Visual direction update (user):** ringing and work screens stay light (cream ground, beige cards `#f3e8d4`, green accents); avoid dark full-bleed screens and busy motion. Solid gold only for must-see warnings (REFILL ONLY).

### 10.1 Polish pass (2026-10-19)

- Fixed dead links on the canvas: Approved → Permission checklist; "I can't deliver" on both ringing screens and the heads-up banner → Pack for rider; rider Skip → Home online.
- Added two missing states: **Home · a permission got turned off** (persistent gold warning with "Turn it back on" → permission checklist, per §4.6) and **Website · order waiting for shop hours** ("Will be delivered from 8 AM", no code yet, per §4.4 item 3).
- Still not designed (build from the existing patterns, or design later): **Forgot password**, **Edit profile** (Profile's edit button has no screen), and rider-path labels on the 6-digit code screen (its stepper reads Packed / On the way / Hand over, which is trader wording).

## 11. Kannada coverage gap (do later, required before launch)

**Most screens have no Kannada design yet.** Only these have Kannada versions on the canvas:

| Screen | Kannada |
|---|---|
| Language picker | bilingual by design |
| Login | `LoginKn` |
| Signup 4 · Vehicle | `SignupVehicleKn` |
| Under review | `UnderReviewKn` |
| Ringing · trader | `RingTraderKn` |

**Every other screen (about 55 boards) is English-only** and still needs Kannada copy and a layout check: intro slides, signup steps 1–3 and 5–8, Approved, Rejected, permission checklist + Autostart guide, home states, base picker, auto-offline, heads-up banner, rider ring, order card, Delivered, all delivery screens (trader + rider), failed-delivery reasons, return to shop, earnings, profile, delivery hours, rider-jobs opt-in, start places, payout details, container pickups. Website/admin pages follow whatever the website does for Kannada (out of scope for the app).

Rules for the app build so this is cheap later:
- No hard-coded strings in Compose; every string lives in `values/strings.xml` with a `values-kn/strings.xml` twin (English fallback until translated).
- Layouts must survive Kannada being ~20–30% longer: no fixed-width text boxes, allow two-line buttons, line-height ≥ 1.35 for Kannada.
- Numbers, ₹ amounts, codes and plates stay in Latin digits in both languages (as on the existing Kannada twins).
- Voice announcements (TTS) need Kannada phrasing too, including "REFILL ONLY / PACK A NEW CAN".

## 12. Android build status (2026-10-19)

**Built (step 2 of §6.3, first half):** Kotlin + Compose + Material 3 shell; package/applicationId `com.sanathanatattva.partner` (still confirm before the first Play upload, §8 item 8); minSdk 26, compileSdk 37 (SDK Platform 37 installed), targetSdk 36; debug → testing backend, release → `st.prodBaseUrl` in gradle.properties (empty until §8 item 1 is answered); Anek Kannada bundled (OFL licence in `assets/licenses`); adaptive launcher icon from the emblem; per-app language (en/kn) via AppCompat.
Screens, matching the canvas: animated splash, language picker with its live "whole app switches" preview, 3 narrated intro pages (TTS, Voice on/off, Skip), Login with oil-wave background (real `POST /api/auth/login`), forgot password (real `POST /api/auth/forgot-password`), logged-in celebration, Under review (ready; nothing routes there until the backend reports `review_status`). Home is a stand-in until step 3.
Short phones (< ~830dp tall) get compact Language/Login layouts so nothing important falls below the fold.

**Firebase (done 2026-10-19, resolves most of §8 item 2):** Android app `com.sanathanatattva.partner` ("ST Partner") added to the website's Firebase project **`sanathanatattva`** (account barathichiru@gmail.com) with the Mac's debug SHA-1 `62:5A:A9:DA:1E:04:BE:90:66:68:37:4A:54:60:FC:56:18:CF:C5:7D`; `app/google-services.json` is in the project; Google is enabled as a sign-in provider. Google sign-in flow: Credential Manager → Firebase ID token → `POST /api/auth/partner/google` (existing endpoint); a Google account with no partner account gets a "No partner account yet" sheet. **Still to do:** add the release/Play App Signing SHA-1 + SHA-256 in Firebase before the Play build, or Google sign-in fails in the store version. No Google Maps key is needed (OSRM decision, §10).

**Not yet:** rider signup (needs the backend rider role, step 1), everything from step 3 on.

**Confirmed:** ST support line for partners is **+91 99729 22514** (owner confirmed 2026-10-19).

**Build:** `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. Lint: 0 errors; `MissingTranslation` is a warning on purpose (§11).

**Play screenshots:** `play-store/screenshots/phone-2160` (1080×2160 crops) and `phone-9x16` (1080×1920 framed), captured on the owner's OnePlus CPH2423. They show onboarding only; replace or extend them with the delivery screens (ring, order card, delivered) once those are built, since those sell the app.

### 12.1 Rider joining, review, notifications (built 2026-10-19)

- **Backend** (website repo, branch `st-partner-app`, uncommitted; deployed to Railway **angelic-reverence → testing** only): `routes/rider.js` (signup with KYC photos, email or Google; application status; fix & resend), `routes/adminRiders.js` (list, detail, private photo stream, approve / send back per item), `routes/devices.js` (FCM tokens), `services/pushService.js` (push on approve / send back), `services/kycStorage.js`, `services/riderApplication.js`. Tests: `tests/rider.test.js` (19). Eight older tests in other files fail on `main` too (not caused by this work).
- **Admin website:** `frontend/src/pages/admin/Riders.tsx` ("Riders" in the admin sidebar): counts, Waiting / Approved / Sent back tabs, per-item Looks good / Problem + reason, "Everything looks good" one-tap, photo zoom. Run locally against testing: `VITE_API_URL=https://sanathanatattava-testing.up.railway.app npm run dev:partner` → http://localhost:3001.
- **App:** 8-step signup (draft saved on the phone), live camera for selfie/licence/PAN with a sharpness hint, MapLibre/OpenFreeMap map pin + phone geocoder, IFSC lookup (Razorpay public API), contact picker; Under review polls every 30 s; Approved celebration; Rejected → fix only flagged items → send again; push notifications (channel "Account updates").
- **Images:** phones upload WebP (JPEG on Android 10 and older), ≤1600px. The server keeps each KYC photo twice as WebP: 1600px (zoom) + 480px thumbnail (lists). KYC is served `Cache-Control: private, no-store`, admins only.
- **Later (user request): Cloudflare.** Move image storage to **Cloudflare R2** (cheap storage, no egress fees) behind Cloudflare's CDN cache for *public* images only (product photos, the rider's small avatar shown to shops/customers, via signed or unguessable URLs). **KYC documents must never be publicly cached**: keep them in a private R2 bucket served only through the admin-only backend route.

### 12.2 In progress: going online + order ring (stopped mid-way, 2026-10-19)

- **Backend done** (branch `st-partner-app`, tests pass: `tests/partner.test.js` + `tests/rider.test.js` = 30): `routes/partner.js` (home, online/offline, live location, start places, test alert), `pushService.sendDataToUser`, new user columns (`partner_online`, `live_*`, `missed_offer_streak`…). **Not yet deployed to testing.**
- **App, written but NOT compiling yet:** `data/partner/PartnerRepository.kt`, `alerts/AlertReadiness.kt` (5 checks, order channel, OEM Autostart guides), `alerts/Offer.kt`, `alerts/AlertSound.kt`, `ui/components/SlideButton.kt`, `ui/ring/RingScreen.kt`, new icons, `VoiceBars`. Missing before it builds: string resources (`ring_*`, `stamp_*`, `channel_orders_*`, `guide_*`), then RingActivity + order-alert notification (full-screen intent) in `push/Push.kt`, OnlineService (location FGS), PermGate / Autostart guide / Home offline-online / BasePicker / Profile + Start places screens, nav wiring (Approved → PermGate), manifest permissions (USE_FULL_SCREEN_INTENT, ACCESS_NOTIFICATION_POLICY, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, FOREGROUND_SERVICE(_LOCATION)).
- After that: dispatch engine (§6.3 step 4), website Fleet / Settings / Hours / customer tracking.
