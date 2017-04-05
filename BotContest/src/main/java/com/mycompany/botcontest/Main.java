package com.mycompany.botcontest;

import java.util.logging.Level;

//import cz.cuni.amis.introspection.java.JProp;
//import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
//import cz.cuni.amis.pogamut.base.agent.navigation.impl.PrecomputedPathFuture;
//import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
//import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
//import cz.cuni.amis.pogamut.base3d.worldview.object.Rotation;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.SendMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
//import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GlobalChat;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
//import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;

/**
 * @author Anthony BORDEAU
 * @author Corentin MADRE
 * @author Alexandre LOOUVEAU
 * @author Kellian BALLENTINE
 */
@AgentScoped
public class Main extends UT2004BotModuleController {

    public String stringProp = "Hello bot example";
    public boolean boolProp = true;
    public int intProp = 2;
    public double doubleProp = 1.0;

    private Player lastPlayer;
    private long   lastLogicTime        = -1;
    private long   logicIterationNumber = 0;    

    public void prepareBot(UT2004Bot bot) {
        // By uncommenting following line, you can make the bot to do the file logging of all its components
        //bot.getLogger().addDefaultFileHandler(new File("EmptyBot.log"));
    }
    
    public Initialize getInitializeCommand() {
    	return new Initialize().setName("Wallah").setSkin("Kragoth");        
    }

    /**
     * Handshake with GameBots2004 is over - bot has information about the map
     * in its world view. Many agent modules are usable since this method is
     * called.
     *
     * @param gameInfo informaton about the game type
     * @param config information about configuration
     * @param init information about configuration
     */
    public void botInitialized(GameInfo gameInfo, ConfigChange config, InitedMessage init) {
    	// By uncommenting line below, you will see all messages that goes trough GB2004 parser (GB2004 -> BOT communication)
    	//bot.getLogger().getCategory("Parser").setLevel(Level.ALL);
    }

    private void attack() {
        move.turnTo(lastPlayer.getLocation());
        move.jump();
        move.moveTo(lastPlayer.getLocation());
        body.getShooting().shoot(lastPlayer);
        if (!lastPlayer.isVisible()) {
            lastPlayer = null;
            shoot.stopShooting();
        }
    }
    
    /**
     * The bot is initilized in the environment - a physical representation of
     * the bot is present in the game.
     *
     * @param gameInfo informaton about the game type
     * @param config information about configuration
     * @param init information about configuration
     * @param self information about the agent
     */
    public void botFirstSpawn(GameInfo gameInfo, ConfigChange config, InitedMessage init, Self self) {
        // Display a welcome message in the game engine
        // right in the time when the bot appears in the environment, i.e., his body has just been spawned 
        // into the UT2004 for the first time.    	
        body.getCommunication().sendGlobalTextMessage("Hello world! I am alive!");

        // alternatively, you may use getAct() method for issuing arbitrary {@link CommandMessage} for the bot's body
        // inside UT2004
        act.act(new SendMessage().setGlobal(true).setText("And I can speak! Hurray!"));

    }
    
    // <=> firstSpawn
    public void beforeFirstLogic() {
    }
    
    private void sayGlobal(String msg) {
    	body.getCommunication().sendGlobalTextMessage(msg);
    	log.info(msg);
    }
    
    public void botKilled(BotKilled event) {
        move.moveTo(lastPlayer.getLocation());
        if (lastPlayer.isVisible())
            attack();
    }
    
    public void botDamaged(BotDamaged event) {
        lastPlayer = players.getNearestVisiblePlayer();
        attack();
    }

    int num;
    
    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second - it
     * is affected by visionTime variable, that can be adjusted in ini file
     * inside UT2004/System/GameBots2004.ini
     *
     */
    public void logic() throws PogamutException {
    	log.log(Level.INFO, "---LOGIC: {0}---", ++logicIterationNumber);  
    	    	
        // Mark that new logic iteration has begun        
        
        // Log logic periods
        long currTime = System.currentTimeMillis();
        if (lastLogicTime > 0) log.log(Level.INFO, "Logic invoked after: {0} ms", currTime - lastLogicTime);
        lastLogicTime = currTime;
        if (players.canSeePlayers()) {
            lastPlayer = players.getNearestVisiblePlayer();
            if (players.isEnemy(lastPlayer))
                attack();
        }
        // Uncomment next block to enable "follow-bot" behavior
        /*        
        // Can I see any player?
        if (players.canSeePlayers()) {
            // YES!
            log.info("Can see any player/s?: YES");
            // Set my target to nearest visible player ...
            lastPlayer = players.getNearestVisiblePlayer();
            
            // ... and try to move with straight movement (without any navigation)
            log.info("Running directly to: " + lastPlayer.getId());
            move.moveTo(players.getNearestVisiblePlayer());            
            // We've just switched to manual movement ... stop path navigation if running
            if (navigation.isNavigating()) {
                navigation.stopNavigation();
            }
        } else {
            // NO, I cannot see any player
            log.info("Can see any player/s?: NO");
            
            if (lastPlayer == null) {
                log.info("lastPlayer == null ... no target to pursue, turning around");
                move.turnHorizontal(30);
            } else {
                log.info("lastPlayer == " + lastPlayer.getId() + " ... going to pursue him/her/it");
                // Yes, I should try to get to its last location
                if (info.getDistance(lastPlayer) < 200) { // are we at the last
                    log.info("Arrived to lastPlayer's last known location.");
                    move.turnTo(lastPlayer.getLocation());
                    if (info.isFacing(lastPlayer.getLocation())) {
                        lastPlayer = null;
                    }
                } else {
                    // We are still far from the last known player position
                    // => just tell the navigation to guide us there
                    log.info("Navigating to lastPlayer's last known location.");
                    navigation.navigate(lastPlayer);                    
                }
            }
        }
        */
    }

    public static void main(String args[]) throws PogamutException {
        new UT2004BotRunner(     // class that wrapps logic for bots executions, suitable to run single bot in single JVM
                Main.class,  // which UT2004BotController it should instantiate
                "EmptyBot"       // what name the runner should be using
        ).setMain(true)          // tells runner that is is executed inside MAIN method, thus it may block the thread and watch whether agent/s are correctly executed
         .startAgent();          // tells the runner to start 1 agent

        // It is easy to start multiple bots of the same class, comment runner above and uncomment following
        // new UT2004BotRunner(EmptyBot.class, "EmptyBot").setMain(true).startAgents(3); // tells the runner to start 3 agents at once
    }
}
