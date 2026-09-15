package edu.eci.arem.lab2;

import edu.eci.arem.lab2.http.HttpRequest;
import edu.eci.arem.lab2.http.HttpRequestParser;
import edu.eci.arem.lab2.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestParserTest {

    @Test
    void parsesPathAndQueryParams() throws IOException, HttpRequestParser.MalformedRequestException {
        String raw = "GET /api/square?value=7 HTTP/1.1\r\nHost: localhost\r\n\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(raw));

        HttpRequest request = HttpRequestParser.parse(reader);

        assertEquals("GET", request.getMethod());
        assertEquals("/api/square", request.getPath());
        assertEquals("7", request.getQueryParams().get("value"));
    }

    @Test
    void decodesUrlEncodedQueryParams() throws IOException, HttpRequestParser.MalformedRequestException {
        String raw = "GET /api/greeting?name=Ada%20Lovelace HTTP/1.1\r\n\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(raw));

        HttpRequest request = HttpRequestParser.parse(reader);

        assertEquals("Ada Lovelace", request.getQueryParams().get("name"));
    }

    @Test
    void rejectsMalformedRequestLine() {
        String raw = "NOT A VALID REQUEST LINE AT ALL\r\n\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(raw));

        assertThrows(HttpRequestParser.MalformedRequestException.class,
                () -> HttpRequestParser.parse(reader));
    }

    @Test
    void rejectsEmptyRequest() {
        BufferedReader reader = new BufferedReader(new StringReader(""));
        assertThrows(HttpRequestParser.MalformedRequestException.class,
                () -> HttpRequestParser.parse(reader));
    }

    @Test
    void jsonUtilEscapesQuotesAndBackslashes() {
        String escaped = JsonUtil.escape("he said \"hi\" \\ ok");
        assertTrue(escaped.contains("\\\""));
        assertTrue(escaped.contains("\\\\"));
    }
}
