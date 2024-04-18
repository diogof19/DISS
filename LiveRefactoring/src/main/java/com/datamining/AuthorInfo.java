package com.datamining;

import java.util.Objects;

public class AuthorInfo implements Comparable<AuthorInfo> {
    private final Integer id;
    private final String authorName;
    private final String authorEmail;
    private Boolean selected;

    public AuthorInfo(Integer id, String authorName, String authorEmail, Boolean selected) {
        this.id = id;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.selected = selected;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuthorInfo other = (AuthorInfo) obj;
        return this.authorEmail.equals(other.authorEmail) && this.authorName.equals(other.authorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorEmail, authorName);
    }

    @Override
    public int compareTo(AuthorInfo authorInfo) {
        return this.authorEmail.compareTo(authorInfo.authorEmail);
    }

    @Override
    public String toString() {
        return authorName + " (" + authorEmail + ")";
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getAuthorName() {
        return authorName;
    }

    public int getId() {
        return id;
    }

    public Boolean isSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
