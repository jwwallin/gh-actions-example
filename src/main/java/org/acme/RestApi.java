package org.acme;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.jackson.ListJacksonDataFormat;
import org.apache.camel.model.rest.RestBindingMode;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@ApplicationScoped
public class RestApi extends EndpointRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(RestApi.class);

    @Location("todos.html")
    Template todosTemplate;

    ListJacksonDataFormat todosDataFormat = new ListJacksonDataFormat(Todo.class);

    private static final String PDF_RESOURCES_PATH = RestApi.class.getClassLoader().getResource("/pdf-resources/").toExternalForm();

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
                    var document = Jsoup.parse(html);
                    document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

                    var outputStream = new ByteArrayOutputStream();
                    var builder = new PdfRendererBuilder();
                    builder.toStream(outputStream);
                    builder.withW3cDocument(new W3CDom().fromJsoup(document), PDF_RESOURCES_PATH);

                    try {
                        builder.run();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to render PDF.", e);
                    }
                    return outputStream.toByteArray();
                })
                .setHeader(Exchange.CONTENT_TYPE, constant("application/pdf"))
        ;
    }
}
