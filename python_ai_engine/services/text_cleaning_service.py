import re
import unicodedata


class TextCleaningService:
    URL_PATTERN = re.compile(r"https?://\S+|www\.\S+", flags=re.IGNORECASE)
    MENTION_PATTERN = re.compile(r"@\w+")
    HASHTAG_PATTERN = re.compile(r"#(\w+)")
    EXTRA_SYMBOL_PATTERN = re.compile(r"[^\w\sÀ-ỹ.,!?;:%/-]", flags=re.UNICODE)
    SPACE_PATTERN = re.compile(r"\s+")

    def clean(self, text: str) -> str:
        text = unicodedata.normalize("NFC", text or "")
        text = self.URL_PATTERN.sub(" ", text)
        text = self.MENTION_PATTERN.sub(" ", text)
        text = self.HASHTAG_PATTERN.sub(r"\1", text)
        text = self.EXTRA_SYMBOL_PATTERN.sub(" ", text)
        text = self.SPACE_PATTERN.sub(" ", text)
        return text.strip()

    def clean_many(self, texts: list[str]) -> list[str]:
        return [self.clean(text) for text in texts if self.clean(text)]
