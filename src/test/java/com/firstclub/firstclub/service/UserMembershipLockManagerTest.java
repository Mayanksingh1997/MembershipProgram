package com.firstclub.firstclub.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class UserMembershipLockManagerTest {

    private final UserMembershipLockManager lockManager = new UserMembershipLockManager();

    @Test
    void lockAndUnlock_allowsSameThreadReentry() {
        lockManager.lock("user-001");
        lockManager.lock("user-001");
        lockManager.unlock("user-001");
        lockManager.unlock("user-001");
    }

    @Test
    void unlock_doesNotThrowWhenLockNotHeld() {
        lockManager.unlock("user-002");
    }

    @Test
    void lock_blocksOtherThreadsUntilReleased() throws InterruptedException {
        AtomicBoolean entered = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);

        lockManager.lock("user-003");

        Thread other = new Thread(() -> {
            started.countDown();
            lockManager.lock("user-003");
            entered.set(true);
            lockManager.unlock("user-003");
            finished.countDown();
        });
        other.start();

        started.await(1, TimeUnit.SECONDS);
        assertThat(entered.get()).isFalse();

        lockManager.unlock("user-003");
        finished.await(1, TimeUnit.SECONDS);
        assertThat(entered.get()).isTrue();
        other.join();
    }
}
