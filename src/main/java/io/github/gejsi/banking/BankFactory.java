package io.github.gejsi.banking;

import io.github.gejsi.banking.base.BaseBank;
import io.github.gejsi.banking.distributed.DistributedBank;

public class BankFactory {
  public Bank createBank() {
    return createDistributedBank();
  }

  public Bank createBaseBank() {
    return new BaseBank();
  }

  public Bank createDistributedBank() {
    return new DistributedBank();
  }
}
