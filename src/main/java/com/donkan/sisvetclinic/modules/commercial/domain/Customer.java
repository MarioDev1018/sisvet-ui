package com.donkan.sisvetclinic.modules.commercial.domain;

public class Customer {

    private Long id;
    private String documentNumber;
    private String documentType;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private boolean active;

    public Customer() {
    }

    public Customer(Long id, String documentNumber, String documentType,
                    String fullName, String phone, String email,
                    String address, boolean active) {
        this.id = id;
        this.documentNumber = documentNumber;
        this.documentType = documentType;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}