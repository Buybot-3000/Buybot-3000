package com.eve.buy.bot.backend.domain.eve;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueft, dass die Entitaeten der Statikdatenbank auf Tabellen zeigen, die der SDE-Import
 * auch wirklich anlegt.
 *
 * <p>Dieser Abgleich hat einen eigenen Test verdient, weil ihn sonst nichts bemerkt: in den
 * uebrigen Tests sind die Repositories gemockt, und Hibernate legt eine falsch benannte
 * Tabelle beim Start stillschweigend leer an. Nach aussen sieht das nicht nach einem Fehler
 * aus, sondern so, als gaebe es die gesuchte Kategorie in EVE nicht.
 */
@DisplayName("Abbildung auf die EVE-Statikdatenbank")
class SdeTableMappingTest {

    /** Das Importskript ist die Wahrheit darueber, welche Tabellen es gibt. */
    private static final Path IMPORT_SCRIPT = Path.of("eve_sde", "update_sde.py");

    private static final String ENTITY_PACKAGE = "com.eve.buy.bot.backend.domain.eve.entity";

    @Test
    @DisplayName("jede Entitaet zeigt auf eine Tabelle, die der Import anlegt")
    void everyEntityPointsToAnImportedTable() throws Exception {
        Set<String> importedTables = importedTableNames();
        assertThat(importedTables)
                .as("Tabellenliste aus %s gelesen", IMPORT_SCRIPT)
                .isNotEmpty();

        List<Class<?>> entities = sdeEntities();
        assertThat(entities).as("Entitaeten in %s", ENTITY_PACKAGE).isNotEmpty();

        for (Class<?> entity : entities) {
            Table table = entity.getAnnotation(Table.class);
            assertThat(table)
                    .as("%s braucht ein @Table, sonst raet Hibernate den Namen", entity.getSimpleName())
                    .isNotNull();
            assertThat(table.schema())
                    .as("%s muss im Schema der Statikdatenbank liegen", entity.getSimpleName())
                    .isEqualTo("evesde");
            assertThat(unquote(table.name()))
                    .as("%s zeigt auf eine Tabelle, die der SDE-Import nicht anlegt", entity.getSimpleName())
                    .isIn(importedTables);
        }
    }

    @Test
    @DisplayName("behaelt die Gross- und Kleinschreibung der EVE-Datenbank bei")
    void keepsTheOriginalSpelling() {
        for (Class<?> entity : sdeEntities()) {
            Table table = entity.getAnnotation(Table.class);
            // Ohne Anfuehrungszeichen schreibt Postgres den Namen klein und findet die
            // importierte Tabelle nicht mehr.
            assertThat(table.name())
                    .as("Tabellenname von %s muss in Anfuehrungszeichen stehen", entity.getSimpleName())
                    .startsWith("\"").endsWith("\"");
        }
    }

    /**
     * Liest die Liste der importierten Tabellen aus dem Importskript.
     *
     * @return die Tabellennamen in der Schreibweise der EVE-Datenbank
     * @throws IOException wenn das Skript nicht lesbar ist
     */
    private Set<String> importedTableNames() throws IOException {
        String script = Files.readString(resolveFromProjectRoot(IMPORT_SCRIPT));
        int start = script.indexOf("TABLES_TO_IMPORT");
        assertThat(start).as("TABLES_TO_IMPORT im Importskript gefunden").isGreaterThanOrEqualTo(0);
        String block = script.substring(script.indexOf('[', start) + 1, script.indexOf(']', start));

        Matcher names = Pattern.compile("\"([^\"]+)\"").matcher(block);
        Set<String> tables = new java.util.LinkedHashSet<>();
        while (names.find()) {
            tables.add(names.group(1));
        }
        return tables;
    }

    /**
     * Sucht alle Entitaeten der Statikdatenbank.
     *
     * @return die gefundenen Entitaetsklassen
     */
    private List<Class<?>> sdeEntities() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<Class<?>> entities = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(ENTITY_PACKAGE)) {
            try {
                entities.add(Class.forName(definition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Entitaet nicht ladbar: " + definition.getBeanClassName(), e);
            }
        }
        return entities;
    }

    /**
     * Findet eine Datei unabhaengig davon, ob der Test aus dem Projektverzeichnis oder
     * einem Untermodul heraus laeuft.
     *
     * @param relative der Pfad ab Projektwurzel
     * @return der gefundene Pfad
     */
    private Path resolveFromProjectRoot(Path relative) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Importskript nicht gefunden: " + relative);
    }

    /**
     * Entfernt die Anfuehrungszeichen, mit denen die Schreibweise geschuetzt wird.
     *
     * @param name der Name aus der Annotation
     * @return der reine Tabellenname
     */
    private String unquote(String name) {
        return name.replace("\"", "");
    }
}
