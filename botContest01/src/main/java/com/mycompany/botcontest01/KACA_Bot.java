/*
 * Copyright (C) 2017 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mycompany.botcontest01;
/**
 *
 * @author Alexandre
 */


import cz.cuni.amis.introspection.java.JProp;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.base3d.worldview.object.Velocity;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.*;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.*;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.vecmath.Vector3d;


@AgentScoped
public class KACA_Bot extends UT2004BotModuleController<UT2004Bot> {
    
    /**
     * booléen pour engager le combat avec un ennemi
     */
    @JProp
    public boolean engage = true;
    /**
     * booléen pour poursuivre un ennemi
     */
    @JProp
    public boolean hunt = true;
    /**
     * booléen pour savoir s'il doit se réarmer
     */
    @JProp
    public boolean rearm = true;
    /**
     * booléen pour aller chercher de la vie
     */
    @JProp
    public boolean healthCollect = true;
    /**
     * niveau de vie avant de chercher à en récupérer
     */
    @JProp
    public int health = 60;
    /**
     * nombre de tuer du bot
     */
    @JProp
    public int kills = 0;
    /**
     * nombre de morts du bot
     */
    @JProp
    public int deaths = 0;

    
    private long escapeCount = 0;
    
    private long logicIterationEscape;
    private final long logic_escape = 30;
    
    final int longRay = 400;
    final int mediumRay = 300;
    final int shortRay = 200;
    
    private boolean injured = false;
    private boolean escape = false;
    private boolean justEscaped = false;
    private boolean isLowAmmoShieldGun = false;
    private boolean turn = false;
    
    private long   logicIterationNumber = 0;

    private final int enought_hp = 85;
    private final double horizontalSpeed = 5;
    private final double lowAmmoShieldGun = 0.6;
    private final double secondJumpDelay = 0.5;
    private final double jumpZ = 680;
    private int rotation;
    
    private UT2004ItemType weaponSelected;
    
    protected static final String BACK = "Back";
    protected static final String LEFT = "Left";
    protected static final String RIGHT = "Right";
    protected static final String BEHIND = "Behind";
    protected static final String LEFTSHOT = "leftshotRay";
    protected static final String UPSHOT = "upshotRay";
    protected static final String RIGHTSHOT = "rightRay";
    protected static final String FRONT = "frontRay";
    protected static final String UNDERSHOT = "undershotRay";

    private boolean sensorFront = false;
    
    private AutoTraceRay left, back, right, behind,leftshot,undershot,rightshot,upshot,front;
    
    boolean leftB, backB, rightB, behindG;
    
    //UT2004ItemType arme=UT2004ItemType.ROCKET_LAUNCHER;
    //UT2004ItemType munition=UT2004ItemType.ROCKET_LAUNCHER_AMMO;
   
    /**
     * {@link JoueurTuer} compteur pour le nombre de tuer.
     * @param event
     */
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
        getAct().act(new SetCrouch(false));
        if (event.getKiller().equals(info.getId())) {
            ennemi = null;
            escape = false;
            ++kills;
        }
        if (ennemi == null) {
            return;
        }
        if (ennemi.getId().equals(event.getId())) {
            ennemi = null;
            escape = false;
        }
        
    }
    
    private Velocity oldVelocity1;
    private int modu=0;
    protected Player ennemi = null;
    protected double distance = 0.0;
    protected double coeff;
    protected double Zdistance;    /**
     * Item visé
     */
    protected Item item = null;
   
    protected TabooSet<Item> useless = null;
    private UT2004PathAutoFixer autoFixer;
    private static int instanceCount = 0;

    /**
     * Preparation du bot avant son entrer sur le serveur.
     * @param bot
     */
    @Override
    public void prepareBot(UT2004Bot bot) {
        useless = new TabooSet<Item>(bot);

        autoFixer = new UT2004PathAutoFixer(bot, navigation.getPathExecutor(), fwMap, aStar, navBuilder);

        // listeners        
        navigation.getState().addListener(new FlagListener<NavigationState>() {

            @Override
            public void flagChanged(NavigationState changedValue) {
                switch (changedValue) {
                    case PATH_COMPUTATION_FAILED:
                    case STUCK:
                        if (item != null) {
                            useless.add(item, 10);
                        }
                        reset();
                        break;

                    case TARGET_REACHED:
                        reset();
                        break;
                }
            }
        });

        // préférence des armes
        weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);
    }

    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
    	// By uncommenting line below, you will see all messages that goes trough GB2004 parser (GB2004 -> BOT communication)
    	//bot.getLogger().getCategory("Parser").setLevel(Level.ALL);
        
        config.setRotationHorizontalSpeed(config.getRotationSpeed().yaw * horizontalSpeed);
        //itemsToIgnore = new TabooSet<Item>(bot);
        
        boolean fastTrace = true;        
        boolean floorCorrection = false; 
        boolean traceActor = false;

        getAct().act(new RemoveRay("All"));
        
        raycasting.createRay(BACK,   new Vector3d(-1, 0, 0), longRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(LEFT,  new Vector3d(-1, -1, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(RIGHT, new Vector3d(-1, 1, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(BEHIND, new Vector3d(-1, 0, -2), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(FRONT,   new Vector3d(1, 0, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(UNDERSHOT,   new Vector3d(1, 0, -0.03), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(UPSHOT,   new Vector3d(1, 0, 0.03), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(LEFTSHOT,   new Vector3d(1, -0.03, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(RIGHTSHOT,   new Vector3d(1, 0.03, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        
         // register listener called when all rays are set up in the UT engine
        raycasting.getAllRaysInitialized().addListener(new FlagListener<Boolean>() {

            public void flagChanged(Boolean changedValue) {
                // once all rays were initialized store the AutoTraceRay objects
                // that will come in response in local variables, it is just
                // for convenience
                left = raycasting.getRay(LEFT);
                back = raycasting.getRay(BACK);
                right = raycasting.getRay(RIGHT);
                behind = raycasting.getRay(BEHIND);
                front = raycasting.getRay(FRONT);
                undershot=raycasting.getRay(UNDERSHOT);
                upshot=raycasting.getRay(UPSHOT);
                leftshot=raycasting.getRay(LEFTSHOT);
                rightshot=raycasting.getRay(RIGHTSHOT);
            }
        });
        
        raycasting.endRayInitSequence();
        getAct().act(new Configuration().setDrawTraceLines(true).setAutoTrace(true));
        
    }
    
    /**
     * initialisation du bot
     * @return
     */
    @Override
    public Initialize getInitializeCommand() {
        //setDesiredSkill(int skill) -> varie de 1 à 9 pour choisir le "niveau de visée du bot"
        return new Initialize().setName("responsiveBot" + (++instanceCount)).setDesiredSkill(5);
    }

    /**
     * réinitialise la navigation du bot
     */
    protected void reset() {
        item = null;
        ennemi = null;
        navigation.stopNavigation();
        CollectItems = null;
        injured = false;
        escape = false;
        isLowAmmoShieldGun = false;
        turn = false;
        huntCount = 0;
        escapeCount = 0;
    }

    @EventListener(eventClass = PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
        log.log(Level.INFO, "J'ai infligé: {0}[{1}]", new Object[]{event.getDamageType(), event.getDamage()});
        
    }

    @EventListener(eventClass = BotDamaged.class)
    public void botDamaged(BotDamaged event) {
        log.log(Level.INFO, "J'ai reçu: {0}[{1}]", new Object[]{event.getDamageType(), event.getDamage()});
        ennemi = players.getPlayer(event.getInstigator());
        if(ennemi != null)
            move.turnTo(ennemi);
        injured = true;
    }

    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second - it
     * is affected by visionTime variable, that can be adjusted in GameBots ini
     * file in UT2004/System folder.
     *
     * @throws cz.cuni.amis.pogamut.base.exceptions.PogamutException
     */
    @Override
    public void logic() {
        
        //prends une arme de la liste des preferences
        weaponry.changeWeapon(weaponPrefs.getWeaponPreference().getWeapon());

        if(justEscaped){
            ++logicIterationNumber;
            if (logicIterationNumber > 8){
                justEscaped = false;
                logicIterationNumber = 0;
            }
        }
        
        if (info.getHealth() < 40 && !justEscaped) 
            escape = true;
        else 
            escape = false;
        
        // 1) niveau de vie faible
        if (ennemi != null && escape && !hasLowAmmoForWeapon(UT2004ItemType.SHIELD_GUN, 0.1)) {
            this.escapeState();
            return;
        }
        else { 
            escape = false;
        }
        

        // 2) ennemi repéré ? 	-> poursuivre (tirer / suivre)
        if (engage && players.canSeeEnemies() && weaponry.hasLoadedWeapon()) {
            initRayToEnnemi();
            engageState();
            return;
        }
        navigation.setFocus(null);
        // 3) en train de tiré    -> arrête de tiré si l'ennemi est perdu de vue
        if (info.isShooting() || info.isSecondaryShooting()) {
            getAct().act(new StopShooting());
            //remise a zero des informations concernant l'ancien ennemi visible
            oldVelocity1=null;
            modu=0;
        }

        // 4) dommages reçu ?	-> tourne sur lui même pour chercher l'ennemi
        if (senses.isBeingDamaged()) {
            this.injuredState();
            return;
        }
        
        // 5) ennemi poursuivis -> va à la dernière position connue de l'ennemi
        if (ennemi != null && hunt && weaponry.hasLoadedWeapon()) {
            this.huntState();
            return;
        }
        
        // 6) blessé ?			-> cherche des soins
        if (healthCollect && info.getHealth() < health) {
            this.healingState();
            return;
        }

        // 7) rien ... ramasses les items
        collectState();
    }

    protected boolean goToPlayer = false;

    protected void engageState() {
        //log.info("Decision: Engager");
        //config.setName("Hunter [Engager]");

        boolean fire = false;
        huntCount = 0;
        escapeCount = 0;
        
        // 1) choisis un nouvel ennemi si le précédent est perdu de vue
        if (ennemi == null || !ennemi.isVisible()) {
            ennemi = players.getNearestVisiblePlayer(players.getVisibleEnemies().values());
            if (ennemi == null) {
                log.info("Pas d'ennemi en vue !");
                return;
            }
        }

        // 2) si l'ennemi n'est plus visible -> arrête de tirer
        if (!ennemi.isVisible()) {
            if (info.isShooting() || info.isSecondaryShooting()) {
                getAct().act(new StopShooting());
            }
            goToPlayer = false;
        } else {
            // 2) tires sur l'ennemi s'il est visible
            
            navigation.setFocus(ennemi.getLocation().add(ennemi.getVelocity()));
            /*
            if (shoot.shoot(weaponPrefs, ennemi) != null) {
                log.info("Tirer sur l'ennemi!!!");
                fire = true;
            }*/
            if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.ROCKET_LAUNCHER) 
                shootRocketLauncher(ennemi);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.LINK_GUN)
                shootLinkGun(ennemi);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.FLAK_CANNON)
                shootFlakCannon(ennemi);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.SHOCK_RIFLE)
                shootShockRifle(ennemi);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.BIO_RIFLE)
                shootBioRifle(ennemi);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.MINIGUN)
                shootMiniGun(ennemi);
            //else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.ASSAULT_RIFLE)
                //shootAssault(ennemi);
            else 
            shoot.shoot(ennemi);
	    fire=true;
        }
        
        // 3) Si l'ennemis n'est pas visible ou trop loin -> vas vers lui
        int distSuffisante = Math.round(random.nextFloat() * 800) + 200;
        if (!ennemi.isVisible() || !fire || distSuffisante < distance) {
            if (!goToPlayer) {
                navigation.navigate(ennemi);
                goToPlayer = true;
            }
        } else {
            goToPlayer = false;
            navigation.stopNavigation();

            int choixBot = getRandom().nextInt(4);
            switch (choixBot) {
                case 0:
                    getAct().act(new SetCrouch(true));
                    break;
                case 1:
                    getAct().act(new Jump(getRandom().nextBoolean(), 0.3d, 755d));
                    break;
                case 2:
                    getAct().act(new SetCrouch(false));
                    break;
                case 3:
                    getAct().act(new Dodge(ennemi.getLocation().invert(), Location.NONE, false, getRandom().nextBoolean()));
                    break;
            }
        }

        item = null;
    }

    ///////////////////
    // Etat Toucher //
    /////////////////
    protected void injuredState() {
        //log.info("Decision is: Toucher");
        bot.getBotName().setInfo("Toucher");
        if (navigation.isNavigating()) {
            navigation.stopNavigation();
            item = null;
        }
        getAct().act(new Rotate().setAmount(25555));
    }

    //////////////////////
    // Etat Poursuite  //
    ////////////////////
    protected void huntState() {
        log.info("Decision: PURSUE");
            ++huntCount;
            if (huntCount > 30) {
                reset();
            }
            if (ennemi != null) {
                bot.getBotName().setInfo("Poursuivre");
                navigation.navigate(ennemi);
                item = null;
            } else {
                reset();
            }
    }
    protected int huntCount = 0;

    
    ///////////////////
    //  Etat Fuite  //
    /////////////////
    protected void escapeState(){
        if (escapeCount == 0) {
            getAct().act(new SetCrouch(false));
        }
        ++escapeCount;
        if(ennemi == null){
            justEscaped = true;
            reset();
        }
        
        bot.getBotName().setInfo("Escape");

        if(ennemi.isVisible())
            escapeCount = 0;

        if(escapeCount < 5){  
            goBackward();
            protect();
        }
        else{
            justEscaped = true;
            reset();
        }
    }
    
    ///////////////////
    //  Etat Soins  //
    /////////////////
    protected void healingState() {
        //log.info("Décision: Soins");
        Item newItem = items.getPathNearestSpawnedItem(ItemType.Category.HEALTH);
        if (newItem == null) {
            log.warning("Pas d'item de vie => Items");
            collectState();
        } else {
            bot.getBotName().setInfo("Soins");
            navigation.navigate(newItem);
            this.item = newItem;
        }
    }
    ///////////////////////////
    // Etat Collecter Items //
    /////////////////////////
    protected List<Item> CollectItems = null;

    protected void collectState() {
        //log.info("Décision: ITEMS");
        //config.setName("Hunter [ITEMS]");
        if (navigation.isNavigatingToItem()) {
            return;
        }

        List<Item> objetInteressant = new ArrayList<Item>();

        // Ajouter Arme
        for (ItemType itemType : ItemType.Category.WEAPON.getTypes()) {
            if (!weaponry.hasLoadedWeapon(itemType)) {
                objetInteressant.addAll(items.getSpawnedItems(itemType).values());
            }
        }
        // Ajouter Armure
        for (ItemType itemType : ItemType.Category.ARMOR.getTypes()) {
            objetInteressant.addAll(items.getSpawnedItems(itemType).values());
        }
        // Ajouter Quad
        objetInteressant.addAll(items.getSpawnedItems(UT2004ItemType.U_DAMAGE_PACK).values());
        // Ajouter Vie
        if (info.getHealth() < 100) {
            objetInteressant.addAll(items.getSpawnedItems(UT2004ItemType.HEALTH_PACK).values());
        }

        Item newItem = MyCollections.getRandom(useless.filter(objetInteressant));
        if (newItem == null) {
            log.warning("Pas d'items à collecter!");
            if (navigation.isNavigating()) {
                return;
            }
            bot.getBotName().setInfo("Navigation aléatoire");
            navigation.navigate(navPoints.getRandomNavPoint());
        } else {
            this.item = newItem;
            log.log(Level.INFO, "Collecte: {0}", newItem.getType().getName());
            bot.getBotName().setInfo("Item: " + newItem.getType().getName() + "");
            navigation.navigate(newItem);
        }
    }

    ///////////////
    // Bot Tuer //
    /////////////
    @Override
    public void botKilled(BotKilled event) {
        reset();
    }

//    public static void main(String args[]) throws PogamutException {
//        /**
//         * Instanciation d'un bot.
//         */    
//        new UT2004BotRunner(KACA_Bot.class, "responsiveBot").setMain(true).setLogLevel(Level.INFO).startAgents(1);
//    }
    
    
    public boolean hasLowAmmoForWeapon(UT2004ItemType weapon, double ratio){
        return((double)weaponry.getWeaponAmmo(weapon)/(double)weaponry.getWeaponDescriptor(weapon).getPriMaxAmount() < ratio);
    }
    
    public void goBackward(){
        
            leftB = left.isResult();
            backB = back.isResult();
            rightB = right.isResult();
            behindG = behind.isResult();
            
            //Si il y a du vide derrière lui
            if(!behindG){
                move.stopMovement();
                System.out.println("Je vais tomber !");
                return;
            }
            
            //si un obstacle detecté pour back
            if (backB){
                turn = true;
                //si pas d'obstacle detecté pour left
                if (!leftB) {
                    //move.strafeLeft(pas, ennemi);
                    rotation = 45;
                }//si pas d'obstacle detecté pour right
                else if (!rightB) {
                    //move.strafeRight(pas, ennemi);
                    rotation = -45;
                }//sinon impasse
                else{
                    //cas ou les 3 rayons sont rouges
                }
            }
            
            
            
        if(turn){
            double sinAlpha = Math.sin(rotation);
            double cosAlpha = Math.cos(rotation);
            Location l = Location.sub(bot.getLocation(), ennemi.getLocation());
            double xPrime = (l.x * cosAlpha - l.y * sinAlpha);
            double yPrime = (l.x * sinAlpha + l.y * cosAlpha);
            double z = l.z;
            Location lPrime = new Location(xPrime, yPrime, z).scale(100);
            l = Location.add(bot.getLocation(), lPrime);
            move.strafeTo(l, ennemi);
            turn = false;
        }
        else{
            //direction de l'ennemi
            Location l = Location.add(bot.getLocation(), Location.sub(bot.getLocation(), ennemi.getLocation()));
            move.strafeTo(l, ennemi);
        }
        
    }
    public void protect(){
        //si il possède le shield_gun, il a des munitions et l'ennemi est en vue
        weaponSelected = UT2004ItemType.SHIELD_GUN;
        
        
        //System.out.println((double)weaponry.getWeaponAmmo(weapon)/(double)weaponry.getWeaponDescriptor(weapon).getPriMaxAmount());

        if(weaponry.hasWeapon(weaponSelected)){
            boolean hasAmmoForWeapon = weaponry.hasAmmoForWeapon(weaponSelected);
            if(hasAmmoForWeapon && ennemi.isVisible() && !isLowAmmoShieldGun){
                info.getBotName().setInfo("Protect");
                //false => tir secondaire
                shoot.shootNow(weaponry.getWeapon(weaponSelected), false, ennemi.getId());
            }
            else{
                shoot.stopShooting();
                if(!hasAmmoForWeapon || isLowAmmoShieldGun){
                    isLowAmmoShieldGun = hasLowAmmoForWeapon(weaponSelected, lowAmmoShieldGun);
                    
                    //definir une action si il n'a plus de shield
                    info.getBotName().setInfo("LowAmmoShieldGun");
                }
            }
        }
        else{
            info.getBotName().setInfo("Can't Protect");
        }

    }
    
    public static void main(String[] args) throws PogamutException {
        
        String host = "localhost";
        int port = 3000;

        if (args.length > 0)
        {
                host = args[0];
        }
        if (args.length > 1)
        {
                String customPort = args[1];
                try
                {
                        port = Integer.parseInt(customPort);
                }
                catch (Exception e)
                {
                        System.out.println("Invalid port. Expecting numeric. Resuming with default port: "+port);
                }
        }
        
        UT2004BotRunner runner = new UT2004BotRunner(KACA_Bot.class, "K-A-C-A", host, port);
        runner.setMain(true);
        runner.setLogLevel(Level.INFO);
        runner.startAgents(1);
    }
    private double calculCoeff(double distance){
    double coeff=1;
    if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.ROCKET_LAUNCHER )
        coeff =distance/1350;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.LINK_GUN )
        coeff =distance/1500;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.SHOCK_RIFLE )
        coeff = distance/1150;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.BIO_RIFLE )
        coeff = distance/2000;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.ASSAULT_RIFLE )
        coeff = distance/933.33;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.FLAK_CANNON )
        coeff = distance/1200;


    return coeff;
}
    private void initRayToEnnemi(){
        ennemi=players.getNearestEnemy(5.0);
        distance=ennemi.getLocation().getDistance(bot.getLocation());
        coeff =calculCoeff(distance);
        final int rayLength = (int) (ennemi.getLocation().add(ennemi.getVelocity().scale(coeff).asLocation()).getDistance(bot.getLocation()))-150;
        Zdistance=ennemi.getLocation().setZ(ennemi.getLocation().getZ()+20).add(ennemi.getVelocity()).getDistanceZ(bot.getLocation())/distance ;;          
        raycasting.createRay(FRONT,   new Vector3d(1, 0, Zdistance), rayLength, true, false, false);
    }
    private IncomingProjectile pickProjectile() {
		return DistanceUtils.getNearest(world.getAll(IncomingProjectile.class).values(), info.getLocation());
                /*if (seeIncomingProjectile()){
                                    proj =pickProjectile();
                                    sayGlobal(String.valueOf(proj.getSpeed()));
                                }*/
	}
        
      private void sayGlobal(String msg) {
    	// Simple way to send msg into the UT2004 chat
    	body.getCommunication().sendGlobalTextMessage(msg);
    	// And user log as well
    	log.info(msg);
    }
      private boolean seeIncomingProjectile() {
    	for (IncomingProjectile proj : world.getAll(IncomingProjectile.class).values()) {
    		if (proj.isVisible()) return true;

    	}
		return false;
	}
    boolean green=false;
   
    
      private void shootRocketLauncher(Player lastPlayer){
         if (lastPlayer!=null){
            if (distance < 500){
                move.jump();
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-40));
            }
            else {
                if (modu==0){
                    oldVelocity1=lastPlayer.getVelocity().scale(coeff);
                }
                modu=(modu+1) %2;
                raycasting.createRay(UNDERSHOT,   new Vector3d(1, 0, Zdistance-0.03), (int)distance, true, false, false);
                sensorFront=undershot.isResult();
                if (!sensorFront||green){
                    if ((oldVelocity1.getX()-lastPlayer.getVelocity().scale(coeff).getX()>150 || oldVelocity1.scale(coeff).getX()-lastPlayer.getVelocity().scale(coeff).getX()<-150)|| (oldVelocity1.scale(coeff).getY() -lastPlayer.getVelocity().scale(coeff).getY()>150 || oldVelocity1.scale(coeff).getY()-lastPlayer.getVelocity().scale(coeff).getY()<-150)){
                        if (jukeTEMP <=1){
                            shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-40));
                        }
                        jukeTEMP =0;
                        return;
                    }
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-40).add(lastPlayer.getVelocity().scale(coeff)));
                    jukeTEMP=+1;
                    green=sensorFront;                
                }
                else{
                        raycasting.createRay(FRONT,   new Vector3d(1, 0, Zdistance), (int)distance, true, false, false);
                        sensorDown=front.isResult();

                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }

                    raycasting.createRay(UPSHOT,   new Vector3d(1, 0, Zdistance+0.03), (int)distance, true, false, false);
                    
                    sensorDown=upshot.isResult();

                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()+40).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }

                    raycasting.createRay(LEFTSHOT,   new Vector3d(1, -0.03, Zdistance), (int)distance, true, false, false);
                    sensorDown=leftshot.isResult();
                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()-40).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }

                    raycasting.createRay(RIGHTSHOT,   new Vector3d(1, 0.03, Zdistance), (int)distance, true, false, false);
                    sensorDown=rightshot.isResult();
                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()+40).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }
                   
                    
                    jukeTEMP=+1;
                    shoot.shoot(ennemi);
                    green=false;
                }
            }     
        }
    }
 private int jukeTEMP;
 private boolean sensorDown;
 private void shootLinkGun(Player lastPlayer){

        if (lastPlayer!=null){
            if (distance < 700)
                shoot.shootSecondary(lastPlayer);
            else {
                if (modu==0){
                    oldVelocity1=lastPlayer.getVelocity().scale(coeff);
                }
                modu=(modu+1) %2;
		sensorFront=front.isResult();
                if (!sensorFront||green){
                    if ((oldVelocity1.getX()-lastPlayer.getVelocity().scale(coeff).getX()>150 || oldVelocity1.scale(coeff).getX()-lastPlayer.getVelocity().scale(coeff).getX()<-150)|| (oldVelocity1.scale(coeff).getY() -lastPlayer.getVelocity().scale(coeff).getY()>150 || oldVelocity1.scale(coeff).getY()-lastPlayer.getVelocity().scale(coeff).getY()<-150)){
                        if (jukeTEMP <=1){
                            shoot.shoot(lastPlayer);
                            sayGlobal("JUKE");

                        }
                        jukeTEMP =0;
                        return;
                    }
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).add(lastPlayer.getVelocity().scale(coeff)));
                    sayGlobal("GREEN SENSOR");
                    jukeTEMP=+1;
                    green=sensorFront;                
                }
                else{
                    sayGlobal("RED SENSOR");
                        raycasting.createRay(UNDERSHOT,   new Vector3d(1, 0, Zdistance-0.03), (int)distance, true, false, false);
                        sensorDown=undershot.isResult();

                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-40).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }

                    raycasting.createRay(UPSHOT,   new Vector3d(1, 0, Zdistance+0.03), (int)distance, true, false, false);
                    
                    sensorDown=upshot.isResult();

                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()+40).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }

                    raycasting.createRay(LEFTSHOT,   new Vector3d(1, -0.03, Zdistance), (int)distance, true, false, false);
                    sensorDown=leftshot.isResult();
                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()-40).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }

                    raycasting.createRay(RIGHTSHOT,   new Vector3d(1, 0.03, Zdistance), (int)distance, true, false, false);
                    sensorDown=rightshot.isResult();
                    if (!sensorDown){
                        shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()+40).add(lastPlayer.getVelocity().scale(coeff)));
                        return;
                    }
                   
                    
                    jukeTEMP=+1;
                    shoot.shoot(ennemi);
                    green=false;
                }
            }/*
                if(!info.isFacing(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-(bot.getLocation().getDistanceZ(lastPlayer.getLocation()))/10).add(lastPlayer.getVelocity().scale(coeff)))) {
                   sayGlobal("not facing");
                   sayGlobal(oldVelocity1.toString() + lastPlayer.getVelocity().scale(coeff).toString());
                   shoot.stopShooting();

                   move.turnTo(lastPlayer.getLocation().setZ(bot.getLocation().getZ()).add(lastPlayer.getVelocity().scale(coeff)));
                  return; 
                }
              */       
        }
    }
    
          // ROCKET 1350  glue 2000 link gun 1500(fake 1000)flak 2500   
    //secondary machine gun 933.33 secondary flak 1200 secondary shock rifle 1150  secondary glue 2000 secondary ROCKET 1350

      private void shootFlakCannon(Player lastPlayer){
        if (lastPlayer!=null){           

            if (distance < 500){
                shoot.shoot(lastPlayer);
                return;
            }
            

            if (Zdistance<-0.25 && distance <800){
                move.jump();
                shoot.shootSecondary(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-150).add(lastPlayer.getVelocity().scale(coeff)));
            
            }
            else {
                coeff = distance / 2500;
                shoot.shoot(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(coeff)));
            // shoot.shoot(lastPlayer);
            }
        }    
      }
      
      private boolean bulleShot=false;
        private void shootShockRifle(Player lastPlayer){

            if (lastPlayer!=null){

                log.info("Shooting  ");
                if (bulleShot && seeIncomingProjectile()){
                    shoot.stopShooting();
                    IncomingProjectile proj = pickProjectile();
                    if (distance <info.getLocation().getDistance(proj.getLocation())+300 ){             
                        log.info("in range?: SHOOT");
                        shoot.shoot(proj);   
                        bulleShot=false;
                        return;
                    }
                }
            if (distance < 1000){
                shoot.shootSecondary(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(coeff)));
                bulleShot=true;

            }
            else shoot.shoot(lastPlayer.getLocation());
          }
      }
    private void shootMiniGun(Player lastPlayer){
            if (lastPlayer!=null){
                if (distance < 1000)
                    shoot.shoot(lastPlayer);
                else 
                    shoot.shootSecondary(lastPlayer);
            }
      }
    private boolean shooting=false;
    private void shootAssault(Player lastPlayer){
         if (lastPlayer!=null){
                if (distance < 1000){
                    if (!shooting){
                    shoot.shootSecondary(ennemi);
                    shooting=true;
                    }
                    else {
                    shoot.stopShooting();
                    shooting=false;
                    }
                    
                    //shoot.stopShooting();
                    sayGlobal("SHOT");
                }
                else 
                    shoot.shootSecondary(lastPlayer);
            }
    }
    private void shootBioRifle(Player lastPlayer){
            if (lastPlayer!=null){
                if (distance > 1000)
                    shoot.shootSecondary();
                else 
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-(bot.getLocation().getDistanceZ(lastPlayer.getLocation()))/10).add(lastPlayer.getVelocity().scale(coeff)));
            }
      }
}