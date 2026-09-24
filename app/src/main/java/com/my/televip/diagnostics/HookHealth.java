package com.my.televip.diagnostics;

import com.my.televip.logging.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records how each hook landed so a Telegram update that moves things is visible at a glance.
 *
 * <p>Hooks address Telegram by plain name, so a client release that renames or reshapes something
 * takes the matching feature out silently: the hook is skipped, the app behaves normally, and the
 * only evidence is one warning buried in a log that nobody reads until something is obviously
 * wrong. That is the real cost of a Telegram update — not that a feature breaks, but that nobody
 * can tell which one.</p>
 *
 * <p>The counters below turn that into a single line at startup: how many hooks resolved, how many
 * were recovered after their signature drifted, and exactly which ones could not be found at all.
 * A bug report can then name the broken symbol instead of "stories stopped working".</p>
 */
public final class HookHealth {

    private HookHealth() {
    }

    /** Keeps the report readable, and bounds what a pathological mismatch can accumulate. */
    private static final int MAX_LISTED = 40;

    private static final AtomicInteger resolvedCount = new AtomicInteger();
    private static final AtomicInteger driftedCount = new AtomicInteger();
    private static final AtomicInteger missingCount = new AtomicInteger();

    // The counters above stay exact; these only hold the names the report has room to print, so
    // a client that moved hundreds of symbols still reports how many rather than how many fitted.
    private static final Set<String> drifted =
            Collections.synchronizedSet(new LinkedHashSet<String>());
    private static final Set<String> missingMembers =
            Collections.synchronizedSet(new LinkedHashSet<String>());
    private static final Set<String> missingClasses =
            Collections.synchronizedSet(new LinkedHashSet<String>());

    /** A hook resolved exactly as the call site described it. */
    public static void resolved() {
        resolvedCount.incrementAndGet();
    }

    /** A hook resolved only after its signature was allowed to differ. */
    public static void drifted(String symbol) {
        if (symbol == null) return;
        driftedCount.incrementAndGet();
        add(drifted, symbol);
    }

    /** A method or constructor the call site asked for does not exist any more. */
    public static void missingMember(String symbol) {
        if (symbol == null) return;
        missingCount.incrementAndGet();
        add(missingMembers, symbol);
    }

    /** A class name no longer resolves in the client. */
    public static void missingClass(String name) {
        if (name == null) return;
        missingCount.incrementAndGet();
        add(missingClasses, name);
    }

    private static void add(Set<String> target, String value) {
        if (target.size() >= MAX_LISTED) return;
        target.add(value);
    }

    public static String report() {
        StringBuilder summary = new StringBuilder("hook health: ")
                .append(resolvedCount.get()).append(" resolved, ")
                .append(driftedCount.get()).append(" drifted, ")
                .append(missingCount.get()).append(" missing");

        append(summary, "drifted (signature changed, hooked anyway)", drifted);
        append(summary, "missing methods (feature inactive)", missingMembers);
        append(summary, "missing classes (feature inactive)", missingClasses);
        return summary.toString();
    }

    private static void append(StringBuilder summary, String label, Set<String> values) {
        if (values.isEmpty()) return;
        summary.append("\n  ").append(label).append(": ");
        synchronized (values) {
            summary.append(join(values));
            if (values.size() >= MAX_LISTED) summary.append(", ...");
        }
    }

    private static String join(Collection<String> values) {
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            if (joined.length() > 0) joined.append(", ");
            joined.append(value);
        }
        return joined.toString();
    }

    /** True when something the module wanted was not where it expected it. */
    public static boolean hasDrift() {
        return driftedCount.get() > 0 || missingCount.get() > 0;
    }

    /**
     * Logs the report once startup has installed its hooks. Anything hooked later — the chat and
     * profile screens are wired on first use — still updates the counters and still logs its own
     * warning, it just lands after this summary.
     */
    public static void logReport() {
        if (hasDrift()) {
            Logger.w(report());
        } else {
            Logger.l(report());
        }
    }
}
