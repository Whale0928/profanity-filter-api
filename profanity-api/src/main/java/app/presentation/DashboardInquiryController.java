package app.presentation;

import app.application.inquiry.InquiryDetailView;
import app.application.inquiry.InquiryService;
import app.application.inquiry.InquiryView;
import app.application.inquiry.NewInquiryCommand;
import app.core.data.response.ApiResponse;
import app.core.data.response.PageView;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.dto.request.CreateInquiryRequest;
import app.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자가 본인 문의를 등록하고 조회하는 API입니다. 관리자도 이 경로에서는 본인 문의만 다룹니다. */
@RestController
@RequiredArgsConstructor
@Hidden
@RequestMapping(value = "/api/v1/dashboard/inquiries", produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardInquiryController {

  private final InquiryService inquiryService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageView<InquiryView>>> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PageResult<InquiryView> result =
        inquiryService.findMine(
            SecurityContextUtil.getCurrentLoginUserId(), query, PageQuery.of(page, size));
    return noStore(new PageView<>(result.items(), result.page(), result.hasNext()));
  }

  @GetMapping("/{inquiryId}")
  public ResponseEntity<ApiResponse<InquiryDetailView>> detail(@PathVariable Long inquiryId) {
    return noStore(
        inquiryService.findMineDetail(SecurityContextUtil.getCurrentLoginUserId(), inquiryId));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResponse<InquiryDetailView>> create(
      @Valid @RequestBody CreateInquiryRequest request) {
    NewInquiryCommand command =
        NewInquiryCommand.of(
            request.type(),
            request.title(),
            request.content(),
            request.word(),
            request.requestType(),
            request.severity());
    return noStore(inquiryService.create(SecurityContextUtil.getCurrentLoginUserId(), command));
  }

  private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
    ResponseEntity<ApiResponse<T>> response = ApiResponse.ok(data);
    return ResponseEntity.status(response.getStatusCode())
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(response.getBody());
  }
}
