package com.example.demo.repository;

import com.example.demo.entity.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

import static com.example.demo.entity.QMember.*;
import static com.example.demo.entity.QTeam.*;

@Repository
public class MemberJpaRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class,id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m",Member.class).getResultList();
    }

    public List<Member> findAll_dsl(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUserName(String userName){
        return em.createQuery("select m from Member m where m.username=:username",Member.class)
                .setParameter("username",userName).getResultList();
    }

    public List<Member> findByUserName_dsl(String userName){
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(userName))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition cond){
        BooleanBuilder builder = new BooleanBuilder();
        if(StringUtils.hasText(cond.getUsername())){
            builder.and(member.username.eq(cond.getUsername()));
        }

        if(StringUtils.hasText(cond.getTeamName())){
            builder.and(team.name.eq(cond.getTeamName()));
        }

        if(cond.getAgeGoe() != null){
            builder.and(member.age.goe(cond.getAgeGoe()));
        }

        if(cond.getAgeLoe() != null){
            builder.and(member.age.loe(cond.getAgeLoe()));
        }
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("member_id"),
                        member.username,
                        member.age,
                        team.id.as("team_id"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition){
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("member_id"),
                        member.username,
                        member.age,
                        team.id.as("team_id"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                        )
                .fetch();
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression userNameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe):null;
    }


}
