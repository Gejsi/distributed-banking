package io.github.gejsi.banking.base;

import io.github.gejsi.banking.Bank;
import io.github.gejsi.banking.BankFactory;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

public class BaseBankTest {
  private static final int MAX_ACCOUNTS = 1000;

  private Bank bank;

  @Before
  public void setup() {
    BankFactory factory = new BankFactory();
    this.bank = factory.createBaseBank();
    for (int i = 0; i < MAX_ACCOUNTS; i++) {
      bank.createAccount(i);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void getBalanceWrongAccount() {
    bank.getBalance(-1);
  }

  @Test
  public void simpleTransfer() {
    bank.performTransfer(1, 2, 100);
    // Accounts start at 0.
    // After the transfer, Account 1 should be -100, Account 2 should be +100.
    assertEquals(bank.getBalance(1), -100);
    assertEquals(bank.getBalance(2), 100);
    assertEquals(bank.getBalance(1) + bank.getBalance(2), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createExistingAccount() {
    // Account 1 is already created in setup()
    bank.createAccount(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void transferFromUnknownAccount() {
    // Account -1 does not exist
    bank.performTransfer(-1, 2, 100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void transferToUnknownAccount() {
    // Account -1 does not exist
    bank.performTransfer(1, -1, 100);
  }
}
