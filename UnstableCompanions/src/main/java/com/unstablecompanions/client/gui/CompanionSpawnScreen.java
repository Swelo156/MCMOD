package com.unstablecompanions.client.gui;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.unstablecompanions.network.SpawnCompanionPayload;

/**
 * The "great UI for spawning stuff" entry point: a full-screen menu listing
 * every Unstable Companions character with a one-line description of their
 * signature mechanic. Selecting an entry sends a {@link SpawnCompanionPayload}
 * to the server, which does the actual (authoritative) spawn -- see
 * {@code network.ModNetworking}.
 *
 * Opened by right-clicking the Companion Spawner item (see
 * {@code UnstableCompanionsClient} for the interaction hook).
 *
 * Kept as a hand-built {@link Screen} (not a synced ScreenHandler/container)
 * since it has no persistent server-side state of its own -- it is purely a
 * picker that fires a single one-shot network message per click.
 */
public class CompanionSpawnScreen extends Screen {

	private record Entry(String id, String displayName, String tagline, Formatting accent) {}

	private static final List<Entry> COMPANIONS = List.of(
			new Entry("flame_frags", "FlameFrags", "Judges your worthiness. Cross him and he confronts you.", Formatting.GOLD),
			new Entry("wemmbu", "Wemmbu", "Loyal to Eggchan. Hurt her and he never stops hunting you.", Formatting.RED),
			new Entry("spoke_is_here", "SpokeIsHere", "Ride-or-die. Goes all-out the moment you're captured.", Formatting.AQUA),
			new Entry("wifies", "Wifies", "Builds prisons nobody escapes. Mostly safe if you're friends.", Formatting.LIGHT_PURPLE),
			new Entry("jumper_who", "JumperWho", "Runs a spy network. Leaves if you get reckless too often.", Formatting.BLUE),
			new Entry("jaden_man", "Jaden_MAN", "Opportunistic. Keep him geared or he'll find a reason to leave.", Formatting.YELLOW),
			new Entry("lettuce_k", "LettuceK", "Founder of The Law. Recruits for order.", Formatting.GRAY),
			new Entry("ashswagg", "Ashswagg", "Runs the Invisible Mafia. Secretive, dangerous hits.", Formatting.DARK_GRAY),
			new Entry("jamato_p", "JamatoP", "Builds NULL/Purgatory. Hunter groups patrol for captives.", Formatting.DARK_RED)
	);

	private static final int ROW_HEIGHT = 26;
	private static final int PANEL_WIDTH = 320;

	public CompanionSpawnScreen() {
		super(Text.literal("Spawn a Companion"));
	}

	@Override
	protected void init() {
		super.init();
		int panelX = (this.width - PANEL_WIDTH) / 2;
		int startY = Math.max(40, this.height / 2 - (COMPANIONS.size() * ROW_HEIGHT) / 2);

		for (int i = 0; i < COMPANIONS.size(); i++) {
			Entry entry = COMPANIONS.get(i);
			int y = startY + i * ROW_HEIGHT;

			ButtonWidget button = ButtonWidget.builder(
					Text.literal(entry.displayName()).formatted(entry.accent(), Formatting.BOLD),
					btn -> spawnAndClose(entry)
			).dimensions(panelX, y, PANEL_WIDTH, ROW_HEIGHT - 4).build();

			this.addDrawableChild(button);
		}

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> this.close())
				.dimensions(panelX, startY + COMPANIONS.size() * ROW_HEIGHT + 8, PANEL_WIDTH, 20)
				.build());
	}

	private void spawnAndClose(Entry entry) {
		ClientPlayNetworking.send(new SpawnCompanionPayload(entry.id()));
		this.close();
	}

	@Override
	public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);

		// Tagline: show the hovered character's mechanic under the title
		// rather than crowding every button, since taglines are long.
		int panelX = (this.width - PANEL_WIDTH) / 2;
		int startY = Math.max(40, this.height / 2 - (COMPANIONS.size() * ROW_HEIGHT) / 2);
		for (int i = 0; i < COMPANIONS.size(); i++) {
			int y = startY + i * ROW_HEIGHT;
			if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= y && mouseY <= y + ROW_HEIGHT - 4) {
				context.drawCenteredTextWithShadow(
						this.textRenderer,
						Text.literal(COMPANIONS.get(i).tagline()).formatted(Formatting.ITALIC, Formatting.GRAY),
						this.width / 2, 30, 0xAAAAAA);
			}
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
