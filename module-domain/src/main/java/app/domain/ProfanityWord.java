package app.domain;

import app.domain.constant.isUsedType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "profanity_word")
@Entity(name = "profanity_word")
public class ProfanityWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private isUsedType isUsed;

    protected ProfanityWord() {
    }

    public ProfanityWord(Long id, String word, isUsedType isUsed) {
        this.id = id;
        this.word = word;
        this.isUsed = isUsed;
    }

    public static ProfanityWord create(String word) {
        return new ProfanityWord(null, word, isUsedType.Y);
    }

    public Long getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public isUsedType getIsUsed() {
        return isUsed;
    }
}
