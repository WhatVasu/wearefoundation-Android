# FindBack Telangana

Native Kotlin and Jetpack Compose MVP for reporting a person found, registering a missing-person case, and searching for potential matches. This is neutral prototype branding; it is **not** an official Telangana Government or Telangana Police service and has no access to police databases.

## What is implemented

- Material 3 Home, Search, My Reports, Profile, found-person and missing-person flows.
- Email/password Firebase Authentication (registration, login, reset and persistent Firebase session).
- Firebase ID-token bearer authentication on protected Retrofit API calls.
- AWS API Gateway HTTP API + Python Lambda + DynamoDB + private S3 photo uploads.
- Bounded attribute-based search using normalized text, token overlap, approximate age, district, and location signals. Results are potential matches only; there is no face recognition.
- Optional photo capture/gallery selection, JPEG downscaling/compression, and presigned upload.
- Optional GPS suggestion, manual location fields, report status review and emergency dial shortcut.

The Android project builds without service credentials, but operations requiring authentication or an API return explicit setup errors until Firebase and AWS are configured. No cloud environment is deployed by this repository.

## Android setup

1. Open this folder in Android Studio and sync Gradle.
2. In Firebase Console, create an Android app for application ID `com.example.prajarakshak`; enable **Email/Password** and **Google** in Authentication.
3. Add these values to the root `local.properties` (do not commit them; Gradle reads these as app build configuration):

   ```properties
   findbackApiBaseUrl=https://YOUR_API_ID.execute-api.YOUR_REGION.amazonaws.com/prod/
   firebaseApiKey=YOUR_FIREBASE_WEB_API_KEY
   firebaseAppId=YOUR_FIREBASE_ANDROID_APP_ID
   firebaseProjectId=YOUR_FIREBASE_PROJECT_ID
   googleWebClientId=YOUR_WEB_APPLICATION_OAUTH_CLIENT_ID
   ```

   Get the Web application OAuth client ID from Firebase Authentication's Google provider configuration or Google Cloud Console → APIs & Services → Credentials. Register the app's Android SHA-1 signing fingerprints in Firebase project settings (include the debug fingerprint for local runs and the release fingerprint for published builds). The Firebase API key identifies the Firebase project; it is not an AWS credential. Restrict its use in Google Cloud/Firebase as appropriate. The Firebase Admin service-account key belongs in AWS Secrets Manager, never in the app.
4. Build and install with Android Studio **Run**, or run `gradlew.bat app:assembleDebug`. The debug APK is written under `app/build/outputs/apk/debug/`.

Search only returns records explicitly marked `VERIFIED` or `POTENTIAL_MATCH` by an administrator. Approved public reports display identification photos through stable public API links that serve private S3 objects; pending reports, personal contact data, and exact locations remain private.

## AWS deployment

Prerequisites: AWS SAM CLI, AWS credentials for the target account, Python 3.12, a Firebase project, and a Firebase Admin service-account JSON saved as a Secrets Manager secret. Use a dedicated, least-privilege service account and rotate it according to your operational policy.

```powershell
python -m pip install -r backend/requirements.txt
sam build --template-file aws/template.yaml
sam deploy --template-file .aws-sam/build/template.yaml --guided
```

Run these commands from the repository root. Do not upload `aws/template.yaml` directly to CloudFormation: its `CodeUri: ../backend/` is a local source path, and CloudFormation requires the Lambda code to have been packaged to S3. `sam build` prepares the artifact and `sam deploy` uploads it and submits the packaged template. If a previous stack is still `ROLLBACK_IN_PROGRESS`, wait until rollback finishes before deploying again.

Provide the secret ARN as `FirebaseServiceAccountSecretArn` when prompted. Copy the `ApiBaseUrl` stack output into `findbackApiBaseUrl`. The Lambda creates citizen profiles on first `/me` call; it never accepts a client-selected role. To assign an administrator, update the corresponding `Users` item through a controlled operator procedure and set `role` to `admin`. Do not expose this privilege through the registration UI.

The SAM template provisions pay-per-request tables, secondary indexes, a private encrypted S3 bucket and one small API Lambda. Review AWS costs, retention, privacy notices, incident response, data-residency obligations, and security settings before any real-person data is processed. Use synthetic data for development.

The report, user, notification and match tables, along with the photo bucket, use CloudFormation `Retain` policies. Removing or replacing the stack will not delete this data; plan and perform any data cleanup separately.

## Tests

```powershell
python -m unittest discover -s backend/tests -v
```

The Android app's debug build can be verified with `gradlew.bat app:assembleDebug`. No claim of a live AWS deployment or configured Firebase project is made.

## API

See [backend/API.md](backend/API.md) for request/response behavior and authorization. The sample records in [backend/sample_records.json](backend/sample_records.json) are fictional and are not loaded automatically.
