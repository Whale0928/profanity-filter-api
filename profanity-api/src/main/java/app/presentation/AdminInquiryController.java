package app.presentation;

import app.application.inquiry.AdminInquiryService;
import app.application.inquiry.InquiryDetailView;
import app.application.inquiry.InquiryView;
import app.application.inquiry.WordDecision;
import app.core.data.response.ApiResponse;
import app.core.data.response.PageView;
import app.domain.inquiry.InquiryStatus;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.dto.request.admin.ChangeInquiryStatusRequest;
import app.dto.request.admin.InquiryReplyRequest;
import app.dto.request.admin.WordDecisionRequest;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 문의 처리 API입니다. 단어 승인은 답변 완료와 분리된 별도 경로입니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/admin/inquiries", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminInquiryController {

  private final AdminInquiryService adminInquiryService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageView<InquiryView>>> list(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageResult<InquiryView> result =
        adminInquiryService.search(type, status, query, PageQuery.of(page, size));
    return noStore(new PageView<>(result.items(), result.page(), result.hasNext()));
  }

  @GetMapping("/{inquiryId}")
  public ResponseEntity<ApiResponse<InquiryDetailView>> detail(@PathVariable Long inquiryId) {
    return noStore(adminInquiryService.findDetail(inquiryId));
  }

  @PatchMapping(value = "/{inquiryId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<InquiryDetailView>> changeStatus(
      @PathVariable Long inquiryId, @Valid @RequestBody ChangeInquiryStatusRequest request) {
    return noStore(
        adminInquiryService.changeStatus(
            SecurityContextUtil.getCurrentLoginUserId(),
            inquiryId,
            InquiryStatus.parse(request.status())));
  }

  @PostMapping(value = "/{inquiryId}/replies", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<InquiryDetailView>> reply(
      @PathVariable Long inquiryId, @Valid @RequestBody InquiryReplyRequest request) {
    return noStore(
        adminInquiryService.reply(
            SecurityContextUtil.getCurrentLoginUserId(),
            inquiryId,
            request.content(),
            request.resolve()));
  }

  @PostMapping(value = "/{inquiryId}/word-decision", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<InquiryDetailView>> decideWordRequest(
      @PathVariable Long inquiryId, @Valid @RequestBody WordDecisionRequest request) {
    return noStore(
        adminInquiryService.decideWordRequest(
            SecurityContextUtil.getCurrentLoginUserId(),
            inquiryId,
            WordDecision.parse(request.decision()),
            request.reason()));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
