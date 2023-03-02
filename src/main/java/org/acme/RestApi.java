package org.acme;

import com.lowagie.text.DocumentException;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.jackson.ListJacksonDataFormat;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;

@ApplicationScoped
public class RestApi extends EndpointRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(RestApi.class);

    @Location("todos.html")
    Template todosTemplate;

    ListJacksonDataFormat todosDataFormat = new ListJacksonDataFormat(Todo.class);

    @Override
    public void configure() throws Exception {
        restConfiguration()
                .bindingMode(RestBindingMode.off);

        rest()
                .get("/todos")
                .description("This endpoint gets the data from https://jsonplaceholder.typicode.com/todos and returns it formatted as a PDF.")
                .produces("application/pdf")
                .to(direct("todosPdf").getUri());

        from(direct("todosPdf"))
                .routeId("todosPdfRoute")
                .autoStartup(true)
                .removeHeaders("*")
                .to(https("{{source.host}}{{source.path}}"))
                .unmarshal(todosDataFormat)
                .setBody(ex -> {
                    String html = todosTemplate.data("todos", ex.getMessage().getBody()).render();
                    var outputStream = new ByteArrayOutputStream();
                    var renderer = new ITextRenderer(20f * 4f / 3f, 20);
                    renderer.setDocumentFromString(html);
                    renderer.layout();
                    try {
                        renderer.createPDF(outputStream);
                    } catch (DocumentException e) {
                        throw new RuntimeException("Failed to render PDF.", e);
                    }
                    return outputStream.toByteArray();
                })
                .setHeader(Exchange.CONTENT_TYPE, constant("application/pdf"))
        ;
    }
}
