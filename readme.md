# 1 Mybatis动态sql

## 1.1 动态sql语句概述

Mybatis 的映射文件中，前面我们的 SQL 都是比较简单的，有些时候业务逻辑复杂时，我们的 SQL是动态变化的，此时在前面的学习中我们的 SQL 就不能满足要求了。

## 1.2 动态 SQL之if

我们根据实体类的不同取值，使用不同的 SQL语句来进行查询。比如在 id如果不为空时可以根据id查询，如果username 不同空时还要加入用户名作为条件。这种情况在我们的多条件组合查询中经常会碰到。

    <select id="findByCondition" parameterType="user" resultType="user">
    	select * from User
    	<where>
    		<if test="id!=0">
    			and id=#{id}
    		</if>
    		<if test="username!=null">
    			and username=#{username}
    		</if>
    	</where>
    </select>
    

当查询条件id和username都存在时，控制台打印的sql语句如下：

    select * from user WHERE id = ? and username = ? 

当查询条件只有id存在时，控制台打印的sql语句如下：

    select * from user WHERE id = ? 

## 1.3 动态 SQL  之foreach

循环执行sql的拼接操作，例如：SELECT * FROM USER WHERE id IN (1,2,5)。

    <select id="findByIds" parameterType="list" resultType="user">
    	select * from User
        <where>        
        	<foreach collection="array" open="id in(" close=")" item="id" separator=",">
            	#{id}
            </foreach>
        </where>
    </select>
    

测试片段：

    ArrayList<Integer> integers = new ArrayList<Integer>();
    integers.add(1);
    integers.add(2);
    List<User> byIds = mapper.findByIds(integers);
    System.out.println(byIds);

控制台打印的sql语句如下：

    select * from user WHERE id in ( ? , ? ) 

## 1.4 SQL片段抽取

        <!--sql语句抽取-->
        <sql id="selectUser">select * from user</sql>
    
        <select id="findByCondition" parameterType="user" resultType="user">
            <include refid="selectUser"></include>
            <where>
                <if test="id!=0">
                    and id=#{id}
                </if>
                <if test="username!=null">
                    and username=#{username}
                </if>
                <if test="password!=null">
                    and password=#{password}
                </if>
            </where>
        </select>
    
        <select id="findByIds" parameterType="list" resultType="user">
            <include refid="selectUser"></include>
            <where>
                <foreach collection="list" open="id in(" close=")" item="id" separator=",">
                    #{id}
                </foreach>
            </where>
        </select>

复用select * from user


# 2 Mybatis 类型处理器：typeHandlers
## 2.1 typeHandlers标签

无论是 MyBatis 在预处理语句（PreparedStatement）中设置一个参数时，还是从结果集中取出一个值时， 都会用类型处理器将获取的值以合适的方式转换成 Java 类型。

你可以重写类型处理器或创建你自己的类型处理器来处理不支持的或非标准的类型。具体做法为：实现 org.apache.ibatis.type.TypeHandler 接口， 或继承一个很便利的类 org.apache.ibatis.type.BaseTypeHandler， 然后可以选择性地将它映射到一个JDBC类型。例如需求：一个Java中的Date数据类型，我想将之存到数据库的时候存成一个1970年至今的毫秒数，取出来时转换成java的Date，即java的Date与数据库的varchar毫秒值之间转换。


**简单来说就是，表中的birthday是long(bigint)类型的毫秒值，javabean中的birthday是Date类型，需要自定义类型处理器，实现sql<--->java不同类型之间的转换**


开发步骤：

1. 定义转换类继承类BaseTypeHandler<T>

2. 覆盖4个未实现的方法，其中setNonNullParameter为java程序设置数据到数据库的回调方法，getNullableResult为查询时 mysql的字符串类型转换成 java的Type类型的方法

3. 在MyBatis核心配置文件中进行注册

4. 测试转换是否正确





1.定义转换类继承类BaseTypeHandler<T>
2.覆盖4个未实现的方法，其中setNonNullParameter为java程序设置数据到数据库的回调方法，getNullableResult为查询时 mysql的字符串类型转换成 java的Type类型的方法

```java
package com.zp.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class DateTypeHandler extends BaseTypeHandler<Date> {
    //将java类型 转换成 数据库需要的类型
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Date date, JdbcType jdbcType) throws SQLException {
        preparedStatement.setLong(i,date.getTime());
    }

    //将数据库中类型 转换成java类型
    //String参数  要转换的字段名称
    //ResultSet 查询出的结果集
    public Date getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return new Date(resultSet.getLong(s));
    }

    //将数据库中类型 转换成java类型
    public Date getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return new Date(resultSet.getLong(i));
    }

    //将数据库中类型 转换成java类型
    public Date getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return callableStatement.getDate(i);
    }
}

```




3.在MyBatis核心配置文件中进行注册
```xml
    <!--注册类型自定义转换器-->
    <typeHandlers>
        <typeHandler handler="com.zp.handler.DateTypeHandler"></typeHandler>
    </typeHandlers>
```

4.测试转换是否正确
```java
    @Test
    public void test2() throws IOException {
        InputStream resourceAsStream = Resources.getResourceAsStream("sqlMapConfig.xml");
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);
        SqlSession sqlSession = sessionFactory.openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User user = new User();
        user.setUsername("ccc");
        user.setPassword("123");
        user.setBirthday(new Date());
        mapper.save(user);


        List<User> all = mapper.findAll(null);
        System.out.println(all);

        sqlSession.commit();

    }
```

数据库中birthd：1677899559823
查询出来的结果：birthday=Sat Mar 04 11:13:35 CST 2023}


