package ru.hotdog.multicam_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.r2dbc.url=r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1",
		"spring.r2dbc.username=sa",
		"spring.r2dbc.password=",
		"spring.sql.init.mode=never",
		"llm.api.key=test",
		"deepseek.api.key=test",
		"app.secret=01234567890123456789012345678901"
})
class MulticamApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
