package com.wbw.user;

import com.wbw.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_sub", nullable = false, unique = true)
    private String googleSub;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "picture_url", length = 1000)
    private String pictureUrl;

    protected User() {
    }

    public User(String googleSub, String email, String name, String pictureUrl) {
        this.googleSub = googleSub;
        updateProfile(email, name, pictureUrl);
    }

    public void updateProfile(String email, String name, String pictureUrl) {
        this.email = email == null ? "" : email;
        this.name = truncate(name == null || name.isBlank() ? this.email : name, 100);
        this.pictureUrl = pictureUrl != null && pictureUrl.length() <= 1000 ? pictureUrl : null;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    public Long getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }
}
