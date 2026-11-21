package com.donkan.sisvetclinic.modules.commercial.domain;

public class User {

    private Long id;
    private String fullName;
    private String username;
    private Role role;
    private boolean active;

    public User() {
    }

    public User(Long id, String fullName, String username, Role role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}