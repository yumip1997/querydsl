package com.example.demo.repository;

import com.example.demo.dto.MemberSearchCondition;
import com.example.demo.dto.MemberTeamDto;
import com.example.demo.entity.Member;
import com.example.demo.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest(){
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        Optional<Member> findMember = memberJpaRepository.findById(member1.getId());
        findMember.ifPresent(member ->
                assertThat(member.getId()).isEqualTo(member1.getId()));

        List<Member> all = memberJpaRepository.findAll();
        assertThat(all).containsExactly(member1);
    }

    @Test
    public void basicTest2(){
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        Optional<Member> findMember = memberJpaRepository.findById(member1.getId());
        findMember.ifPresent(member ->
                assertThat(member.getId()).isEqualTo(member1.getId()));

        List<Member> all = memberJpaRepository.findAllV2();
        assertThat(all).containsExactly(member1);
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

        List<MemberTeamDto> results = memberJpaRepository.searchByBuilder(condition);
        assertThat(results).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchTest2(){
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
        condition.setTeamName("teamB");

        List<MemberTeamDto> results = memberJpaRepository.searchByBuilder(condition);
        assertThat(results).extracting("username").containsExactly("member3","member4");
    }

    @Test
    public void searchTestByDynamicWhere(){
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

        List<MemberTeamDto> results = memberJpaRepository.search(condition);
        assertThat(results).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchTestByDynamicWhere2(){
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
        condition.setTeamName("teamB");

        List<MemberTeamDto> results = memberJpaRepository.search(condition);
        assertThat(results).extracting("username").containsExactly("member3","member4");
    }

}