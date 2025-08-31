package com.example.demo;

import com.example.demo.entity.Hello;
import com.example.demo.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Transactional
class DemoApplicationTests {

    @PersistenceContext
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);

        QHello qHello = new QHello("h");
        Hello hello1 = query
                .selectFrom(qHello)
                .fetchOne();


        Assertions.assertThat(hello1).isEqualTo(hello);
    }

}
