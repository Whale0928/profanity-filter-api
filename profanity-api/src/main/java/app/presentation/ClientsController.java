package app.presentation;

import app.application.apikey.ClientsCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clients")
public class ClientsController {
    private final ClientsCommandService clientsCommandService;

    @PostMapping("/register")
    public void registerClient() {
    }
}
