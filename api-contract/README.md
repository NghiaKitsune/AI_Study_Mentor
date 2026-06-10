# AI Study Mentor — API contract

Backend contract for the `/api/chat` endpoint. Wire-format only — pick any stack you like (FastAPI, Node/Express, Spring, Flask) that can serve this shape.

## Endpoint

```
POST /api/chat
Content-Type: application/json
Authorization: Bearer <token>   ← optional for MVP
```

## Files

| File | Purpose |
|---|---|
| `chat-request.schema.json`  | JSON Schema for request body |
| `chat-response.schema.json` | JSON Schema for response body |
| `sample-request.json`       | Example request — copy into Postman / curl |
| `sample-response.json`      | Example response — what to return |

## Quick curl test

```bash
curl -X POST https://your-backend.example.com/api/chat \
  -H "Content-Type: application/json" \
  -d @sample-request.json
```

## Backend implementation hint (FastAPI + OpenAI)

```python
from fastapi import FastAPI
from pydantic import BaseModel
from openai import OpenAI

app = FastAPI()
client = OpenAI()

class Context(BaseModel):
    user_level: str | None = None
    subject: str | None = None
    locale: str = "vi-VN"

class ChatRequest(BaseModel):
    request_id: str
    conversation_id: int | None = None
    message: str
    context: Context | None = None

@app.post("/api/chat")
def chat(req: ChatRequest):
    system = "You are Milo, a friendly study mentor. Reply in the user's locale."
    completion = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": system},
            {"role": "user",   "content": req.message},
        ],
    )
    reply = completion.choices[0].message.content
    return {
        "request_id": req.request_id,
        "conversation_id": req.conversation_id,
        "reply": reply,
        "final_answer": None,
        "steps": [],
        "follow_ups": ["Simpler version", "Another method", "Practice 3"],
        "tokens_used": completion.usage.total_tokens,
    }
```

## Pointing the Android app at your backend

1. Open `app/build.gradle`
2. Change `USE_MOCK_AI` to `false` in the `debug` block
3. Change `API_BASE_URL` to your backend root (trailing slash required)
4. Rebuild and run.
