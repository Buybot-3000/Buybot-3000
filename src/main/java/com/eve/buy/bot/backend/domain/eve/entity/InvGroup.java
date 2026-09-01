package com.eve.buy.bot.backend.domain.eve.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Item-Gruppe aus der EVE-Statikdatenbank; die Ebene zwischen Kategorie und Item.
 *
 * <p>Die Kategorie ist fuer den Ankauf oft zu grob: in "Material" stecken Mineralien, Gase
 * und Eisprodukte gemeinsam. Die Gruppe trennt genau das.
 *
 * <p>Tabellen- und Spaltennamen stehen in Anfuehrungszeichen, weil der SDE-Import sie
 * unveraendert aus der EVE-Datenbank uebernimmt und Postgres sonst alles kleinschreibt.
 */
@Entity
@Table(name = "\"invGroups\"", schema = "evesde")
@Getter @Setter
public class InvGroup {

    @Id
    @Column(name = "\"groupID\"")
    private Long groupId;

    @Column(name = "\"groupName\"")
    private String groupName;

    @Column(name = "\"categoryID\"")
    private Long categoryId;
}
