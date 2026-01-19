package itsi.api.steuerung;

import itsi.api.steuerung.service.CedarService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SteuerungApplicationTests {

    @MockitoBean
    private CedarService cedarService;

    @Test
    void contextLoads() {
    }

}
