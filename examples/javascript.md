#### JavaScript

```JavaScript
const fetch = require('node-fetch');

const url = 'https://api.profanity-filter.run/api/v1/filter/basic';
const apiKey = 'YOUR_API_KEY';
const data = {
    text: '나쁜말',
    mode: 'FILTER',
    callbackUrl: 'http://example.com/callback'
};

fetch(url, {
    method: 'POST',
    headers: {
        'accept': 'application/json',
        'Content-Type': 'application/json',
        'x-api-key': apiKey
    },
    body: JSON.stringify(data)
})
    .then(response => response.json())
    .then(data => console.log('Request was successful:', data))
    .catch(error => console.error('Request failed:', error));
```
