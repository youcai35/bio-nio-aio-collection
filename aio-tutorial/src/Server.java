import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
public class Server {
    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;
    AsynchronousServerSocketChannel serverSocketChannel;

    private void close(Closeable closeable){
        if (closeable!=null){
            try {
                closeable.close();
                System.out.println("关闭"+closeable);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start(){
        // 绑定监听端口
        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(LOCALHOST,DEFAULT_PORT));
            System.out.println("启动服务器，监听端口:"+DEFAULT_PORT);
            while (true){
                serverSocketChannel.accept(null,new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close(serverSocketChannel);
        }

    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel,Object>{

        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            if (serverSocketChannel.isOpen()){
                serverSocketChannel.accept(null,this);
            }
            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel!=null&& clientChannel.isOpen()){
                ClientHandler handler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Map<String,Object> info = new HashMap<>();
                info.put("type","read");
                info.put("buffer",buffer);
                clientChannel.read(buffer,info,handler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            //处理错误
        }
    }

    private class ClientHandler implements CompletionHandler<Integer,Object>{
        private AsynchronousSocketChannel clientChannel;
        public ClientHandler(AsynchronousSocketChannel channel){
            this.clientChannel = channel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            Map<String,Object> info = (Map<String, Object>) attachment;
            String type = (String) info.get("type");
            if ("read".equals(type)){
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                buffer.flip();
                info.put("type","write");
                clientChannel.write(buffer,info,this);
                buffer.clear();
            } else if ("write".equals(type)) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                info.put("type","read");
                info.put("buffer",buffer);
                clientChannel.read(buffer,info,this);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            //处理错误
        }
    }
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
