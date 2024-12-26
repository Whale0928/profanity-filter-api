package app.domain.manage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

@Table(name = "manage_account")
@Entity(name = "manage_account")
public class ManageAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("관리자명")
    @Column(unique = true, nullable = false)
    private String username;

    @Comment("비밀번호")
    @Column(unique = true, nullable = false)
    private String password;

    protected ManageAccount() {
    }

    public ManageAccount(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public static ManageAccount createAccount(String username, String password) {
        return new ManageAccount(null, username, password);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
