package io.github.gejsi.banking;

public interface Bank{
  void createAccount(int id) throws IllegalArgumentException;
  int getBalance(int id) throws IllegalArgumentException;
  void performTransfer(int from, int to, int amount) throws IllegalArgumentException;
  void clear();
  void open();
  void close();
}
