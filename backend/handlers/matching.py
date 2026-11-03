import re
from difflib import SequenceMatcher


def normalize(value):
    return " ".join(re.findall(r"[a-z0-9]+", str(value or "").lower()))


def score(candidate, query):
    points = 0
    signals = []

    query_name = normalize(query.get("name"))
    candidate_name = normalize(candidate.get("name"))
    if query_name and candidate_name:
        ratio = SequenceMatcher(None, query_name, candidate_name).ratio()
        if ratio >= 0.55:
            points += round(ratio * 35)
            signals.append("similar name")

    age = candidate.get("age")
    if age is not None:
        low = query.get("ageMin")
        high = query.get("ageMax")
        if low is not None and high is not None and low <= int(age) <= high:
            points += 20
            signals.append("age range")
        elif low is not None and abs(int(age) - low) <= 3:
            points += 10
            signals.append("approximate age")
        elif high is not None and abs(int(age) - high) <= 3:
            points += 10
            signals.append("approximate age")

    for key, label, weight in (
        ("district", "same district", 20),
        ("gender", "gender", 8),
        ("area", "nearby area", 10),
        ("lastSeenLocation", "last-seen location", 10),
        ("location", "nearby location", 10),
    ):
        sought = normalize(query.get(key))
        found = normalize(candidate.get(key))
        if sought and found and (sought in found or found in sought):
            points += weight
            signals.append(label)

    query_text = " ".join(
        str(query.get(key) or "")
        for key in ("features", "hair", "height", "mole", "scar", "birthmark")
    )
    query_tokens = set(normalize(query_text).split())
    physical_features = candidate.get("physicalFeatures") or {}
    candidate_text = " ".join(
        [str(candidate.get("description") or "")]
        + [str(value) for value in physical_features.values()]
    )
    candidate_tokens = set(normalize(candidate_text).split())
    overlap = query_tokens & candidate_tokens
    if overlap:
        points += min(25, 5 * len(overlap))
        signals.append("matching physical details")

    return points, signals


def rank(candidates, query):
    ranked = []
    for candidate in candidates:
        points, signals = score(candidate, query)
        if points > 0:
            safe_candidate = {
                key: candidate.get(key)
                for key in ("reportId", "reportType", "age", "gender",
                            "district", "status", "createdAt", "lastSeenDate",
                            "physicalFeatures")
            }
            if candidate.get("name"):
                safe_candidate["name"] = candidate.get("name")
            safe_candidate["matchingSignals"] = signals
            ranked.append((points, safe_candidate))
    ranked.sort(key=lambda result: (result[0], result[1].get("createdAt", "")), reverse=True)
    return [record for _, record in ranked]
