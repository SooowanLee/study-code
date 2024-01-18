# SpringBoot 게시판 만들기

---
SpringBoot를 사용하여 기본적인 CRUD 기능이 포함된 REST API를 만들어봅니다.
> **REST API**
>
> Representational state transfer의 약자로
> 분산 시스템에서 리소스를 표현하고 다루기 위한 규칙을 제시합니다.

간단한 예제이기 때문에 H2데이터 베이스를 사용합니다.
Lombok과 JPA를 사용하여 게시글을 저장 및 조회, 수정, 삭제합니다.

## 기능 정의

- 전체 게시글 목록 조회
    - 제목, 작성자명, 작성 내용, 작성 날짜를 조회합니다.
    - 작성 날짜 기준 내림차순으로 정렬합니다.
- 게시글 작성
    - 제목, 작성자명, 비밀번호, 작성 내용을 저장합니다.
    - 저장된 게시글을 저장이 잘 되었는지 확인하기 위해 Client로 반환합니다.
- 특정 게시글 조회
    - 특정 게시글의 제목, 작성자명, 작성 날짜, 작성 내용을 조회합니다.
- 특정 게시글 수정
    - 수정할 게시글 데이터를 서버에 보내, 검증을 한 뒤 통과를 하면 수정이 가능합니다.
- 특정 게시글 삭제
    - 수정과 마찬가지로 삭제할 게시글의 데이터를 서버로 보내 검증을 한 뒤 통과하면 삭제를 합니다.
    - 삭제 성공 여부를 Client에게 응답합니다.

## 유스케이스 다이어그램

> **유스케이스 다이어그램**
>
> 사용자와 시스템 간의 상호작용을 보여주며, 주요 기능과 시나리오를 나타냅니다.

[유스케이스 다이어그램 사진넣기]

## API 명세서

![APISpecification.png](src%2Fmain%2Fresources%2Fimage%2FAPISpecification.png)

### 프로젝트 dependency 및 datadase 설정

- depencency

```
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-web'
compileOnly 'org.projectlombok:lombok'
developmentOnly 'org.springframework.boot:spring-boot-devtools'
runtimeOnly 'com.h2database:h2'
annotationProcessor 'org.projectlombok:lombok'
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

- properties

```properties
// h2 database 설정
spring.h2.console.enabled=true
spring.datasource.url=jdbc:h2:mem:db;MODE=MYSQL;
spring.datasource.username=sa
spring.datasource.password=
```

## 도메인 클래스 설계(Entity 구현)

게시글을 관리하는 `Board`라는 Entity를 구현합니다.

Board Entity는 id, title, content, author, password, createdAt, modifiedAt 속성을 가집니다.

createdAt, modifiedAt 속성은 Timestamped Entity를 `@MappedSuperclass` 애노테이션을 통해 속성만 상속 받습니다.

### board/entity/Timestamped.class
- 작성일시와 수정일시는 Spring Data JPA의 Auditing 기능을 사용하여 자동으로 부여합니다.
```java

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class Timestamped {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;
}
```

- `@Mappedsuperclass` : 공통 매핑 정보가 필요할 때, 부모 클래스에 선언하고 속성만 상속 받아서 사용할 수 있습니다.
- Spring Data JPA `Auditing` 기능 사용법 및 설명
    - `AuditingEntityListener.class` :  Spring Data JPA에서 제공하는 클래스로, 엔티티의 변경 사항을 추적하고 관리하는 데 사용됩니다.
    - 이를 위해 먼저 `@EntityListeners(AuditingEntityListener.class)` 어노테이션을 엔티티 클래스에 추가하여 해당 클래스가 엔티티 리스너에 의해 관리되어야 함을 명시합니다.
    - `@SpringBootApplication`이 있는 class에 `@EnableJpaAuditing`을 추가합니다.
    - `@CreatedDate` : 엔티티 생성시간을 자동으로 저장합니다.
    - `@LastModifiedDate` : 엔티티가 수정된 시간을 자동으로 저장합니다.


### board/entity/Board
```java

@Getter
@Entity
@NoArgsConstructor
public class Board extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String password;
}
```

- `@Entity` : JPA가 관리하는 엔티티 클래스, DB의 레코드와 1:1로 매핑이 됩니다.
- 생성일시와 수정일시는 `Timestamped`를 상속받아 사용합니다.
- `@Id`와 `@GeneratedValue`를 사용해 JPA가 기본키를 관리하게 합니다.
- `@Column` :  클래스의 필드와 데이터베이스 테이블의 컬럼을 매핑하는 데 사용됩니다.

## DTO 구현

> **DTO(Data Tranfer Object)**
>
> **계층간 데이터 전송을 위한 객체**
> 단순히 데이터 전송을 위한 객체이기 때문에 비즈니스 로직이 포함되면 안된다.

DTO에 어떤 데이터를 담을 지 확인하기 위해 위의 API 명세를 확인 합니다.

- Request(요청) DTO : /api/{...} `Board`의 필드 값을 받아 요청을 보낸다.
    - `게시글 작성`, `게시글 수정` : { title, content, author, password }
    - `게시글 삭제` : { password }
- Response(응답) DTO : 요청에 대한 `Board`의 필드값 또는 성공여부를 응답으로 보낸다.
    - `전체 목록 조회` : [{ id, title, content, author, createdAt, modifiedAt }, {...}]
    - `게시글 조회`, `게시글 작성`, `게시글 수정` : { id, title, content, author, createdAt, modifiedAt }
    - `게시글 삭제` : { success }


### board/dto/BoardRequestDTO
```java
@Getter
public class BoardRequestDTO {
    private String title;
    private String content;
    private String author;
    private String password;
```
- 요청에 필요한 속성을 모아 `BoardRequestDTO`를 구현합니다.


### board/dto/BoardResponseDTO
```java
@Getter
@NoArgsConstructor
public class BoardResponseDTO {

  private Long id;
  private String title;
  private String content;
  private String author;
  private LocalDateTime createdAt;
  private LocalDateTime modifiedAt;

  public BoardResponseDTO(Board board) {
      this.id = board.getId();
      this.title = board.getTitle();
      this.content = board.getContent();
      this.author = board.getAuthor();
      this.createdAt = board.getCreatedAt();
      this.modifiedAt = board.getModifiedAt();
  }
}
```
- `Board` 엔티티를 받아 `BoardResponseDTO` 객체로 만들기 위한 생성자를 추가했습니다.


### board/dto/SuccessResponseDTO
```java
@Getter
public class SuccessResponseDTO {
    private boolean success;

    public SuccessResponseDTO(boolean success) {
        this.success = success;
    }
}
```
- 게시글 삭제 성공 여부를 담아서 응답해줄 `SuccessResponseDTO`를 구현했습니다.

## CRUD 기능 구현
각 기능 구현을 하기 전에 미리 Controller, Service, Repository를 만들어 놓겠습니다.

### board/controller/BoardController
```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
}
```
  - view 없이 api를 응답하기 때문에 `@RestController`를 사용합니다.
  - `/api` 라는 uri는 공통으로 사용하기 때문에 `@RequestMapping`을 사용해서 class 레벨에 선언해줍니다
  - lombok의 `@RequiredArgsConstructor`와 `final` 제어자를 사용해서 DI를 받습니다.

### board/service/BoardService
```java
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
}
```
- Controller와 같은 방식으로 `boardRepository` DI를 받습니다.
- 데이터를 DB에 저장, 조회, 수정, 삭제 등을 해야 하기 때문에 `BoardRepository`를 사용합니다.

### board/repository/BoardRepository
```java
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
}
```
- Spring Data JPA를 사용해서 데이터를 다룰 것이기 때문에 `JpaRepository`를 상속받습니다.
- 이 때, 제네릭스 타입은 `JpaRepository<Entity, ID타입>`입니다.

## 전체 목록 조회
### BoardController
```java
@GetMapping("/posts")
public List<BoardResponseDTO> getPosts() {
  return boardService.getPosts();
}
```
- GET "/api/posts"으로 요청이 들어오면 `getPosts` 메소드가 실행됩니다.
- 게시글 전체를 반환하기 때문에 반환 타입을 `List<BoardResponseDTO>`로 응답합니다.

### BoardService
```java
@Transactional(readOnly = true)
public List<BoardResponseDTO> getPosts() {
    return boardRepository.findAllByOrderByModifiedAtDesc()
            .stream()
            .map(BoardResponseDTO::new)
            .toList();
}
```
- `BoardRepository`를 통해 수정일시 기준 내림차순(최근 글 먼저)으로 모든 데이터를 가져온다.
  - 기본적으로 Spring Data JPA에서 `findAll`메소드는 제공해 주지만 findAllBy{...}는 사용자가 선언 해야한다.
  - 참고 https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
  - `BoardResponseDTO`에서 `Board`를 매개변수로 받아서 `BoardResponseDTO` 필드로 할당해주는 생성자를 만들었기 때문에 `map(BoardResponseDTO::new)`를 통해 간편하게 DTO로 변환 가능하다.
  - DTO로 변환한 데이터들을 `toList()`를 사용해서 불변 List로 리턴합니다. 조회만 하고 변경을 할 필요가 없기 때문입니다.

### BoardRepository
```java
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderByModifiedAtDesc();
}
```
- 수정된 날을 기준으로 내림차순 정렬할 것이기 때문에 `findAllByOrderByModifiedAtDesc()` 메소드를 선언합니다.
- Spring Data JPA가 선언된 메소드 이름을 분석하여 해당 정보를 기반으로 쿼리를 생성합니다.

## 게시글 작성
### BoardController
```java
@PostMapping("/post")
  public BoardResponseDTO createPost(@RequestBody BoardRequestDTO boardRequestDTO) {
      return boardService.createPost(boardRequestDTO);
  }
```
- POST "/api/post"로 요청이 오면 `createPost`가 실행됩니다.
- 클라이언트로부터 `BoardRequestDTO`를 받아 게시물을 생성하는 서비스 계층의 메소드 `boardService.createPost(boardRequestDTO)`로 넘깁니다.

### BoardService
```java
@Transactional
public BoardResponseDTO createPost(BoardRequestDTO boardRequestDTO) {
    Board board = new Board(boardRequestDTO);
    boardRepository.save(board);
    return new BoardResponseDTO(board);
}
```
- Controller에서 넘어온 `BoardRequestDTO`를 `Entity`로 변환해서 게시물 저장 로직을 실행합니다.
  - DTO -> Entity로 변환하기 위해 `Board` 클래스에 생성자를 만들어줍니다.
    - Board
    ```java
    public Board(BoardRequestDTO boardRequestDTO) {
        this.title = boardRequestDTO.getTitle();
        this.content = boardRequestDTO.getContent();
        this.author = boardRequestDTO.getAuthor();
        this.password = boardRequestDTO.getPassword();
    }
    ```
- 반환타입이 `BoardResponseDTO`이기 때문에 `Board` 엔티티를 `BoardResponseDTO`로 변환해서 반환합니다.

## 특정 게시글 조회
### BoardController
```java
@GetMapping("/post/{id}")
public BoardResponseDTO getPost(@PathVariable Long id) {
    return boardService.getPost(id);
}
```
- GET "/api/post/{id}"로 요청이 오면 `getPost`가 실행됩니다.(ID에 해당하는 글을 조회합니다.)
- 선택한 게시물의 id를 path 파라미터 형태로 받습니다.
- 조회된 게시글을 `BoardResponseDTO`에 담아 응답합니다.

### BoardService
```java
 @Transactional
public BoardResponseDTO getPost(Long id) {
    return boardRepository.findById(id).map(BoardResponseDTO::new)
            .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));
}
```
- 게시글의 id로 데이터를 찾고 `map()`을 통해 새로운 `BoardResponseDTO` 객체를 반환합니다.
- 만약, id로 게시글을 찾지 못하면 `IllegalArgumentException()`을 발생시킵니다.

## 특정 게시글 수정
### BoardController
```java
 @PutMapping("/post/{id}")
public BoardResponseDTO updatePost(@PathVariable Long id, @RequestBody BoardRequestDTO boardRequestDTO) throws Exception {
    return boardService.updatePost(id, boardRequestDTO);
}
```
- PUT "/api/post/{id}"로 요청하면 `updatePost`가 실행됩니다.
- 선택한 게시물의 id를 path 파라미터 형태로 받습니다. 수정할 내용을 담은 `boardRequestDTO`는 body 형태로 받습니다.
- 수정된 게시글을 `BoardResponseDTO`에 담아서 응답합니다.

### BoardService
```java
@Transactional
public BoardResponseDTO updatePost(Long id, BoardRequestDTO boardRequestDTO) throws Exception {
    Board board = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

    if (!boardRequestDTO.getPassword().equals(board.getPassword())) {
        throw new InvalidPasswordException();
    }

    board.update(boardRequestDTO);
    return new BoardResponseDTO(board);
}
```
- 파라미터로 받은 `id`를 boardRepository.findById()에 넘기면 JPA가 `id`에 맞는 `Board` 객체를 찾아서 반환합니다.
  - 만약 `id`로 `Board` 객체를 찾을 수 없다면 예외를 발생시킵니다.
- `BoardRequestDTO`의 패스워드와 `id`로 찾아온 기존의 `Board`객체의 패스워드가 일치하지 않으면, 예외를 발생시킵니다.
  - src/main/java/soowan/study/board/exception/InvalidPasswordException.class
  ```java
  public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("비밀번호가 일치하지 않습니다.");
    }
  }
  ```
- `id`로 객체를 찾을 수 있고, 패스워드도 일치한다면, board의 내용을 update하고, `BoardResponseDTO`에 update된 board를 담아서 반환합니다.

## 특정 게시글 삭제
### BoardController
```java
@DeleteMapping("/post/{id}")
public SuccessResponseDTO deletePost(@PathVariable Long id, @RequestBody BoardRequestDTO boardRequestDTO) {
    return boardService.deletePost(id, boardRequestDTO);
}
```
- DELETE "/api/post/{id}" 로 요청이 오면 `deletePost`가 실행됩니다.
- 선택한 게시물의 id를 path 파라미터 형태로 받습니다. 패스워드를 담은 boardRequestDTO는 body 형태로 받습니다.
- 삭제 성공 여부는 `SuccessResponseDTO`에 담아서 응답합니다.

### BoardService
```java
@Transactional
public SuccessResponseDTO deletePost(Long id, BoardRequestDTO boardRequestDTO) {
    Board board = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

    if (!boardRequestDTO.getPassword().equals(board.getPassword())) {
        throw new InvalidPasswordException();
    }

    boardRepository.deleteById(id);
    return new SuccessResponseDTO(true);
}
```
- 파라미터로 받은 `id`를 boardRepository.findById()에 넘기면 JPA가 `id`에 맞는 `Board` 객체를 찾아서 반환합니다.
  - 만약 `id`로 `Board` 객체를 찾을 수 없다면 예외를 발생시킵니다.
  - 위의 특정 게시글 수정에서 발생시킨 방법과 같습니다.
- `id`가 존재하고, 비밀번호도 일치한다면, boardRespository에서 해당 `id` 값을 가진 데이터를 지웁니다.
- 성공 여부를 `SuccessResponseDTO`에 담아서 반환합니다.

## 구현 결과
Postman을 사용해서 요청을 보내고 반환 결과를 확인합니다.
- 게시글 저장
![postSave.png](src%2Fmain%2Fresources%2Fimage%2FpostSave.png)


- 게시글 전체 조회
![getPosts.png](src%2Fmain%2Fresources%2Fimage%2FgetPosts.png)


- 특정 게시글 조회
![getSpecificPost.png](src%2Fmain%2Fresources%2Fimage%2FgetSpecificPost.png)


- 특정 게시글 수정
![modifiedSpecificPost.png](src%2Fmain%2Fresources%2Fimage%2FmodifiedSpecificPost.png)


- 특정 게시글 삭제
![successDeletedSpecificPost.png](src%2Fmain%2Fresources%2Fimage%2FsuccessDeletedSpecificPost.png)


- 성공적으로 삭제 후 게시글 전체 조회
![successDeletedAfterGetPosts.png](src%2Fmain%2Fresources%2Fimage%2FsuccessDeletedAfterGetPosts.png)