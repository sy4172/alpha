package com.example.alpha;

public class User {
    private String password;
    private String email;

    public User () {}
    public User (String password, String email){
        this.password = password;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
