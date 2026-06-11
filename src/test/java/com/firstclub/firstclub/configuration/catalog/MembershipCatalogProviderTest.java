package com.firstclub.firstclub.configuration.catalog;

import com.firstclub.firstclub.exception.MembershipException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MembershipCatalogProviderTest {

    private MembershipCatalogProvider provider;

    @BeforeEach
    void setUp() {
        MembershipCatalogProperties properties = new MembershipCatalogProperties();
        properties.setPlans(List.of());
        properties.setTiers(List.of());
        provider = new MembershipCatalogProvider(properties);
    }

    @Test
    void resolvePlanCode_invalidValueThrowsPlanNotFound() {
        MembershipException ex = assertThrows(MembershipException.class, () -> provider.resolvePlanCode("MONTHL"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("PLAN_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void resolveTierCode_invalidValueThrowsTierNotFound() {
        MembershipException ex = assertThrows(MembershipException.class, () -> provider.resolveTierCode("SILVR"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("TIER_NOT_FOUND", ex.getErrorCode());
    }
}
