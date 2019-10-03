package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ChatHandler implements Runnable{
    private  ChatServer server;
    private Socket socket;
    public ChatHandler(ChatServer server,Socket socket){
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //存储新先上线用户
            server.addClient(socket);
            //读取用户发送的消息
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg =null;
            while ((msg=reader.readLine())!=null){
                String fwdMsg = "客户端["+socket.getPort()+"]"+msg +"\n";
                System.out.println(fwdMsg);
                //将消息转发给聊天室里在线其他用户
                server.forwardMessage(socket,fwdMsg);
                //检测用户是否准备退出
                if (server.readyToQuit(msg)){
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                server.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
