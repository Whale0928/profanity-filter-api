package app.application;

import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;

public interface ProfanityFilter {
    Boolean containsProfanity(String text);

    FilterResponse allMatched(String text);

    FilterWord firstMatched(String text);
}
