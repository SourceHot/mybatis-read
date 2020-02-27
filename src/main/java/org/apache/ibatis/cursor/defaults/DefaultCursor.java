/**
 * Copyright 2009-2017 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.cursor.defaults;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetWrapper;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * This is the default implementation of a MyBatis Cursor.
 * This implementation is not thread safe.
 *
 * @author Guillaume Darmont / guillaume@dropinocean.com
 */
public class DefaultCursor<T> implements Cursor<T> {

  // ResultSetHandler stuff


  /**
   * ResultSet 处理器
   */
  private final DefaultResultSetHandler resultSetHandler;

  /**
   * 结果映射
   */
  private final ResultMap resultMap;

  /**
   * ResultSet 包装对象
   */
  private final ResultSetWrapper rsw;

  /**
   * 分页的
   */
  private final RowBounds rowBounds;


  /**
   * 对象包装结果处理类
   */
  private final ObjectWrapperResultHandler<T> objectWrapperResultHandler = new ObjectWrapperResultHandler<T>();

  /**
   * 游标的迭代器
   */
  private final CursorIterator cursorIterator = new CursorIterator();

  /**
   * 游标开启判断
   */
  private boolean iteratorRetrieved;

  /**
   * 游标状态,默认是创建未使用
   */
  private CursorStatus status = CursorStatus.CREATED;

  /**
   * 分页索引,默认-1
   */
  private int indexWithRowBound = -1;

  public DefaultCursor(DefaultResultSetHandler resultSetHandler, ResultMap resultMap, ResultSetWrapper rsw, RowBounds rowBounds) {
    this.resultSetHandler = resultSetHandler;
    this.resultMap = resultMap;
    this.rsw = rsw;
    this.rowBounds = rowBounds;
  }

  @Override
  public boolean isOpen() {
    return status == CursorStatus.OPEN;
  }

  @Override
  public boolean isConsumed() {
    return status == CursorStatus.CONSUMED;
  }


  /**
   * 当前索引
   *
   * @return
   */
  @Override
  public int getCurrentIndex() {
    return rowBounds.getOffset() + cursorIterator.iteratorIndex;
  }

  /**
   * 迭代器获取
   *
   * @return
   */
  @Override
  public Iterator<T> iterator() {
    // 是否获取过
    if (iteratorRetrieved) {
      throw new IllegalStateException("Cannot open more than one iterator on a Cursor");
    }
    iteratorRetrieved = true;
    return cursorIterator;
  }

  @Override
  public void close() {
    // 判断是否关闭
    if (isClosed()) {
      return;
    }

    ResultSet rs = rsw.getResultSet();
    try {
      if (rs != null) {
        Statement statement = rs.getStatement();

        rs.close();
        if (statement != null) {
          statement.close();
        }
      }
      // 设置游标状态
      status = CursorStatus.CLOSED;
    }
    catch (SQLException e) {
      // ignore
    }
  }

  /**
   * 去到真正的数据行
   *
   * @return
   */
  protected T fetchNextUsingRowBound() {
    T result = fetchNextObjectFromDatabase();
    while (result != null && indexWithRowBound < rowBounds.getOffset()) {
      result = fetchNextObjectFromDatabase();
    }
    return result;
  }

  /**
   * 从数据库获取数据
   *
   * @return
   */
  protected T fetchNextObjectFromDatabase() {
    if (isClosed()) {
      return null;
    }

    try {
      // 游标状态设置
      status = CursorStatus.OPEN;
      // 处理数据结果放入，objectWrapperResultHandler
      resultSetHandler.handleRowValues(rsw, resultMap, objectWrapperResultHandler, RowBounds.DEFAULT, null);
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }

    // 获取处理结果
    T next = objectWrapperResultHandler.result;
    // 结果不为空
    if (next != null) {
      // 索引+1
      indexWithRowBound++;
    }
    // No more object or limit reached
    // 如果没有数据, 或者 当前读取条数= 偏移量+限额量
    if (next == null || getReadItemsCount() == rowBounds.getOffset() + rowBounds.getLimit()) {
      // 关闭游标
      close();
      // 游标状态设置
      status = CursorStatus.CONSUMED;
    }
    // 设置结果为null
    objectWrapperResultHandler.result = null;

    return next;
  }

  /**
   * 是否关闭状态判断
   *
   * @return
   */
  private boolean isClosed() {
    return status == CursorStatus.CLOSED || status == CursorStatus.CONSUMED;
  }

  /**
   * 下一个索引
   *
   * @return
   */
  private int getReadItemsCount() {
    return indexWithRowBound + 1;
  }

  /**
   * 游标的状态,枚举
   */
  private enum CursorStatus {

    /**
     * 新创建的游标, ResultSet 还没有使用过
     * A freshly created cursor, database ResultSet consuming has not started
     */
    CREATED,
    /**
     * 游标使用过, ResultSet 被使用
     * A cursor currently in use, database ResultSet consuming has started
     */
    OPEN,
    /**
     * 游标关闭, 可能没有被消费完全
     * A closed cursor, not fully consumed
     */
    CLOSED,
    /**
     * 游标彻底消费完毕, 关闭了
     * A fully consumed cursor, a consumed cursor is always closed
     */
    CONSUMED
  }

  /**
   * 对象处理结果的包装类
   *
   * @param <T>
   */
  private static class ObjectWrapperResultHandler<T> implements ResultHandler<T> {

    /**
     * 数据结果
     */
    private T result;

    /**
     * 从{@link ResultContext} 获取结果对象
     *
     * @param context
     */
    @Override
    public void handleResult(ResultContext<? extends T> context) {
      this.result = context.getResultObject();
      context.stop();
    }
  }

  /**
   * 游标迭代器
   */
  private class CursorIterator implements Iterator<T> {

    /**
     * 下一个数据
     * Holder for the next object to be returned
     */

    T object;

    /**
     * 下一个的索引
     * Index of objects returned using next(), and as such, visible to users.
     */
    int iteratorIndex = -1;

    /**
     * 是否有下一个值
     *
     * @return
     */
    @Override
    public boolean hasNext() {
      if (object == null) {
        object = fetchNextUsingRowBound();
      }
      return object != null;
    }

    /**
     * 下一个值
     *
     * @return
     */
    @Override
    public T next() {
      // Fill next with object fetched from hasNext()
      T next = object;

      if (next == null) {
        next = fetchNextUsingRowBound();
      }

      if (next != null) {
        object = null;
        iteratorIndex++;
        return next;
      }
      throw new NoSuchElementException();
    }

    /**
     * 不可执行抛出异常,拒绝使用这个方法
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove element from Cursor");
    }
  }
}
