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
```
