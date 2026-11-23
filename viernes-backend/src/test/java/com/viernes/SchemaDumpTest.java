package com.viernes;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@org.junit.jupiter.api.Disabled("Utilidad manual: genera target/db/schema.sql a partir de las entidades")
class SchemaDumpTest {

    @Test
    void dumpSchema() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Entity.class));
        var entityClasses = scanner.findCandidateComponents("com.viernes").stream()
                .map(bd -> {
                    try {
                        return Class.forName(bd.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();
        System.out.println("Entidades encontradas: " + entityClasses.size());

        Files.createDirectories(Path.of("target/db"));
        String target = Path.of("target/db/schema.sql").toAbsolutePath().toString();

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .build();

        MetadataSources sources = new MetadataSources(registry);
        entityClasses.forEach(sources::addAnnotatedClass);
        var metadata = sources.buildMetadata();

        Map<String, Object> settings = new HashMap<>();
        settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
        settings.put("jakarta.persistence.schema-generation.scripts.create-target", target);
        settings.put("hibernate.hbm2ddl.auto", "none");

        SchemaManagementToolCoordinator.process(metadata, registry, settings, null);
        System.out.println("DDL generado en: " + target);
    }
}
