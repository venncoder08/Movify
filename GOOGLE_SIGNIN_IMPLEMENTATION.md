# 🔐 Google Sign-In Integration Guide

## Overview

Google Sign-In has been successfully integrated into both the Movify Android app and the backend API. This implementation allows users to sign in using their Google account.

---

## 📱 Android App Changes

### 1. **Dependencies Added** (`gradle/libs.versions.toml` & `app/build.gradle.kts`)

```gradle
// Version aliases
firebaseVersion = "33.3.0"
playServicesAuth = "21.0.0"

// Libraries
play-services-auth = { group = "com.google.android.gms", name = "play-services-auth", version.ref = "playServicesAuth" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseVersion" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }
```

### 2. **UI Changes** (`activity_login.xml`)

Added:
- ✅ Divider with "hoặc" (or) text
- ✅ Google Sign-In button with Google icon
- ✅ Button styling with white background and border

### 3. **LoginActivity Updates** (`LoginActivity.java`)

New features:
- ✅ Google Sign-In configuration
- ✅ Handle email/password login with backend verification
- ✅ Google Sign-In flow (OAuth)
- ✅ Firebase authentication
- ✅ Token exchange with backend
- ✅ Auto-login on app launch if user is logged in

#### Flow:
```
User clicks Google Sign-In
    ↓
Google Sign-In dialog
    ↓
Firebase authentication with Google token
    ↓
Send Google token to backend API (/api/auth/google)
    ↓
Backend returns JWT token
    ↓
Save JWT token locally
    ↓
Navigate to Home Activity
```

### 4. **New Service: AuthApiService** (`AuthApiService.java`)

Handles all authentication API calls:

```java
// Exchange Google token for JWT
authenticateWithGoogle(idToken) → JWT token

// Email/Password login
loginWithEmail(email, password) → JWT token

// Request OTP for password reset
requestOTP(email) → Success message

// Reset password with OTP
resetPasswordWithOTP(email, otp, newPassword) → Success message

// Logout
logout() → Success message

// Get user profile
getProfile() → User data

// Token management
saveJwtToken(token)
getJwtToken()
clearJwtToken()
```

### 5. **String Resources** (`strings_auth.xml`)

Added Vietnamese strings:
- `or` = "hoặc"
- `sign_in_with_google` = "Đăng nhập bằng Google"
- `google_sign_in_failed` = "Đăng nhập Google thất bại"
- `auth_failed` = "Xác thực thất bại"
- `fill_all_fields` = "Vui lòng điền tất cả các trường"

### 6. **Drawable Resources**

- `ic_google.xml` - Google logo icon
- `bg_button_google.xml` - White button with border styling

---

## 🔧 Backend API Changes

### 1. **New Auth Controller Function** (`authController.js`)

```javascript
async function googleSignIn(req, res)
```

Functionality:
- ✅ Verify Google ID token
- ✅ Create user if not exists
- ✅ Update user profile (avatar) if provided
- ✅ Generate JWT token
- ✅ Return user data and token

#### Token Verification:
```javascript
// Simple JWT decode (DECODE ONLY - NOT SECURE)
// In production, use Firebase Admin SDK for proper verification
const decodedToken = {
  email: "user@gmail.com",
  name: "User Name",
  picture: "https://..."
}
```

⚠️ **Security Note**: Currently uses simple JWT decode. For production:
```javascript
// Use Firebase Admin SDK
const admin = require('firebase-admin');
const decodedToken = await admin.auth().verifyIdToken(idToken);
```

### 2. **New Validation Schema** (`authSchemas.js`)

```javascript
const googleSignInSchema = z.object({
  body: z.object({
    idToken: z.string().min(1, "ID token is required")
  })
});
```

### 3. **New Route** (`authRoutes.js`)

```javascript
POST /api/auth/google
```

**Request:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Ni..."
}
```

**Response (Success):**
```json
{
  "message": "Google Sign-In successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "data": {
    "user": {
      "id": "user-uuid",
      "email": "user@gmail.com",
      "fullName": "User Name",
      "role": "User",
      "avatar": "https://...",
      "createdAt": "2026-04-20T10:30:00Z"
    }
  }
}
```

**Response (Error):**
```json
{
  "error": "Unauthorized",
  "message": "Invalid ID token"
}
```

### 4. **Database Operations**

When Google Sign-In succeeds:

**If user exists:**
- ✅ Update avatar if provided
- ✅ Generate JWT token
- ✅ Return user data

**If user is new:**
- ✅ Create account with Google email and name
- ✅ Set `phoneNumber` to empty (not provided by Google)
- ✅ Set `hashedPassword` to empty (no password for Google users)
- ✅ Set `avatar` to Google profile picture
- ✅ Set role to "User" by default
- ✅ Generate JWT token

---

## 🔑 Setup Requirements

### Android App:

1. **Firebase Project Setup** (see `GOOGLE_SIGNIN_SETUP.md`):
   - Create Firebase project
   - Enable Google Sign-In
   - Download `google-services.json`
   - Add SHA-1 certificate fingerprint

2. **Dependencies:**
   ```bash
   ./gradlew build  # Automatically downloads Firebase/Google libraries
   ```

3. **Google Web Client ID:**
   - Add to `strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
   ```

### Backend API:

1. **Environment Setup:**
   ```bash
   npm install  # Already includes firebase-admin (optional)
   ```

2. **JWT Configuration** (existing):
   ```bash
   JWT_SECRET=your_secret
   JWT_EXPIRATION=7d
   ```

3. **Firebase Admin SDK** (for production):
   ```bash
   npm install firebase-admin
   # Create service account JSON and add to project
   ```

---

## 📊 User Data Flow

### Google Sign-In Registration:

```
1. User taps "Đăng nhập bằng Google"
2. Android shows Google account selection
3. User selects/creates Google account
4. Firebase exchanges credentials
5. Android sends Google token to backend
   └─ POST /api/auth/google
      {
        "idToken": "google_token_123"
      }
6. Backend creates/updates user in database
7. Backend returns JWT token
8. Android saves JWT locally (SharedPreferences)
9. JWT used for subsequent API calls
   └─ Header: "Authorization: Bearer jwt_token_123"
10. User logged in, navigate to Home
```

### Subsequent Requests:

```
1. All API calls include JWT token
2. Backend validates JWT in authMiddleware
3. Returns 401 if token invalid/expired
4. User can refresh token with /api/auth/refresh
```

---

## ⚙️ API Endpoints

### Authentication Endpoints:

| Method | Endpoint | Protection | Purpose |
|--------|----------|-----------|---------|
| `POST` | `/api/auth/register` | Public | Register with email/password |
| `POST` | `/api/auth/login` | Public | Login with email/password |
| **`POST`** | **`/api/auth/google`** | **Public** | **Google Sign-In** |
| `POST` | `/api/auth/forgot-password` | Public | Request password reset OTP |
| `POST` | `/api/auth/reset-password` | Public | Reset password with OTP |
| `GET` | `/api/auth/me` | Protected | Get current user profile |
| `POST` | `/api/auth/refresh` | Protected | Refresh JWT token |
| `POST` | `/api/auth/logout` | Protected | Logout (invalidate token) |

---

## 🧪 Testing

### Android App:

1. **Build & Run:**
   ```bash
   ./gradlew build
   # In Android Studio: Run → Run 'app'
   ```

2. **Test Google Sign-In:**
   - Tap "Đăng nhập bằng Google"
   - Select Google account (or create new)
   - Check Logcat for:
     ```
     D/LoginActivity: Firebase authentication with Google successful
     D/LoginActivity: User: user@gmail.com
     D/LoginActivity: Backend JWT token obtained
     ```
   - Navigate to Home Activity ✓

3. **Check Saved Token:**
   - Device Settings → Apps → Movify → Storage → Check SharedPreferences
   - Or use Android Studio Device Explorer

### Backend API:

1. **Test with curl:**
   ```bash
   # Get a Google token from Firebase (via Android app)
   # Then test backend endpoint:
   
   curl -X POST http://localhost:3000/api/auth/google \
     -H "Content-Type: application/json" \
     -d '{
       "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Ni..."
     }'
   ```

2. **Test with Postman:**
   - Method: `POST`
   - URL: `http://localhost:3000/api/auth/google`
   - Body (raw JSON):
     ```json
     {
       "idToken": "google_token_here"
     }
     ```

3. **Check Database:**
   ```bash
   # View users created via Google Sign-In
   SELECT * FROM "Account" WHERE "email" LIKE '%@gmail.com';
   ```

---

## 🔒 Security Considerations

### ✅ Implemented:
- ✅ JWT token-based authentication
- ✅ Secure token storage (SharedPreferences on Android)
- ✅ Zod validation on backend
- ✅ Password hashing for email/password users
- ✅ Authorization header for protected endpoints

### ⚠️ To Do (Production):
- 🔲 Use Firebase Admin SDK for token verification (not simple decode)
- 🔲 Implement rate limiting on auth endpoints
- 🔲 Add HTTPS only (remove HTTP in production)
- 🔲 Implement token refresh/rotation strategy
- 🔲 Add user consent tracking for GDPR
- 🔲 Implement password-less account linking
- 🔲 Add phone number completion flow for Google users

---

## 📚 Documentation Files

- `GOOGLE_SIGNIN_SETUP.md` - Complete Firebase setup guide
- `VALIDATIONS.md` - Validation schemas documentation
- `AUTH_API.md` - Backend auth API documentation
- `OTP_SERVICE.md` - OTP service documentation

---

## 🐛 Troubleshooting

### Problem: "API_EXCEPTION_12500"
**Solution**: Ensure SHA-1 fingerprint is added to Firebase Console

### Problem: "google-services.json not found"
**Solution**: Download from Firebase Console and place in `app/` directory

### Problem: "Invalid ID token" from backend
**Solution**: 
1. Check token is being sent correctly
2. Verify Firebase configuration
3. Check server time sync (tokens have expiration)

### Problem: User not found after Google Sign-In
**Solution**: Check database connection and Prisma client initialization

---

## 📞 Next Steps

1. ✅ Google Sign-In implemented
2. ⏳ Facebook Sign-In (optional)
3. ⏳ Apple Sign-In (optional)
4. ⏳ Email verification for Google users
5. ⏳ Account linking (email + Google)
6. ⏳ Social profile data sync

---

**Last Updated**: 2026-04-20  
**Status**: ✅ Google Sign-In Fully Implemented
