package org.testng.internal;

import java.io.Serializable;

import org.testng.IInvokedMethod;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class InvokedMethod implements Serializable, IInvokedMethod {
  private static final long serialVersionUID = 2126127194102819222L;
  transient private Object m_instance;
  private ITestNGMethod m_testMethod;
  private long m_date = System.currentTimeMillis();
  private ITestResult m_testResult;

  public InvokedMethod(Object instance,
                       ITestNGMethod method,
                       long date,
                       ITestResult testResult) {
    m_instance = instance;
    m_testMethod = method;
    m_date = date;
    m_testResult = testResult;
  }

  /* (non-Javadoc)
   * @see org.testng.internal.IInvokedMethod#isTestMethod()
   */
  @Override
  public boolean isTestMethod() {
    return m_testMethod.isTest();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder().append(m_testMethod);
    for (Object p : m_testResult.getParameters()) {
      result.append(p).append(" ");
    }
    result.append(" ").append(m_instance != null ? m_instance.hashCode() : " <static>");

    return result.toString();
  }

  /* (non-Javadoc)
   * @see org.testng.internal.IInvokedMethod#isConfigurationMethod()
   */
  @Override
  public boolean isConfigurationMethod() {
    return TestNgMethodUtils.isConfigurationMethod(m_testMethod);
  }

  /* (non-Javadoc)
   * @see org.testng.internal.IInvokedMethod#getTestMethod()
   */
  @Override
  public ITestNGMethod getTestMethod() {
    return m_testMethod;
  }

  /* (non-Javadoc)
   * @see org.testng.internal.IInvokedMethod#getDate()
   */
  @Override
  public long getDate() {
    return m_date;
  }

  @Override
  public ITestResult getTestResult() {
    return m_testResult;
  }
}
