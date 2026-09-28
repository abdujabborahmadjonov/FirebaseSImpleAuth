# Firebase Simple Auth

A small Android app (Kotlin) showing a complete e-mail/password sign-in flow with
Firebase Authentication.

## Features

- **Log in / register** on one screen, with a toggle between the two modes
- **Input validation**: e-mail format, minimum password length, password confirmation
- **Password strength meter** while registering
- **Forgot password**: sends a Firebase password-reset e-mail
- **E-mail verification**: sent on sign-up, with resend and refresh on the account screen
  (the status also refreshes when you come back to the app)
- **Change password** and **delete account**, both re-confirm your current password first
- **Friendly error messages** for wrong credentials, existing accounts, network errors, rate limits
- Loading indicator, and inputs disabled while a request is in flight
- Signed-in users skip straight to the account screen

## Architecture

```
app/src/main/java/dev/abdujabbor/firebaseservice/
├── MainActivity.kt            # Log in / register screen
├── data/
│   ├── AuthRepository.kt      # Suspending wrapper around FirebaseAuth (awaits every Task)
│   └── AuthErrorMapper.kt     # Firebase exceptions -> string resources
├── ui/
│   ├── auth/AuthViewModel.kt  # Form state, validation, auth actions
│   └── home/                  # Account screen + HomeViewModel
├── util/FlowExt.kt            # Lifecycle-aware Flow collection
└── validation/AuthValidator.kt  # Pure Kotlin validation (unit tested)
```

Screens are plain Activities with View Binding. Each has a ViewModel that exposes a
`StateFlow` of UI state plus a one-off event stream (toasts, navigation).

## Setup

1. Create a Firebase project and add an Android app with package `dev.abdujabbor.firebaseservice`.
2. Download `google-services.json` into `app/` (replacing the existing one if you use your own project).
3. In the Firebase console enable **Authentication → Sign-in method → Email/Password**.
4. Open the project in Android Studio and run the `app` configuration.

## Tests

```bash
./gradlew testDebugUnitTest
```
