/**
 * Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.session;

/**
 * <p>SIMPLE: 简单执行器,直接将sql执行</p>
 * <p>REUSE: 可重用执行器 重用对象 Statement </p>
 * <p>BATCH: 批量执行器</p>
 * @author Clinton Begin
 */
public enum ExecutorType {
  SIMPLE, REUSE, BATCH
}
