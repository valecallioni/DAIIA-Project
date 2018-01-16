package Helpers;

import jade.core.AID;

import java.io.Serializable;

public class SoldInformation implements Serializable {
    private AID buyer;
    private double soldPrice;

    public SoldInformation(){};

    public AID getBuyer() {
        return buyer;
    }

    public void setBuyer(AID buyer) {
        this.buyer = buyer;
    }

    public double getSoldPrice() {
        return soldPrice;
    }

    public void setSoldPrice(double soldPrice) {
        this.soldPrice = soldPrice;
    }
}