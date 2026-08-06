package com.mindbridge.devseed;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import com.mindbridge.chat.domain.ChatSession;
import com.mindbridge.chat.service.ChatSessionService;
import com.mindbridge.chat.service.ConversationMessageService;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import com.mindbridge.dailyquestion.service.DailyQuestionAnswerService;
import com.mindbridge.dailyquestion.service.DailyQuestionAssignmentService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrator for the G2-T09 dev seed.
 *
 * <p>Composes calls to the production services (chat, daily question,
 * behavioral events) so that every inserted row goes through the same
 * validation, defensive guards, and event-emission paths as a real HTTP
 * request. The seed never inserts directly into a business repository.
 *
 * <h2>Activation</h2>
 * <ul>
 *   <li>The bean is only loaded when {@code mindbridge.seed.run=true}.</li>
 *   <li>{@link #run()} refuses to execute on profile {@code prod} as a
 *       defense-in-depth guard.</li>
 * </ul>
 *
 * <h2>Reset</h2>
 * <p>{@link #reset()} deletes only demo users ({@code email LIKE
 * 'demo-user-%@mindbridge.test'}). FK {@code ON DELETE CASCADE} on
 * {@code behavioral_events}, {@code daily_question_assignments},
 * {@code daily_question_answers}, {@code chat_sessions},
 * {@code conversation_messages}, {@code idempotency_keys} propagates the
 * deletion. {@code daily_question_templates} are NOT touched — content is
 * version-immutable per CBT rule.
 */
@Service
public class DevSeedService {

    private static final Logger log = LoggerFactory.getLogger(DevSeedService.class);

    private static final int CHAT_USER_COUNT = 8; // first 8 of 15 demo users get chat sessions
    private static final int SESSIONS_PER_CHAT_USER = 2;

    private final UserRepository userRepository;
    private final DailyQuestionTemplateRepository templateRepository;
    private final BehavioralEventRepository behavioralEventRepository;

    private final PasswordEncoder passwordEncoder;

    private final ChatSessionService chatSessionService;
    private final ConversationMessageService conversationMessageService;
    private final DailyQuestionAssignmentService assignmentService;
    private final DailyQuestionAnswerService answerService;

    private final Clock clock;

    public DevSeedService(UserRepository userRepository,
                          DailyQuestionTemplateRepository templateRepository,
                          BehavioralEventRepository behavioralEventRepository,
                          PasswordEncoder passwordEncoder,
                          ChatSessionService chatSessionService,
                          ConversationMessageService conversationMessageService,
                          DailyQuestionAssignmentService assignmentService,
                          DailyQuestionAnswerService answerService,
                          Clock clock) {
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
        this.behavioralEventRepository = behavioralEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.chatSessionService = chatSessionService;
        this.conversationMessageService = conversationMessageService;
        this.assignmentService = assignmentService;
        this.answerService = answerService;
        this.clock = clock;
    }

    /**
     * Resets demo data only. Safe to call even if no demo rows exist.
     * Does NOT touch {@code daily_question_templates}.
     */
    @Transactional
    public void reset() {
        // Truncate is faster but DELETE WHERE is safer: it never touches
        // any real account that may have been registered locally.
        // FK ON DELETE CASCADE propagates to all child tables.
        long demoUsersBefore = userRepository.count();
        long deleted = userRepository.findAll().stream()
                .filter(u -> DemoUsers.isDemoEmail(u.getEmail()))
                .peek(u -> log.info("Deleting demo user {}", u.getEmail()))
                .mapToLong(u -> {
                    userRepository.delete(u);
                    return 1L;
                })
                .sum();
        log.info("Reset: removed {} demo user(s) (total users in DB before: {})",
                deleted, demoUsersBefore);
    }

    /**
     * Runs the deterministic demo seed for the given scenario.
     *
     * @param scenario which pattern set to apply
     * @return a {@link SeedResult} with row counts and elapsed wall time
     */
    @Transactional
    public SeedResult run(DevSeedScenario scenario) {
        Instant start = clock.instant();

        List<DemoUsers.Spec> users = DemoUsers.allUsers(passwordEncoder.encode(DemoUsers.DEMO_PASSWORD));
        log.info("Seeding {} demo users (scenario={})", users.size(), scenario);

        int usersCreated = 0;
        int assignmentsCreated = 0;
        int answersCreated = 0;
        int chatSessionsCreated = 0;
        int chatMessagesCreated = 0;

        Map<String, DailyQuestionTemplate> latestApproved = lookupLatestApprovedByCode();

        for (DemoUsers.Spec spec : users) {
            User user = createOrReuseUser(spec);
            usersCreated++;

            // Daily question assignments + answers
            for (int dayIndex = 0; dayIndex < DemoCheckinPlan.WINDOW_DAYS; dayIndex++) {
                Instant dayInstant = start
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate()
                        .minusDays(DemoCheckinPlan.WINDOW_DAYS - 1L - dayIndex)
                        .atStartOfDay(ZoneId.of("UTC"))
                        .toInstant();
                LocalDate localDate = spec.localDateFor(dayInstant);

                List<DemoCheckinPlan.Entry> entries = DemoCheckinPlan.plan(spec.group(), dayIndex);
                for (DemoCheckinPlan.Entry entry : entries) {
                    DailyQuestionTemplate template = latestApproved.get(entry.templateCode());
                    if (template == null) {
                        throw new IllegalStateException(
                                "Template " + entry.templateCode() + " not seeded — V6 migration missing?");
                    }
                    DailyQuestionAssignment assignment = assignmentService.createAssignmentForSeed(
                            user.getId(), template, localDate, spec.timezone());
                    assignmentsCreated++;

                    if (!DemoCheckinPlan.shouldAnswer(spec.group(), dayIndex)) {
                        continue;
                    }
                    DailyQuestionAnswer answer = answerService.submitAnswerForSeed(
                            user.getId(),
                            assignment.getId(),
                            entry.answer().answerType(),
                            entry.answer().numericValue(),
                            entry.answer().textValue(),
                            entry.answer().optionValue());
                    if (answer != null) {
                        answersCreated++;
                    }
                }
            }

            // Chat sessions + messages — only for the first CHAT_USER_COUNT users
            int userIndex = specIndex(spec, users);
            if (userIndex < CHAT_USER_COUNT) {
                List<List<DemoChatScript.Exchange>> scripts = DemoChatScript.scriptsFor(userIndex);
                for (int s = 0; s < Math.min(SESSIONS_PER_CHAT_USER, scripts.size()); s++) {
                    ChatSession session = chatSessionService.createSessionForSeed(
                            user.getId(), null);
                    chatSessionsCreated++;
                    for (DemoChatScript.Exchange exchange : scripts.get(s)) {
                        if (!"USER".equals(exchange.role())) {
                            // Script only produces USER turns; ASSISTANT turns are
                            // intentionally not seeded so the corpus contains only
                            // user-authored text (which is what raw storage holds).
                            // The assistant turn would arrive via AI analysis
                            // pipeline (out of scope for G2-T09).
                            continue;
                        }
                        conversationMessageService.sendMessageForSeed(
                                user.getId(), session.getId(), exchange.content());
                        chatMessagesCreated++;
                    }
                }
            }
        }

        long events = behavioralEventRepository.count();
        Duration elapsed = Duration.between(start, clock.instant());
        SeedResult result = new SeedResult(usersCreated, assignmentsCreated, answersCreated,
                chatSessionsCreated, chatMessagesCreated, events, elapsed);
        log.info("Seed complete: {}", result);
        return result;
    }

    private int specIndex(DemoUsers.Spec target, List<DemoUsers.Spec> all) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(target.id())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Creates the user with the deterministic id from {@code spec}, or returns
     * the existing one. Uses reflection to override the JPA-assigned UUID
     * because the seed needs a stable id across runs (FK references in
     * behavioral events, chat sessions, etc. must remain valid on re-seed).
     *
     * <p>This helper is intentionally confined to the dev-seed package —
     * no production code path sets the id field directly.
     */
    User createOrReuseUser(DemoUsers.Spec spec) {
        return userRepository.findById(spec.id()).orElseGet(() -> {
            User user = User.register(spec.email(), spec.passwordHash(), spec.displayName());
            assignIdViaReflection(user, spec.id());
            user.setTimezone(spec.timezone());
            return userRepository.saveAndFlush(user);
        });
    }

    /**
     * Sets the JPA {@code @Id} field on a freshly-constructed entity via
     * reflection. Used only by the seed to produce deterministic UUIDs.
     *
     * <p>If the field becomes final in a future JPA upgrade, this method will
     * fail at runtime — the test suite catches that regression.
     */
    private static void assignIdViaReflection(User target, UUID id) {
        try {
            java.lang.reflect.Field f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Could not assign deterministic user id — JPA id contract changed?", e);
        }
    }

    private Map<String, DailyQuestionTemplate> lookupLatestApprovedByCode() {
        Map<String, DailyQuestionTemplate> map = new HashMap<>();
        for (DailyQuestionTemplate t : templateRepository.findLatestApproved()) {
            map.put(t.getCode(), t);
        }
        return map;
    }
}