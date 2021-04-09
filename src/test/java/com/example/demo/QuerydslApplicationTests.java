package com.example.demo;

import com.example.demo.entity.Hello;
import com.example.demo.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Commit
class QuerydslApplicationTests {
    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QHello qHello = new QHello("h");

        Hello hello1 = queryFactory.selectFrom(qHello).fetchOne();
        assertThat(hello1).isEqualTo(hello);
        assertThat(hello1.getId()).isEqualTo(hello.getId());
    }

}
