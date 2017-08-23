package be.jschoreels.apache.camel.lab;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Optional;
import java.util.Properties;

import static org.mockito.Mockito.*;


/**
 *  Goal : To test a RouteBuilder (coded here for the example) without the need to create a blueprint context.
 *  1. Inject Bean in custom registry
 *  2. Inject Properties in context
 *  3. Run it !
 */
public class CamelStandaloneTest extends CamelTestSupport {

    Processor processorTest = mock(Processor.class);

    /* Simple route. The goal is to inject the processorTest and the 2 uri as properties in camel context */
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("{{input.uri}}")
                    .to("bean:processorTest")
                    .to("{{output.uri}}");
            }
        };
    }
    @Test
    public void testRegistry() throws Exception {
        ProducerTemplate producerTemplate = context.createProducerTemplate();
        producerTemplate.send("direct:start", exchange -> exchange.getIn().setBody("dummy body"));
        final MockEndpoint mockEndpoint = getMockEndpoint("mock:result");

        verify(processorTest, times(1)).process(any(Exchange.class));
        verifyNoMoreInteractions(processorTest);

        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.assertIsSatisfied();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final Properties properties =
            Optional.ofNullable(super.useOverridePropertiesWithPropertiesComponent())
                .orElse(new Properties()); // Looks like by default it's null !
        properties.put("input.uri", "direct:start");
        properties.put("output.uri", "mock:result"); // mock:result will be the final endpoint of our route
        return properties;
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        final JndiRegistry registry = super.createRegistry();
        registry.bind("processorTest", processorTest); // inject the bean into registry like blueprint would have done
        return registry;
    }

}