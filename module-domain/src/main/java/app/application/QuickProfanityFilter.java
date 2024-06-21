package app.application;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 원색적인 욕설 필터링을 위한 정규 표현식 필터
 */
@Service
public class QuickProfanityFilter {
    private static final String BASIC_PROFANITY = "[시씨슈쓔쉬쉽쒸쓉]([0-9]*|[0-9]+ *)[바발벌빠빡빨뻘파팔펄]|지랄|니[애에]미|새 *[키퀴]";
    private static final String NUMBER_INCLUDED_PROFANITY = "[존좉좇][0-9 ]*나|[자보][0-9]+지|갈[0-9]*보[^가-힣]";
    private static final String SPECIAL_CHARS_PROFANITY = "ㅅㅣㅂㅏㄹ?|ㅂ[0-9]*ㅅ|[ㅄᄲᇪᄺᄡᄣᄦᇠ]|[ㅅㅆᄴ][0-9]*[ㄲㅅㅆᄴㅂ]";
    private static final String SENSITIVE_TERMS = "盧|무현|文在|在寅";
    private static final String FINAL_REGEX = BASIC_PROFANITY + "|" + NUMBER_INCLUDED_PROFANITY + "|" + SPECIAL_CHARS_PROFANITY + "|" + SENSITIVE_TERMS;
    Pattern profanityPattern = Pattern.compile(FINAL_REGEX);

    public Boolean containsProfanity(String word) {
        return profanityPattern.matcher(word).find();
    }
}
