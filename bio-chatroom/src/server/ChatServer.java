package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ChatServer {
    private int DEFAULT_PORT =8888;
    private final String QUIT = "quit";

    private ServerSocket serverSocket;
    private Map<Integer, Writer> connectedClients;
    private ExecutorService executorService;

    public ChatServer(){
        connectedClients = new HashMap<>();
    }

    public synchronized void addClient(Socket socket) throws IOException {
        if (socket!=null){
            int port =socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port,writer);
            System.out.println("客户端["+port+"]已经连接到服务器");
        }
    }

    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket!=null){
            int port = socket.getPort();
            if (connectedClients.containsKey(port)){
                connectedClients.get(port).close();
                System.out.println("客户端["+port+"]已经断开连接");
            }
        }
    }
    public synchronized void forwardMessage(Socket socket,String fwdMsg) throws IOException {
        for (Integer id: connectedClients.keySet()){
            if (!id.equals(socket.getPort())){
                Writer writer = connectedClients.get(id);
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }

    public synchronized void close(){
        if (serverSocket!=null){
            try {
                serverSocket.close();
                System.out.println("关闭serverSocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public boolean readyToQuit(String msg){
        return QUIT.equals(msg);
    }

    public void start(){
        //绑定监听端口
        try {
            serverSocket= new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口:"+DEFAULT_PORT+"...");
            while (true){
                //等待客户端连接
                Socket socket = serverSocket.accept();
                //创建ChatHandler线程
                executorService.execute(new ChatHandler(this,socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
