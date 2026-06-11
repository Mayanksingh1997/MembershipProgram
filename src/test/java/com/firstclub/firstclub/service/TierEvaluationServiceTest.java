package com.firstclub.firstclub.service;

import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProvider;
import com.firstclub.firstclub.configuration.catalog.TierDefinition;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.domain.factory.MembershipDomainFactory;
import com.firstclub.firstclub.domain.factory.MembershipStateFactory;
import com.firstclub.firstclub.domain.factory.TierEvaluatorFactory;
import com.firstclub.firstclub.domain.subscription.state.ActiveMembershipState;
import com.firstclub.firstclub.domain.subscription.state.CancelledMembershipState;
import com.firstclub.firstclub.domain.subscription.state.ExpiredMembershipState;
import com.firstclub.firstclub.domain.subscription.state.PendingMembershipState;
import com.firstclub.firstclub.domain.tier.CohortEvaluator;
import com.firstclub.firstclub.domain.tier.MonthlyOrderValueEvaluator;
import com.firstclub.firstclub.domain.tier.OrderCountEvaluator;
import com.firstclub.firstclub.domain.tier.TierEvaluationContext;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserCohort;
import com.firstclub.firstclub.entity.UserOrderAggregate;
import com.firstclub.firstclub.exception.MembershipException;
import com.firstclub.firstclub.repository.UserCohortRepository;
import com.firstclub.firstclub.repository.UserOrderAggregateRepository;
import com.firstclub.firstclub.service.support.ServiceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TierEvaluationServiceTest {

    @Mock
    private UserOrderAggregateRepository orderAggregateRepository;

    @Mock
    private UserCohortRepository cohortRepository;

    private TierEvaluationService tierEvaluationService;
    private MembershipCatalogProvider catalogProvider;

    @BeforeEach
    void setUp() {
        catalogProvider = ServiceTestFixtures.catalogProvider();

        TierEvaluatorFactory tierEvaluatorFactory = new TierEvaluatorFactory(List.of(
                new OrderCountEvaluator(),
                new MonthlyOrderValueEvaluator(),
                new CohortEvaluator()));
        MembershipStateFactory stateFactory = new MembershipStateFactory(
                new PendingMembershipState(),
                new ActiveMembershipState(),
                new CancelledMembershipState(),
                new ExpiredMembershipState());
        MembershipDomainFactory domainFactory = new MembershipDomainFactory();
        ReflectionTestUtils.setField(domainFactory, "tierEvaluatorFactory", tierEvaluatorFactory);
        ReflectionTestUtils.setField(domainFactory, "membershipStateFactory", stateFactory);
        tierEvaluationService = new TierEvaluationService(
                catalogProvider, orderAggregateRepository, cohortRepository, domainFactory);
    }

    @Test
    void determineEligibleTier_returnsGoldForFifteenOrders() {
        TierEvaluationContext context = TierEvaluationContext.builder()
                .totalOrders(15)
                .monthlyOrderValue(BigDecimal.valueOf(3500))
                .cohortCodes(new ArrayList<>())
                .build();

        TierDefinition tier = tierEvaluationService.determineEligibleTier(context);
        assertThat(tier.getCode()).isEqualTo(TierCode.GOLD);
    }

    @Test
    void determineEligibleTier_returnsPlatinumForHighValueAndCohort() {
        List<String> cohorts = new ArrayList<>();
        cohorts.add("PREMIUM_COHORT");

        TierEvaluationContext context = TierEvaluationContext.builder()
                .totalOrders(20)
                .monthlyOrderValue(BigDecimal.valueOf(6000))
                .cohortCodes(cohorts)
                .build();

        TierDefinition tier = tierEvaluationService.determineEligibleTier(context);
        assertThat(tier.getCode()).isEqualTo(TierCode.PLATINUM);
    }

    @Test
    void buildContext_loadsOrderStatsAndCohorts() {
        UserAccount user = ServiceTestFixtures.user("user-003");
        user.setId(3L);
        UserOrderAggregate aggregate = UserOrderAggregate.builder()
                .user(user)
                .totalOrders(20)
                .monthlyOrderValue(BigDecimal.valueOf(6000))
                .build();
        UserCohort cohort = UserCohort.builder().user(user).cohortCode("PREMIUM_COHORT").build();

        when(orderAggregateRepository.findByUserId(3L)).thenReturn(Optional.of(aggregate));
        when(cohortRepository.findByUserId(3L)).thenReturn(List.of(cohort));

        TierEvaluationContext context = tierEvaluationService.buildContext(user);

        assertThat(context.getTotalOrders()).isEqualTo(20);
        assertThat(context.getMonthlyOrderValue()).isEqualByComparingTo("6000");
        assertThat(context.getCohortCodes()).containsExactly("PREMIUM_COHORT");
    }

    @Test
    void validateTierEligibility_throwsWhenUserNotEligible() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        user.setId(1L);
        when(orderAggregateRepository.findByUserId(1L)).thenReturn(Optional.of(UserOrderAggregate.builder()
                .user(user)
                .totalOrders(5)
                .monthlyOrderValue(BigDecimal.valueOf(1200))
                .build()));
        when(cohortRepository.findByUserId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> tierEvaluationService.validateTierEligibility(user, TierCode.GOLD))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("not eligible");
    }

    @Test
    void determineEligibleTier_returnsSilverWhenNoHigherTierMatches() {
        TierEvaluationContext context = TierEvaluationContext.builder()
                .totalOrders(2)
                .monthlyOrderValue(BigDecimal.valueOf(500))
                .cohortCodes(new ArrayList<>())
                .build();

        TierDefinition tier = tierEvaluationService.determineEligibleTier(context);

        assertThat(tier.getCode()).isEqualTo(TierCode.SILVER);
    }
}
