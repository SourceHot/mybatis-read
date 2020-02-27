package com.sourcehot.ibatis.mapper;

import java.util.List;

import com.sourcehot.ibatis.entity.Person;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PersonMapper {
    int ins(Person person);

    List<Person> list(@Param("iid") Integer id);

}
