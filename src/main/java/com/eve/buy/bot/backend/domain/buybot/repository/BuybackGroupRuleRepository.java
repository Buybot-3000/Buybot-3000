package com.eve.buy.bot.backend.domain.buybot.repository;

import com.eve.buy.bot.backend.domain.buybot.entity.BuybackGroupRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistenz der Gruppen-Regeln. */
@Repository
public interface BuybackGroupRuleRepository extends JpaRepository<BuybackGroupRule, Long> {
}
