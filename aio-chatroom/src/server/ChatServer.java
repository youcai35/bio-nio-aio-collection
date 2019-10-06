package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;

    private List<ClientHandler> connectedClients;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer(){
        this(DEFAULT_PORT);
    }
    public ChatServer(int port){
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }

    private boolean readyToQuit(String msg){
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable){
        if (closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void start(){
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            serverChannel.bind(new InetSocketAddress(LOCALHOST,port));
            System.out.println("启动服务器，监听端口:"+port);

            while (true){
                serverChannel.accept(null,new AcceptHandler());
                System.in.read();
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            close(serverChannel);
        }

    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel,Object>{

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            if (serverChannel.isOpen()){
                serverChannel.accept(null,this);
            }
            if (clientChannel!=null&&clientChannel.isOpen()){
                ClientHandler  handler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
                //将新用户添加到在线用户列表
                addClient(handler);
                clientChannel.read(buffer,buffer,handler);
            }
        }

        private synchronized void addClient(ClientHandler handler) {
            connectedClients.add(handler);
            System.out.println(getClientName(handler.clientChannel)+"已连接到服务器");
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败"+exc);
        }


    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }

    private class ClientHandler implements CompletionHandler<Integer,Object> {
        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel channel){
            this.clientChannel = channel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            ByteBuffer buffer = (ByteBuffer) attachment;
            if (buffer!=null){
                if (result<=0){
                    //客户端异常
                    //将客户移除在线客户列表
                    removeClient(this);
                }else {
                    buffer.flip();
                    String fwdMsg = receive(buffer);
                    System.out.println(getClientName(clientChannel) +":"+fwdMsg);
                    forwardMessage(clientChannel,fwdMsg);
                    buffer.clear();
                    //检查用户是是否退出
                    if (readyToQuit(fwdMsg)){
                        removeClient(this);
                    }else {
                        clientChannel.read(buffer,buffer,this);
                    }
                }
            }
        }

        private synchronized void forwardMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {
            for (ClientHandler handler:connectedClients){
                if (!clientChannel.equals(handler.clientChannel)){
                    ByteBuffer buffer = charset.encode(getClientName(handler.clientChannel) + ":" + fwdMsg);
                    handler.clientChannel.write(buffer,null,handler);
                }
            }
        }

        private synchronized void removeClient(ClientHandler handler) {
            connectedClients.remove(handler);
            System.out.println(getClientName(handler.clientChannel)+"已断开连接");
            close(handler.clientChannel);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("读写失败："+exc);
        }
    }

    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort =-1;
        try {
            InetSocketAddress address = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = address.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "客户端["+clientPort+"]";
    }

    private String receive(ByteBuffer buffer) {
        CharBuffer charBuffer = charset.decode(buffer);
        return String.valueOf(charBuffer);
    }
}
