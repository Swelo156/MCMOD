package com.unstablecompanions.ai.dialogue;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.minecraft.entity.player.PlayerEntity;

import com.unstablecompanions.entity.CompanionEntity;

/**
 * Single seam for turning a companion's situation into a line of dialogue.
 *
 * The default implementation below is a static line-bank fallback so the mod
 * is fully playable with zero network dependency. To hook a real LLM:
 *
 *   1. Implement a second class (e.g. LlmDialogueProvider) that builds a
 *      prompt from CompanionEntity state (character id, personality traits,
 *      current relationship/tension, recent events) and calls out to either:
 *        - a local model server (llama.cpp / Ollama / LM Studio, plain HTTP),
 *          or
 *        - a cloud API (Anthropic/OpenAI/etc), via java.net.http.HttpClient.
 *   2. Do the network call OFF the server thread (see the ScheduledExecutorService
 *      below) and never block tick() waiting on it -- fire-and-forget, then
 *      apply the result via server.execute(...) once it resolves.
 *   3. Cache/rate-limit per entity to avoid spamming the API on every tick;
 *      only call out on discrete triggers (see {@link Trigger}), not
 *      continuously.
 *   4. Swap the INSTANCE below (e.g. behind a ModConfig toggle: "useLlmDialogue")
 *      so server owners without an API key/local model still get sensible
 *      fallback lines.
 */
public interface DialogueProvider {

	enum Trigger {
		GREETING,
		WORTHINESS_CONFRONTATION,   // FlameFrags
		REVENGE_HUNT_START,         // Wemmbu
		RESCUE_CALLOUT,             // SpokeIsHere
		PRISON_THREAT,              // Wifies
		SPY_INTEL_REPORT,           // JumperWho
		LEAVING_IN_DISAPPOINTMENT,  // JumperWho
		BETRAYAL,                   // Jaden_MAN
		GEAR_APPRECIATION,          // Jaden_MAN
		RIGHT_HAND_OFFER,           // any villain leader
		RECRUITMENT_PITCH,          // player faction / villain factions
		GENERIC_IDLE
	}

	/**
	 * Returns a line of dialogue for the given trigger. Implementations may
	 * return immediately (line-bank) or kick off async generation and return
	 * a provisional line, applying the "real" one later via chat message once
	 * ready -- do not make callers assume this is synchronous-fast forever.
	 */
	String generateLine(CompanionEntity companion, PlayerEntity player, Trigger trigger);

	static DialogueProvider getInstance() {
		return Holder.INSTANCE;
	}

	final class Holder {
		// Swap this for an LLM-backed implementation once one exists, e.g.:
		//   static DialogueProvider INSTANCE = ModConfig.INSTANCE.useLlmDialogue
		//       ? new LlmDialogueProvider() : new FallbackDialogueProvider();
		static DialogueProvider INSTANCE = new FallbackDialogueProvider();
	}

	/**
	 * Deterministic, zero-dependency placeholder implementation. Replace or
	 * wrap with an LLM-backed provider for real personality-driven text.
	 */
	class FallbackDialogueProvider implements DialogueProvider {

		// Reserved for a future LLM-backed provider: run network calls here so
		// the server tick thread is never blocked on I/O.
		protected final ScheduledExecutorService dialogueExecutor =
				Executors.newSingleThreadScheduledExecutor(r -> {
					Thread t = new Thread(r, "unstablecompanions-dialogue");
					t.setDaemon(true);
					return t;
				});

		private final Random random = new Random();

		private static final List<String> GENERIC_LINES = List.of(
				"Yeah, yeah, I'm here.",
				"What do you need?",
				"Busy day.",
				"..."
		);

		@Override
		public String generateLine(CompanionEntity companion, PlayerEntity player, Trigger trigger) {
			// TODO: replace this switch with a call into an LLM-backed provider.
			// Example async shape for that implementation:
			//
			//   CompletableFuture.supplyAsync(() -> callModel(buildPrompt(companion, player, trigger)), dialogueExecutor)
			//       .thenAccept(line -> player.sendMessage(Text.literal(line), false));
			//   return "..."; // provisional placeholder shown immediately, if needed
			return switch (trigger) {
				case WORTHINESS_CONFRONTATION -> companion.getCharacterId() + ": You need to prove you're worth it.";
				case REVENGE_HUNT_START -> companion.getCharacterId() + ": You didn't have to do that. Now I don't stop.";
				case RESCUE_CALLOUT -> companion.getCharacterId() + ": They took one of ours. Everybody, NOW.";
				case PRISON_THREAT -> companion.getCharacterId() + ": Nobody's getting out of what I build.";
				case SPY_INTEL_REPORT -> companion.getCharacterId() + ": My people saw something you should know about.";
				case LEAVING_IN_DISAPPOINTMENT -> companion.getCharacterId() + ": That wasn't smart. I'm out.";
				case BETRAYAL -> companion.getCharacterId() + ": Nothing personal. I just don't need you anymore.";
				case GEAR_APPRECIATION -> companion.getCharacterId() + ": Now THIS is more like it.";
				case RIGHT_HAND_OFFER -> companion.getCharacterId() + ": You've earned a real seat at the table.";
				case RECRUITMENT_PITCH -> companion.getCharacterId() + ": Join us. It's better on this side.";
				case GREETING, GENERIC_IDLE -> companion.getCharacterId() + ": " + randomGeneric();
			};
		}

		private String randomGeneric() {
			return GENERIC_LINES.get(random.nextInt(GENERIC_LINES.size()));
		}
	}
}
