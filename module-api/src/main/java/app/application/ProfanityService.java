package app.application;

import app.core.data.response.ProfanityResponse;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class ProfanityService {

    private final DataSourceProfanityFilter dataSourceProfanityFilter;
    private final RegexProfanityFilter regexProfanityFilter;

    public ProfanityService(
            DataSourceProfanityFilter dataSourceProfanityFilter,
            RegexProfanityFilter regexProfanityFilter
    ) {
        this.dataSourceProfanityFilter = dataSourceProfanityFilter;
        this.regexProfanityFilter = regexProfanityFilter;
    }

    public ProfanityResponse basicFilter(String word) {
        // 정규 표현식 필터 결과
        boolean regexResult = regexProfanityFilter.containsProfanity(word);
        // 데이터 소스 필터 결과
        Collection<?> emits = dataSourceProfanityFilter.containsProfanity(word);

        // regexResult가 true이거나 emits가 비어있지 않은 경우 true 반환
        boolean result = regexResult || !emits.isEmpty();

        return ProfanityResponse.success(result);
    }

    public ProfanityResponse advancedFilter(String word) {
        // 정규 표현식 필터 결과
        boolean regexResult = regexProfanityFilter.containsProfanity(word);
        // 데이터 소스 필터 결과
        Collection<?> emits = dataSourceProfanityFilter.containsProfanity(word);

        // regexResult가 true이거나 emits가 비어있지 않은 경우 true 반환
        boolean result = regexResult || !emits.isEmpty();

        return ProfanityResponse.success(result);
    }
}
