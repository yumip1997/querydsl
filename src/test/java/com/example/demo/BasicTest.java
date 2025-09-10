package com.example.demo;

import com.example.demo.dto.MemberDto;
import com.example.demo.dto.QMemberDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.Member;
import com.example.demo.entity.QMember;
import com.example.demo.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.example.demo.entity.QMember.member;
import static com.example.demo.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class BasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory query;

    @BeforeEach
    public void before(){
        query = new JPAQueryFactory(em);

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
        String query = "select m " +
                "from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(query, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDSL(){
        Member findMember = query
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member member1 = query.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
        assertThat(member1.getAge()).isEqualTo(10);
    }

    @Test
    public void searchAndParam(){
        Member member1 = query.selectFrom(member)
                .where(member.username.eq("member1")
                        , (member.age.eq(10)))  // and와 똑같음
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
        assertThat(member1.getAge()).isEqualTo(10);
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        // 결과가 둘 이상이면 NonUniqueResultException이 터짐
        assertThatThrownBy(() -> query.selectFrom(member)
                .fetchOne())
                .isInstanceOf(NonUniqueResultException.class);


        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();
    }

    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> fetch = query.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(fetch.get(0).getUsername()).isEqualTo("member5");
        assertThat(fetch.get(1).getUsername()).isEqualTo("member6");
        assertThat(fetch.get(2).getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> fetch = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        List<Member> fetch = query.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        Long total = query
                .select(member.count())
                .from(member)
                .fetchOne();

        assertThat(fetch.size()).isEqualTo(2);
        assertThat(total).isEqualTo(4L);
    }

    @Test
    public void aggregation(){
        // 실무에서는 튜플보다 dto로 조회
        Tuple tuple = query.select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetchOne();


        assertThat(tuple.get(member.count())).isEqualTo(4L);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void group(){
        List<Tuple> fetch = query.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> fetch1 = query.selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();


        assertThat(fetch1).extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    public void join_on_filtering(){
        List<Tuple> fetch = query.select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple);
        }
    }

    /**
     * 서로 관련없는 필드 기준으로 조인하기
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA", 100));
        em.persist(new Member("teamB", 100));

        List<Tuple> fetch = query.select(member, team)
                .from(member)
                .join(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple);
        }
    }

    @Test
    public void joinUse_문제발생(){
        em.flush();
        em.clear();

        // N+1 문제 발생하는 코드
        List<Member> memberList = query.selectFrom(member)
                .fetch();

        // 각 Member마다 Team을 조회하는 쿼리가 추가로 실행됨 (N+1)
        for (Member findMember : memberList) {
            System.out.println(findMember.getTeam().getName()); // 여기서 추가 쿼리 발생
        }
    }

    @Test
    public void fetchJoin_올바르지못한해결방법(){
        em.flush();
        em.clear();

        // Member를 조회하면서 Team도 함께 조인하는데 그냥 조인 (-> 연관된 엔티티 정보를 가져오지 못함)
        // 실행되는 SQL: SELECT m.* FROM Member m INNER JOIN Team t ON m.team_id = t.id
        List<Member> memberList = query.selectFrom(member)
                .join(member.team, team)
                .fetch();

        for (Member findMember : memberList) {
            System.out.println(findMember.getTeam().getName()); // 추가 쿼리 존재
        }
    }

    @Test
    public void fetchJoin_올바른해결방법(){
        em.flush();
        em.clear();

        // Member를 조회하면서 Team도 함께 페치조인
        List<Member> memberList = query.selectFrom(member)
                .join(member.team, team).fetchJoin()  // 이 부분이 핵심!
                .fetch();

        // 추가 쿼리 없이 Team 정보 사용 가능
        for (Member findMember : memberList) {
            System.out.println(findMember.getTeam().getName()); // 추가 쿼리 없음
        }
    }

    /**
     * 서브 쿼리 활용하여 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query.selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();


        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 서브 쿼리 활용하여 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();


        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브 쿼리 활용하여 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query.selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();


        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = query.select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple);
        }
    }

    @Test
    public void basicCase(){
        List<String> fetch = query.select(
                        member.age.when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }
    }

    @Test
    public void complexCase(){
        List<String> fetch = query.select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21살~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }
    }

    @Test
    public void constant(){
        List<Tuple> fetch = query.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println(tuple);
        }
    }

    @Test
    public void concat(){
        List<String> fetch = query.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println(s);
        }
    }

    @Test
    public void simpleProjection(){
        List<String> result = query.select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = query.select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username: " + username);
            System.out.println("age: " + age);
        }
    }

    @Test
    public void findDtoByJPQL(){
        List<MemberDto> resultList = em.createQuery(
                "select " +
                        "new com.example.demo.dto.MemberDto(m.username, m.age) " +
                        "from Member m", MemberDto.class
        ).getResultList();


        for (MemberDto memberDto : resultList) {
            System.out.println("username: " + memberDto.getUsername());
            System.out.println("age: " + memberDto.getAge());
        }
    }

    @Test
    public void findDtoBySQLSetter() {
        List<MemberDto> resultList = query.select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : resultList) {
            System.out.println("username: " + memberDto.getUsername());
            System.out.println("age: " + memberDto.getAge());
        }
    }

    @Test
    public void findDtoBySQLField() {
        List<MemberDto> resultList = query.select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : resultList) {
            System.out.println("username: " + memberDto.getUsername());
            System.out.println("age: " + memberDto.getAge());
        }
    }

    @Test
    public void findDtoBySQLConstructor() {
        List<MemberDto> resultList = query.select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : resultList) {
            System.out.println("username: " + memberDto.getUsername());
            System.out.println("age: " + memberDto.getAge());
        }
    }

    @Test
    public void findUserDtoBySQLConstructor() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> resultList = query.select(Projections.constructor(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")))
                .from(member)
                .fetch();

        for (UserDto userDto : resultList) {
            System.out.println("username: " + userDto.getName());
            System.out.println("age: " + userDto.getAge());
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        // projection 방식보다 안전 이상한 필드 추가 시 컴파일 시점에 오류를 잡아낼 수 있기 때문
        // 실무에서 가장 많이 사용되긴 하지만 MemberDto가 querydsl에 의존적이게 돔
        List<MemberDto> resultList = query.select
                        (new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : resultList) {
            System.out.println("username: " + memberDto.getUsername());
            System.out.println("age: " + memberDto.getAge());
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParm = "member1";
        Integer ageParm = 10;

        List<Member> members = searchMember1(usernameParm, ageParm);
        assertThat(members.size()).isEqualTo(1);
    }

    @Test
    public void dynamicQuery2_BooleanBuilder(){
        String usernameParm =null;
        Integer ageParm = 10;

        List<Member> members = searchMember1(usernameParm, ageParm);
        assertThat(members.size()).isEqualTo(1);
    }

    public List<Member> searchMember1(String usernameCon, Integer ageCond){
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCon != null){
            builder.and(member.username.eq(usernameCon));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return query.selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery2_BooleanBuilder2(){
        String nameParm = "member1";
        Integer ageParm = null;

        List<Member> members = searchMember2(nameParm, ageParm);
        assertThat(members.size()).isEqualTo(1);
    }

    public List<Member> searchMember2(String usernameCon, Integer ageCond){
        return query
                .selectFrom(member)
                .where(allEq(usernameCon, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCon) {
        return usernameCon == null ? null : member.username.eq(usernameCon);

    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    // Expressions.allOf -> 매개변수들은 꼭 null 처리를 해둔 BooleanExpression 이어야함
    private BooleanExpression allEq(String usernameCon, Integer ageCond){
        return Expressions.allOf(usernameEq(usernameCon), ageEq(ageCond));
    }

    // 영속성 컨텍스트에 값이 있으면 디비에서 읽었어도 영속성 컨텍스의 값을 유지
    @Test
    void bulkUpdate(){
        query.update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(20))
                .execute();

        List<Member> resultList = query.selectFrom(member)
                .fetch();

        // DB와 값이 맞지 않음! 영속성 컨텍스트의 값을 가져오기 때문
        for (Member member : resultList) {
            System.out.println("username: " + member.getUsername());
        }
    }


    @Test
    void bulkUpdate2(){
        query.update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(20))
                .execute();

        List<Member> resultList = query.selectFrom(member)
                .fetch();

        // 벌크 연산 후 영속성 컨텍스트와 DB 데이터 동기화를 위해 필요
        // 벌크 연산은 영속성 컨텍스트를 거치지 않고 직접 DB에 쿼리를 실행하므로
        // 영속성 컨텍스트에 남아있는 기존 엔티티들과 실제 DB 상태가 불일치 상태가 됨
        // flush(): 영속성 컨텍스트의 변경 내용을 DB에 반영
        // clear(): 영속성 컨텍스트를 초기화하여 이후 조회 시 DB에서 최신 데이터를 가져옴
        em.flush();
        em.clear();

        for (Member member : resultList) {
            System.out.println("username: " + member.getUsername());
        }
    }
}
