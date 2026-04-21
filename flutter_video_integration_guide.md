# 🎥 Video Integration Guide for Flutter

This document explains how to fetch, upload, and play videos from the Remote Classroom backend and S3 storage.

## 1. Authentication
All video APIs require a **Bearer Token** in the header:
```dart
headers: {
  'Authorization': 'Bearer YOUR_JWT_TOKEN',
}
```

---

## 2. Playing Videos (Crucial)
If the video does not play in the UI but works in a browser tab, it is likely due to **CORS**. Ensure the backend developer has updated the S3 CORS policy.

### Fetching a Playable URL
Since S3 objects are secured, you should not use the static URL directly if the bucket is private. Instead, call the download endpoint to get a temporary authorized link.

**Endpoint:** `GET /api/video/download/{videoId}`

**Example Response:**
```json
{
  "downloadUrl": "https://remote-classroom-videos.s3.ap-south-1.amazonaws.com/video-xyz.mp4?X-Amz-Algorithm=...",
  "title": "Algebra Lesson 1"
}
```

### Implementation (video_player)
Use the `downloadUrl` received above:

```dart
import 'package:video_player/video_player.dart';

VideoPlayerController _controller = VideoPlayerController.networkUrl(
  Uri.parse(responseData['downloadUrl']),
);

await _controller.initialize();
_controller.play();
```

---

## 3. Uploading Videos (Two-Step Process)
To save server bandwidth and support large files, we use **Direct S3 Upload**.

### Step A: Get Upload Permission
Request a pre-signed URL from the backend.
*   **Endpoint:** `GET /api/video/upload-url?fileName=lesson1.mp4`
*   **Returns:** `uploadUrl` (for S3) and `fileUrl` (for our DB).

### Step B: Upload to S3 (Direct)
Use the `http` package to PUT the file directly to S3.
> [!IMPORTANT]
> Do **NOT** send the `Authorization` header to S3. Only send the raw file bytes.

```dart
final response = await http.put(
  Uri.parse(uploadUrl),
  body: fileBytes, // Raw bytes of the video
);
```

### Step C: Confirm to Backend
Once S3 returns `200 OK`, notify our database to save the video entry.
*   **Endpoint:** `POST /api/video/save`
*   **Body (form-data):**
    *   `title`: "My Video Title"
    *   `url`: `fileUrl` (received in Step A)
    *   `transcript`: "Optional text"

---

## 4. Common Issues
| Issue | Cause | Solution |
| :--- | :--- | :--- |
| **Video fails on Web** | CORS Policy | Update S3 CORS settings to allow your domain. |
| **403 Forbidden on S3** | Signed URL Expired | Re-fetch the download/upload URL. They usually expire in 15 mins. |
| **S3 Upload Failed** | Extra Headers | Do not send `Content-Type` or `Authorization` headers when PUTing to S3. |
