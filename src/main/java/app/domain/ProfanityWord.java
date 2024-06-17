package app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
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
    private isUsed isUsed;

    protected ProfanityWord() {
    }

    public ProfanityWord(Long id, String word, app.domain.isUsed isUsed) {
        this.id = id;
        this.word = word;
        this.isUsed = isUsed;
    }
}
