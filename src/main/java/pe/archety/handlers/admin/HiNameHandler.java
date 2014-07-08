package pe.archety.handlers.admin;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import static pe.archety.ArchetypeServer.TEXT_PLAIN;

public class HiNameHandler implements HttpHandler {
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN);
        String name = exchange.getQueryParameters().get("name").getFirst();

        exchange.getResponseSender().send("Hi " + name);
    }
}