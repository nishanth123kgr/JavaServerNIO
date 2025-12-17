package com.server.protocol.http;

import com.server.Constants;
import com.server.transport.Poller;
import com.server.transport.SocketProcessor;
import com.server.transport.SocketWrapper;
import com.server.utils.BufferCache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;

public class HttpProcessor extends SocketProcessor {

    public HttpProcessor(SocketWrapper wrapper, Poller poller) {
        super(wrapper, poller);
    }

    @Override
    protected void processRequest(SocketWrapper socketWrapper, ByteBuffer readBuffer) {


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
                    ByteBuffer dataBuffer = BufferCache.getBufferFromCache();

                    if (dataBuffer == null) {
                        dataBuffer = ByteBuffer.allocate(Constants.KB_64);
                    }

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

    }

    Map<String, String> readQueryParamsFromRequestLines() {
        ByteBuffer readBuffer = super.socketWrapper.getReadBuffer();
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
        int bodyLength = (responseBuffers.size() - 1) * Constants.KB_64 + (lastBuffer.capacity() - lastBuffer.remaining());

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

}
