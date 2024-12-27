package app.dto.response;

public record FilterWord(
        String word,
        Integer length,
        int startIndex,
        int endIndex
) {
    public static FilterWord create(
            String word,
            Integer start,
            Integer end
    ) {
        return new FilterWord(word, word.length(), start, end);
    }

    public static FilterWord empty() {
        return new FilterWord("", 0, 0, 0);
    }
}
