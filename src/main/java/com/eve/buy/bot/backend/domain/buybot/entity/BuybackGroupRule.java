package com.eve.buy.bot.backend.domain.buybot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Regel fuer eine Item-Gruppe - die Ebene zwischen Kategorie und Einzelitem.
 *
 * <p>Kategorien sind fuer den Ankauf zu grob geschnitten: "Material" enthaelt Mineralien,
 * Gase und Eisprodukte zugleich. Wer nur Mineralien ankaufen will, muesste sonst jedes Gas
 * und jedes Eisprodukt einzeln sperren. Die Gruppe fasst genau diese Buendel zusammen.
 *
 * <p>Sie ueberlagert die Kategorie und wird selbst vom Einzelitem ueberlagert.
 */
@Entity
@Table(name = "buyback_group_rules")
@Getter
@Setter
public class BuybackGroupRule {

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "modifier")
    private Double modifier;

    @Column(name = "is_blacklisted", nullable = false)
    private Boolean isBlacklisted = false;

    /**
     * true = statt des Marktpreises wird der Reprocessing-Wert der Ausbeute angesetzt.
     * NULL = keine Angabe, dann greift die Kategorie-Einstellung.
     */
    @Column(name = "use_reprocessed_value")
    private Boolean useReprocessedValue;

    /**
     * Ausbeute dieser Gruppe in Prozent, NULL = die globale Ausbeute aus der Konfiguration.
     *
     * <p>Im Spiel haengt die Ausbeute davon ab, was verwertet wird: Erz, Eis und Schrott
     * brauchen verschiedene Skills und kommen deshalb auf verschiedene Werte. Eine einzige
     * globale Rate rechnet mindestens eine dieser Gruppen falsch.
     */
    @Column(name = "reprocessing_rate")
    private Double reprocessingRate;
}
