import unittest

from backend.handlers.matching import normalize, rank, score


class MatchingTests(unittest.TestCase):
    def test_normalize_is_case_and_punctuation_insensitive(self):
        self.assertEqual(normalize("  HYDERABAD, Telangana! "), "hyderabad telangana")

    def test_rank_prefers_combined_matching_signals(self):
        query = {
            "name": "Ravi Kumar",
            "ageMin": 20,
            "ageMax": 30,
            "district": "Hyderabad",
            "features": "small scar blue shirt",
        }
        candidates = [
            {
                "reportId": "lower",
                "name": "Ravi",
                "age": 23,
                "district": "Hyderabad",
                "description": "blue shirt",
                "createdAt": "2026-01-01",
            },
            {
                "reportId": "higher",
                "name": "Ravi Kumar",
                "age": 25,
                "district": "Hyderabad",
                "description": "small scar blue shirt",
                "createdAt": "2026-01-01",
            },
        ]
        results = rank(candidates, query)
        self.assertEqual(results[0]["reportId"], "higher")
        self.assertIn("same district", results[0]["matchingSignals"])
        self.assertNotIn("location", results[0])
        self.assertNotIn("description", results[0])
        self.assertNotIn("score", results[0])

    def test_different_attributes_do_not_create_a_candidate(self):
        points, signals = score(
            {"reportId": "no-match", "name": "Anil", "district": "Nizamabad"},
            {"name": "Ravi", "district": "Hyderabad", "features": "scar"},
        )
        self.assertEqual(points, 0)
        self.assertEqual(signals, [])


if __name__ == "__main__":
    unittest.main()
