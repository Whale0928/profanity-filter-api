package app.e2e.client;

import app.core.data.response.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * E2E 테스트에서 FE가 받은 HTTP 호출 결과를 다루는 테스트 전용 wrapper.
 *
 * <p>API client가 endpoint별 body 타입을 선언하고, 테스트는 역직렬화된 body로 시나리오를 검증한다.
 */
public final class ApiCallResponse<T> {

  private final MvcTestResult result;
  private final ObjectMapper objectMapper;
  private final JavaType bodyType;
  private T body;
  private boolean bodyParsed;

  static <T> ApiCallResponse<T> of(
      MvcTestResult result, ObjectMapper objectMapper, Class<T> bodyType) {
    return new ApiCallResponse<>(result, objectMapper, objectMapper.constructType(bodyType));
  }

  static <T> ApiCallResponse<T> of(
      MvcTestResult result, ObjectMapper objectMapper, TypeReference<T> bodyType) {
    return new ApiCallResponse<>(result, objectMapper, objectMapper.constructType(bodyType));
  }

  private ApiCallResponse(MvcTestResult result, ObjectMapper objectMapper, JavaType bodyType) {
    this.result = result;
    this.objectMapper = objectMapper;
    this.bodyType = bodyType;
  }

  public MvcTestResult result() {
    return result;
  }

  public String content() throws Exception {
    return result.getResponse().getContentAsString();
  }

  public T body() throws Exception {
    if (!bodyParsed) {
      body = read(bodyType);
      bodyParsed = true;
    }
    return body;
  }

  public JsonNode json() throws Exception {
    return objectMapper.readTree(content());
  }

  public ApiResponse<Void> errorBody() throws Exception {
    return bodyAs(new TypeReference<>() {});
  }

  public <R> R bodyAs(Class<R> type) throws Exception {
    return read(objectMapper.constructType(type));
  }

  public <R> R bodyAs(TypeReference<R> type) throws Exception {
    return read(objectMapper.constructType(type));
  }

  private <R> R read(JavaType type) throws Exception {
    if (type.hasRawClass(String.class)) {
      return objectMapper.convertValue(content(), type);
    }
    return objectMapper.readValue(content(), type);
  }
}
