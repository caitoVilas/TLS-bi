package ar.com.eldars.client1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/client1")
@RequiredArgsConstructor
public class Client1Controller {
    private final WebClient webClient;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return webClient.get()
                .uri("https://localhost:50090/inyector/test")
                .retrieve()
                .toEntity(String.class)
                .block();
    }

    @GetMapping("/key")
    public ResponseEntity<byte[]> getKey() {

        return webClient.get()
                .uri("https://localhost:50090/inyector/key")
                .retrieve()
                .toEntity(byte[].class)
                .block();
    }
}
