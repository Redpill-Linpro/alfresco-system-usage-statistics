package org.redpill.alfresco.repo.statistics.jobs;

import org.alfresco.repo.admin.RepositoryState;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VmShutdownListener;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.InitializingBean;

public abstract class ClusteredExecuter implements Job, InitializingBean {

  private static Logger LOG = Logger.getLogger(ClusteredExecuter.class);
  protected static long DEFAULT_LOCK_TTL = 30000L;

  public final String XPATH_DATA_DICTIONARY = "/app:company_home/app:dictionary";
  public final String FOLDER_NAME_REDPILL_LINPRO = "Redpill-Linpro";
  public final String FOLDER_NAME_STATISTICS = "Statistics";

  protected ThreadLocal<String> lockThreadLocal = new ThreadLocal<String>();
  protected JobLockService jobLockService;
  protected long lockTTL = DEFAULT_LOCK_TTL;
  protected TransactionService transactionService;
  protected RepositoryState repositoryState;

  public void setJobLockService(JobLockService jobLockService) {
    this.jobLockService = jobLockService;
  }

  public void setLockTTL(long lockTTL) {
    this.lockTTL = lockTTL;
  }

  public void setTransactionService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  public void setRepositoryState(RepositoryState repositoryState) {
    this.repositoryState = repositoryState;
  }

  @Override
  public void execute(JobExecutionContext jobCtx) throws JobExecutionException {
    // Bypass if the system is in read-only mode
    if (transactionService.isReadOnly()) {
      LOG.debug(getJobName() + " bypassed; the system is read-only.");
      return;
    }

    // Bypass if the system is bootstrapping
    if (repositoryState != null && repositoryState.isBootstrapping()) {
      LOG.debug(getJobName() + " bypassed; the system is bootstrapping.");
      return;
    }

    // bypass if there already exists a lock
    if (hasLock()) {
      LOG.debug(getJobName() + " bypassed; a lock already exists.");
      return;
    }

    try {
      createLock();
      LOG.debug(getJobName() + " started.");
      refreshLock();
      executeInternal();
      // Done
      if (LOG.isDebugEnabled()) {
        LOG.debug("   " + getJobName() + " completed.");
      }
    } catch (LockAcquisitionException e) {
      // Job being done by another process
      if (LOG.isDebugEnabled()) {
        LOG.debug("   " + getJobName() + " already underway.");
      }
    } catch (VmShutdownListener.VmShutdownException e) {
      // Aborted
      if (LOG.isDebugEnabled()) {
        LOG.debug("   " + getJobName() + " aborted.");
      }
    } finally {
      try {
        releaseLock();
      } catch (Exception ex) {
        // just swallow the exception here...
      }
    }
  }

  /**
   * Attempts to get the lock. If it fails, the current transaction is marked
   * for rollback.
   *
   */
  protected void refreshLock() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Refreshing lock");
    }
    String lockToken = lockThreadLocal.get();

    if (isBlank(lockToken)) {
      throw new IllegalArgumentException("Must provide existing lockToken");
    }

    jobLockService.refreshLock(lockToken, getLockQName(), lockTTL);
  }

  protected void releaseLock() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Releasing lock");
    }
    String lockToken = lockThreadLocal.get();

    if (isBlank(lockToken)) {
      throw new IllegalArgumentException("Must provide existing lockToken");
    }

    jobLockService.releaseLock(lockToken, getLockQName());

    lockThreadLocal.remove();
  }

  /**
   * Method for checking whether a lock exists or not. There are currently no
   * good ways of doing this, so a try...catch procedure is used.
   *
   * @return true if lock exists
   */
  protected boolean hasLock() {
    boolean hasLock = false;

    String lockToken = lockThreadLocal.get();

    if (isBlank(lockToken)) {
      return false;
    }

    RetryingTransactionCallback<String> txnWork = new RetryingTransactionCallback<String>() {

      @Override
      public String execute() throws Exception {
        return jobLockService.getLock(getLockQName(), lockTTL);
      }

    };

    try {
      lockToken = transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false, true);
    } catch (Exception ex) {
      hasLock = true;
    } finally {
      // silently release the lock, ignoring all errors
      try {
        jobLockService.releaseLock(lockToken, getLockQName());
      } catch (Exception ex) {
      }
    }

    return hasLock;
  }

  protected String createLock() {
    String lockToken = lockThreadLocal.get();

    if (isBlank(lockToken)) {
      RetryingTransactionCallback<String> txnWork = new RetryingTransactionCallback<String>() {

        @Override
        public String execute() throws Exception {
          return jobLockService.getLock(getLockQName(), lockTTL);
        }

      };

      lockToken = transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false, true);

      lockThreadLocal.set(lockToken);
    }

    return lockToken;
  }

  protected abstract String getJobName();

  protected abstract void executeInternal();

  protected QName getLockQName() {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Lock QName is: " + getClass().getSimpleName());
    }

    return QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, getClass().getSimpleName());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    PropertyCheck.mandatory(this, "jobLockService", jobLockService);
    PropertyCheck.mandatory(this, "transactionService", transactionService);
  }

  /**
   * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/StringUtils.java
   *
   * @param cs
   * @return
   */
  boolean isBlank(CharSequence cs) {
    final int strLen = length(cs);
    if (strLen == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/StringUtils.java
   *
   * @param cs
   * @return
   */
  public static int length(final CharSequence cs) {
    return cs == null ? 0 : cs.length();
  }
}
