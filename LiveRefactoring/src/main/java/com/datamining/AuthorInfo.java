package com.datamining;

import java.io.*;
import java.util.Objects;
import java.util.Set;

public class AuthorInfo implements Serializable, Comparable<AuthorInfo> {
    private static final long serialVersionUID = 42L;
    private String authorName;
    private String authorEmail;

    public AuthorInfo(String authorName, String authorEmail) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuthorInfo other = (AuthorInfo) obj;
        return this.authorEmail.equals(other.authorEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.authorEmail);
    }

    @Override
    public int compareTo(AuthorInfo authorInfo) {
        return this.authorEmail.compareTo(authorInfo.authorEmail);
    }

    @Override
    public String toString() {
        return authorName + " (" + authorEmail + ")";
    }

    public static Set<AuthorInfo> readAuthorInfoSet(String filePath) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath));

        Object obj = ois.readObject();
        if (obj instanceof Set) {
            return (Set<AuthorInfo>) obj;
        }

        return null;
    }

    public static void writeAuthorInfoSet(Set<AuthorInfo> authors, String filePath) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath));
        oos.writeObject(authors);
        oos.close();
    }

}
