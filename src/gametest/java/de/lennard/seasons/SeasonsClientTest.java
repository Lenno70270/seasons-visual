package de.lennard.seasons;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Echter Client gegen einen echten Server: Menü bedienen, alle Jahreszeiten durchschalten, Screenshots zum Ansehen. */
public class SeasonsClientTest implements FabricClientGameTest {
	private static final int SETTLE_TICKS = 100;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		String suffix = FabricLoader.getInstance().isModLoaded("sodium") ? "-sodium" : "-vanilla";
		try (TestDedicatedServerContext server = ctx.worldBuilder().createServer(new Properties());
				TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			server.runCommand("op @a");
			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear 100000");
			ctx.waitTicks(20);

			BlockPos home = ctx.computeOnClient(mc -> mc.player.blockPosition());
			int x = home.getX();
			int y = home.getY();
			int z = home.getZ();
			buildScene(ctx, server, x, y, z);
			// Im Kreativmodus fällt die Kamera nach jedem Teleport zu Boden, Zuschauer bleiben in der Luft.
			server.runCommand("gamemode spectator @a");
			server.runCommand("tp @a " + (x + 0.5) + " " + (y + 7) + " " + (z - 13.5) + " 0 22");
			ctx.waitTicks(40);
			connection.waitForChunksRender();

			// Bedienung wie ein Spieler: Taste J, dann Button.
			ctx.getInput().pressKey(GLFW.GLFW_KEY_J);
			ctx.waitForScreen(SeasonScreen.class);
			ctx.takeScreenshot("menu" + suffix);
			ctx.clickScreenButton("seasons_visual.season.autumn");
			ctx.waitFor(mc -> mc.gui.screen() == null);
			check(SeasonsVisual.season() == Season.AUTUMN, "Button stellt Herbst ein");
			settle(ctx, connection);
			ctx.takeScreenshot("herbst" + suffix);
			check(particleCount(ctx) > 0, "im Herbst fallen Blätter");

			select(ctx, connection, Season.SPRING);
			ctx.takeScreenshot("fruehling" + suffix);
			check(particleCount(ctx) > 0, "im Frühling fallen Blüten");

			select(ctx, connection, Season.WINTER);
			ctx.takeScreenshot("winter" + suffix);
			server.runCommand("tp @a " + (x + 0.5) + " " + (y + 7) + " " + (z - 13.5) + " 0 22");
			settle(ctx, connection);
			check(particleCount(ctx) > 50, "im Winter schneit es");

			// Der Schnee darf nur im Mesh existieren: Die Welt des Clients bleibt unverändert.
			BlockPos open = new BlockPos(x + 2, y, z - 6);
			check(ctx.computeOnClient(mc -> mc.level.getBlockState(open).isAir()), "kein echter Schneeblock in der Client-Welt");
			check(ctx.computeOnClient(mc -> !mc.level.getBlockState(open).is(Blocks.SNOW)), "kein Schneeblock");

			String config = readConfig();
			check(config.contains("WINTER"), "Winter steht in der Konfiguration: " + config);

			// Ein Ressourcen-Reload baut den Atlas neu. Das Aufhellen muss das überleben.
			ctx.runOnClient(mc -> mc.reloadResourcePacks());
			ctx.waitTicks(60);
			connection.waitForChunksRender();
			ctx.waitTicks(40);
			ctx.takeScreenshot("winter-nach-reload" + suffix);

			views(ctx, server, connection, "winter", suffix, x, y, z);
			select(ctx, connection, Season.SUMMER);
			ctx.takeScreenshot("sommer" + suffix);
			views(ctx, server, connection, "sommer", suffix, x, y, z);
		}
	}

	private static void buildScene(ClientGameTestContext ctx, TestDedicatedServerContext server, int x, int y, int z) {
		int ground = y - 1;
		server.runCommand("fill " + (x - 24) + " " + y + " " + (z - 16) + " " + (x + 24) + " " + (y + 24) + " " + (z + 28) + " air");
		server.runCommand("place feature minecraft:oak " + (x - 6) + " " + y + " " + (z + 2));
		server.runCommand("place feature minecraft:fancy_oak " + (x - 1) + " " + y + " " + (z + 9));
		server.runCommand("place feature minecraft:birch " + (x + 5) + " " + y + " " + (z + 2));
		server.runCommand("place feature minecraft:spruce " + (x + 10) + " " + y + " " + (z + 7));
		server.runCommand("place feature minecraft:oak " + (x - 11) + " " + y + " " + (z + 8));
		server.runCommand("place feature minecraft:birch " + (x + 1) + " " + y + " " + (z + 3));

		// Teich, Pflanzen, Dach und Treppe: Unter dem Dach darf kein Schnee liegen, auf dem Dach schon.
		server.runCommand("fill " + (x - 4) + " " + ground + " " + (z - 7) + " " + (x - 1) + " " + ground + " " + (z - 3) + " water");
		server.runCommand("fill " + (x + 1) + " " + y + " " + (z - 4) + " " + (x + 4) + " " + y + " " + (z - 2) + " short_grass");
		server.runCommand("setblock " + (x + 5) + " " + y + " " + (z - 3) + " poppy");
		server.runCommand("fill " + (x + 6) + " " + (y + 3) + " " + (z - 7) + " " + (x + 10) + " " + (y + 3) + " " + (z - 3) + " stone");
		server.runCommand("fill " + (x + 6) + " " + y + " " + (z - 7) + " " + (x + 6) + " " + (y + 2) + " " + (z - 7) + " oak_log");
		server.runCommand("fill " + (x + 10) + " " + y + " " + (z - 3) + " " + (x + 10) + " " + (y + 2) + " " + (z - 3) + " oak_log");
		server.runCommand("setblock " + (x - 8) + " " + y + " " + (z - 6) + " stone_slab");
		server.runCommand("setblock " + (x - 9) + " " + y + " " + (z - 6) + " oak_fence");

		// Plattform, deren Oberseite genau die letzte Ebene eines Abschnitts ist: Darüber liegt ein leerer Abschnitt.
		int sectionTop = (Math.floorDiv(y, 16) + 1) * 16 - 1;
		server.runCommand("fill " + (x - 20) + " " + sectionTop + " " + (z - 8) + " " + (x - 15) + " " + sectionTop + " " + (z - 3) + " stone");
		ctx.waitTicks(40);
	}

	/** Von der Seite gesehen, weil Schnee von oben vor Schnee nicht zu erkennen ist. */
	private static void views(ClientGameTestContext ctx, TestDedicatedServerContext server, TestDedicatedServerConnection connection, String name, String suffix, int x, int y, int z) {
		int sectionTop = (Math.floorDiv(y, 16) + 1) * 16 - 1;
		server.runCommand("tp @a " + (x - 17.5) + " " + (sectionTop + 2) + " " + (z - 13.5) + " 0 14");
		settle(ctx, connection);
		ctx.takeScreenshot(name + "-plattform" + suffix);
		server.runCommand("tp @a " + (x + 8.5) + " " + (y + 5) + " " + (z - 13.5) + " 0 16");
		settle(ctx, connection);
		ctx.takeScreenshot(name + "-dach" + suffix);
	}

	private static void select(ClientGameTestContext ctx, TestDedicatedServerConnection connection, Season season) {
		ctx.runOnClient(mc -> SeasonsVisual.setSeason(season));
		settle(ctx, connection);
	}

	private static void settle(ClientGameTestContext ctx, TestDedicatedServerConnection connection) {
		ctx.waitTicks(10);
		connection.waitForChunksRender();
		ctx.waitTicks(SETTLE_TICKS);
	}

	private static int particleCount(ClientGameTestContext ctx) {
		String summary = ctx.computeOnClient(mc -> mc.particleEngine.countParticles());
		return Integer.parseInt(summary.substring(summary.lastIndexOf(' ') + 1));
	}

	private static String readConfig() {
		try {
			return Files.readString(FabricLoader.getInstance().getConfigDir().resolve("seasons_visual.json"));
		} catch (java.io.IOException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
		SeasonsVisual.LOGGER.info("OK: {}", message);
	}
}
