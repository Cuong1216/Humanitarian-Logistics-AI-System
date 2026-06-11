import json
import sys
from pathlib import Path

from schemas import AnalyzeRequest, KeywordAnalyzeRequest
from services.nlp_service import NlpService


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    sample_path = Path(__file__).with_name("sample_request.json")
    payload = json.loads(sample_path.read_text(encoding="utf-8-sig"))

    if "post" in payload:
        post = AnalyzeRequest(**payload).post
    else:
        keyword_request = KeywordAnalyzeRequest(**payload)
        post = keyword_request.posts[0].model_copy(update={"keyword": keyword_request.keyword})

    result = NlpService().analyze_post(post)
    print(result.model_dump_json(indent=2))


if __name__ == "__main__":
    main()

