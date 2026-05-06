package com.example.pi_dev.Entities.Marketplace;

import java.util.Date;

public class Facture {
    private int    id;
    private String userId;
    private Date   date;
    private float  total;
    private String paymentMethod;   // "online" or "cash"
    private String deliveryStatus;  // "pending", "confirmed", "cancelled"

    public Facture() {}

    public Facture(String userId, Date date, float total) {
        this.userId = userId;
        this.date   = date;
        this.total  = total;
    }

    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }

    public String getUserId()              { return userId; }
    public void   setUserId(String userId)    { this.userId = userId; }

    public Date   getDate()            { return date; }
    public void   setDate(Date date)   { this.date = date; }

    public float  getTotal()               { return total; }
    public void   setTotal(float total)    { this.total = total; }

    public String getPaymentMethod()                     { return paymentMethod != null ? paymentMethod : "cash"; }
    public void   setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDeliveryStatus()                      { return deliveryStatus != null ? deliveryStatus : "pending"; }
    public void   setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
}
