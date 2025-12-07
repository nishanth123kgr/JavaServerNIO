import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SocketProcessor implements Runnable {

    final SocketWrapper socketWrapper;

    private Poller poller;

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

    private ByteBuffer buildHttpResponse(int statusCode, String reason, String contentType, byte[] body) {
        String responseString = "HTTP/1.1 " + statusCode + " " + reason + "\r\n" + "Content-Type: " + contentType + "\r\n" + "Content-Length: " + body.length + "\r\n" + "Connection: close\r\n" + "\r\n";

        byte[] headerBytes = responseString.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + body.length);
        buffer.put(headerBytes);
        buffer.put(body);
        buffer.flip();
        return buffer;
    }

    void closeConnection() {
        poller.register(socketWrapper, -1);
    }

    @Override
    public void run() {
        if (readSocket()) {
            Map<String, String> queryParams = readQueryParamsFromRequestLines();

            ByteBuffer buffer;

            if (queryParams != null) {

                String responseBody, message;
                int statusCode;
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

                buffer = buildHttpResponse(statusCode, message, "text/html; charset=utf-8", responseBytes);
            } else {
                buffer = buildHttpResponse(400, "Bad Request", "text/html; charset=utf-8", "Invalid Request".getBytes(StandardCharsets.UTF_8));
            }

            socketWrapper.getResponseOutputQueue().add(buffer);
            poller.register(socketWrapper, SelectionKey.OP_WRITE);


        } else {
            closeConnection();
        }

    }
}
