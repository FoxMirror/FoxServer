package catserver.server.eventhandler;

import catserver.server.FoxServer;
import catserver.server.eventhandler.events.BlockEventDispatcher;
import net.minecraftforge.common.MinecraftForge;

public class EventHandler {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new BlockEventDispatcher());
        FoxServer.LOGGER.info("Initialization FoxServer EventHandler.");
    }
}
