package com.eve.buy.bot.backend.domain.eve.repository;

import com.eve.buy.bot.backend.domain.eve.entity.InvGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** Lesezugriff auf die Item-Gruppen der EVE-Statikdatenbank. */
@Repository
public interface InvGroupRepository extends JpaRepository<InvGroup, Long> {

    /**
     * Sucht Gruppen ueber ihren Namen.
     *
     * <p>Liefert bewusst eine Liste: vier Gruppennamen sind in EVE doppelt vergeben, etwa
     * "Miscellaneous" in Abstract und in Commodity. Mit {@code Optional} wuerde die Abfrage
     * dort mit einem Serverfehler abbrechen, statt die Mehrdeutigkeit zu melden.
     *
     * @param groupName der Gruppenname, Gross- und Kleinschreibung egal
     * @return alle Gruppen dieses Namens, meist genau eine
     */
    List<InvGroup> findByGroupNameIgnoreCase(String groupName);

    /**
     * Ermittelt, welche der angegebenen Gruppen ueberhaupt verwertbare Items enthalten.
     *
     * <p>Mineralien und Mondgueter sind Endprodukte und haben keine Ausbeute. Wer das
     * Reprocessing-Haekchen auf so einer Gruppe setzt, aendert am Preis nichts - und sucht
     * den Fehler dann an der falschen Stelle.
     *
     * @param groupIds die zu pruefenden Gruppen
     * @return die Teilmenge, in der mindestens ein Item eine Ausbeute hat
     */
    @Query(value = """
        SELECT DISTINCT t."groupID"
        FROM evesde."invTypes" t
        JOIN evesde."invTypeMaterials" m ON m."typeID" = t."typeID"
        WHERE t."groupID" IN (:groupIds)
          AND m.quantity > 0
        """, nativeQuery = true)
    List<Long> findGroupIdsWithReprocessableItems(@Param("groupIds") Collection<Long> groupIds);
}
