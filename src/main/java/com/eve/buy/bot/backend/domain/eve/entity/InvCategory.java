package com.eve.buy.bot.backend.domain.eve.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Item-Kategorie aus der EVE-Statikdatenbank, Grundlage der Kategorie-Whitelist.
 *
 * <p>Tabellen- und Spaltennamen stehen in Anfuehrungszeichen, weil der SDE-Import sie
 * unveraendert aus der EVE-Datenbank uebernimmt und Postgres sonst alles kleinschreibt.
 * Dieselbe Schreibweise nutzt {@link InvType}.
 */
@Entity
@Table(name = "\"invCategories\"", schema = "evesde")
@Getter @Setter
public class InvCategory {

    @Id
    @Column(name = "\"categoryID\"")
    private Long categoryId;

    @Column(name = "\"categoryName\"")
    private String categoryName;
}
