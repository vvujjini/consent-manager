package in.org.projecteka.hdaf.link;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.*;

import java.io.IOException;
import java.util.List;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientRegistryClientTest {
    @Captor
    ArgumentCaptor<ClientRequest> captor;

    @Mock
    private ExchangeFunction exchangeFunction;

    ClientRegistryClient clientRegistryClient;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(exchangeFunction);
        ClientRegistryProperties clientRegistryProperties = new ClientRegistryProperties("localhost:8000", "", "");
        clientRegistryClient = new ClientRegistryClient(webClientBuilder, clientRegistryProperties);
    }


    @Test
    void getProvidersByGivenName() throws IOException {
        var source = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(
                ClassLoader.getSystemClassLoader().getResource("provider.json"),
                new TypeReference<List<Provider>>(){});
        var jsonNode = new ObjectMapper().readValue(
                ClassLoader.getSystemClassLoader().getResource("provider.json"),
                new TypeReference<List<JsonNode>>(){});

        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(jsonNode.toString()).build()));

        StepVerifier.create(clientRegistryClient.providersOf("Max"))
                .assertNext(provider -> {
                    assertThat(provider.getName()).isEqualTo(source.get(0).getName());
                    assertThat(provider.getAddresses().get(0).getCity()).isEqualTo(source.get(0).getAddresses().get(0).getCity());
                    assertThat(provider.getTelecoms().get(0).getValue()).isEqualTo(source.get(0).getTelecoms().get(0).getValue());
                    assertThat(provider.getTypes().get(0).getCoding().get(0).getCode()).isEqualTo(source.get(0).getTypes().get(0).getCoding().get(0).getCode());
                })
                .verifyComplete();

        assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers?name=Max");
    }
}