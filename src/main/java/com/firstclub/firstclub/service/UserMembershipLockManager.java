package com.firstclub.firstclub.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class UserMembershipLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void lock(String externalUserId) {
        ReentrantLock lock = locks.computeIfAbsent(externalUserId, key -> new ReentrantLock());
        lock.lock();
    }

    public void unlock(String externalUserId) {
        ReentrantLock lock = locks.get(externalUserId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
