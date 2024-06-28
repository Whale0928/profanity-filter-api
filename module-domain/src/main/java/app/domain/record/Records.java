package app.domain.record;

import app.core.data.constant.Mode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "records")
@Entity
public class Records {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("요청 트래킹 ID")
    @Column(nullable = false, updatable = false, unique = true)
    private UUID trackingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "mode")
    private Mode mode;

    @Comment("API Key")
    @Column
    private String apiKey;

    @Comment("요청된 텍스트")
    @Column(nullable = false)
    private String requestText;

    @Comment("필터링 된 욕설들")
    @Column
    private String words;

    @Comment("요청 referrer")
    private String referrer;

    @Comment("요청 IP")
    private String ip;

    @Comment("요청 시각")
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Comment("요청 수정 시각")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    protected Records() {
    }

    // Private constructor for Builder
    private Records(Builder builder) {
        this.trackingId = builder.trackingId;
        this.apiKey = builder.apiKey;
        this.requestText = builder.requestText;
        this.words = builder.words;
        this.referrer = builder.referrer;
        this.ip = builder.ip;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public UUID getTrackingId() {
        return trackingId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getRequestText() {
        return requestText;
    }

    public String getWords() {
        return words;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getIp() {
        return ip;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * The type Builder.
     */
// Builder class
    public static class Builder {
        private UUID trackingId;
        private String apiKey;
        private Mode mode;
        private String requestText;
        private String words;
        private String referrer;
        private String ip;

        public Builder trackingId(UUID trackingId) {
            this.trackingId = trackingId;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder requestText(String requestText) {
            this.requestText = requestText;
            return this;
        }

        public Builder words(String words) {
            this.words = words;
            return this;
        }

        public Builder referrer(String referrer) {
            this.referrer = referrer;
            return this;
        }


        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public Records build() {
            return new Records(this);
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "trackingId=" + trackingId +
                    "mode=" + mode +
                    ", apiKey='" + apiKey + '\'' +
                    ", requestText='" + requestText + '\'' +
                    ", words='" + words + '\'' +
                    ", referrer='" + referrer + '\'' +
                    ", ip='" + ip + '\'' +
                    '}';
        }
    }
}
