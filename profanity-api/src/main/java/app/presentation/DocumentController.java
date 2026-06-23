package app.presentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DocumentController {

  private static final MediaType TEXT_MARKDOWN_UTF_8 =
      new MediaType("text", "markdown", StandardCharsets.UTF_8);
  private static final MediaType TEXT_PLAIN_UTF_8 =
      new MediaType("text", "plain", StandardCharsets.UTF_8);
  private static final List<ClassPathResource> OVERVIEW_RESOURCES =
      List.of(
          new ClassPathResource("openapi/overview.md"),
          new ClassPathResource("openapi/error-model.md"),
          new ClassPathResource("openapi/authentication.md"));
  private static final ClassPathResource LLMS_RESOURCE = new ClassPathResource("static/llms.txt");

  @GetMapping("/overview.md")
  public ResponseEntity<String> overview() {
    return ResponseEntity.ok()
        .contentType(TEXT_MARKDOWN_UTF_8)
        .body(readMarkdownInOrder(OVERVIEW_RESOURCES));
  }

  @GetMapping({"/llms.txt", "/llm.txt"})
  public ResponseEntity<String> llms() {
    return ResponseEntity.ok().contentType(TEXT_PLAIN_UTF_8).body(readResource(LLMS_RESOURCE));
  }

  private static String readMarkdownInOrder(List<ClassPathResource> resources) {
    return resources.stream()
        .map(DocumentController::readResource)
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("");
  }

  private static String readResource(ClassPathResource resource) {
    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "문서 리소스를 찾을 수 없습니다.", exception);
    }
  }
}
