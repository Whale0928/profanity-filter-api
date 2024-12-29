package app.presentation;

import app.TestConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

@Import(TestConfig.class)
@WebMvcTest(ClientsController.class)
class ClientsControllerTest {

}
