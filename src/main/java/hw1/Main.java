package hw1;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(System.getProperty("port", "8080"));
        Map<String, String> storage = new HashMap<>();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        HttpHandler httpHandler = exchange -> {
            try {
                System.out.println("processing " + exchange.getRequestURI());
                String query1 = exchange.getRequestURI().getQuery();
                System.out.println(query1);
                Map<String, String> requestParams = Stream.of(Optional.ofNullable(query1))
                        .flatMap(query -> Arrays.stream(query.get().split("&")))
                        .filter(param -> param.indexOf("=") > 0)
                        .collect(Collectors.toMap(
                                param -> param.substring(0, param.indexOf("=")),
                                param -> param.substring(param.indexOf("=") + 1)
                        ));
                System.out.println(requestParams.size());


                String resp = doBusiness(requestParams);

                String s = String.format("<html><body><h1>%s</h1></body></html>", resp);

                exchange.sendResponseHeaders(200, s.length());
                exchange.getResponseBody().write(s.getBytes());
                exchange.close();
                System.out.println(exchange.getRequestURI() + " processed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        HttpHandler handlerWithoutKey = methodWork -> {
            try {
                String reqMethod = methodWork.getRequestMethod();
                if (reqMethod.equalsIgnoreCase("POST")) {
                    String body = new BufferedReader(new InputStreamReader(methodWork.getRequestBody()))
                            .lines().collect(Collectors.joining("\n"));
                    String s;
                    if (!storage.containsValue(body)) {
                        storage.put(Long.toString(System.currentTimeMillis()), body);
                        s = "Content was successfully saved";
                    } else {
                        String resp = getKeyByValue(storage, body);
                        s = String.format("this value has following key: %s", resp);
                    }
                    methodWork.sendResponseHeaders(200, s.length());
                    methodWork.getResponseBody().write(s.getBytes());
                }
                if (reqMethod.equalsIgnoreCase("GET")) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Map.Entry<String, String> entry : storage.entrySet()) {
                        stringBuilder.append("key: " + entry.getKey() + " ,values is " + entry.getValue() + "\n");
                    }
                    methodWork.sendResponseHeaders(200, stringBuilder.length());
                    methodWork.getResponseBody().write(stringBuilder.toString().getBytes());
                }

                methodWork.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        HttpHandler handlerByKey = methodWorkByKey -> {
            try {
                String key = methodWorkByKey.getRequestURI().toString();
                String[] path = key.split("/");
                key = path[path.length - 1];
                System.out.println();
                System.out.println(key);
                String reqMethod = methodWorkByKey.getRequestMethod();
                String result;
                if (reqMethod.equalsIgnoreCase("GET")) {
                    result = "For this key,there is following value:" + storage.get(key);
                    methodWorkByKey.sendResponseHeaders(200, result.length());
                    methodWorkByKey.getResponseBody().write(result.getBytes());
                }
                if (reqMethod.equalsIgnoreCase("DELETE")) {
                    String value = storage.get(key);
                    storage.remove(key);
                    result = "The following value was deleted : " + value;
                    methodWorkByKey.sendResponseHeaders(200, result.length());
                    methodWorkByKey.getResponseBody().write(result.getBytes());
                }
                if (reqMethod.equalsIgnoreCase("POST")) {
                    String body = new BufferedReader(new InputStreamReader(methodWorkByKey.getRequestBody()))
                            .lines().collect(Collectors.joining("\n"));
                    if (!storage.containsKey(key)) {
                        result = "value by key " + key + " was not found.";
                    } else {
                        storage.put(key, body);
                        result = "Value by key " + key + " was changed";
                    }
                    methodWorkByKey.sendResponseHeaders(200, result.length());
                    methodWorkByKey.getResponseBody().write(result.getBytes());
                }
                methodWorkByKey.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        httpServer.createContext("/app", httpHandler);
        httpServer.createContext("/app/txts", handlerWithoutKey);
        httpServer.createContext("/app/txts/", handlerByKey);
        httpServer.start();
    }

    private static String doBusiness(Map<String, String> requestParams) {
        String recipient = requestParams.getOrDefault("name", "world");
        return String.format("Hello, %s!", recipient);
    }


    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

}
