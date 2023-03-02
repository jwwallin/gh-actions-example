package org.acme;

import de.redsix.pdfcompare.PdfComparator;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RestApiTest {

    private static byte[] todosPdfCorrect;
    private static byte[] todosPdfWithMistake;

    private static Client client;

    private static final String HOST = "http://localhost:8081";

    @BeforeAll
    public static void loadTestResources() throws Exception {
        todosPdfCorrect = readClassPathFile("_files/todos_correct.pdf");
        todosPdfWithMistake = readClassPathFile("_files/todos_w_mistake.pdf");
    }

    @BeforeAll
    public static void setupHttpClient() throws Exception {
        client = ClientBuilder.newBuilder()
                .build();
    }

    @AfterAll
    public static void closeHttpClient() throws Exception {
        client.close();
    }

    @Test
    public void testGettingFile() throws IOException {
        Response res = client.target(HOST + "/todos").request().get();
        assertEquals(200, res.getStatus());
        var responseData = res.readEntity(InputStream.class);
        var result = new PdfComparator<>(new ByteArrayInputStream(todosPdfCorrect), responseData).compare();
        assertTrue(result.isEqual());
    }

    /**
     * This test fails on purpose.
     */
    @Test
    public void testGettingFile2() throws IOException {
        Response res = client.target(HOST + "/todos").request().get();
        assertEquals(200, res.getStatus());
        var responseData = res.readEntity(InputStream.class);
        var result = new PdfComparator<>(new ByteArrayInputStream(todosPdfWithMistake), responseData).compare();
        assertTrue(result.isNotEqual());
    }

    private static byte[] readClassPathFile(String path) throws IOException {
        var url = Objects.requireNonNull(RestApiTest.class.getClassLoader().getResource(path));
        return ClassPathUtils.readStream(url, is -> {
            try {
                return is.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
