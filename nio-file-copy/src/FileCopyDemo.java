import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

interface FileCopyRunner {
    void copyFile(File source,File target);
}

public class FileCopyDemo {

    private static final int ROUNDS = 5;
    private static void benchmark(FileCopyRunner test,File source,File target){
        long elapsed =0L;
        for (int i=0;i<ROUNDS;i++){
            long startTime = System.currentTimeMillis();
            test.copyFile(source,target);
            elapsed+=System.currentTimeMillis()-startTime;
            target.delete();
        }
        System.out.println(test+":"+elapsed/ROUNDS);
    }


    private static void close(Closeable closeable){
        if (closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin =null;
                OutputStream fout = null;
                try {
                    fin = new FileInputStream(source);
                    fout = new FileOutputStream(target);
                    int result ;
                    while ((result=fin.read())!=-1){
                        fout.write(result);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "noBufferStreamCopy";
            }
        };

        FileCopyRunner nioBufferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;

                try {
                    fin= new FileInputStream(source).getChannel();
                    fout = new FileInputStream(target).getChannel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    while (fin.read(buffer)!=-1){
                        buffer.flip();
                        while (buffer.hasRemaining()){
                            fout.read(buffer);
                        }
                        buffer.clear();
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "nioBufferCopy";
            }
        };

        FileCopyRunner nioTransferCopy = new FileCopyRunner() {

            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;
                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    long transferred =0L;
                    long size = fin.size();
                    while (transferred!=size){
                       transferred+=  fin.transferTo(0,size,fout);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "nioTransferCopy";
            }
        };
    }
}
