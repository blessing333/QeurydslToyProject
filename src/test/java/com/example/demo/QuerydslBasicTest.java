package com.example.demo;

import com.example.demo.entity.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryFactory;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.h2.engine.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static com.example.demo.entity.QMember.*;
import static com.example.demo.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;
    @BeforeEach
    public void before() {
         queryFactory = new JPAQueryFactory(em);
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
    }
    @Test
    public void startJPQL(){
        //member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        //member1을 찾아라
        Member findMember = queryFactory.selectFrom(member).where(member.username.eq("member1")).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1")
                        ,(member.age.eq(10))
                )
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .fetchResults();
        memberQueryResults.getTotal();
        memberQueryResults.getResults();
        queryFactory.selectFrom(member).fetchCount();
    }

    @Test
    public void order(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.min(),
                        member.age.max(),
                        member.age.avg()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    @Test
    public void group(){
        List<Tuple> result = queryFactory.select(
                team.name,
                member.age.avg()
        )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA=result.get(0);
        Tuple teamB=result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }
    @Test
    public void join(){
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");
    }
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username").containsExactly("teamA","teamB");
    }
    @Test
    public void join_on(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team,team)
                .on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);

        }
    }

    @Test
    public void theta_join_on(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        List<Tuple> result = queryFactory
                .select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();
        assertThat(result).extracting("age").containsExactly(40);
    }

    @Test
    @DisplayName("나이가 평균 이상인 회원")
    public void subQuery2(){
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();
        assertThat(result).extracting("age").containsExactly(30,40);
    }

    @Test
    public void selectSubQuery(){
        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(member.age.avg())
                                .from(member)
                )
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
    }
    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            tuple.get(member.username);
            tuple.get(member.age);
        }
    }
    @Test
    public void findDtoByJPQL(){
        List<MemberDTO> result = em.createQuery("select new com.example.demo.entity.MemberDTO(m.username,m.age) from Member m", MemberDTO.class)
                .getResultList();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDTOBySetter(){
        List<MemberDTO> result = queryFactory
                .select(Projections.bean(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }
    @Test
    public void findDTOByField(){
        List<MemberDTO> result = queryFactory
                .select(Projections.fields(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDTOByConstructor(){
        List<MemberDTO> result = queryFactory
                .select(Projections.constructor(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findUserDTO(){
        QMember memberSub = new QMember("memberSub");
        List<UserDTO> result = queryFactory
                .select(Projections.fields(UserDTO.class,
                        QMember.member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                        .select(memberSub.age.max())
                                .from(memberSub),"age")
                        )
                )
                .from(QMember.member)
                .fetch();

        for (UserDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findUserDTOByConstructor(){
        List<UserDTO> result = queryFactory
                .select(Projections.constructor(UserDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDTO> result = queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void dynamicQueryBooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = searchMember1(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQueryWhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(
                        allEq(usernameCond,ageCond)
                        //usernameEq(usernameCond), ageEq(ageCond)
                )
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if(usernameCond == null)
            return null;
        else
            return member.username.eq(usernameCond);
    }

    private BooleanExpression allEq(String usernameCond,Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

}
