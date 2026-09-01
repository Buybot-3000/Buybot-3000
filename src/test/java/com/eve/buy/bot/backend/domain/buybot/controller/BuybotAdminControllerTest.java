package com.eve.buy.bot.backend.domain.buybot.controller;

import com.eve.buy.bot.backend.audit.AuditService;
import com.eve.buy.bot.backend.domain.auth.service.AuthService;
import com.eve.buy.bot.backend.domain.buybot.dto.ReprocessMaterialProjection;
import com.eve.buy.bot.backend.domain.buybot.entity.BuybackTypeRule;
import com.eve.buy.bot.backend.domain.buybot.repository.BuybackCategoryRuleRepository;
import com.eve.buy.bot.backend.domain.buybot.repository.BuybackConfigRepository;
import com.eve.buy.bot.backend.domain.buybot.repository.BuybackLocationRepository;
import com.eve.buy.bot.backend.domain.buybot.repository.BuybackTypeRuleRepository;
import com.eve.buy.bot.backend.domain.buybot.service.ContractCheckService;
import com.eve.buy.bot.backend.domain.character.repository.CharacterRepository;
import com.eve.buy.bot.backend.domain.buybot.entity.BuybackGroupRule;
import com.eve.buy.bot.backend.domain.eve.entity.InvCategory;
import com.eve.buy.bot.backend.domain.eve.entity.InvGroup;
import com.eve.buy.bot.backend.domain.eve.entity.InvType;
import com.eve.buy.bot.backend.domain.buybot.repository.BuybackGroupRuleRepository;
import com.eve.buy.bot.backend.domain.eve.repository.InvCategoryRepository;
import com.eve.buy.bot.backend.domain.eve.repository.InvGroupRepository;
import com.eve.buy.bot.backend.domain.eve.repository.InvTypeRepository;
import com.eve.buy.bot.backend.esi.EsiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests der Admin-Schnittstelle.
 *
 * <p>Schwerpunkt ist die Rueckmeldung, ob ein Item ueberhaupt verwertbar ist. Ohne die
 * wundert sich ein Admin, warum das gesetzte Reprocessing-Haekchen bei Mineralien den Preis
 * nicht aendert.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BuybotAdminController")
class BuybotAdminControllerTest {

    private static final long TRITANIUM = 34L;
    private static final long VELDSPAR = 1230L;

    @Mock private BuybackConfigRepository configRepo;
    @Mock private BuybackLocationRepository locationRepo;
    @Mock private BuybackCategoryRuleRepository categoryRuleRepo;
    @Mock private BuybackTypeRuleRepository typeRuleRepo;
    @Mock private InvTypeRepository invTypeRepo;
    @Mock private InvCategoryRepository invCategoryRepo;
    @Mock private InvGroupRepository invGroupRepo;
    @Mock private BuybackGroupRuleRepository groupRuleRepo;
    @Mock private AuthService authService;
    @Mock private CharacterRepository characterRepo;
    @Mock private EsiService esiService;
    @Mock private ContractCheckService contractCheckService;
    @Mock private AuditService auditService;

    private BuybotAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new BuybotAdminController(configRepo, locationRepo, categoryRuleRepo, typeRuleRepo,
                invTypeRepo, invCategoryRepo, invGroupRepo, groupRuleRepo, authService, characterRepo,
                esiService, contractCheckService, auditService);
    }

    /**
     * Legt eine Einzelitem-Regel an.
     *
     * @param typeId    Type-ID
     * @param reprocess ob das Reprocessing-Häkchen gesetzt ist
     * @return die Regel
     */
    private BuybackTypeRule regel(long typeId, boolean reprocess) {
        BuybackTypeRule rule = new BuybackTypeRule();
        rule.setTypeId(typeId);
        rule.setModifier(90.0);
        rule.setIsBlacklisted(false);
        rule.setUseReprocessedValue(reprocess);
        return rule;
    }

    /**
     * Hinterlegt einen Itemnamen in der Statikdatenbank.
     *
     * @param typeId Type-ID
     * @param name   Anzeigename
     */
    private void bekannterName(long typeId, String name) {
        InvType type = new InvType();
        type.setTypeId(typeId);
        type.setTypeName(name);
        lenient().when(invTypeRepo.findById(typeId)).thenReturn(Optional.of(type));
    }

    /**
     * Hinterlegt eine Gruppe samt Kategorie in der Statikdatenbank.
     *
     * @param groupId      Group-ID
     * @param groupName    Gruppenname
     * @param categoryId   Kategorie der Gruppe
     * @param categoryName Kategoriename
     * @return die hinterlegte Gruppe
     */
    private InvGroup gruppe(long groupId, String groupName, long categoryId, String categoryName) {
        InvGroup group = new InvGroup();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setCategoryId(categoryId);

        InvCategory category = new InvCategory();
        category.setCategoryId(categoryId);
        category.setCategoryName(categoryName);
        lenient().when(invCategoryRepo.findById(categoryId)).thenReturn(Optional.of(category));
        return group;
    }

    private InvGroup bekannteGruppe(long groupId, String groupName, long categoryId, String categoryName) {
        InvGroup group = new InvGroup();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setCategoryId(categoryId);
        lenient().when(invGroupRepo.findById(groupId)).thenReturn(Optional.of(group));
        // Die echte Abfrage ignoriert die Schreibweise - das Doppel muss das auch tun,
        // sonst prueft der Test eine Strenge, die es gar nicht gibt.
        lenient().when(invGroupRepo.findByGroupNameIgnoreCase(anyString())).thenAnswer(call ->
                groupName.equalsIgnoreCase(call.getArgument(0)) ? List.of(group) : List.of());

        InvCategory category = new InvCategory();
        category.setCategoryId(categoryId);
        category.setCategoryName(categoryName);
        lenient().when(invCategoryRepo.findById(categoryId)).thenReturn(Optional.of(category));
        return group;
    }

    @Test
    @DisplayName("legt eine Gruppen-Regel ueber den Gruppennamen an")
    void addsGroupRuleByName() {
        bekannteGruppe(423L, "Ice Product", 4L, "Material");
        when(groupRuleRepo.save(any(BuybackGroupRule.class))).thenAnswer(call -> call.getArgument(0));

        BuybackGroupRule saved = controller.addGroupRule(
                new BuybotAdminController.AddGroupRuleRequest("  ice product  ", null, 80.0, false, true, 62.0)).getBody();

        assertThat(saved).isNotNull();
        assertThat(saved.getGroupId()).isEqualTo(423L);
        assertThat(saved.getModifier()).isEqualTo(80.0);
        assertThat(saved.getUseReprocessedValue()).isTrue();
        assertThat(saved.getReprocessingRate()).isEqualTo(62.0);
    }

    @Test
    @DisplayName("lehnt eine unbekannte Gruppe ab, statt sie stillschweigend anzulegen")
    void rejectsUnknownGroup() {
        bekannteGruppe(423L, "Ice Product", 4L, "Material");

        assertThatThrownBy(() -> controller.addGroupRule(
                new BuybotAdminController.AddGroupRuleRequest("Gibtsnicht", null, 90.0, false, false, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Gruppe nicht in der EVE DB gefunden");
        verify(groupRuleRepo, never()).save(any());
    }

    @Test
    @DisplayName("zeigt zu jeder Gruppe ihre Kategorie zur Einordnung")
    void showsCategoryOfEachGroup() {
        bekannteGruppe(423L, "Ice Product", 4L, "Material");
        BuybackGroupRule rule = new BuybackGroupRule();
        rule.setGroupId(423L);
        rule.setModifier(80.0);
        rule.setIsBlacklisted(false);
        when(groupRuleRepo.findAll()).thenReturn(List.of(rule));
        lenient().when(invGroupRepo.findGroupIdsWithReprocessableItems(any())).thenReturn(List.of());

        List<BuybotAdminController.GroupRuleDto> rules = controller.getGroupRules().getBody();

        assertThat(rules).isNotNull().singleElement().satisfies(dto -> {
            assertThat(dto.groupName()).isEqualTo("Ice Product");
            assertThat(dto.categoryName()).isEqualTo("Material");
        });
    }

    @Test
    @DisplayName("nennt die Kategorien, wenn ein Gruppenname doppelt vergeben ist")
    void namesCategoriesForAmbiguousGroupName() {
        // "Miscellaneous" gibt es in EVE in Abstract und in Commodity - frueher endete das
        // in einem Serverfehler, statt dem Admin zu sagen, was zu tun ist.
        InvGroup abstrakt = gruppe(314L, "Miscellaneous", 0L, "Abstract");
        InvGroup ware = gruppe(526L, "Miscellaneous", 17L, "Commodity");
        when(invGroupRepo.findByGroupNameIgnoreCase("Miscellaneous")).thenReturn(List.of(abstrakt, ware));

        assertThatThrownBy(() -> controller.addGroupRule(
                new BuybotAdminController.AddGroupRuleRequest("Miscellaneous", null, 90.0, false, false, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Abstract")
                .hasMessageContaining("Commodity");
        verify(groupRuleRepo, never()).save(any());
    }

    @Test
    @DisplayName("nimmt bei doppeltem Namen die angegebene Kategorie")
    void usesGivenCategoryToDisambiguate() {
        InvGroup abstrakt = gruppe(314L, "Miscellaneous", 0L, "Abstract");
        InvGroup ware = gruppe(526L, "Miscellaneous", 17L, "Commodity");
        when(invGroupRepo.findByGroupNameIgnoreCase("Miscellaneous")).thenReturn(List.of(abstrakt, ware));
        when(groupRuleRepo.save(any(BuybackGroupRule.class))).thenAnswer(call -> call.getArgument(0));

        BuybackGroupRule saved = controller.addGroupRule(
                new BuybotAdminController.AddGroupRuleRequest("Miscellaneous", "commodity", 90.0, false, false, null))
                .getBody();

        assertThat(saved).isNotNull();
        assertThat(saved.getGroupId()).isEqualTo(526L);
    }

    @Test
    @DisplayName("markiert eine Gruppe aus lauter Endprodukten als nicht verwertbar")
    void marksGroupWithoutYieldAsNotReprocessable() {
        // Genau die Falle aus der Praxis: Reprocessing auf der Gruppe "Mineral" bewirkt
        // nichts, weil Mineralien selbst schon das Endprodukt sind.
        bekannteGruppe(18L, "Mineral", 4L, "Material");
        bekannteGruppe(423L, "Ice Product", 4L, "Material");
        BuybackGroupRule mineral = new BuybackGroupRule();
        mineral.setGroupId(18L);
        mineral.setIsBlacklisted(false);
        mineral.setUseReprocessedValue(true);
        BuybackGroupRule ice = new BuybackGroupRule();
        ice.setGroupId(423L);
        ice.setIsBlacklisted(false);
        ice.setUseReprocessedValue(true);
        when(groupRuleRepo.findAll()).thenReturn(List.of(mineral, ice));
        when(invGroupRepo.findGroupIdsWithReprocessableItems(Set.of(18L, 423L)))
                .thenReturn(List.of(423L));

        List<BuybotAdminController.GroupRuleDto> rules = controller.getGroupRules().getBody();

        assertThat(rules).isNotNull();
        assertThat(rules).anySatisfy(dto -> {
            assertThat(dto.groupName()).isEqualTo("Ice Product");
            assertThat(dto.reprocessable()).isTrue();
        });
        assertThat(rules).anySatisfy(dto -> {
            assertThat(dto.groupName()).isEqualTo("Mineral");
            assertThat(dto.reprocessable()).isFalse();
        });
    }

    @Test
    @DisplayName("fragt die Ausbeute nicht ab, wenn es keine Gruppen-Regeln gibt")
    void doesNotQueryGroupYieldsWithoutRules() {
        when(groupRuleRepo.findAll()).thenReturn(List.of());

        controller.getGroupRules();

        verify(invGroupRepo, never()).findGroupIdsWithReprocessableItems(any());
    }

    @Test
    @DisplayName("meldet eine geloeschte Gruppe als unbekannt, statt die Anzeige abzubrechen")
    void survivesGroupMissingFromSde() {
        // Nach einem SDE-Wechsel kann eine Group-ID verschwinden - die Liste muss trotzdem laden
        BuybackGroupRule rule = new BuybackGroupRule();
        rule.setGroupId(99999L);
        rule.setIsBlacklisted(false);
        when(groupRuleRepo.findAll()).thenReturn(List.of(rule));
        when(invGroupRepo.findById(99999L)).thenReturn(Optional.empty());

        List<BuybotAdminController.GroupRuleDto> rules = controller.getGroupRules().getBody();

        assertThat(rules).isNotNull().singleElement().satisfies(dto -> {
            assertThat(dto.groupName()).isEqualTo("Unknown Group");
            assertThat(dto.categoryName()).isNull();
        });
    }

    @Test
    @DisplayName("markiert ein Endprodukt als nicht verwertbar")
    void marksEndProductAsNotReprocessable() {
        when(typeRuleRepo.findAll()).thenReturn(List.of(regel(TRITANIUM, true), regel(VELDSPAR, true)));
        bekannterName(TRITANIUM, "Tritanium");
        bekannterName(VELDSPAR, "Veldspar");

        // Nur Veldspar liefert eine Ausbeute - Tritanium ist selbst das Endprodukt
        when(invTypeRepo.findReprocessMaterials(Set.of(TRITANIUM, VELDSPAR)))
                .thenReturn(List.of(new Yield(VELDSPAR, TRITANIUM, 400L, 100)));

        List<BuybotAdminController.TypeRuleDto> rules = controller.getTypeRules().getBody();

        assertThat(rules).isNotNull();
        assertThat(rules).anySatisfy(rule -> {
            assertThat(rule.typeName()).isEqualTo("Veldspar");
            assertThat(rule.reprocessable()).isTrue();
        });
        assertThat(rules).anySatisfy(rule -> {
            assertThat(rule.typeName()).isEqualTo("Tritanium");
            assertThat(rule.reprocessable()).isFalse();
        });
    }

    @Test
    @DisplayName("fragt die Ausbeute nicht ab, wenn es keine Regeln gibt")
    void doesNotQueryYieldsWithoutRules() {
        when(typeRuleRepo.findAll()).thenReturn(List.of());

        List<BuybotAdminController.TypeRuleDto> rules = controller.getTypeRules().getBody();

        assertThat(rules).isEmpty();
        // Eine Abfrage mit leerer Liste wuerde als IN () auf der Datenbank scheitern
        verify(invTypeRepo, never()).findReprocessMaterials(any());
    }

    @Test
    @DisplayName("gibt auch ohne gesetztes Häkchen zurück, ob ein Item verwertbar wäre")
    void reportsReprocessabilityRegardlessOfFlag() {
        when(typeRuleRepo.findAll()).thenReturn(List.of(regel(VELDSPAR, false)));
        bekannterName(VELDSPAR, "Veldspar");
        when(invTypeRepo.findReprocessMaterials(Set.of(VELDSPAR)))
                .thenReturn(List.of(new Yield(VELDSPAR, TRITANIUM, 400L, 100)));

        List<BuybotAdminController.TypeRuleDto> rules = controller.getTypeRules().getBody();

        assertThat(rules).hasSize(1);
        assertThat(rules.getFirst().useReprocessedValue()).isFalse();
        assertThat(rules.getFirst().reprocessable()).isTrue();
    }

    @Test
    @DisplayName("nennt ein unbekanntes Item beim Namen, statt abzubrechen")
    void handlesUnknownItemName() {
        when(typeRuleRepo.findAll()).thenReturn(List.of(regel(999999L, false)));
        lenient().when(invTypeRepo.findById(anyLong())).thenReturn(Optional.empty());
        when(invTypeRepo.findReprocessMaterials(Set.of(999999L))).thenReturn(List.of());

        List<BuybotAdminController.TypeRuleDto> rules = controller.getTypeRules().getBody();

        assertThat(rules).hasSize(1);
        assertThat(rules.getFirst().typeName()).isEqualTo("Unknown Item");
        assertThat(rules.getFirst().reprocessable()).isFalse();
    }

    /**
     * Testdoppel für eine Zeile der Reprocessing-Ausbeute.
     *
     * @param typeId         das zu verwertende Item
     * @param materialTypeId das gewonnene Material
     * @param quantity       Menge je Portion
     * @param portionSize    Portionsgröße
     */
    private record Yield(Long typeId, Long materialTypeId, Long quantity, Integer portionSize)
            implements ReprocessMaterialProjection {

        @Override
        public Long getTypeId() {
            return typeId;
        }

        @Override
        public Long getMaterialTypeId() {
            return materialTypeId;
        }

        @Override
        public Long getQuantity() {
            return quantity;
        }

        @Override
        public Integer getPortionSize() {
            return portionSize;
        }
    }
}
