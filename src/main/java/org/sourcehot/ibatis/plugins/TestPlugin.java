package org.sourcehot.ibatis.plugins;

import java.util.Properties;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;

//@Intercepts({
//        @Signature(
//                type = Executor.class,
//                method = "query",
//                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
//        )
//})
public class TestPlugin implements Interceptor {
  @Override
  public Object intercept(Invocation invocation) throws Throwable {

    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    System.out.println(properties);
  }

  /**
   * 为测试this.configuration.getInterceptors()使用
   *
   * @return
   */
  public String hello() {
    return "hello-mybatis-plugins";
  }
}
