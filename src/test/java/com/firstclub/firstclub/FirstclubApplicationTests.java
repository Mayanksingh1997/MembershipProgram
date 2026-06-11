package com.firstclub.firstclub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.docker.compose.enabled=false"
})
class FirstclubApplicationTests {

	@Test
	void contextLoads() {
	}

}
