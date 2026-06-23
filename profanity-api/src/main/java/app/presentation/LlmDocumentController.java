package app.presentation;

import app.openapi.LlmDocumentOpenApi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@LlmDocumentOpenApi.ApiTag
public class LlmDocumentController {

  private static final MediaType TEXT_PLAIN_UTF_8 =
      new MediaType("text", "plain", StandardCharsets.UTF_8);
  private static final ClassPathResource LLMS_RESOURCE = new ClassPathResource("static/llms.txt");

  @GetMapping({"/llms.txt", "/llm.txt"})
  @LlmDocumentOpenApi.GetIndex
  public ResponseEntity<String> index() {
    try (var inputStream = LLMS_RESOURCE.getInputStream()) {
      return ResponseEntity.ok()
          .contentType(TEXT_PLAIN_UTF_8)
          .body(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "LLM 문서 색인을 찾을 수 없습니다.", exception);
    }
  }
}
