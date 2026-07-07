package ru.yandex.practicum.mybankfront;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"spring.config.import=",
		"eureka.client.enabled=false"
})
class MyBankFrontAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
