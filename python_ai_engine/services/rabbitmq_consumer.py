import asyncio
import json
import logging
from collections.abc import Awaitable, Callable

import aio_pika

from schemas import AnalyzeRequest, SocialMediaPost

logger = logging.getLogger("ai_engine.rabbitmq")

RABBITMQ_URL = "amqp://guest:guest@localhost/"
QUEUE_NAME = "ai.analysis.queue"
MAX_RETRIES = 3

async def handle_message(message: aio_pika.IncomingMessage, channel: aio_pika.Channel, process_func: Callable[[AnalyzeRequest], Awaitable[None]]):
    """Hàm xử lý logic cho từng message, với cơ chế Retry / DLQ"""
    try:
        body_str = message.body.decode()
        body = json.loads(body_str)
        post_id = body.get("postId")
        
        if not post_id:
            logger.warning("Received message without postId, dropping.")
            await message.ack()
            return
            
        logger.info(f"Processing RabbitMQ message for postId: {post_id}")
        
        # Build mock post for processing
        # In a real scenario, you'd fetch the post from DB or the message payload
        post = SocialMediaPost(
            id=post_id,
            text="[Mock from RabbitMQ]", # Need to implement DB fetching here in future
            source="rabbitmq",
            timestamp=body.get("timestamp", ""),
            comments=[]
        )
        request = AnalyzeRequest(post=post)
        
        # Gọi xử lý AI Engine thông qua process_func (được truyền từ main)
        await process_func(request)
        
        # Thành công
        await message.ack()
        logger.info(f"Successfully processed postId: {post_id}")
        
    except Exception as e:
        logger.error(f"Lỗi khi xử lý message RabbitMQ: {e}")
        
        # Retry logic
        headers = message.headers or {}
        retries = headers.get("x-retries", 0)
        
        if retries < MAX_RETRIES:
            logger.info(f"Retry lần {retries + 1}/{MAX_RETRIES} cho postId: {post_id}")
            headers["x-retries"] = retries + 1
            
            # Đẩy lại message với retry count mới
            await channel.default_exchange.publish(
                aio_pika.Message(
                    body=message.body,
                    headers=headers
                ),
                routing_key=QUEUE_NAME
            )
            await message.ack()
        else:
            logger.warning(f"Vượt quá số lần retry ({MAX_RETRIES}) cho postId: {post_id}. Đẩy vào DLQ.")
            # Chuyển vào DLQ
            await message.reject(requeue=False)

async def start_consumer(process_func: Callable[[AnalyzeRequest], Awaitable[None]]):
    """Khởi động RabbitMQ consumer"""
    while True:
        try:
            connection = await aio_pika.connect_robust(RABBITMQ_URL)
            channel = await connection.channel()
            
            # Cấu hình QoS để tránh bị quá tải bộ nhớ
            await channel.set_qos(prefetch_count=10)
            
            # Khai báo queue (giả định Spring Boot đã tạo sẵn cùng tham số DLX, dùng passive=True)
            # Bỏ passive=True nếu FastAPI được khởi động trước Spring Boot.
            queue = await channel.declare_queue(QUEUE_NAME, durable=True, arguments={
                "x-dead-letter-exchange": "ai.dlx",
                "x-dead-letter-routing-key": "ai.dlq.routing"
            })
            break
        except Exception as e:
            logger.warning(f"RabbitMQ Connection Error: {e}. Retrying in 5 seconds...")
            await asyncio.sleep(5)
            
    logger.info("RabbitMQ Consumer started. Waiting for messages...")
    
    async with queue.iterator() as queue_iter:
        async for message in queue_iter:
            # Xử lý đồng thời từng message
            asyncio.create_task(handle_message(message, channel, process_func))
