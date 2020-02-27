package com.sourcehot.ibatis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.sourcehot.ibatis.entity.Person;
import com.sourcehot.ibatis.mapper.PersonMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class Main {
    public static void main(String[] args) {
//        String resource = "mybatis-config-demo.xml";
        String resource = "mybatis-config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        SqlSessionFactory sqlSessionFactory = null;
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = null;
        try {
            sqlSession = sqlSessionFactory.openSession();
            PersonMapper roleMapper = sqlSession.getMapper(PersonMapper.class);
            Person p = new Person();
            p.setName("zs");
            p.setAge(11);
            p.setPhone("123");
            p.setEmail("123@qq.com");
            p.setAddress(new Date().toString());

            roleMapper.ins(p);


            sqlSession.commit();

        }
        catch (Exception e) {
            sqlSession.rollback();
            e.printStackTrace();
        }
        finally {
            sqlSession.close();
        }
    }
}