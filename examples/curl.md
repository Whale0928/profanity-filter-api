#### bash

```bash
    curl -X POST "https://api.profanity-filter.run/api/v1/filter" \
    -H "accept: application/json" \
    -H "Content-Type: application/json" \
    -d '{"text": "나쁜말", "mode": "filter"}'
```
