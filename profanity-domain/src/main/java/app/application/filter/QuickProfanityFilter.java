package app.application.filter;

import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * 원색적인 욕설 필터링을 위한 정규 표현식 필터
 */
//@Deprecated
@Service
public class QuickProfanityFilter implements ProfanityFilter {
    private static final String BASIC_PROFANITY = "[시씨슈쓔쉬쉽쒸쓉]([0-9]*|[0-9]+ *)[바발벌빠빡빨뻘파팔펄]|지랄|니[애에]미|새 *[키퀴]";
    private static final String NUMBER_INCLUDED_PROFANITY = "[존좉좇][0-9 ]*나|[자보][0-9]+지|갈[0-9]*보[^가-힣]";
    private static final String SPECIAL_CHARS_PROFANITY = "ㅅㅣㅂㅏㄹ?|ㅂ[0-9]*ㅅ|[ㅄᄲᇪᄺᄡᄣᄦᇠ]|[ㅅㅆᄴ][0-9]*[ㄲㅅㅆᄴㅂ]";
    private static final String SENSITIVE_TERMS = "盧|무현|文在|在寅";
    private static final String FINAL_REGEX = BASIC_PROFANITY + "|" + NUMBER_INCLUDED_PROFANITY + "|" + SPECIAL_CHARS_PROFANITY + "|" + SENSITIVE_TERMS;
    Pattern profanityPattern = Pattern.compile(FINAL_REGEX);

    @Override
    public Boolean containsProfanity(String word) {
        return profanityPattern.matcher(word).find();
    }

    @Override
    public FilterResponse allMatched(final String text) {
        HashSet<FilterWord> filterWords = new HashSet<>();

        ElapsedStartAt startAt = ElapsedStartAt.now();
        var matcher = profanityPattern.matcher(text);
        while (matcher.find()) {
            var word = matcher.group();
            var start = matcher.start();
            var end = matcher.end();
            filterWords.add(FilterWord.create(word, start, end));
        }
        Elapsed end = Elapsed.end(startAt);

        return FilterResponse.create(text, filterWords, end);
    }

    @Override
    public FilterWord firstMatched(String text) {
        var matcher = profanityPattern.matcher(text);
        if (matcher.find()) {
            var word = matcher.group();
            var start = matcher.start();
            var end = matcher.end();
            return FilterWord.create(word, start, end);
        }
        return FilterWord.empty();
    }
}
