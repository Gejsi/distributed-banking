package eu.tsp.transactions.distributed;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;

import eu.tsp.transactions.Bank;
import eu.tsp.transactions.Account;
import eu.tsp.transactions.distributed.AccountSchemaBuilderImpl;

public class DistributedBank implements Bank {

  private DefaultCacheManager cacheManager;
  private Cache<Integer, Account> accounts;

  public DistributedBank() {
    GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
    gbuilder.transport().addProperty("configurationFile", "default-jgroups-google.xml");
    gbuilder.serialization().addContextInitializer(new AccountSchemaBuilderImpl());

    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.clustering().cacheMode(CacheMode.DIST_SYNC);

    builder.transaction()
        .transactionMode(TransactionMode.TRANSACTIONAL)
        .lockingMode(LockingMode.PESSIMISTIC);
    builder.locking().lockAcquisitionTimeout(15, TimeUnit.SECONDS);

    this.cacheManager = new DefaultCacheManager(gbuilder.build());
    this.cacheManager.defineConfiguration("accounts", builder.build());
    this.accounts = this.cacheManager.getCache("accounts");
  }

  @Override
  public void createAccount(int id) throws IllegalArgumentException {
    if (this.accounts.containsKey(id)) {
      throw new IllegalArgumentException("account already existing: " + id);
    }
    accounts.put(id, new Account(id, 0));
  }

  @Override
  public int getBalance(int id) throws IllegalArgumentException {
    if (!this.accounts.containsKey(id)) {
      throw new IllegalArgumentException("account not existing: " + id);
    }
    return accounts.get(id).getBalance();
  }

  @Override
  public void performTransfer(int from, int to, int amount) {
    TransactionManager tm = accounts.getAdvancedCache().getTransactionManager();
    try {
      tm.begin();

      if (!this.accounts.containsKey(from))
        throw new IllegalArgumentException("No account: " + from);
      if (!this.accounts.containsKey(to))
        throw new IllegalArgumentException("No account: " + to);

      // Always acquire locks in deterministic order (Smallest ID first)
      // In Pessimistic mode, the 'get()' call acquires the lock.
      Account firstLock, secondLock;
      if (from < to) {
        firstLock = accounts.get(from); // Lock smaller
        secondLock = accounts.get(to); // Lock larger
      } else {
        firstLock = accounts.get(to); // Lock smaller
        secondLock = accounts.get(from); // Lock larger
      }

      Account fromAccount = (from < to) ? firstLock : secondLock;
      Account toAccount = (from < to) ? secondLock : firstLock;

      fromAccount.setBalance(fromAccount.getBalance() - amount);
      toAccount.setBalance(toAccount.getBalance() + amount);

      accounts.put(from, fromAccount);
      accounts.put(to, toAccount);

      tm.commit();
    } catch (Exception e) {
      try {
        if (tm.getStatus() == javax.transaction.Status.STATUS_ACTIVE)
          tm.rollback();
      } catch (Exception rb) {
        rb.printStackTrace();
      }
      throw new RuntimeException("Transaction failed", e);
    }
  }

  @Override
  public void clear() {
    this.accounts.clear();
  }

  @Override
  public void open() {
  }

  @Override
  public void close() {
    this.cacheManager.stop();
  }
}
