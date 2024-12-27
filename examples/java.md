#### Java

```java
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;

import java.util.Collections;

public class ProfanityFilterClient {

    private static final String API_URL = "https://api.profanity-filter.run/api/v1/filter/";
    private static final String API_KEY = "YOUR_API_KEY";

    public String filterProfanity(String text, String mode, String callbackUrl) {
        RestTemplate restTemplate = new RestTemplate();

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-api-key", API_KEY);

        // Set the request body
        String jsonInputString = String.format("{ \"text\": \"%s\", \"mode\": \"%s\", \"callbackUrl\": \"%s\" }", text, mode, callbackUrl);

        // Create the request entity
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonInputString, headers);

        // Make the request
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, requestEntity, String.class);

        // Check the response
        if (response.getStatusCode().is2xxSuccessful()) {
            return "Request was successful. Response body: " + response.getBody();
        } else {
            return "Request failed. Response code: " + response.getStatusCode();
        }
    }
}
```
