package com.mindbridge.devseed;

import java.util.List;

/**
 * Pre-written conversation scripts for the G2-T09 dev seed.
 *
 * <h2>Demo-only content policy</h2>
 * <p>The scripted exchanges below are intended <strong>only</strong> for local
 * development and integration testing. They contain <strong>NEUTRAL, NON-CRISIS
 * CONTENT</strong> — generic day-to-day topics (work, study, weather, hobbies)
 * with no language that resembles a real mental-health emergency.
 *
 * <p><strong>Vietnamese crisis keyword set is intentionally empty here.</strong>
 * Per {@code docs/04_SAFETY_AND_CBT_RULES.md §6}, the production Vietnamese
 * crisis keyword list is marked {@code TODO_EXPERT_REVIEW} — Cursor is
 * explicitly forbidden from inventing it. A runtime crisis-keyword scanner
 * is therefore not part of the seed test suite; this file relies on
 * <strong>static code review</strong> to enforce the no-crisis policy.
 *
 * <p>Any future addition to this file MUST be reviewed against the safety
 * rules before merge. Adding crisis-like or personally-identifiable
 * language is a contract violation.
 *
 * <h2>Determinism</h2>
 * <p>Each user gets two sessions of four scripted exchanges. The script
 * template is selected by the user index modulo the number of scripts.
 */
public final class DemoChatScript {

    /**
     * Two neutral scripted sessions (4 exchanges each) used as the per-user
     * chat baseline. The script is intentionally generic and contains no
     * references to identity, location, or crisis-adjacent language.
     */
    private static final List<List<Exchange>> SCRIPTS = List.of(
            // Script A — work / study pressure, neutral framing
            List.of(
                    new Exchange("USER",      "Hom nay toi cam thay kha ap luc voi cong viec."),
                    new Exchange("ASSISTANT", "Cam on ban da chia se. Ban co the ke them ve ap luc do khong?"),
                    new Exchange("USER",      "Deadline di sat va toi chua nhieu tien trien."),
                    new Exchange("ASSISTANT", "Nghe co ve ban dang qua tai. Ban da thu chia nho cong viec chua?")
            ),
            // Script B — sleep and routine, neutral framing
            List.of(
                    new Exchange("USER",      "Toi muon noi ve mot vai thoi quen hang ngay."),
                    new Exchange("ASSISTANT", "Chung toi san sang nghe. Ban muon bat dau tu dau?"),
                    new Exchange("USER",      "Toi thu di ngu som hon nhung van kho ngu."),
                    new Exchange("ASSISTANT", "Mot so nguoi thay mot ban ghi nho truyen thong giup. Ban muon thu khong?")
            )
    );

    private DemoChatScript() {
    }

    /**
     * Returns the script for a user by index. Two sessions worth of exchanges
     * are produced — the caller creates one session per script and writes
     * each exchange into the session.
     */
    public static List<List<Exchange>> scriptsFor(int userIndex) {
        // Caller iterates over the full script list; userIndex is accepted
        // here so future per-user script variants can be added without
        // changing the signature.
        return SCRIPTS;
    }

    /** A single user / assistant turn. */
    public record Exchange(String role, String content) {
    }
}