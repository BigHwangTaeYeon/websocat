https://fvor001.tistory.com/123
https://programmingrecoding.tistory.com/47


[DevLogWebSocketHandler](###DevLogWebSocketHandler)

[WebSocketConfig](###WebSocketConfig)

[ChatController](###ChatController)

[ChatScript](###ChatScript)

[Script Point](###Script-Point)

### DevLogWebSocketHandler

```java
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DevLogWebSocketHandler extends TextWebSocketHandler{
	
	Map<String, WebSocketSession> sessionMap = new HashMap<>(); //웹소켓 세션을 담아둘 맵
	Map<String, String> userMap = new HashMap<>();	//사용자
	
	/* 클라이언트로부터 메시지 수신시 동작 */
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String msg = message.getPayload();
		log.info("===============Message=================");
		log.info("{}", msg);
		log.info("===============Message=================");
		
		JSONObject obj = jsonToObjectParser(msg);
		//로그인된 Member (afterConnectionEstablished 메소드에서 session을 저장함)
		for(String key : sessionMap.keySet()) {
			WebSocketSession wss = sessionMap.get(key);
			
			if(userMap.get(wss.getId()) == null) {
				userMap.put(wss.getId(), (String)obj.get("userName"));
			}
			
			//클라이언트에게 메시지 전달
			wss.sendMessage(new TextMessage(obj.toJSONString()));
		}
	}
	
	/* 클라이언트가 소켓 연결시 동작 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.info("{} 연결되었습니다.", session.getId());
		super.afterConnectionEstablished(session);
		sessionMap.put(session.getId(), session);
		
		JSONObject obj = new JSONObject();
		obj.put("type", "getId");
		obj.put("sessionId", session.getId());
        
        //클라이언트에게 메세지 전달
		session.sendMessage(new TextMessage(obj.toJSONString()));
	}
	
	/* 클라이언트가 소켓 종료시 동작 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		log.info("{} 연결이 종료되었습니다.", session.getId());
		super.afterConnectionClosed(session, status);
		sessionMap.remove(session.getId());
		
		String userName = userMap.get(session.getId());
		for(String key : sessionMap.keySet()) {
			WebSocketSession wss = sessionMap.get(key);
			
			if(wss == session) continue;

			JSONObject obj = new JSONObject();
			obj.put("type", "close");
			obj.put("userName", userName);
			
			wss.sendMessage(new TextMessage(obj.toJSONString()));
		}
		userMap.remove(session.getId());
	}
	
	/**
	 * JSON 형태의 문자열을 JSONObejct로 파싱
	 */
	private static JSONObject jsonToObjectParser(String jsonStr) throws Exception{
		JSONParser parser = new JSONParser();
		JSONObject obj = null;
		obj = (JSONObject) parser.parse(jsonStr);
		return obj;
	}
}
```

Socket의 Connect와 Disconnect, Message처리를 해주는 핸들러이다.(DevLogWebSocketHandler) 
TextWebSocketHandler 메소드를 상속받는다. 
내가 사용할것이 단순한 채팅에 필요한 메세지형태이기 때문에 TextWebSocketHandler를 상속받았지만, 
이미지와 같은 다른 리소스를 통신으로 받는다고 한다면 BinaryWebSocketHandler를 상속받으면 되겠다.

afterConnectionEstablished : 소켓 연결 시 동작
acafterConnectionClosed : 소켓이 종료되면 동작
handleTextMessage : 메시지 수신시 동작 (TextWebSocketHandler를 상속받은 경우)
handleBinaryMessage: 메시지 수신시 동작 (BinaryWebSocketHandler를 상속받은 경우)

### WebSocketConfig
```java
@Configuration
@EnableWebSocket// 웹소켓 활성화
public class WebSocketConfig implements WebSocketConfigurer{

	@Autowired
	private DevLogWebSocketHandler devLogWebSocketHandler;
	
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// WebSocketHandler를 추가
		registry.addHandler(devLogWebSocketHandler, "/chating");
	}
}
```
Spring에서 웹소켓을 사용하기 위해서 클라이언트가 보내는 통신을 처리할 핸들러가 필요하다.
위에서 작성한 DevLogWebSocketHandler를 Handshake할 주소와 함께 추가한다.
주소는 PORT 뒤에 endpoint를 입력하면 된다.

### ChatController
```java 
@Controller
public class ChatController {
	@RequestMapping("/chat")
	public ModelAndView chat() {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("chat");
		return mv;
	}
}
```


### ChatScript
```html
<script type="text/javascript">
	var ws;

	function wsOpen(){
		//websocket을 지정한 URL로 연결
		ws = new WebSocket("ws://" + location.host + "/chating");
		wsEvt();
	}
		
	function wsEvt() {
		//소켓이 열리면 동작
		ws.onopen = function(e){
			
		}
		
		//서버로부터 데이터 수신 (메세지를 전달 받음)
		ws.onmessage = function(e) {
			//e 파라미터는 websocket이 보내준 데이터
			var msg = e.data; // 전달 받은 데이터
			if(msg != null && msg.trim() != ''){
				var d = JSON.parse(msg);
				
				//socket 연결시 sessionId 셋팅
				if(d.type == "getId"){
					var si = d.sessionId != null ? d.sessionId : "";
					if(si != ''){
						$("#sessionId").val(si); 
						
						var obj ={
							type: "open",
							sessionId : $("#sessionId").val(),
							userName : $("#userName").val()
						}
						//서버에 데이터 전송
						ws.send(JSON.stringify(obj))
					}
				}
				//채팅 메시지를 전달받은 경우
				else if(d.type == "message"){
					if(d.sessionId == $("#sessionId").val()){
						$("#chating").append("<p class='me'>" + d.msg + "</p>");	
					}else{
						$("#chating").append("<p class='others'>" + d.userName + " : " + d.msg + "</p>");
					}
						
				}
				//새로운 유저가 입장하였을 경우
				else if(d.type == "open"){
					if(d.sessionId == $("#sessionId").val()){
						$("#chating").append("<p class='start'>[채팅에 참가하였습니다.]</p>");
					}else{
						$("#chating").append("<p class='start'>[" + d.userName + "]님이 입장하였습니다." + "</p>");
					}
				}
				//유저가 퇴장하였을 경우
				else if(d.type == "close"){
					$("#chating").append("<p class='exit'>[" + d.userName + "]님이 퇴장하였습니다." + "</p>");
					
				}
				else{
					console.warn("unknown type!")
				}
			}
		}

		document.addEventListener("keypress", function(e){
			if(e.keyCode == 13){ //enter press
				send();
			}
		});
	}

	function chatName(){
		var userName = $("#userName").val();
		if(userName == null || userName.trim() == ""){
			alert("사용자 이름을 입력해주세요.");
			$("#userName").focus();
		}else{
			wsOpen();
			$("#yourName").hide();
			$("#yourMsg").show();
		}
	}

	function send() {
		var obj ={
			type: "message",
			sessionId : $("#sessionId").val(),
			userName : $("#userName").val(),
			msg : $("#chatting").val()
		}
		//서버에 데이터 전송
		ws.send(JSON.stringify(obj))
		$('#chatting').val("");
	}
</script>
```

### Script-Point
시작
```html
<script>
    function chatName(){
        var userName = $("#userName").val();
        if(userName == null || userName.trim() == ""){
            alert("사용자 이름을 입력해주세요.");
            $("#userName").focus();
        }else{
            wsOpen();
            $("#yourName").hide();
            $("#yourMsg").show();
        }
    }

    function wsOpen(){
        //websocket을 지정한 URL로 연결
        ws = new WebSocket("ws://" + location.host + "/chating");
        wsEvt();
    }

    function wsEvt() {
        //소켓이 열리면 동작
        ws.onopen = function(e){
            ...
        }
        
        //서버로부터 데이터 수신 (메세지를 전달 받음)
        ws.onmessage = function(e) {
            ...
        }
    }
</script>
```

데이터 수신
```html
<script>
    //서버로부터 데이터 수신 (메세지를 전달 받음)
    ws.onmessage = function(e) {
        //e 파라미터는 websocket이 보내준 데이터
        var msg = e.data; // 전달 받은 데이터
        if(msg != null && msg.trim() != ''){
            var d = JSON.parse(msg);
            
            //socket 연결시 sessionId 셋팅
            if(d.type == "getId"){
                var si = d.sessionId != null ? d.sessionId : "";
                if(si != ''){
                    $("#sessionId").val(si); 
                    
                    var obj ={
                        type: "open",
                        sessionId : $("#sessionId").val(),
                        userName : $("#userName").val()
                    }
                    //서버에 데이터 전송
                    ws.send(JSON.stringify(obj))
                }
            }
        }
    }
</script>
```