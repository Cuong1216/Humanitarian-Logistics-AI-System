import json
import sys
from pathlib import Path

from schemas import AnalyzeRequest
from services.nlp_service import NlpService


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    sample_path = Path(__file__).with_name("sample_request.json")
    payload = json.loads(sample_path.read_text(encoding="utf-8"))
    request = AnalyzeRequest(**payload)
    result = NlpService().analyze_post(request.post)
    print(result.model_dump_json(indent=2))


if __name__ == "__main__":
    main()
