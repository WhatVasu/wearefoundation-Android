# FindBack API

All responses are JSON. Errors have the form `{"message":"..."}`. Protected endpoints require `Authorization: Bearer <Firebase ID token>`. Lambda verifies tokens with Firebase Admin using the service-account secret configured in Secrets Manager. The client must never supply an admin role.

The API is served over HTTPS by API Gateway. Report status values are `SUBMITTED`, `UNDER_REVIEW`, `POTENTIAL_MATCH`, `VERIFIED`, `RESOLVED`, and `CLOSED`.

| Method and path | Access | Behavior |
|---|---|---|
| `GET /me` | Signed in | Creates/returns the user's citizen profile |
| `PATCH /me` | Signed in | Updates `name`, `phone`, or `district`; role is immutable to clients |
| `POST /reports` | Signed in | Creates a `FOUND`, `MISSING`, or linked `SIGHTING` report; district and locality required |
| `GET /reports/my` | Signed in | Lists caller's reports, newest first (maximum 100) |
| `GET /reports/{id}` | Owner/admin | Returns a report detail |
| `PATCH /reports/{id}` | Owner | Updates description while status is `SUBMITTED` or `UNDER_REVIEW` |
| `POST /search` | Public, safe summary only | Bounded recent browse, approved case details, exact reference lookup, or ranked potential matches; accepts `mode`, `referenceId`, `name`, `district`, `location`, `area`, `lastSeenLocation`, age/gender/physical/date filters, page and page size |
| `GET /public/reports/{id}/photos/{index}` | Public, approved cases only | Redirects to a short-lived private S3 photo URL after checking the report is approved |
| `POST /uploads/presign` | Signed in | Returns a private S3 PUT URL for JPEG, PNG or WebP |
| `GET /admin/reports` | Admin | Lists reports by status; optional district filter |
| `PATCH /admin/reports/{id}/status` | Admin | Changes a report status and adds an in-app notification |
| `PATCH /admin/reports/{id}` | Admin | Edits report name, age, district, locality, or description |
| `DELETE /admin/reports/{id}` | Admin | Deletes a report and its photos, and notifies its owner |
| `GET /notifications` | Signed in | Lists caller's in-app notifications |
| `PATCH /notifications/{id}/read` | Owner | Marks a notification read |

Browse uses DynamoDB indexes and cursor tokens, with bounded reads per request; matching evaluates at most 200 candidates. Public responses omit reporter contact data, exact locality, photo keys, and unstructured observations. A reported person's name is included when provided, including for `FOUND` reports. Browse is limited to verified reports; case details are available only for verified/potential-match cases. These public reports include stable photo URLs; each request checks that the report is approved and redirects to a freshly signed private S3 URL. Pending and unapproved reports remain private.

For report submission, request a presigned URL with `fileName`, `contentType`, and `contentLength` (1–8 MiB), upload those bytes using the returned URL, then pass returned `objectKey` values in `imageKeys` on `POST /reports`. The API validates the requested size and accepted media type, but does not sign HTTP `Content-Length` or `Content-Type` headers, avoiding signature differences from client/intermediary header normalization. Keys must belong to the authenticated user. Owner/admin report responses include short-lived (15-minute) signed photo URLs. Public photo URLs remain stable, but serve only approved reports and use short-lived private redirects. Never grant public S3 access.

A `SIGHTING` submission must include `potentialMatchReportId` referencing an existing, publicly approved `FOUND` report. The API stores it as a separate relationship field; the sighting remains `SUBMITTED` for administrator review and is excluded from public browse and search results.

The SAM stack creates `Users`, `Reports`, `Notifications`, and `MatchRecords` tables. Reports use `UserCreatedIndex`, `TypeDistrictCreatedIndex`, and `StatusCreatedIndex`; report creation and status changes use those indexes for profile reports, search, and admin queues. `MatchRecords` is reserved for later human-reviewed matching workflows; no face matching is implemented.
