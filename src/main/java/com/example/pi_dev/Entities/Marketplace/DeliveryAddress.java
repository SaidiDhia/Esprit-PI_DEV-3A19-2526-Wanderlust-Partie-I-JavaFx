package com.example.pi_dev.Entities.Marketplace;

public class DeliveryAddress {
    private int    id;
    private int    factureId;
    private String fullName;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String notes;
    private String email;      // optional — used to send order confirmation email

    public DeliveryAddress() {}

    public DeliveryAddress(int factureId, String fullName, String phone,
                           String address, String city, String postalCode, String notes) {
        this.factureId  = factureId;
        this.fullName   = fullName;
        this.phone      = phone;
        this.address    = address;
        this.city       = city;
        this.postalCode = postalCode;
        this.notes      = notes;
    }

    public int    getId()          { return id; }
    public void   setId(int id)    { this.id = id; }

    public int    getFactureId()              { return factureId; }
    public void   setFactureId(int factureId) { this.factureId = factureId; }

    public String getFullName()                { return fullName; }
    public void   setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone()             { return phone; }
    public void   setPhone(String phone) { this.phone = phone; }

    public String getAddress()               { return address; }
    public void   setAddress(String address) { this.address = address; }

    public String getCity()            { return city; }
    public void   setCity(String city) { this.city = city; }

    public String getPostalCode()                  { return postalCode != null ? postalCode : ""; }
    public void   setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getNotes()             { return notes != null ? notes : ""; }
    public void   setNotes(String notes) { this.notes = notes; }

    // email is optional — getter returns empty string if null (never NPE)
    public String getEmail()               { return email != null ? email.trim() : ""; }
    public void   setEmail(String email)   { this.email = email; }
}
