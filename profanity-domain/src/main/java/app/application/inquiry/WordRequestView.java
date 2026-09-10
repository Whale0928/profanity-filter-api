package app.application.inquiry;

import app.domain.manage.WordManagementRequest;
import app.domain.manage.WordRequestType;

/** 문의에 연결된 단어 요청입니다. */
public record WordRequestView(
    Long id, String word, String reason, String severity, String requestType, String status) {

  public static WordRequestView from(WordManagementRequest request) {
    WordRequestType exposedType = request.exposedRequestType();
    return new WordRequestView(
        request.getId(),
        request.getWord(),
        request.getReason(),
        request.getSeverity(),
        exposedType == null ? request.getRequestType() : exposedType.name(),
        request.exposedStatus());
  }
}
