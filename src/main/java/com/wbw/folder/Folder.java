package com.wbw.folder;

import com.wbw.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "folders")
public class Folder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Folder() {
    }

    public Folder(Long userId, String name, int sortOrder) {
        this.userId = userId;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
}
