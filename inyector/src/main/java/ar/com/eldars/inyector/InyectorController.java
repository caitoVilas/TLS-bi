package ar.com.eldars.inyector;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/inyector")
public class InyectorController {

    @GetMapping("/test")
    public String test() {
        return "Inyector response OK - TLS secured";
    }

    @GetMapping("/key")
    public byte[] getKey() {
        try {
            return Files.readAllBytes(Paths.get("D:\\certificates\\server\\server.cer"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
