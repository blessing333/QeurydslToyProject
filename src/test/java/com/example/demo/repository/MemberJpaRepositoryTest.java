package com.example.demo.repository;

import com.example.demo.entity.Member;
import com.example.demo.entity.MemberSearchCondition;
import com.example.demo.entity.MemberTeamDto;
import com.example.demo.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional

class MemberJpaRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired MemberJpaRepository repository;
    @Test
    public void basicTest(){
        Member member = new Member("member1",10);
        repository.save(member);
        Member findMember = repository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result = repository.findAll();
        assertThat(result.size()).isEqualTo(1);

        List<Member> member2 = repository.findByUserName("member1");
        assertThat(member2).containsExactly(member);
    }
    @Test
    public void basicQueryDsl(){
        Member member = new Member("member1",10);
        repository.save(member);

        List<Member> result = repository.findAll_dsl();
        assertThat(result.size()).isEqualTo(1);

        List<Member> member2 = repository.findByUserName_dsl("member1");
        assertThat(member2).containsExactly(member);
    }

    @Test
    public void searchTest(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = repository.search(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }
}