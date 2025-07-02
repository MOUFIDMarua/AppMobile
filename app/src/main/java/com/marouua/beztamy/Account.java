package com.marouua.beztamy;

public class Account {
    private double balance;
    public Account() {} // Firestore
    public double getBalance(){ return balance; }
    public void setBalance(double b){ this.balance = b; }
}
