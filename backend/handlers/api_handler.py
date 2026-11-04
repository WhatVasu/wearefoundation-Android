import json
import os
import re
import uuid
import base64
from datetime import datetime, timezone

import boto3
import firebase_admin
from boto3.dynamodb.conditions import Attr, Key
from firebase_admin import auth as firebase_auth
from firebase_admin import credentials
from handlers.matching import normalize, rank

_dynamodb = boto3.resource("dynamodb")
_s3 = boto3.client("s3")
_secrets = boto3.client("secretsmanager")
_firebase_app = None

USERS = _dynamodb.Table(os.environ["USERS_TABLE"])
REPORTS = _dynamodb.Table(os.environ["REPORTS_TABLE"])
NOTIFICATIONS = _dynamodb.Table(os.environ["NOTIFICATIONS_TABLE"])
MATCHES = _dynamodb.Table(os.environ["MATCHES_TABLE"])


class ApiError(Exception):
    def __init__(self, status, message):
        self.status = status
        self.message = message


def _now():
    return datetime.now(timezone.utc).isoformat()


def _json_default(value):
    if hasattr(value, "to_eng_string"):
        return int(value)
    raise TypeError("Unsupported response value")


def _response(status, body):
    return {
        "statusCode": status,
        "headers": {"content-type": "application/json", "cache-control": "no-store"},
        "body": json.dumps(body, default=_json_default),
    }


def _body(event):
    try:
        body = json.loads(event.get("body") or "{}")
    except (TypeError, json.JSONDecodeError):
        raise ApiError(400, "Request body must be valid JSON.")
    if not isinstance(body, dict):
        raise ApiError(400, "Request body must be a JSON object.")
    return body


def _bounded_int(value, default, minimum, maximum, field):
    if isinstance(value, bool) or isinstance(value, float):
        raise ApiError(400, f"{field} must be a whole number.")
    try:
        result = default if value in (None, "") else int(value)
    except (TypeError, ValueError):
        raise ApiError(400, f"{field} must be a whole number.")
    if not minimum <= result <= maximum:
        raise ApiError(400, f"{field} must be between {minimum} and {maximum}.")
    return result


def _iso_date(value, field):
    if value in (None, ""):
        return None
    try:
        parsed = datetime.strptime(str(value), "%Y-%m-%d").date()
    except ValueError:
        raise ApiError(400, f"{field} must use YYYY-MM-DD format.")
    if parsed.isoformat() != str(value):
        raise ApiError(400, f"{field} must use YYYY-MM-DD format.")
    return parsed.isoformat()


def _phone_is_valid(value):
    normalized = re.sub(r"[\s\-()]", "", str(value or ""))
    return bool(re.fullmatch(r"(?:\+?91)?[6-9]\d{9}", normalized))


def _firebase():
    global _firebase_app
    if _firebase_app:
        return _firebase_app
    secret_arn = os.environ.get("FIREBASE_SERVICE_ACCOUNT_SECRET_ARN", "")
    if not secret_arn:
        raise ApiError(503, "Firebase token verification is not configured.")
    secret = _secrets.get_secret_value(SecretId=secret_arn)
    service_account = json.loads(secret["SecretString"])
    _firebase_app = firebase_admin.initialize_app(
        credentials.Certificate(service_account), name="findback-api"
    )
    return _firebase_app


def _user(event, required=True):
    header = event.get("headers") or {}
    authorization = header.get("authorization") or header.get("Authorization") or ""
    token = authorization.removeprefix("Bearer ").strip()
    if not token:
        if required:
            raise ApiError(401, "Sign in is required.")
        return None
    try:
        decoded = firebase_auth.verify_id_token(token, app=_firebase())
        return {"userId": decoded["uid"], "email": decoded.get("email", "")}
    except ApiError:
        raise
    except Exception:
        raise ApiError(401, "Authentication token is invalid or expired.")


def _admin(user):
    item = USERS.get_item(Key={"userId": user["userId"]}).get("Item")
    if not item or item.get("role") != "admin":
        raise ApiError(403, "Administrator access is required.")


def _safe_report(item, include_description=False):
    keys = ("reportId", "reportType", "name", "age", "gender", "district",
            "location", "status", "createdAt", "potentialMatchReportId")
    result = {key: item[key] for key in keys if key in item}
    if include_description and "description" in item:
        result["description"] = item["description"]
    if include_description and "statusHistory" in item:
        result["statusHistory"] = [
            {"status": entry.get("status"), "at": entry.get("at")}
            for entry in item["statusHistory"]
        ]
    if include_description and item.get("imageKeys"):
        result["imageUrls"] = [
            _s3.generate_presigned_url(
                "get_object",
                Params={"Bucket": os.environ["PHOTOS_BUCKET"], "Key": key},
                ExpiresIn=900,
            )
            for key in item["imageKeys"]
        ]
    return result


def _public_report(item, event):
    report = {
        key: item[key]
        for key in (
            "reportId", "reportType", "name", "age", "gender", "district",
            "status", "createdAt", "lastSeenDate", "physicalFeatures",
            "matchingSignals",
        )
        if key in item
    }
    if item.get("imageKeys"):
        report["imageUrls"] = [
            f"{_public_api_origin(event)}/public/reports/{item['reportId']}/photos/{index}"
            for index, _ in enumerate(item["imageKeys"])
        ]
    return report


def _public_api_origin(event):
    context = event.get("requestContext") or {}
    domain = context.get("domainName")
    if not domain:
        raise ApiError(500, "Public photo links are not configured.")
    stage = context.get("stage")
    raw_path = event.get("rawPath", "")
    stage_path = (
        f"/{stage}"
        if stage and stage != "$default" and raw_path.startswith(f"/{stage}/")
        else ""
    )
    return f"https://{domain}{stage_path}"


def _public_photo(event, path):
    parts = path.strip("/").split("/")
    if len(parts) != 5 or parts[0] != "public" or parts[1] != "reports" or parts[3] != "photos":
        raise ApiError(404, "Photo not found.")
    report_id = parts[2]
    try:
        photo_index = int(parts[4])
    except ValueError:
        raise ApiError(404, "Photo not found.")
    report = REPORTS.get_item(Key={"reportId": report_id}).get("Item")
    image_keys = report.get("imageKeys", []) if report else []
    if (
        not report
        or report.get("reportType") not in ("FOUND", "MISSING")
        or report.get("status") not in ("VERIFIED", "POTENTIAL_MATCH")
        or photo_index < 0
        or photo_index >= len(image_keys)
    ):
        raise ApiError(404, "Photo not found.")
    signed_url = _s3.generate_presigned_url(
        "get_object",
        Params={"Bucket": os.environ["PHOTOS_BUCKET"], "Key": image_keys[photo_index]},
        ExpiresIn=300,
    )
    return {
        "statusCode": 302,
        "headers": {"location": signed_url, "cache-control": "no-store"},
        "body": "",
    }


def _attach_public_images(results, candidates, event):
    origin = _public_api_origin(event)
    image_urls_by_report = {
        item["reportId"]: [
            f"{origin}/public/reports/{item['reportId']}/photos/{index}"
            for index, _ in enumerate(item.get("imageKeys", []))
        ]
        for item in candidates if item.get("reportId") and item.get("imageKeys")
    }
    for result in results:
        image_urls = image_urls_by_report.get(result.get("reportId"))
        if image_urls:
            result["imageUrls"] = image_urls
    return results


def _get_profile(user):
    result = USERS.get_item(Key={"userId": user["userId"]})
    item = result.get("Item")
    if not item:
        item = {
            "userId": user["userId"],
            "email": user.get("email", ""),
            "name": "",
            "phone": "",
            "district": "",
            "role": "citizen",
            "createdAt": _now(),
        }
        USERS.put_item(Item=item, ConditionExpression="attribute_not_exists(userId)")
    return item


def _my_reports(user):
    response = REPORTS.query(
        IndexName="UserCreatedIndex",
        KeyConditionExpression=Key("userId").eq(user["userId"]),
        ScanIndexForward=False,
        Limit=100,
    )
    return [_safe_report(item, include_description=True) for item in response.get("Items", [])]


def _search(event):
    query = _body(event)
    mode = query.get("mode") or "match"
    if mode == "details":
        report_id = str(query.get("reportId") or "")
        if not report_id:
            raise ApiError(400, "reportId is required.")
        item = REPORTS.get_item(Key={"reportId": report_id}).get("Item")
        if (
            not item
            or item.get("reportType") not in ("FOUND", "MISSING")
            or item.get("status") not in ("VERIFIED", "POTENTIAL_MATCH")
        ):
            raise ApiError(404, "This report is unavailable.")
        return {"results": [_public_report(item, event)], "page": 1, "hasMore": False}

    if mode == "browse":
        district = normalize(query.get("district"))
        page_size = _bounded_int(query.get("pageSize"), 20, 1, 50, "pageSize")
        candidates = []
        token = query.get("nextToken")
        try:
            cursor = json.loads(
                base64.urlsafe_b64decode(str(token) + "=" * (-len(str(token)) % 4))
            ) if token else None
        except (ValueError, json.JSONDecodeError):
            raise ApiError(400, "Invalid feed pagination token.")
        for _ in range(5):
            if len(candidates) >= page_size or cursor is None and token:
                break
            params = {
                "ScanIndexForward": False,
                "Limit": min(100, page_size - len(candidates)),
                "FilterExpression": Attr("status").eq("VERIFIED") if district
                else Attr("reportType").eq("FOUND"),
            }
            if district:
                params.update({
                    "IndexName": "TypeDistrictCreatedIndex",
                    "KeyConditionExpression": Key("typeDistrict").eq(f"FOUND#{district}"),
                })
            else:
                params.update({
                    "IndexName": "StatusCreatedIndex",
                    "KeyConditionExpression": Key("status").eq("VERIFIED"),
                })
            if cursor:
                params["ExclusiveStartKey"] = cursor
            response = REPORTS.query(**params)
            candidates.extend(response.get("Items", []))
            cursor = response.get("LastEvaluatedKey")
            token = "continue" if cursor else None
            if cursor is None:
                break
        next_token = (
            base64.urlsafe_b64encode(json.dumps(cursor).encode()).decode().rstrip("=")
            if cursor else None
        )
        return {
            "results": [_public_report(item, event) for item in candidates[:page_size]],
            "page": 1,
            "hasMore": next_token is not None,
            "nextToken": next_token,
        }

    if mode != "match":
        raise ApiError(400, "Unsupported search mode.")
    for field in ("ageMin", "ageMax"):
        if query.get(field) not in (None, ""):
            query[field] = _bounded_int(query[field], 0, 0, 120, field)
    if (
        query.get("ageMin") is not None
        and query.get("ageMax") is not None
        and query["ageMin"] > query["ageMax"]
    ):
        raise ApiError(400, "ageMin cannot be greater than ageMax.")
    query["dateFrom"] = _iso_date(query.get("dateFrom"), "dateFrom")
    query["dateTo"] = _iso_date(query.get("dateTo"), "dateTo")
    if query["dateFrom"] and query["dateTo"] and query["dateFrom"] > query["dateTo"]:
        raise ApiError(400, "dateFrom cannot be later than dateTo.")
    reference_id = str(query.get("referenceId") or "").strip()
    if reference_id:
        item = REPORTS.get_item(Key={"reportId": reference_id}).get("Item")
        if (
            not item
            or item.get("status") not in ("VERIFIED", "POTENTIAL_MATCH")
            or item.get("reportType") not in ("FOUND", "MISSING")
        ):
            return {"results": [], "page": 1, "hasMore": False}
        has_other_filters = any(
            query.get(key)
            for key in (
                "name", "district", "area", "lastSeenLocation",
                "ageMin", "ageMax", "gender", "features", "hair",
                "height", "mole", "scar", "birthmark", "dateFrom", "dateTo",
            )
        )
        results = rank([item], query) if has_other_filters else [_public_report(item, event)]
        if has_other_filters:
            results = _attach_public_images(results, [item], event)
        return {"results": results, "page": 1, "hasMore": False}
    district = normalize(query.get("district"))
    candidates = []
    if district:
        for report_type in ("MISSING", "FOUND"):
            response = REPORTS.query(
                IndexName="TypeDistrictCreatedIndex",
                KeyConditionExpression=Key("typeDistrict").eq(
                    f"{report_type}#{district}"
                ),
                ScanIndexForward=False,
                Limit=100,
            )
            candidates.extend(
                item for item in response.get("Items", [])
                if item.get("status") in ("VERIFIED", "POTENTIAL_MATCH")
            )
    else:
        for status in ("VERIFIED", "POTENTIAL_MATCH"):
            response = REPORTS.query(
                IndexName="StatusCreatedIndex",
                KeyConditionExpression=Key("status").eq(status),
                ScanIndexForward=False,
                Limit=100,
            )
            candidates.extend(response.get("Items", []))
    date_from = str(query.get("dateFrom") or "")
    date_to = str(query.get("dateTo") or "")
    if date_from or date_to:
        candidates = [
            item for item in candidates
            if item.get("lastSeenDate")
            and (not date_from or item["lastSeenDate"] >= date_from)
            and (not date_to or item["lastSeenDate"] <= date_to)
        ]
    results = rank(candidates, query)
    results = _attach_public_images(results, candidates, event)
    page = _bounded_int(query.get("page"), 1, 1, 100000, "page")
    page_size = _bounded_int(query.get("pageSize"), 20, 1, 50, "pageSize")
    start = (page - 1) * page_size
    return {
        "results": results[start:start + page_size],
        "page": page,
        "hasMore": len(results) > start + page_size,
    }


def _create_report(event, user):
    payload = _body(event)
    report_type_value = payload.get("reportType")
    if not isinstance(report_type_value, str):
        raise ApiError(400, "reportType must be FOUND, MISSING or SIGHTING.")
    report_type = report_type_value.upper()
    if report_type not in ("FOUND", "MISSING", "SIGHTING"):
        raise ApiError(400, "reportType must be FOUND, MISSING or SIGHTING.")
    potential_match_report_id = str(payload.get("potentialMatchReportId") or "").strip()
    if report_type == "SIGHTING":
        if not potential_match_report_id:
            raise ApiError(400, "A linked found-person report is required for a sighting.")
        related_report = REPORTS.get_item(
            Key={"reportId": potential_match_report_id}
        ).get("Item")
        if (
            not related_report
            or related_report.get("reportType") != "FOUND"
            or related_report.get("status") not in ("VERIFIED", "POTENTIAL_MATCH")
        ):
            raise ApiError(404, "The linked found-person report is unavailable.")
    elif potential_match_report_id:
        raise ApiError(400, "Only a sighting can reference another report.")
    district_value = payload.get("district")
    location_value = payload.get("location")
    if not isinstance(district_value, str) or not isinstance(location_value, str):
        raise ApiError(400, "District and area/locality must be text.")
    district = district_value.strip()
    location = location_value.strip()
    if not district or not location:
        raise ApiError(400, "District and area/locality are required.")
    if len(district) > 100 or len(location) > 200:
        raise ApiError(400, "District or area/locality is too long.")
    if report_type == "MISSING":
        if not isinstance(payload.get("name"), str) or not payload["name"].strip():
            raise ApiError(400, "A missing-person name is required.")
        if not isinstance(payload.get("contactName"), str) or not payload["contactName"].strip():
            raise ApiError(400, "A family contact name is required.")
        if not _phone_is_valid(payload.get("contactPhone")):
            raise ApiError(400, "Enter a valid family contact mobile number.")
    description_value = payload.get("description", "")
    if not isinstance(description_value, str):
        raise ApiError(400, "Description must be text.")
    description = description_value[:4000]
    physical_features = payload.get("physicalFeatures") or {}
    if not isinstance(physical_features, dict):
        raise ApiError(400, "physicalFeatures must be an object.")
    if any(not isinstance(value, str) for value in physical_features.values()):
        raise ApiError(400, "Physical feature values must be text.")
    physical_features = {
        str(key)[:40]: str(value)[:200]
        for key, value in physical_features.items()
        if value and str(key) in {
            "height", "hair", "clothing", "mole", "scar",
            "birthmark", "distinctiveMarks", "other",
        }
    }
    image_keys = payload.get("imageKeys") or []
    if not isinstance(image_keys, list) or any(not isinstance(key, str) for key in image_keys):
        raise ApiError(400, "imageKeys must be a list of photo keys.")
    if len(image_keys) > 4:
        raise ApiError(400, "A report can include at most four photos.")
    for image_key in image_keys:
        if not str(image_key).startswith(f"private/{user['userId']}/"):
            raise ApiError(400, "Photo key does not belong to this account.")
    now = _now()
    report_id = str(uuid.uuid4())
    try:
        age_value = payload.get("age")
        if isinstance(age_value, (bool, float)):
            raise ValueError
        age = int(age_value) if age_value not in (None, "") else None
    except (TypeError, ValueError):
        raise ApiError(400, "Age must be a whole number.")
    if age is not None and not 0 <= age <= 120:
        raise ApiError(400, "Age must be between 0 and 120.")
    if payload.get("contactPhone") and not _phone_is_valid(payload["contactPhone"]):
        raise ApiError(400, "Enter a valid 10-digit mobile number.")
    item = {
        "reportId": report_id,
        "reportType": report_type,
        "userId": user["userId"],
        "name": str(payload.get("name") or "")[:160],
        "age": age,
        "gender": str(payload.get("gender") or "")[:40],
        "district": district[:100],
        "districtNormalized": normalize(district),
        "location": location[:200],
        "physicalFeatures": physical_features,
        "description": description,
        "imageKeys": image_keys,
        "status": "SUBMITTED",
        "typeDistrict": f"{report_type}#{normalize(district)}",
        "createdAt": now,
        "updatedAt": now,
        "statusHistory": [{"status": "SUBMITTED", "at": now, "by": user["userId"]}],
    }
    if report_type == "SIGHTING":
        item["potentialMatchReportId"] = potential_match_report_id
    for optional_key in ("lastSeenDate", "contactName", "contactPhone", "policeStation", "firReference"):
        if payload.get(optional_key):
            value = (
                _iso_date(payload[optional_key], "lastSeenDate")
                if optional_key == "lastSeenDate"
                else str(payload[optional_key])[:300]
            )
            item[optional_key] = value
    REPORTS.put_item(Item=item)
    NOTIFICATIONS.put_item(Item={
        "notificationId": str(uuid.uuid4()),
        "userId": user["userId"],
        "message": f"Report {report_id} was submitted.",
        "read": False,
        "createdAt": now,
    })
    return _safe_report(item, include_description=True)


def _presign(event, user):
    payload = _body(event)
    content_type = str(payload.get("contentType") or "image/jpeg").lower()
    if content_type not in ("image/jpeg", "image/png", "image/webp"):
        raise ApiError(400, "Only JPEG, PNG and WebP images are accepted.")
    try:
        content_length = int(payload.get("contentLength", 0))
    except (TypeError, ValueError):
        raise ApiError(400, "A valid photo size is required.")
    if not 0 < content_length <= 8 * 1024 * 1024:
        raise ApiError(400, "Photos must be smaller than or equal to 8 MB.")
    extension = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}[content_type]
    key = f"private/{user['userId']}/{uuid.uuid4()}.{extension}"
    url = _s3.generate_presigned_url(
        "put_object",
        Params={
            "Bucket": os.environ["PHOTOS_BUCKET"],
            "Key": key,
        },
        ExpiresIn=300,
    )
    return {"uploadUrl": url, "objectKey": key}


def _admin_reports(event, user):
    _admin(user)
    params = event.get("queryStringParameters") or {}
    if not isinstance(params, dict):
        raise ApiError(400, "Invalid report filters.")
    district = normalize(params.get("district"))
    status = str(params.get("status") or "SUBMITTED").upper()
    if district:
        items = []
        for report_type in ("FOUND", "MISSING", "SIGHTING"):
            response = REPORTS.query(
                IndexName="TypeDistrictCreatedIndex",
                KeyConditionExpression=Key("typeDistrict").eq(
                    f"{report_type}#{district}"
                ),
                ScanIndexForward=False,
                Limit=100,
            )
            items.extend(item for item in response.get("Items", []) if item.get("status") == status)
    else:
        response = REPORTS.query(
            IndexName="StatusCreatedIndex",
            KeyConditionExpression=Key("status").eq(status),
            ScanIndexForward=False,
            Limit=100,
        )
        items = response.get("Items", [])
    return {"reports": [_safe_report(item, include_description=True) for item in items]}


def _set_status(event, user, report_id):
    _admin(user)
    payload = _body(event)
    status = str(payload.get("status", "")).upper()
    allowed = {"UNDER_REVIEW", "POTENTIAL_MATCH", "VERIFIED", "RESOLVED", "CLOSED"}
    if status not in allowed:
        raise ApiError(400, "Unsupported report status.")
    now = _now()
    updated = REPORTS.update_item(
        Key={"reportId": report_id},
        UpdateExpression="SET #status = :status, updatedAt = :updated, updatedBy = :admin, statusHistory = list_append(if_not_exists(statusHistory, :empty), :history)",
        ConditionExpression="attribute_exists(reportId)",
        ExpressionAttributeNames={"#status": "status"},
        ExpressionAttributeValues={
            ":status": status, ":updated": now, ":admin": user["userId"],
            ":empty": [], ":history": [{"status": status, "at": now, "by": user["userId"]}]
        },
        ReturnValues="ALL_NEW",
    ).get("Attributes")
    audit = {
        "notificationId": str(uuid.uuid4()),
        "userId": updated["userId"],
        "message": f"Report status changed to {status.replace('_', ' ').title()}.",
        "read": False,
        "createdAt": now,
    }
    NOTIFICATIONS.put_item(Item=audit)
    return _safe_report(updated, include_description=True)


def _admin_edit_report(event, user, report_id):
    _admin(user)
    report = REPORTS.get_item(Key={"reportId": report_id}).get("Item")
    if not report:
        raise ApiError(404, "Report not found.")
    payload = _body(event)
    allowed = {"name", "age", "district", "location", "description"}
    if not payload or set(payload) - allowed:
        raise ApiError(400, "Provide valid report fields to update.")
    updates = {}
    for field in ("name", "district", "location", "description"):
        if field in payload:
            if not isinstance(payload[field], str):
                raise ApiError(400, f"{field} must be text.")
            updates[field] = payload[field].strip()
    if "age" in payload:
        age_value = payload["age"]
        if age_value is None or age_value == "":
            updates["age"] = None
        elif isinstance(age_value, bool) or isinstance(age_value, float):
            raise ApiError(400, "Age must be a whole number.")
        else:
            try:
                updates["age"] = int(age_value)
            except (TypeError, ValueError):
                raise ApiError(400, "Age must be a whole number.")
            if not 0 <= updates["age"] <= 120:
                raise ApiError(400, "Age must be between 0 and 120.")
    if "name" in updates and len(updates["name"]) > 160:
        raise ApiError(400, "Name must be 160 characters or fewer.")
    if "district" in updates:
        if not updates["district"] or len(updates["district"]) > 100:
            raise ApiError(400, "District is required and must be 100 characters or fewer.")
        updates["districtNormalized"] = normalize(updates["district"])
        updates["typeDistrict"] = (
            f"{report['reportType']}#{updates['districtNormalized']}"
        )
    if "location" in updates and (not updates["location"] or len(updates["location"]) > 200):
        raise ApiError(400, "Location is required and must be 200 characters or fewer.")
    if "description" in updates and len(updates["description"]) > 4000:
        raise ApiError(400, "Description must be 4000 characters or fewer.")
    names = {f"#{field}": field for field in updates}
    values = {}
    set_expressions = []
    remove_expressions = []
    for field, value in updates.items():
        if field == "age" and value is None:
            remove_expressions.append("#age")
        else:
            names.setdefault(f"#{field}", field)
            values[f":{field}"] = value
            set_expressions.append(f"#{field} = :{field}")
    values[":updated"] = _now()
    set_expressions.append("updatedAt = :updated")
    expression = "SET " + ", ".join(set_expressions)
    if remove_expressions:
        expression += " REMOVE " + ", ".join(remove_expressions)
    updated = REPORTS.update_item(
        Key={"reportId": report_id},
        UpdateExpression=expression,
        ExpressionAttributeNames=names,
        ExpressionAttributeValues=values,
        ReturnValues="ALL_NEW",
    ).get("Attributes")
    return _safe_report(updated, include_description=True)


def _admin_delete_report(user, report_id):
    _admin(user)
    report = REPORTS.get_item(Key={"reportId": report_id}).get("Item")
    if not report:
        raise ApiError(404, "Report not found.")
    REPORTS.delete_item(Key={"reportId": report_id})
    image_keys = report.get("imageKeys", [])
    if image_keys:
        result = _s3.delete_objects(
            Bucket=os.environ["PHOTOS_BUCKET"],
            Delete={"Objects": [{"Key": key} for key in image_keys], "Quiet": True},
        )
        if result.get("Errors"):
            print("Report photo cleanup failed", report_id, len(result["Errors"]))
    NOTIFICATIONS.put_item(Item={
        "notificationId": str(uuid.uuid4()),
        "userId": report["userId"],
        "message": f"Report {report_id} was removed by an administrator.",
        "read": False,
        "createdAt": _now(),
    })
    return {
        "message": "Report deleted.",
        "photoCleanupPending": bool(result.get("Errors")) if image_keys else False,
    }


def _route(event):
    method = event.get("requestContext", {}).get("http", {}).get("method", "")
    path = event.get("rawPath", "")
    stage = event.get("requestContext", {}).get("stage", "")
    if stage and stage != "$default" and path.startswith(f"/{stage}/"):
        path = path[len(stage) + 1:]
    if method == "GET" and path.startswith("/public/reports/"):
        return _public_photo(event, path)
    if method == "POST" and path == "/search":
        return 200, _search(event)

    user = _user(event)
    if method == "GET" and path == "/me":
        return 200, _get_profile(user)
    if method == "PATCH" and path == "/me":
        _get_profile(user)
        fields = _body(event)
        allowed = {"name", "phone", "district"}
        if not isinstance(fields, dict):
            raise ApiError(400, "Request body must be a JSON object.")
        updates = {}
        for key, value in fields.items():
            if key not in allowed:
                continue
            if not isinstance(value, str):
                raise ApiError(400, f"{key} must be text.")
            updates[key] = value.strip()[:200]
        if not updates:
            return 400, {"message": "Provide name, phone or district to update."}
        if "name" in updates and not updates["name"]:
            raise ApiError(400, "Full name is required.")
        if "district" in updates and not updates["district"]:
            raise ApiError(400, "District is required.")
        if "phone" in updates and not _phone_is_valid(updates["phone"]):
            raise ApiError(400, "Enter a valid 10-digit mobile number.")
        names = {f"#{key}": key for key in updates}
        values = {f":{key}": value for key, value in updates.items()}
        expression = "SET " + ", ".join(f"#{key} = :{key}" for key in updates)
        item = USERS.update_item(
            Key={"userId": user["userId"]},
            UpdateExpression=expression,
            ExpressionAttributeNames=names,
            ExpressionAttributeValues=values,
            ReturnValues="ALL_NEW",
        ).get("Attributes")
        return 200, item
    if method == "POST" and path == "/reports":
        return 201, _create_report(event, user)
    if method == "GET" and path == "/reports/my":
        return 200, _my_reports(user)
    if method == "POST" and path == "/uploads/presign":
        return 200, _presign(event, user)
    if method == "GET" and path == "/notifications":
        response = NOTIFICATIONS.query(
            IndexName="UserCreatedIndex",
            KeyConditionExpression=Key("userId").eq(user["userId"]),
            ScanIndexForward=False,
            Limit=100,
        )
        return 200, {"notifications": response.get("Items", [])}
    if method == "PATCH" and path.startswith("/notifications/") and path.endswith("/read"):
        notification_id = path.removeprefix("/notifications/").removesuffix("/read").strip("/")
        notification = NOTIFICATIONS.get_item(Key={"notificationId": notification_id}).get("Item")
        if not notification:
            raise ApiError(404, "Notification not found.")
        if notification.get("userId") != user["userId"]:
            raise ApiError(403, "You cannot update this notification.")
        NOTIFICATIONS.update_item(
            Key={"notificationId": notification_id},
            UpdateExpression="SET #read = :read",
            ExpressionAttributeNames={"#read": "read"},
            ExpressionAttributeValues={":read": True},
        )
        return 200, {"notificationId": notification_id, "read": True}
    if method == "GET" and path == "/admin/reports":
        return 200, _admin_reports(event, user)
    if method == "PATCH" and path.startswith("/admin/reports/") and path.endswith("/status"):
        report_id = path.removeprefix("/admin/reports/").removesuffix("/status").strip("/")
        return 200, _set_status(event, user, report_id)
    if method == "PATCH" and path.startswith("/admin/reports/"):
        report_id = path.removeprefix("/admin/reports/").strip("/")
        if not report_id:
            raise ApiError(404, "Report not found.")
        return 200, _admin_edit_report(event, user, report_id)
    if method == "DELETE" and path.startswith("/admin/reports/"):
        report_id = path.removeprefix("/admin/reports/").strip("/")
        if not report_id:
            raise ApiError(404, "Report not found.")
        return 200, _admin_delete_report(user, report_id)
    if method == "GET" and path.startswith("/reports/"):
        report_id = path.removeprefix("/reports/")
        result = REPORTS.get_item(Key={"reportId": report_id}).get("Item")
        if not result:
            raise ApiError(404, "Report not found.")
        if result.get("userId") != user["userId"]:
            _admin(user)
        return 200, _safe_report(result, include_description=True)
    if method == "PATCH" and path.startswith("/reports/"):
        report_id = path.removeprefix("/reports/")
        report = REPORTS.get_item(Key={"reportId": report_id}).get("Item")
        if not report:
            raise ApiError(404, "Report not found.")
        if report.get("userId") != user["userId"]:
            raise ApiError(403, "You cannot update this report.")
        if report.get("status") not in ("SUBMITTED", "UNDER_REVIEW"):
            raise ApiError(409, "This report can no longer be edited.")
        payload = _body(event)
        description = str(payload.get("description", report.get("description", "")))[:4000]
        updated = REPORTS.update_item(
            Key={"reportId": report_id},
            UpdateExpression="SET description = :description, updatedAt = :updated",
            ExpressionAttributeValues={":description": description, ":updated": _now()},
            ReturnValues="ALL_NEW",
        ).get("Attributes")
        return 200, _safe_report(updated, include_description=True)
    raise ApiError(404, "Endpoint not found.")


def lambda_handler(event, _context):
    try:
        result = _route(event)
        if isinstance(result, dict):
            return result
        status, body = result
        return _response(status, body)
    except ApiError as error:
        return _response(error.status, {"message": error.message})
    except Exception as error:
        print("Unhandled request error", event.get("rawPath", ""), type(error).__name__)
        return _response(500, {"message": "The request could not be completed."})
