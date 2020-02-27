/**
 * Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * @author Iwao AVE!
 */
public class TypeParameterResolver {

  private TypeParameterResolver() {
    super();
  }

  /**
   * @return The field type as {@link Type}. If it has type parameters in the declaration,<br>
   * they will be resolved to the actual runtime {@link Type}s.
   */
  public static Type resolveFieldType(Field field, Type srcType) {
    //获取需要解析的字段
    Type fieldType = field.getGenericType();
    //获取字段类型
    Class<?> declaringClass = field.getDeclaringClass();
    // 解析类型
    return resolveType(fieldType, srcType, declaringClass);
  }

  /**
   * @return The return type of the method as {@link Type}. If it has type parameters in the declaration,<br>
   * they will be resolved to the actual runtime {@link Type}s.
   */
  public static Type resolveReturnType(Method method, Type srcType) {
    // 获取返回值类型
    Type returnType = method.getGenericReturnType();
    // 获取方法的类型
    Class<?> declaringClass = method.getDeclaringClass();
    return resolveType(returnType, srcType, declaringClass);
  }

  /**
   * @return The parameter types of the method as an array of {@link Type}s. If they have type parameters in the declaration,<br>
   * they will be resolved to the actual runtime {@link Type}s.
   */
  public static Type[] resolveParamTypes(Method method, Type srcType) {
    // 获取方法参数类型
    Type[] paramTypes = method.getGenericParameterTypes();
    // 方法定义类型
    Class<?> declaringClass = method.getDeclaringClass();
    Type[] result = new Type[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      result[i] = resolveType(paramTypes[i], srcType, declaringClass);
    }
    return result;
  }

  private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
    if (type instanceof TypeVariable) {
      return resolveTypeVar((TypeVariable<?>) type, srcType, declaringClass);
    }
    else if (type instanceof ParameterizedType) {
      return resolveParameterizedType((ParameterizedType) type, srcType, declaringClass);
    }
    else if (type instanceof GenericArrayType) {
      return resolveGenericArrayType((GenericArrayType) type, srcType, declaringClass);
    }
    else {
      return type;
    }
  }

  private static Type resolveGenericArrayType(GenericArrayType genericArrayType, Type srcType, Class<?> declaringClass) {
    Type componentType = genericArrayType.getGenericComponentType();
    Type resolvedComponentType = null;
    if (componentType instanceof TypeVariable) {
      // TypeVariable 类型解析
      resolvedComponentType = resolveTypeVar((TypeVariable<?>) componentType, srcType, declaringClass);
    }
    else if (componentType instanceof GenericArrayType) {
      // GenericArrayType 类型解析
      resolvedComponentType = resolveGenericArrayType((GenericArrayType) componentType, srcType, declaringClass);
    }
    else if (componentType instanceof ParameterizedType) {
      // ParameterizedType 类型解析
      resolvedComponentType = resolveParameterizedType((ParameterizedType) componentType, srcType, declaringClass);
    }
    if (resolvedComponentType instanceof Class) {
      // class 类型解析
      return Array.newInstance((Class<?>) resolvedComponentType, 0).getClass();
    }
    else {
      // 直接返回
      return new GenericArrayTypeImpl(resolvedComponentType);
    }
  }

  /**
   *  解析 ParameterizedType
   * @param parameterizedType
   * @param srcType
   * @param declaringClass
   * @return
   */
  private static ParameterizedType resolveParameterizedType(ParameterizedType parameterizedType, Type srcType, Class<?> declaringClass) {
    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
    Type[] typeArgs = parameterizedType.getActualTypeArguments();
    Type[] args = new Type[typeArgs.length];
    for (int i = 0; i < typeArgs.length; i++) {
      if (typeArgs[i] instanceof TypeVariable) {
        args[i] = resolveTypeVar((TypeVariable<?>) typeArgs[i], srcType, declaringClass);
      }
      else if (typeArgs[i] instanceof ParameterizedType) {
        args[i] = resolveParameterizedType((ParameterizedType) typeArgs[i], srcType, declaringClass);
      }
      else if (typeArgs[i] instanceof WildcardType) {
        args[i] = resolveWildcardType((WildcardType) typeArgs[i], srcType, declaringClass);
      }
      else {
        args[i] = typeArgs[i];
      }
    }
    return new ParameterizedTypeImpl(rawType, null, args);
  }

  private static Type resolveWildcardType(WildcardType wildcardType, Type srcType, Class<?> declaringClass) {
    Type[] lowerBounds = resolveWildcardTypeBounds(wildcardType.getLowerBounds(), srcType, declaringClass);
    Type[] upperBounds = resolveWildcardTypeBounds(wildcardType.getUpperBounds(), srcType, declaringClass);
    return new WildcardTypeImpl(lowerBounds, upperBounds);
  }

  private static Type[] resolveWildcardTypeBounds(Type[] bounds, Type srcType, Class<?> declaringClass) {
    Type[] result = new Type[bounds.length];
    for (int i = 0; i < bounds.length; i++) {
      if (bounds[i] instanceof TypeVariable) {
        result[i] = resolveTypeVar((TypeVariable<?>) bounds[i], srcType, declaringClass);
      }
      else if (bounds[i] instanceof ParameterizedType) {
        result[i] = resolveParameterizedType((ParameterizedType) bounds[i], srcType, declaringClass);
      }
      else if (bounds[i] instanceof WildcardType) {
        result[i] = resolveWildcardType((WildcardType) bounds[i], srcType, declaringClass);
      }
      else {
        result[i] = bounds[i];
      }
    }
    return result;
  }

  private static Type resolveTypeVar(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass) {
    Type result = null;
    Class<?> clazz = null;
    if (srcType instanceof Class) {
      clazz = (Class<?>) srcType;
    }
    else if (srcType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) srcType;
      clazz = (Class<?>) parameterizedType.getRawType();
    }
    else {
      throw new IllegalArgumentException("The 2nd arg must be Class or ParameterizedType, but was: " + srcType.getClass());
    }

    if (clazz == declaringClass) {
      Type[] bounds = typeVar.getBounds();
      if (bounds.length > 0) {
        return bounds[0];
      }
      return Object.class;
    }
    /**
     * 获取父类类型，如果父类是泛型，则这里返回的是参数化类型ParameterizedType
     */
    Type superclass = clazz.getGenericSuperclass();
    result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
    if (result != null) {
      return result;
    }

    /**
     * 获取实现的接口
     */
    Type[] superInterfaces = clazz.getGenericInterfaces();
    for (Type superInterface : superInterfaces) {
      result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superInterface);
      if (result != null) {
        return result;
      }
    }
    return Object.class;
  }

  /**
   *  扫描 上级类型
   * @param typeVar
   * @param srcType
   * @param declaringClass
   * @param clazz
   * @param superclass
   * @return
   */
  private static Type scanSuperTypes(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass, Class<?> clazz, Type superclass) {
    Type result = null;
    if (superclass instanceof ParameterizedType) {
      ParameterizedType parentAsType = (ParameterizedType) superclass;
      Class<?> parentAsClass = (Class<?>) parentAsType.getRawType();
      if (declaringClass == parentAsClass) {
        Type[] typeArgs = parentAsType.getActualTypeArguments();
        TypeVariable<?>[] declaredTypeVars = declaringClass.getTypeParameters();
        for (int i = 0; i < declaredTypeVars.length; i++) {
          if (declaredTypeVars[i] == typeVar) {
            if (typeArgs[i] instanceof TypeVariable) {
              TypeVariable<?>[] typeParams = clazz.getTypeParameters();
              for (int j = 0; j < typeParams.length; j++) {
                if (typeParams[j] == typeArgs[i]) {
                  if (srcType instanceof ParameterizedType) {
                    result = ((ParameterizedType) srcType).getActualTypeArguments()[j];
                  }
                  break;
                }
              }
            }
            else {
              result = typeArgs[i];
            }
          }
        }
      }
      else if (declaringClass.isAssignableFrom(parentAsClass)) {
        result = resolveTypeVar(typeVar, parentAsType, declaringClass);
      }
    }
    else if (superclass instanceof Class) {
      if (declaringClass.isAssignableFrom((Class<?>) superclass)) {
        result = resolveTypeVar(typeVar, superclass, declaringClass);
      }
    }
    return result;
  }

  static class ParameterizedTypeImpl implements ParameterizedType {
    private Class<?> rawType;

    private Type ownerType;

    private Type[] actualTypeArguments;

    public ParameterizedTypeImpl(Class<?> rawType, Type ownerType, Type[] actualTypeArguments) {
      super();
      this.rawType = rawType;
      this.ownerType = ownerType;
      this.actualTypeArguments = actualTypeArguments;
    }

    /**
     * 获取参数列表的泛型
     * @return
     */
    @Override
    public Type[] getActualTypeArguments() {
      return actualTypeArguments;
    }

    @Override
    public Type getOwnerType() {
      return ownerType;
    }

    /**
     * 获取<>前面的类型，即基本类型
     * @return
     */
    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public String toString() {
      return "ParameterizedTypeImpl [rawType=" + rawType + ", ownerType=" + ownerType + ", actualTypeArguments=" + Arrays.toString(actualTypeArguments) + "]";
    }
  }

  static class WildcardTypeImpl implements WildcardType {
    /**
     *  数据类型下限 super
     */
    private Type[] lowerBounds;

    /**
     * 数据类型上限  extend
     */
    private Type[] upperBounds;

    private WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
      super();
      this.lowerBounds = lowerBounds;
      this.upperBounds = upperBounds;
    }

    @Override
    public Type[] getLowerBounds() {
      return lowerBounds;
    }

    @Override
    public Type[] getUpperBounds() {
      return upperBounds;
    }
  }

  static class GenericArrayTypeImpl implements GenericArrayType {
    private Type genericComponentType;

    private GenericArrayTypeImpl(Type genericComponentType) {
      super();
      this.genericComponentType = genericComponentType;
    }

    @Override
    public Type getGenericComponentType() {
      return genericComponentType;
    }
  }
}
