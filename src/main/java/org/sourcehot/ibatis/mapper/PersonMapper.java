package org.sourcehot.ibatis.mapper;

import java.util.List;

import org.sourcehot.ibatis.entity.Person;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PersonMapper {
  int ins(Person person);

  List<Person> list(@Param("iid") Integer id);

}
