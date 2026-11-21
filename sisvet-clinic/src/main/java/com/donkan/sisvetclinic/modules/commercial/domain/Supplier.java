package com.donkan.sisvetclinic.modules.commercial.domain;

public class Supplier {

    private Long id;
    private String ruc;
    private String businessName;
    private String phone;
    private String email;
    private boolean active;

    public Supplier() {
    }

    public Supplier(Long id, String ruc, String businessName,
                    String phone, String email, boolean active) {
        this.id = id;
        this.ruc = ruc;
        this.businessName = businessName;
        this.phone = phone;
        this.email = email;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}