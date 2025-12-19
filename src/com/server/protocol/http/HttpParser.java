package com.server.protocol.http;

import com.server.protocol.Parser;
import com.server.protocol.Protocol;
import com.server.protocol.Request;
import com.server.transport.SocketWrapper;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class HttpParser implements Parser {


    private static boolean isNotSpace(ByteBuffer readBuffer, int index) {
        return (char) readBuffer.get(index) != ' ';
    }

    private static boolean isNotCRLFStart(ByteBuffer readBuffer, int index) {
        return (index + 1 < readBuffer.limit()) && ((char) readBuffer.get(index) != '\r') && ((char) readBuffer.get(index + 1) != '\n');
    }

    @Override
    public Request parse(SocketWrapper socketWrapper) {
        HttpRequest request = new HttpRequest(Protocol.HTTP);

        ByteBuffer readBuffer = socketWrapper.getReadBuffer();

        int index = 0;

        StringBuilder method = new StringBuilder(), path = new StringBuilder(), version = new StringBuilder();

        index = readIntoStringBuilder(readBuffer, index, method, HttpParser::isNotSpace);

        request.setMethod(method.toString());

        index++;

        while ((char) readBuffer.get(index) != '?' && isNotSpace(readBuffer, index)) {
            path.append((char) readBuffer.get(index++));
        }

        request.setUri(path.toString());

        Map<String, String> queryParams = new HashMap<>();

        if ((char) readBuffer.get(index) == '?') {
            index++;
            while (isNotSpace(readBuffer, index)) {
                int[] values = readParams(index, readBuffer, queryParams);
                index = values[0] - 1;
            }
        }

        index++;


        index = readIntoStringBuilder(readBuffer, index, version, HttpParser::isNotCRLFStart);

        index += 2;

        request.setVersion(version.toString());

        Map<String, String> headers = new HashMap<>();


        while (true) {
            if (!isNotCRLFStart(readBuffer, index)) {
                index += 2;
                break;
            }

            StringBuilder headerName = new StringBuilder();

            index = readIntoStringBuilder(readBuffer, index, headerName, ((bytes, i) -> (char) bytes.get(i) != ':'));

            index += 2;

            StringBuilder headerValue = new StringBuilder();

            index = readIntoStringBuilder(readBuffer, index, headerValue, HttpParser::isNotCRLFStart);

            index += 2;

            headers.put(headerName.toString(), headerValue.toString());
        }

        request.setHeaders(headers);

        int contentLength = headers.containsKey("Content-Length") ? Integer.parseInt(headers.get("Content-Length")) : 0;

        Map<String, String> requestBody = new HashMap<>();

        while (contentLength > 0) {

            int[] values = readParams(index, readBuffer, requestBody);

            index = values[0];
            contentLength -= values[1];
        }

        request.setRequestBody(requestBody);

        if (headers.containsKey("Cookie")) {
            parseCookies(request, headers.get("Cookie"));
        }

        return request;
    }

    private static int[] readParams(int index, ByteBuffer readBuffer, Map<String, String> params) {
        StringBuilder bodyKey = new StringBuilder();

        index = readIntoStringBuilder(readBuffer, index, bodyKey, ((bytes, i) -> (char) bytes.get(i) != '='));

        index++;

        StringBuilder bodyValue = new StringBuilder();

        while ((char) readBuffer.get(index) != '&' && isNotCRLFStart(readBuffer, index) && isNotSpace(readBuffer, index)) {
            bodyValue.append((char) readBuffer.get(index++));
        }

        index++;

        params.put(URLDecoder.decode(bodyKey.toString(), StandardCharsets.UTF_8), URLDecoder.decode(bodyValue.toString(), StandardCharsets.UTF_8));
        return new int[]{index, bodyValue.length() + bodyKey.length() + 2};
    }

    private static int readIntoStringBuilder(ByteBuffer readBuffer, int index, StringBuilder stringBuilder, BiPredicate<ByteBuffer, Integer> validation) {
        while (validation.test(readBuffer, index)) {
            stringBuilder.append((char) readBuffer.get(index++));
        }
        return index;
    }

    private static void parseCookies(HttpRequest request, String cookieString) {
        String[] cookiesArray = cookieString.split("; ");
        HttpCookie[] httpCookies = new HttpCookie[cookiesArray.length];
        for (int i = 0; i < cookiesArray.length; i++) {
            String cookie = cookiesArray[i];
            String[] cookieSplit = cookie.split("=");
            HttpCookie httpCookie = new HttpCookie.Builder().setName(cookieSplit[0]).setValue(cookieSplit[1]).build();
            httpCookies[i] = httpCookie;
        }

        request.setCookies(httpCookies);
    }

}
