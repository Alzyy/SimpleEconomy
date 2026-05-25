package it.alzy.simpleeconomy.plugin.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionLock {
    private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public static void lock(String uuid) {
        locks.computeIfAbsent(uuid, k -> new ReentrantLock()).lock();
    }

    public static void unlock(String uuid) {
        ReentrantLock lock = locks.get(uuid);
        if (lock != null) {
            lock.unlock();
        }
    }
}