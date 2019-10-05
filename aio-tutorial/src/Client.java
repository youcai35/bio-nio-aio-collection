import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class Client {
    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;
    AsynchronousSocketChannel clientChannel;

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
        try {
            //创建连接
            clientChannel = AsynchronousSocketChannel.open();
            Future<Void> connect = clientChannel.connect(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            //等待用户的录入
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                String input = consoleReader.readLine();
                byte[] inputBytes = input.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(inputBytes);
                Future<Integer> writeResult = clientChannel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
