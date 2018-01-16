package Helpers;

import jade.core.AID;

import java.io.Serializable;
import Helpers.SoldInformation;

public class Auction implements Serializable {

    private static final double FIRST_BID_FACTOR = 5.0/3.0; //Actual price + 2/3 of it
    private static final double RESERVED_PRICE_FACTOR = 0.5; // The reserved price is half the price


    private Artifact artifact;
    private double actualBid;
    private double reservedPrice;
    private double realPrice;
    private AuctionStatus status;
    private SoldInformation soldInformation;
    private double proposal;


    public Auction() {
        this.artifact = new Artifact();
        this.realPrice = this.artifact.getPrice();
        this.actualBid = realPrice*FIRST_BID_FACTOR;
        this.reservedPrice = realPrice * RESERVED_PRICE_FACTOR;
        this.status = AuctionStatus.NOT_ACTIVE;
        this.soldInformation = new SoldInformation();
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public double getActualBid() {
        return actualBid;
    }

    public void setActualBid(double actualBid) {
        this.actualBid = actualBid;
    }

    public double getReservedPrice() {
        return reservedPrice;
    }

    public void setReservedPrice(double reservedPrice) {
        this.reservedPrice = reservedPrice;
    }

    public double getRealPrice() {
        return realPrice;
    }

    private void setRealPrice(double realPrice) {
        this.realPrice = realPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public SoldInformation getSoldInformation() {
        return soldInformation;
    }

    public void setSoldInformation(AID buyer, double price) {
        soldInformation.setBuyer(buyer);
        soldInformation.setSoldPrice( price);
    }

    public double getProposal() {
        return proposal;
    }

    public void setProposal(double proposal) {
        this.proposal = proposal;
    }

    /*
    public class SoldInformation implements Serializable {
        private AID buyer;
        private double soldPrice;


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
    }*/

}
