package app.application.news;

import app.domain.news.NewsCategory;
import app.domain.news.NewsStatus;

/**
 * 소식 작성과 수정에 공통으로 쓰는 입력입니다.
 *
 * @param title 제목
 * @param category 분류
 * @param content 마크다운 본문
 * @param status 공개 상태
 */
public record NewsWriteCommand(
    String title, NewsCategory category, String content, NewsStatus status) {

  public static NewsWriteCommand of(String title, String category, String content, String status) {
    return new NewsWriteCommand(
        title, NewsCategory.parse(category), content, NewsStatus.parse(status));
  }
}
