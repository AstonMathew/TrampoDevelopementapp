package com.trampo.process.domain;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class Customer {

  private long id;
  private String email;
  private String firstname;
  private String surename;
  private double balance;
  private boolean acceptsMarketing;
  private int ordersCount;
  private boolean state;
  private double totalSpent;
  private boolean verifiedEmail;
  private String phone;
  private ZonedDateTime shopifyCreatedDate;
  private ZonedDateTime shopifyUpdatedDate;
  private LocalDateTime createdDate;
  private LocalDateTime updatedDate;
  
  public long getId() {
    return id;
  }
  public void setId(long id) {
    this.id = id;
  }
  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }
  public String getFirstname() {
    return firstname;
  }
  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }
  public String getSurename() {
    return surename;
  }
  public void setSurename(String surename) {
    this.surename = surename;
  }
  public double getBalance() {
    return balance;
  }
  public void setBalance(double balance) {
    this.balance = balance;
  }
  public boolean isAcceptsMarketing() {
    return acceptsMarketing;
  }
  public void setAcceptsMarketing(boolean acceptsMarketing) {
    this.acceptsMarketing = acceptsMarketing;
  }
  public int getOrdersCount() {
    return ordersCount;
  }
  public void setOrdersCount(int ordersCount) {
    this.ordersCount = ordersCount;
  }
  public boolean isState() {
    return state;
  }
  public void setState(boolean state) {
    this.state = state;
  }
  public double getTotalSpent() {
    return totalSpent;
  }
  public void setTotalSpent(double totalSpent) {
    this.totalSpent = totalSpent;
  }
  public boolean isVerifiedEmail() {
    return verifiedEmail;
  }
  public void setVerifiedEmail(boolean verifiedEmail) {
    this.verifiedEmail = verifiedEmail;
  }
  public String getPhone() {
    return phone;
  }
  public void setPhone(String phone) {
    this.phone = phone;
  }
  public ZonedDateTime getShopifyCreatedDate() {
    return shopifyCreatedDate;
  }
  public void setShopifyCreatedDate(ZonedDateTime shopifyCreatedDate) {
    this.shopifyCreatedDate = shopifyCreatedDate;
  }
  public ZonedDateTime getShopifyUpdatedDate() {
    return shopifyUpdatedDate;
  }
  public void setShopifyUpdatedDate(ZonedDateTime shopifyUpdatedDate) {
    this.shopifyUpdatedDate = shopifyUpdatedDate;
  }
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }
  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }
  public LocalDateTime getUpdatedDate() {
    return updatedDate;
  }
  public void setUpdatedDate(LocalDateTime updatedDate) {
    this.updatedDate = updatedDate;
  }
}
