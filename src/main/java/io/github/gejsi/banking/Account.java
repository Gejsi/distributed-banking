package io.github.gejsi.banking;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class Account {
  private int id;
  private int balance;

  @ProtoFactory
  public Account(int id, int balance) {
    this.id = id;
    this.balance = balance;
  }

  public Account() {
  }

  @ProtoField(number = 1, defaultValue = "0")
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @ProtoField(number = 2, defaultValue = "0")
  public int getBalance() {
    return balance;
  }

  public void setBalance(int balance) {
    this.balance = balance;
  }
}
