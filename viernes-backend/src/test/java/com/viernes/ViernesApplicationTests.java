package com.viernes;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test del contexto completo. Requiere Oracle arriba, por eso se marca
 * como test de integración y se excluye de `mvn test` por defecto.
 * Ejecutar con: mvn test -Dgroups=integration
 */
@Tag("integration")
@SpringBootTest
class ViernesApplicationTests {

	@Test
	void contextLoads() {
	}

}
