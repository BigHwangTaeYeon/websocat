# websocat

https://doozi0316.tistory.com/entry/WebSocket%EC%9D%B4%EB%9E%80-%EA%B0%9C%EB%85%90%EA%B3%BC-%EB%8F%99%EC%9E%91-%EA%B3%BC%EC%A0%95-socketio-Polling-Streaming

    블로그 내용
나는 이 기능을 구현하기 위해 5초마다 한번 씩 데이터의 수정된 시간를 DB에서 가져왔고,
DB에서 가져온 수정된 시간이 기존 수정된 시간과 다르다면 화면이 refresh 되도록 구현했다.
(이렇게 일정 주기로 통신하여 가져오는 방법을 Polling 이라고 한단다.)

Polling 방식으로 구현된 코드를 웹 소켓 방식으로 변경했다.

===========================================================================================================================

### 웹 소켓 : 서버와 클라이언트 간의 메시지 교환을 위한 통신 규약(프로토콜)

##### 특징
- 양방향 통신 (Full-Duplex)
    데이터 송수신을 동시에! 처리할 수 있는 방법.
- 실시간 네트워킹 (Real Time Networking)
    웹 환경에서 연속된 데이터를 빠르게 노출하는 것. (ex. 채팅, 주식)

웹 소켓이 나오기 전까지의 통신 방식
- Polling
    setTimeout, setInterval 등으로 일정 주기마다 서버에 요청(Request)을 보낸다.
        1) 불필요한 Request 와 Connection을 생성하여 서버에 부담을 주게된다.
        2) 요청 주기가 짧을 수록 부하가 커진다.
        3) 요청 주기가 짧으면 실시간 처럼 보이지만, 실제로 실시간은 아니다.
        4) HTTP 통신을 하기에 Request, Response 헤더가 불필요하게 크다.
    Polling 방식을 선택하는 경우
        1) 응답을 실시간으로 받지 않아도 되는 경우
        2) 다수의 사용자가 동시에 사용하는 경우
        3) ex. facebook 웹 채팅, google 메신저, msn 웹 메신저

- Long Polling
    Polling처럼 일정 주기마다 요청을 보내지만 서버가 응답을 바로 전달하지 않는 방식이다.
    요청을 보냈을 때, 서버가 응답을 바로 보내지 않고 특정 이벤트나 타임아웃이 발생했을 때 응답을 전달하는 방식.
    응답을 받은 클라이언트는 다시 서버에 데이터를 요청한다.
        1) Long Polling 도 동시 다발적인 요청과 응답이 생기면 부하가 발생할 수 있다.
        2) HTTP 통신을 하기 때문에 Request, Response 헤더가 불필요하게 크다.
    Long Polling 방식을 선택하는 경우
        1) 응답을 실시간으로 받아야하는 경우
        2) 적은 수의 사용자가 동시에 사용하는 경우

- Streaming
    이벤트가 발생했을 때 응답을 내려주되, 응답을 완료시키지 않고 계속 연결을 유지하는 방식.
        1) 연결 시간이 길어질 수록 연결 유효성 관리의 부담이 발생한다.
        2) HTTP 통신을 하기 때문에 Request, Response 헤더가 불필요하게 크다.

### 웹 소켓 동작 과정

![WebSocket](https://blog.kakaocdn.net/dn/dDiLTo/btrG4Iebgdo/8KL22qu1Iu4rQ1YJlziWY1/img.png)
 
웹 소켓 동작 과정은 크게 세가지로 나눌 수 있다.
위 이미지의 빨간 색 박스에 해당하는 Opening Handshake
위 이미지의 노란 색 박스에 해당하는 Data transfer
위 이미지의 보라 색 박스에 해당하는 Closing Handshake

##### Handshake
Opening Handshake 와 Closing Handshake 는 일반적인 HTTP TCP 통신의 과정 중 하나이다.
접속 요청은 HTTP 로 한 뒤, 웹소켓 프로토콜로 변경된다. (WS)

웹소켓 프로토콜로 변경되기 위한 HTTP 헤더는 아래처럼 구성되어 있다.
(ws://localhost:8080/chat으로 접속하려고 한다고 가정한다.)

##### 요청(Request) 헤더
```html
GET /chat HTTP/1.1
Host: localhost:8080
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==
Sec-WebSocket-Protocol: chat, superchat
Sec-WebSocket-Version: 13
Origin: http://localhost:9000
```

- GET /chat HTTP/1.1
    웹소켓의 통신 요청에서, HTTP 버전은 1.1 이상이어야하고 GET 메서드를 사용해야한다.

- Upgrade
    프로토콜을 전환하기 위해 사용하는 헤더.
    웹소켓 요청시에는 반스에 websocket 이라는 값을 가진다.
    이 값이 없거나 다른 값이면 cross-protocol attack 이라고 간주하여 웹 소켓 접속을 중지시킨다.

- Connection
    현재의 전송이 완료된 후 네트워크 접속을 유지할 것인가에 대한 정보.
    웹 소켓 요청 시에는 반드시 Upgrade 라는 값을 가진다.
    Upgrade 와 마찬가지로 이 값이 없거나 다른 값이면 웹소켓 접속을 중지시킨다.

- Sec-WebSocket-Key
    유효한 요청인지 확인하기 위해 사용하는 키 값

- Sec-WebSocket-Protocol
    사용하고자 하는 하나 이상의 웹 소켓 프로토콜 지정.
    필요한 경우에만 사용

- Sec-WebSocket-Version
    클라이언트가 사용하고자 하는 웹소켓 프로토콜 버전.
    현재 버전 13

- Origin
    CORS 정책으로 만들어진 헤더.
    Cross-Site Websocket Hijacking과 같은 공격을 피하기 위함.

##### 응답(Response) 헤더
```html
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: HSmrc0sMlYUkAGmm5OPpG2HaGWk=
Sec-WebSocket-Protocol: chat
```

- HTTP/1.1 101 Switching Protocols
    101은 HTTP에서 WS로 프로토콜 전환이 승인 되었다는 응답코드이다.

- Sec-WebSocket-Accept
    요청 헤더의 Sec-WebSocket-Key(x3JJHMbDL1EzLkh9GBhXDw==)에 유니크 아이디를 더해서 SHA-1로 해싱한 후 base64로 인코딩한 결과이다.
    웹 소켓 연결이 개시되었음을 알린다.

##### Data Transfer
Opening HandShake에서 승인이 나고나면,
웹 소켓 프로토콜로 노란색 박스 부분인 Data transfer 이 진행된다.
여기서 데이터는 메시지라는 단위로 전달된다.

- 메시지
    여러 프레임(frame)이 모여서 구성되는 하나의 논리적인 메시지 단위

- 프레임
    통신에서 가장 작은 단위의 데이터.
    (패킷은 전 네트워크 통신 과정에서 가장 작은 단위의 데이터를 뜻하고 프레임은 데이터 링크계층(이더넷)에서 주고 받는 가장 작은 단위를 의미한다.)
    작은 헤더 + payload 로 구성되어 있다.


