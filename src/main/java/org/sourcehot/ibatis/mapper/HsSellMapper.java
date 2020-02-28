package org.sourcehot.ibatis.mapper;

import java.util.List;

import org.sourcehot.ibatis.entity.HsSell;
import org.apache.ibatis.annotations.Param;

public interface HsSellMapper {
  int deleteByPrimaryKey(Integer id);

  int insert(HsSell record);

  int insertSelective(HsSell record);

  HsSell selectByPrimaryKey(Integer id);

  int updateByPrimaryKeySelective(HsSell record);

  int updateByPrimaryKey(HsSell record);

  List<HsSell> list(@Param("ID") Integer id);

}