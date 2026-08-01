import hashlib
import logging
import os

import redis

from schemas import AnalysisResult, SocialMediaPost

logger = logging.getLogger(__name__)

class CacheService:
    def __init__(self) -> None:
        self.enabled = os.getenv("CACHE_ENABLED", "true").lower() == "true"
        self.ttl = int(os.getenv("CACHE_TTL_SECONDS", "3600"))
        self.redis_url = os.getenv("REDIS_URL", "redis://localhost:6379/0")
        
        self.hits = 0
        self.misses = 0
        
        self.client = None
        if self.enabled:
            try:
                self.client = redis.Redis.from_url(self.redis_url, decode_responses=True)
                self.client.ping()
            except Exception as e:
                logger.warning(f"Failed to connect to Redis: {e}. Caching will be disabled.")
                self.client = None

    def get(self, key: str) -> AnalysisResult | None:
        if not self.client:
            return None
        
        try:
            data = self.client.get(key)
            if data:
                self.hits += 1
                logger.info(f"Cache hit for key: {key}")
                return AnalysisResult.model_validate_json(data)
            self.misses += 1
            logger.info(f"Cache miss for key: {key}")
        except Exception as e:
            logger.warning(f"Redis get failed: {e}")
        return None

    def set(self, key: str, result: AnalysisResult, ttl: int = 3600) -> None:
        if not self.client:
            return
        
        try:
            self.client.setex(key, ttl, result.model_dump_json())
        except Exception as e:
            logger.warning(f"Redis set failed: {e}")

    def make_key(self, post: SocialMediaPost) -> str:
        comments_str = "".join(sorted(post.comments))
        raw = f"{post.text}{post.platform.value if hasattr(post.platform, 'value') else post.platform}{comments_str}"
        return hashlib.sha256(raw.encode("utf-8")).hexdigest()

    def get_stats(self) -> dict:
        total = self.hits + self.misses
        hit_rate = (self.hits / total) if total > 0 else 0.0
        return {
            "hits": self.hits,
            "misses": self.misses,
            "hit_rate": round(hit_rate, 4)
        }
