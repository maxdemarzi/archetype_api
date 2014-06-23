package pe.archety.handlers.admin;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import static pe.archety.ArchetypeServer.TEXT_PLAIN;

public class HelloNameHandler implements HttpHandler {
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN);

        String name = exchange.getAttachment(io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY)
                .getParameters().get("name");

        exchange.getResponseSender().send("Hello " + name);
    }
}


/*
        If you set the 'rewriteQueryParameters' option to true in the RoutingHandler
        then they will be available in the exchange query parameters.

        Otherwise they can be retrieved from the exchange as an attachment under
        the io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY key.

*/
