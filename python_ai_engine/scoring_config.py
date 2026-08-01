import math
from dataclasses import dataclass


@dataclass
class ScoringWeights:
    """
    Configuration for heuristic scoring weights used in the AI engine.
    
    These coefficients are chosen based on heuristic assumptions about 
    the relative importance of various signals in emergency logistics:
    - severity: rule-based urgency is the primary anchor, supplemented by sentiment (negative score)
      and historical context (knn predicted urgency).
    - area: prioritizing average score for a baseline, max score for peak severity,
      and emergency ratio to detect widespread distress.
    """
    # severity scoring
    rule_weight: float = 0.45
    negative_weight: float = 0.30
    knn_weight: float = 0.15
    knn_confidence_weight: float = 0.10
    
    # area scoring
    area_average_weight: float = 0.60
    area_max_weight: float = 0.30
    area_emergency_weight: float = 0.10
    
    # urgency thresholds
    critical_threshold: float = 0.80
    high_threshold: float = 0.62
    medium_threshold: float = 0.38
    
    # context boost
    high_term_boost: float = 0.18
    medium_term_boost: float = 0.08
    max_context_boost: float = 0.25

    def __post_init__(self):
        severity_sum = self.rule_weight + self.negative_weight + self.knn_weight + self.knn_confidence_weight
        assert math.isclose(severity_sum, 1.0, rel_tol=1e-5), f"Severity weights must sum to 1.0, got {severity_sum}"
        
        area_sum = self.area_average_weight + self.area_max_weight + self.area_emergency_weight
        assert math.isclose(area_sum, 1.0, rel_tol=1e-5), f"Area weights must sum to 1.0, got {area_sum}"

default_weights = ScoringWeights()
