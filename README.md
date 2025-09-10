# QueryDSL 기본 문법 가이드

## 🚀 QueryDSL이란?
QueryDSL은 타입 안전한 쿼리를 작성할 수 있게 해주는 프레임워크입니다. SQL과 유사한 문법을 Java 코드로 작성할 수 있어서 컴파일 타임에 오류를 잡을 수 있고, IDE의 자동완성 기능을 활용할 수 있습니다.

## ⚙️ 기본 설정
```java
@Autowired
EntityManager em;

JPAQueryFactory query;

@BeforeEach
public void before(){
    query = new JPAQueryFactory(em);
    // 테스트 데이터 설정...
}
```

## 🔍 기본 조회

### 단건 조회
```java
// JPQL 방식
String jpql = "select m from Member m where m.username = :username";
Member findMember = em.createQuery(jpql, Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

// QueryDSL 방식 (타입 안전!)
Member findMember = query
        .select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();
```

### 다건 조회
```java
List<Member> memberList = query
        .selectFrom(member)  // select + from 축약
        .fetch();
```

## 🔍 조건 검색

### AND 조건
```java
// 방법 1: .and() 사용
Member result = query.selectFrom(member)
        .where(member.username.eq("member1")
                .and(member.age.eq(10)))
        .fetchOne();

// 방법 2: 쉼표로 구분 (더 깔끔!)
Member result = query.selectFrom(member)
        .where(member.username.eq("member1"),
               member.age.eq(10))  // and와 동일
        .fetchOne();
```

## 📊 결과 조회 메서드

### fetch 계열 메서드
```java
// 리스트 조회
List<Member> list = query.selectFrom(member).fetch();

// 단건 조회 (결과가 둘 이상이면 NonUniqueResultException)
Member one = query.selectFrom(member).fetchOne();

// 첫 번째 결과만 (limit 1과 동일)
Member first = query.selectFrom(member).fetchFirst();

// 카운트 조회 (deprecated)
// Long count = query.selectFrom(member).fetchCount(); // 사용 X

// 대신 이렇게 사용
Long count = query.select(member.count())
        .from(member)
        .fetchOne();
```

## 📈 정렬

### 기본 정렬
```java
List<Member> result = query.selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(),           // 나이 내림차순
                member.username.asc().nullsLast())  // 이름 오름차순, null은 마지막
        .fetch();
```

## 📄 페이징

### offset/limit 사용
```java
List<Member> result = query.selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)    // 1번째부터 (0-based)
        .limit(2)     // 2개만
        .fetch();

// 총 개수는 별도 쿼리
Long total = query.select(member.count())
        .from(member)
        .fetchOne();
```

## 📊 집계 함수

### 기본 집계
```java
Tuple result = query.select(
                member.count(),    // 개수
                member.age.sum(),  // 합계
                member.age.avg(),  // 평균
                member.age.max(),  // 최대값
                member.age.min())  // 최소값
        .from(member)
        .fetchOne();

// 결과 사용
Long count = result.get(member.count());
Integer sum = result.get(member.age.sum());
Double avg = result.get(member.age.avg());
```

## 📋 그룹핑

### GROUP BY + 집계
```java
List<Tuple> result = query.select(team.name, member.age.avg())
        .from(member)
        .join(member.team, team)
        .groupBy(team.name)
        .fetch();

for (Tuple tuple : result) {
    String teamName = tuple.get(team.name);
    Double avgAge = tuple.get(member.age.avg());
    System.out.println("팀: " + teamName + ", 평균 나이: " + avgAge);
}
```

## 🔗 조인

### 기본 조인
```java
// 내부 조인
List<Member> result = query.selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();

// 좌외부 조인
List<Tuple> result = query.select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .fetch();
```

### ON 절 조인
```java
// 조인 대상 필터링
List<Tuple> result = query.select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .on(team.name.eq("teamA"))  // 조인 시점에 팀A만 조인
        .fetch();

// 연관관계 없는 조인 (theta join)
List<Tuple> result = query.select(member, team)
        .from(member)
        .join(team)
        .on(member.username.eq(team.name))  // 이름이 같은 경우만 조인
        .fetch();
```

### 🎯 페치 조인 (N+1 해결)
```java
// 일반 조인 (N+1 발생)
List<Member> members = query.selectFrom(member)
        .join(member.team, team)  // fetchJoin() 없음
        .fetch();
// getTeam() 호출 시 추가 쿼리 발생!

// 페치 조인 (N+1 해결)
List<Member> members = query.selectFrom(member)
        .join(member.team, team).fetchJoin()  // 핵심!
        .fetch();
// getTeam() 호출 시 추가 쿼리 없음!
```

## 🔍 서브쿼리

### WHERE 절 서브쿼리
```java
QMember memberSub = new QMember("memberSub");

// 나이가 최대인 회원
List<Member> result = query.selectFrom(member)
        .where(member.age.eq(
                select(memberSub.age.max())
                        .from(memberSub)
        ))
        .fetch();

// 평균 나이 이상인 회원
List<Member> result = query.selectFrom(member)
        .where(member.age.goe(
                select(memberSub.age.avg())
                        .from(memberSub)
        ))
        .fetch();

// IN 서브쿼리
List<Member> result = query.selectFrom(member)
        .where(member.age.in(
                select(memberSub.age)
                        .from(memberSub)
                        .where(memberSub.age.gt(10))
        ))
        .fetch();
```

### SELECT 절 서브쿼리
```java
QMember memberSub = new QMember("memberSub");

List<Tuple> result = query.select(
                member.username,
                select(memberSub.age.avg()).from(memberSub))  // 서브쿼리
        .from(member)
        .fetch();
```

## 🔀 조건문 (CASE)

### 기본 CASE
```java
List<String> result = query.select(
                member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
        .from(member)
        .fetch();
```

### 복잡한 CASE
```java
List<String> result = query.select(new CaseBuilder()
                .when(member.age.between(0, 20)).then("0~20살")
                .when(member.age.between(21, 30)).then("21살~30살")
                .otherwise("기타"))
        .from(member)
        .fetch();
```

## 🔧 함수 & 상수

### 상수 사용
```java
List<Tuple> result = query.select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();
```

### 문자열 연결
```java
List<String> result = query.select(
                member.username
                        .concat("_")
                        .concat(member.age.stringValue()))  // 숫자를 문자로 변환
        .from(member)
        .fetch();
```

## 💡 주요 포인트

### ✅ 좋은 습관
- `selectFrom()` 사용으로 간결하게 작성
- 조건은 쉼표로 구분해서 AND 연산
- N+1 문제는 fetchJoin()으로 해결
- 별칭이 필요한 서브쿼리는 새로운 Q클래스 생성

### ⚠️ 주의사항
- `fetchCount()`는 deprecated → `select(count()).fetchOne()` 사용
- 1:N 페치조인 + 페이징은 메모리에서 처리됨
- 복잡한 쿼리보다는 단순하고 명확하게 작성

## 📤 프로젝션 (Projection)

### 단순 프로젝션
```java
// 단일 필드 조회
List<String> result = query.select(member.username)
        .from(member)
        .fetch();

// 여러 필드를 Tuple로 조회
List<Tuple> result = query.select(member.username, member.age)
        .from(member)
        .fetch();

for (Tuple tuple : result) {
    String username = tuple.get(member.username);
    Integer age = tuple.get(member.age);
}
```

## 📦 DTO 매핑 방법

### 1. JPQL 생성자 방식
```java
// JPQL로 DTO 조회 (패키지명 포함한 긴 문법)
List<MemberDto> resultList = em.createQuery(
        "select new com.example.demo.dto.MemberDto(m.username, m.age) " +
        "from Member m", MemberDto.class)
        .getResultList();
```

### 2. QueryDSL Projections.bean() - Setter 사용
```java
// Setter를 통한 주입 (기본 생성자 + Setter 필요)
List<MemberDto> resultList = query.select(
        Projections.bean(MemberDto.class,
                member.username,
                member.age))
        .from(member)
        .fetch();
```

### 3. QueryDSL Projections.fields() - 필드 직접 주입
```java
// 필드에 직접 값 주입 (private 필드도 가능)
List<MemberDto> resultList = query.select(
        Projections.fields(MemberDto.class,
                member.username,
                member.age))
        .from(member)
        .fetch();
```

### 4. QueryDSL Projections.constructor() - 생성자 사용
```java
// 생성자를 통한 주입
List<MemberDto> resultList = query.select(
        Projections.constructor(MemberDto.class,
                member.username,
                member.age))
        .from(member)
        .fetch();

// 별칭이 다른 DTO 매핑 (서브쿼리와 함께)
QMember memberSub = new QMember("memberSub");
List<UserDto> result = query.select(
        Projections.constructor(UserDto.class,
                member.username.as("name"),  // 별칭 사용
                ExpressionUtils.as(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub), "age")))
        .from(member)
        .fetch();
```

### 5. @QueryProjection 활용 (권장) 🌟
```java
// 가장 안전한 방법! 컴파일 타임에 오류 검출
// DTO에 @QueryProjection 애노테이션을 생성자에 추가 후 빌드
List<MemberDto> resultList = query.select(
        new QMemberDto(member.username, member.age))  // Q클래스 생성됨
        .from(member)
        .fetch();
```

### 장단점 비교
| 방법 | 장점 | 단점 |
|------|------|------|
| JPQL | 표준 | 문자열 기반, 런타임 오류 |
| Projections.bean | Setter 재활용 | 기본 생성자 + Setter 필수 |
| Projections.fields | 간단함 | 필드명 일치 필요 |
| Projections.constructor | 생성자 재활용 | 타입 순서 일치 필요 |
| @QueryProjection | **컴파일 타임 안전** | QueryDSL 의존성 |

## 🔄 동적 쿼리

### BooleanBuilder 활용
```java
// 동적 조건 생성
public List<Member> searchMember(String username, Integer age) {
    BooleanBuilder builder = new BooleanBuilder();
    
    if (username != null) {
        builder.and(member.username.eq(username));
    }
    
    if (age != null) {
        builder.and(member.age.eq(age));
    }
    
    return query.selectFrom(member)
            .where(builder)  // 동적 조건 적용
            .fetch();
}

// 사용 예시
List<Member> result1 = searchMember("member1", 10);  // 두 조건 모두
List<Member> result2 = searchMember(null, 10);       // 나이만
List<Member> result3 = searchMember("member1", null); // 이름만
```

### BooleanExpression 메서드 활용 (권장) 🌟
```java
// 깔끔한 동적 쿼리
public List<Member> searchMember2(String username, Integer age) {
    return query.selectFrom(member)
            .where(allEq(username, age))  // null 조건 자동 무시
            .fetch();
}

// Expressions.allOf를 활용한 조건 조합 (null 자동 처리)
private BooleanExpression allEq(String usernameCond, Integer ageCond) {
    return Expressions.allOf(usernameEq(usernameCond), ageEq(ageCond));
}

// null 안전한 조건 메서드들
private BooleanExpression usernameEq(String usernameCond) {
    return usernameCond == null ? null : member.username.eq(usernameCond);
}

private BooleanExpression ageEq(Integer ageCond) {
    return ageCond == null ? null : member.age.eq(ageCond);
}

```

### BooleanExpression 장점
- **조건 재사용**: `usernameEq()` 메서드를 다른 쿼리에서도 활용
- **가독성 향상**: 복잡한 조건을 메서드명으로 표현  
- **null 안전**: `Expressions.allOf()`가 null 조건을 자동으로 필터링 -> 매개변수로 null 처리가 된 BooleanExpression 필수 (매개변수가 null이면 null을 반환 그렇지 않으면 BooleanExpression 반환)
- **조합 가능**: 여러 조건 메서드를 조합해서 복잡한 동적 쿼리 구성

## 🎯 자주 사용하는 패턴
```java
// 기본 조회 + 조건
List<Member> members = query.selectFrom(member)
        .where(member.team.name.eq("teamA"),
               member.age.between(20, 30))
        .orderBy(member.username.asc())
        .fetch();

// 페치조인 + 조건
List<Member> members = query.selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(team.name.eq("teamA"))
        .fetch();

// 페이징 + 카운트
List<Member> content = query.selectFrom(member)
        .offset(offset)
        .limit(limit)
        .fetch();

Long total = query.select(member.count())
        .from(member)
        .fetchOne();

// DTO 조회 + 동적 쿼리
public List<Member> searchMember2(String username, Integer age) {
    return query.selectFrom(member)
            .where(allEq(username, age))  // null 조건 자동 무시
            .fetch();
}

private BooleanExpression allEq(String usernameCond, Integer ageCond) {
    return Expressions.allOf(usernameEq(usernameCond), ageEq(ageCond));
}

// null 안전한 조건 메서드들
private BooleanExpression usernameEq(String usernameCond) {
    return usernameCond == null ? null : member.username.eq(usernameCond);
}

private BooleanExpression ageEq(Integer ageCond) {
    return ageCond == null ? null : member.age.eq(ageCond);
}
```

## 🔄 벌크 연산 (Bulk Operations)

### ⚠️ 벌크 연산의 주의사항
벌크 연산은 **영속성 컨텍스트를 거치지 않고 직접 DB에 쿼리를 실행**합니다. 따라서 영속성 컨텍스트와 DB 간의 데이터 불일치가 발생할 수 있습니다.

### 잘못된 벌크 연산 예시 ❌
```java
@Test
void bulkUpdate() {
    // 벌크 업데이트 실행 (영속성 컨텍스트 무시하고 DB 직접 수정)
    query.update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(20))
            .execute();

    // 조회 시 영속성 컨텍스트의 기존 값을 반환 (DB 변경사항 반영 안됨)
    List<Member> resultList = query.selectFrom(member)
            .fetch();

    // ❌ DB에서는 "비회원"으로 변경되었지만, 영속성 컨텍스트의 기존 값 출력
    for (Member member : resultList) {
        System.out.println("username: " + member.getUsername()); // member1, member2 출력
    }
}
```

### 올바른 벌크 연산 예시 ✅
```java
@Test
void bulkUpdate2() {
    // 벌크 업데이트 실행
    query.update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(20))
            .execute();

    List<Member> resultList = query.selectFrom(member)
            .fetch();

    // ✅ 벌크 연산 후 영속성 컨텍스트와 DB 데이터 동기화를 위해 필요
    // 벌크 연산은 영속성 컨텍스트를 거치지 않고 직접 DB에 쿼리를 실행하므로
    // 영속성 컨텍스트에 남아있는 기존 엔티티들과 실제 DB 상태가 불일치 상태가 됨
    // flush(): 영속성 컨텍스트의 변경 내용을 DB에 반영
    // clear(): 영속성 컨텍스트를 초기화하여 이후 조회 시 DB에서 최신 데이터를 가져옴
    em.flush();
    em.clear();

    // ✅ 이제 DB의 최신 데이터 출력
    for (Member member : resultList) {
        System.out.println("username: " + member.getUsername()); // "비회원" 출력
    }
}
```

### 벌크 연산 베스트 프랙티스
1. **벌크 연산 후 항상 `em.flush()`와 `em.clear()` 호출**
2. **벌크 연산은 트랜잭션 시작 직후나 끝나기 직전에 실행**
3. **벌크 연산 후에는 영속성 컨텍스트의 엔티티 사용 주의**
4. **대량 데이터 처리 시에만 사용 (소량은 일반적인 dirty checking 활용)**

### 💡 핵심 포인트
- 벌크 연산은 **성능상 이점**이 있지만 **영속성 컨텍스트 동기화 문제** 주의
- **`flush()` + `clear()`**는 벌크 연산 후 필수 작업
- JPA의 1차 캐시와 변경 감지 기능이 무시되므로 신중하게 사용
