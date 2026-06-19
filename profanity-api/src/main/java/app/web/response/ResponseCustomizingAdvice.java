package app.web.response;

import app.core.data.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * ApiResponse 응답을 가로채, 조건에 맞는 ResponseCustomizer 들을 적용한다. meta가 비어 있으면 ApiResponse 직렬화 시 NON_EMPTY
 * 로 생략되어 기존 응답 스펙은 변하지 않는다.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class ResponseCustomizingAdvice implements ResponseBodyAdvice<Object> {
  private final List<ResponseCustomizer> customizers;

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body instanceof ApiResponse<?> apiResponse && apiResponse.meta() != null) {
      RequestContext context = RequestContext.from(request);
      for (ResponseCustomizer customizer : customizers) {
        if (customizer.supports(context)) {
          customizer.customize(apiResponse.meta(), context);
        }
      }
    }
    return body;
  }
}
