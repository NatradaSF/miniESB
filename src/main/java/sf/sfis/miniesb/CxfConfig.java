package sf.sfis.miniesb;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.xml.ws.Endpoint;
import sf.sfis.miniesb.service.ESBRequestService;
import sf.sfis.miniesb.service.SubscribeRequestService;

@Configuration
public class CxfConfig {
    @Bean
    public Endpoint endpointSubscribe(SubscribeRequestService service, Bus bus) {
        EndpointImpl endpoint = new EndpointImpl(bus, service);
        endpoint.publish("/subscribe"); // WSDL at /services/subscribe?wsdl
        return endpoint;
    }
    
    @Bean
    public Endpoint endpointRequestInbound(ESBRequestService service, Bus bus) {
        EndpointImpl endpoint = new EndpointImpl(bus, service);
        endpoint.publish("/request"); // WSDL at /services/request?wsdl
        return endpoint;
    }
}