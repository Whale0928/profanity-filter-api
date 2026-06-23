package app.presentation;

import app.openapi.OpenApiMarkdownOpenApi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/openapi")
@OpenApiMarkdownOpenApi.ApiTag
public class OpenApiMarkdownController {

  private static final MediaType TEXT_MARKDOWN_UTF_8 =
      new MediaType("text", "markdown", StandardCharsets.UTF_8);
  private static final Set<String> ALLOWED_DOCUMENTS =
      Set.of("overview.md", "error-model.md", "authentication.md");

  @GetMapping("/{documentName:.+\\.md}")
  @OpenApiMarkdownOpenApi.GetMarkdown
  public ResponseEntity<String> markdown(@PathVariable String documentName) {
    if (!ALLOWED_DOCUMENTS.contains(documentName)) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    var resource = new ClassPathResource("openapi/" + documentName);
    try (var inputStream = resource.getInputStream()) {
      return ResponseEntity.ok()
          .contentType(TEXT_MARKDOWN_UTF_8)
          .body(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND,
          "OpenAPI Markdown 문서를 찾을 수 없습니다.",
          exception);
    }
  }
}
