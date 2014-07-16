package com.abstratt.kirra.tests.mdd.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.abstratt.kirra.Instance;
import com.abstratt.kirra.Repository;
import com.abstratt.kirra.Tuple;
import com.abstratt.kirra.TupleType;
import com.abstratt.kirra.TypeRef;
import com.abstratt.kirra.TypeRef.TypeKind;
import com.abstratt.kirra.mdd.rest.KirraRESTExternalService;
import com.abstratt.kirra.mdd.runtime.KirraMDDConstants;
import com.abstratt.mdd.frontend.web.JsonHelper;

public class KirraMDDRuntimeExternalServiceTests extends AbstractKirraMDDRuntimeTests {

    private static final int TEST_SERVER_PORT = 38080;
    private Server server;
    private Handler delegate;

    public KirraMDDRuntimeExternalServiceTests(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Server server = new Server(KirraMDDRuntimeExternalServiceTests.TEST_SERVER_PORT);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                delegate.handle(target, baseRequest, request, response);
            }
        });
        server.start();
        this.server = server;
    }

    public void testEvent() throws Exception {
        String source = "";
        source += "model tests;\n";
        source += "enumeration IssueStatus Open, Resolved, Closed end;\n";
        source += "signal IssueChange\n";
        source += "    attribute issueNumber : String;\n";
        source += "    attribute newStatus : IssueStatus;\n";
        source += "end;\n";
        source += "interface UserNotifier\n";
        source += "    reception issueChanged(change : IssueChange);\n";
        source += "end;\n";
        source += "external class EmailService implements UserNotifier\n";
        source += "end;\n";
        source += "component IssueTrackingComponent\n";
        source += "    composition emailService : EmailService;\n";
        source += "    provided port userNotifier : UserNotifier connector emailService;\n";
        source += "end;\n";
        source += "end.";
        parseAndCheck(source);

        this.externalService = new KirraRESTExternalService();

        final String[] requestPath = { null };
        final String[] requestQuery = { null };
        final String[] requestMethod = { null };
        final String[] requestEntity = { null };
        delegate = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                requestMethod[0] = request.getMethod();
                baseRequest.setHandled(true);
                requestPath[0] = URI.create(request.getRequestURI()).getPath();
                requestQuery[0] = request.getQueryString();
                requestEntity[0] = IOUtils.toString(request.getInputStream());
            }
        };

        Repository kirra = getKirra();

        TupleType tupleType = kirra.getTupleType("tests", "IssueChange");
        TestCase.assertNotNull(tupleType);

        Tuple event = new Tuple(new TypeRef("tests.IssueChange", TypeKind.Tuple));
        event.setValue("issueNumber", "CLOU-1234");
        event.setValue("newStatus", "Resolved");

        executeKirraOperation("tests", "EmailService", null, "issueChanged", Arrays.asList(event));
        TestCase.assertNotNull(requestPath[0]);
        TestCase.assertEquals(null, requestQuery[0]);
        TestCase.assertEquals("POST", requestMethod[0]);
        TestCase.assertEquals("/externalpath/events/tests.EmailService/issueChanged", requestPath[0]);

        TestCase.assertNotNull(requestEntity[0]);
        ObjectNode parsedEntity = JsonHelper.parse(new StringReader(requestEntity[0]));
        TestCase.assertEquals(Arrays.asList("typeName", "values"), IteratorUtils.toList(parsedEntity.getFieldNames()));
        TestCase.assertEquals(new TextNode("tests.IssueChange"), parsedEntity.get("typeName"));

        ObjectNode values = (ObjectNode) parsedEntity.get("values");

        List<String> fieldNames = IteratorUtils.toList(values.getFieldNames());
        Collections.sort(fieldNames);
        TestCase.assertEquals(Arrays.asList("issueNumber", "newStatus"), fieldNames);
    }

    public void testRetriever() throws Exception {
        String source = "";
        source += "model tests;\n";
        source += "class AdditionResult\n";
        source += "    attribute result : Integer;\n";
        source += "end;\n";
        source += "interface Calculator\n";
        source += "    operation addNumbers(number1 : Integer, number2 : Integer) : Integer;\n";
        source += "end;\n";
        source += "external class CalculatorService implements Calculator\n";
        source += "end;\n";
        source += "component CalculatorComponent\n";
        source += "    composition calculatorService : CalculatorService;\n";
        source += "    composition entities : Sum[*];\n";
        source += "    provided port calculator : Calculator connector calculatorService, entities.calculator;\n";
        source += "end;\n";
        source += "class Sum\n";
        source += "    attribute term1 : Integer;\n";
        source += "    attribute term2 : Integer;\n";
        source += "    attribute result : Integer[0,1];\n";
        source += "    required port calculator : Calculator;\n";
        source += "    operation compute();\n";
        source += "    begin\n";
        source += "        self.result := self.calculator.addNumbers(self.term1, self.term2);\n";
        source += "    end;\n";
        source += "end;\n";
        source += "end.";
        parseAndCheck(source);

        this.externalService = new KirraRESTExternalService();

        final String[] requestPath = { null };
        final String[] requestQuery = { null };
        final String[] requestMethod = { null };
        delegate = new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                requestMethod[0] = request.getMethod();
                baseRequest.setHandled(true);
                requestPath[0] = URI.create(request.getRequestURI()).getPath();
                requestQuery[0] = request.getQueryString();
                BigInteger result = new BigInteger(baseRequest.getParameter("number1")).add(new BigInteger(baseRequest
                        .getParameter("number2")));
                response.getWriter().println(result);
            }
        };

        Repository kirra = getKirra();

        Instance newInstance = new Instance();
        newInstance.setEntityName("Sum");
        newInstance.setEntityNamespace("tests");
        newInstance.setValue("term1", 31);
        newInstance.setValue("term2", 11);
        Instance created = kirra.createInstance(newInstance);

        executeKirraOperation(created.getEntityNamespace(), created.getEntityName(), created.getObjectId(), "compute",
                Collections.emptyList());
        TestCase.assertNotNull(requestPath[0]);
        TestCase.assertEquals("number1=31&number2=11", requestQuery[0]);
        TestCase.assertEquals("GET", requestMethod[0]);
        TestCase.assertEquals("/externalpath/retrievers/tests.CalculatorService/addNumbers", requestPath[0]);

        Instance reloaded = kirra.getInstance("tests", "Sum", created.getObjectId(), true);
        TestCase.assertEquals(42L, reloaded.getValue("result"));
    }

    @Override
    protected Properties createDefaultSettings() {
        Properties creationSettings = super.createDefaultSettings();
        creationSettings.setProperty(KirraMDDConstants.EXTERNAL_CONNECTOR_URI, "http://localhost:"
                + KirraMDDRuntimeExternalServiceTests.TEST_SERVER_PORT + "/externalpath/");
        return creationSettings;
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
        server.join();
        super.tearDown();
    }

}
