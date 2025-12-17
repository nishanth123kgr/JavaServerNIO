import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;

public class SocketProcessor implements Runnable {

    static int KB_64 = 6 * 1024;

    final SocketWrapper socketWrapper;

    private final Poller poller;

    SocketProcessor(SocketWrapper wrapper, Poller poller) {
        this.socketWrapper = wrapper;
        this.poller = poller;
    }

    boolean readSocket() {
        try {
            SocketChannel channel = socketWrapper.getChannel();
            ByteBuffer readBuffer = socketWrapper.getReadBuffer();

            readBuffer.clear();

            int totalBytesRead = 0;
            while (true) {
                int r = channel.read(readBuffer);
                if (r > 0) {
                    totalBytesRead += r;
                } else if (r == 0) {
                    break;
                } else {
                    closeConnection();
                    return false;
                }
            }

            return totalBytesRead > 0;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    Map<String, String> readQueryParamsFromRequestLines() {
        ByteBuffer readBuffer = socketWrapper.getReadBuffer();
        readBuffer.flip();

        byte[] data = new byte[readBuffer.remaining()];

        readBuffer.get(data);

        String requestString = new String(data, StandardCharsets.UTF_8);

        System.out.println("-------------------------------------------");
        System.out.println("-------------Request Received--------------");
        System.out.println("-------------------------------------------");
        System.out.println(requestString);
        System.out.println("-------------------------------------------");
        System.out.println("--------------Parsing Request--------------");
        System.out.println("-------------------------------------------");

        String[] requestLines = requestString.split("\r\n");
        String[] requestLine = requestLines[0].split(" ");
        System.out.println("METHOD: " + requestLine[0]);
        System.out.println("-------------------------------------------");
        System.out.println("PATH: " + requestLine[1]);
        System.out.println("-------------------------------------------");
        System.out.println("PROTOCOL: " + requestLine[2]);
        System.out.println("-------------------------------------------");

        Map<String, String> queryParams = new HashMap<>();

        String path = requestLine[1];

        if (path.contains("?")) {
            String[] queryStringArray = path.split("\\?");
            if (queryStringArray.length < 2) return null;

            String[] query = queryStringArray[1].split("&");

            for (String q : query) {
                String[] param = q.split("=");
                queryParams.put(param[0], param[1]);
            }

        } else {
            return null;
        }

        return queryParams;
    }

    private void buildHttpResponse(SocketWrapper socketWrapper, int statusCode, String reason, ConcurrentLinkedDeque<ByteBuffer> responseBuffers) {

        ByteBuffer lastBuffer = responseBuffers.getLast();
        int bodyLength = (responseBuffers.size() - 1) * KB_64 + (lastBuffer.capacity() - lastBuffer.remaining());

        String responseString = "HTTP/1.1 " + statusCode + " " + reason + "\r\n" + "Content-Type: text/html; charset=utf-8" + "\r\n" + "Content-Length: " + bodyLength + "\r\n" + "Connection: close\r\n" + "\r\n";

        byte[] headerBytes = responseString.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer headerBuffer = ByteBuffer.allocate(headerBytes.length).put(headerBytes);
        headerBuffer.flip();
        ConcurrentLinkedDeque<ByteBuffer> responseQueue = socketWrapper.getResponseOutputQueue();

        responseQueue.add(headerBuffer);

        for (ByteBuffer response : responseBuffers) {
            response.flip();
            responseQueue.add(response);
        }


    }

    void closeConnection() {
        poller.register(socketWrapper, -1);
    }


    @Override
    public void run() {
        if (readSocket()) {
            Map<String, String> queryParams = readQueryParamsFromRequestLines();

            ConcurrentLinkedDeque<ByteBuffer> responseBytesQueue = new ConcurrentLinkedDeque<>();

            String message;
            int statusCode;

            if (queryParams != null) {

                String responseBody;
                if (queryParams.containsKey("name")) {
                    responseBody = "<h3>Hello <i>" + queryParams.get("name") + "</i></h3><br><br>";
                    message = "OK";
                    statusCode = 200;
                } else {

                    responseBody = "<h4>No Name Found<h4><br><br>";
                    message = "Not Found";
                    statusCode = 404;
                }
                responseBody = responseBody + queryParams;

                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

                responseBytesQueue.add(ByteBuffer.allocate(responseBytes.length).put(responseBytes));


            } else {

                Path indexPath = Paths.get(System.getProperty("user.dir"), "static", "index.html");
                try (AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(indexPath, StandardOpenOption.READ)) {


                    int totalBytesRead = 0, bytesRead;
                    do {
                        ByteBuffer dataBuffer = ByteBuffer.allocate(KB_64);
                        bytesRead = fileChannel.read(dataBuffer, totalBytesRead).get();
                        totalBytesRead += bytesRead;

                        if (bytesRead > 0) {
                            responseBytesQueue.add(dataBuffer);
                        }


                    } while (bytesRead > 0);


                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }


                message = "OK";
                statusCode = 200;

            }

            buildHttpResponse(socketWrapper, statusCode, message, responseBytesQueue);
            poller.register(socketWrapper, SelectionKey.OP_WRITE);


        } else {
            closeConnection();
        }

    }
}
