# 🇿🇦 ProConnectSA — On-Demand Services & Escrow Platform

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java%20%7C%20Kotlin-orange.svg)](https://developer.android.com/kotlin)
[![Backend](https://img.shields.io/badge/Backend-Supabase%20PostgreSQL-emerald.svg?logo=supabase)](https://supabase.com/)
[![Payment](https://img.shields.io/badge/Payments-PayFast%20by%20Network-blue.svg)](https://www.payfast.co.za/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**ProConnectSA** is a native South African Android mobile application connecting clients with verified local tradespeople (plumbers, electricians, mechanics, handymen) featuring integrated escrow payments, admin oversight, and real-time job tracking.

---

## 🌟 Key Highlights & Features

### 🛡️ 1. Escrow Payment & Immediate Refund Engine
* **PayFast Integration**: Supports **Credit/Debit Cards (VISA & Mastercard)**, **Capitec Pay**, **Instant EFT** (FNB, Capitec, ABSA, Nedbank, Standard Bank), **SnapScan**, and **Zapper**.
* **Admin Escrow System**: Client payments are held safely in escrow (`PENDING_APPROVAL`) until job completion.
* **Instant Admin Release / Refund**:
  * **Approve**: Releases funds directly to the provider's **Clear Cash Balance**.
  * **Decline**: **Immediately refunds 100% of the funds back to the client's account balance**.
* **Dual Gateway Support**: Features a seamless toggle between **PayFast Sandbox (Test Mode)** and **Live Production Real-Money Mode**.

### 🔐 2. Precise Authentication & Role-Based Access
* **Granular Role System**: Supports `CLIENT`, `TRADESPERSON`, `ADMIN`, and `SUPER_ADMIN`.
* **Precise Error Distinction**: Differentiates between non-existent email accounts (*"Incorrect email address. No account found."*) vs incorrect passwords (*"Incorrect password. Please try again."*).
* **FICA Verification**: Service providers upload ID and trade qualification documents reviewed by Admins before receiving work.

### 📊 3. Real-Time Dashboards & Analytics
* **Admin Dashboard**: System Pulse analytics (User count, Active jobs, Pros, Staff), collapsible metrics, scrollable tabs (`Verify`, `Categories`, `Jobs`, `Payments`, `Users`, `Me`).
* **Worker Dashboard**: Real-time **Cleared Earnings** vs **Pending Admin Escrow** balance cards, active job management, and South African bank payout settings.

### 🌐 4. Offline-First Resilience
* **Local Fallback Caching**: Utilizes persistent `SharedPreferences` + JSON caching to ensure zero crashes and seamless functionality if cloud database connections are offline or moved.

---

## 🛠️ Technical Architecture & Tech Stack

```
   ┌──────────────────────────────────────────────────────────┐
   │                     ANDROID CLIENT                       │
   │  (Material Design 3 | ViewBinding | Jetpack Navigation)  │
   └─────────────┬──────────────────────────────┬─────────────┘
                 │                              │
        HTTPS / REST API                  PayFast SDK / WebView
                 │                              │
                 ▼                              ▼
   ┌──────────────────────────┐   ┌───────────────────────────┐
   │     SUPABASE CLOUD       │   │    PAYFAST GATEWAY (SA)   │
   │  • PostgreSQL DB         │   │  • Capitec Pay            │
   │  • JWT Auth & Storage    │   │  • Instant EFT / Cards    │
   └──────────────────────────┘   └───────────────────────────┘
```

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 11 & Kotlin | Core native Android application logic |
| **UI Design** | Material Design 3 | Google MaterialComponents, CoordinatorLayout, ViewPager2 |
| **Networking** | Retrofit 2 & OkHttp 3 | Async REST communication & JSON parsing |
| **Cloud Backend** | Supabase Cloud | PostgreSQL database, JWT Auth, RLS Policies |
| **Cloud Storage** | Supabase Storage | Document & profile picture uploads (`avatars` bucket) |
| **Payment Gateway** | PayFast by Network | Live & Sandbox payment processing |
| **Maps & GPS** | Google Maps SDK | Fused Location Provider & interactive job mapping |
| **Email Service** | JavaMail API | Automated transactional email alerts |

---

## 🚀 Installation & Setup Guide

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/ProConnectSA.git
cd ProConnectSA
```

### 2. Open in Android Studio
* Open **Android Studio** (Koala / Ladybug or newer).
* Select **Open an Existing Project** and choose the `ProConnectSA` folder.
* Allow Gradle to sync dependencies (`Gradle 8.7`, `JDK 11`).

### 3. Configure Credentials (Optional)
* **PayFast Keys**: Set your credentials in `app/src/main/java/com/example/proconnectsa/network/PayFastConfig.java`:
  ```java
  public static final String LIVE_MERCHANT_ID = "YOUR_MERCHANT_ID";
  public static final String LIVE_MERCHANT_KEY = "YOUR_MERCHANT_KEY";
  ```
* **Supabase Backend**: Set your URL & anon key in `app/src/main/java/com/example/proconnectsa/network/NetworkClient.java`.

### 4. Build & Run
* Connect an Android device or launch the **Pixel 7 Pro Emulator**.
* Click **Run (`Shift + F10`)** or execute:
  ```bash
  ./gradlew assembleDebug
  ```

---

## 📸 Presentation & Testing Guide
For a full start-to-finish presentation script, evaluation checklist, and step-by-step demo guide, refer to [`PRESENTATION_GUIDE.txt`](./PRESENTATION_GUIDE.txt).

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
