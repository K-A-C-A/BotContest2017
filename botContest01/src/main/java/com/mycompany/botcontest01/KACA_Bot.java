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
package com.mycompany.botcontest1;
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
import tools.*;


@AgentScoped
public class KACA_Bot extends UT2004BotModuleController<UT2004Bot> {

    private Profile profile = new Profile();
    private Situation situation = new Situation();
    private Action actionChoice = Action.BASIC_COLLECT;
    
    Item nextItem = null;
    Item proximityItem = null;
    Item savedItem = null;
    
    private long escapeCount = 0;
    //private final int healthNeeded = 60;
    
    private final long logic_escape = 5;
    private int huntCount = 0;
    
    final int veryLongRay = 600;
    final int longRay = 400;
    final int mediumRay = 300;
    final int shortRay = 200;
    
    private boolean isLowAmmoShieldGun = false;
    private boolean turn = false;

    private final double horizontalSpeed = 5;
    private final double lowAmmoShieldGun = 0.6;
    private double angular;
    private final double rotateLeftBack = Math.PI/3;
    private final double rotateRightBack = -Math.PI/3;
    
    private UT2004ItemType weaponSelected;
    
    protected static final String BACK = "Back";
    protected static final String LEFT_BACK = "LeftBack";
    protected static final String RIGHT_BACK = "RightBack";
    protected static final String BEHIND = "Behind";
    
    protected static final String LEFT_FRONT = "LeftFront";
    protected static final String RIGHT_FRONT = "RightFront";
    protected static final String LEFT = "Left";
    protected static final String RIGHT = "Right";
    
    protected static final String FRONT = "Front";
    protected static final String UP_FRONT = "UpFront";
    protected static final String DOWN_FRONT = "DownFront";
    
	
    protected static final String LEFTSHOT = "leftshotRay";
    protected static final String UPSHOT = "upshotRay";
    protected static final String RIGHTSHOT = "rightRay";
    protected static final String FRONTSHOT = "frontRay";
    protected static final String UNDERSHOT = "undershotRay";
    protected static final String FRONTAL= "frontalRay";

    private boolean sensorFront = false;
    
    private AutoTraceRay frontal;

    private AutoTraceRay leftshot, undershot, rightshot, upshot, frontshot;
    private AutoTraceRay leftBack, back, rightBack, right, left, leftFront, rightFront, behind;
    private AutoTraceRay front, upFront, downFront;
    
    boolean frontalF;
    boolean leftB, backB, rightB, behindG;
    boolean leftF, leftL, rightF, rightR;
    boolean frontF, upFrontF, downFrontF;
    
    private double timer = 0.0;
    private boolean secondary = false;
    
    private int jukeTEMP;
    private boolean sensorDown;
    private boolean shooting = false;
    private int tempo = 0;
    private boolean bulleShot = false;
    private boolean hasAvoided = false;
    private int previousChoiceBackward = 0;
    protected double reaction = 0.0;
   
    /**
     * {@link JoueurTuer} compteur pour le nombre de tuer.
     * @param event
     */
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
        getAct().act(new SetCrouch(false));
        if (event.getKiller().equals(info.getId())) {
            enemy = null;
            shoot.stopShooting();
            situation.escape = false;
            situation.incKills();
            situation.nb_ennemy_engaged -= 1;
            profile.angerModified(-5);
            profile.trustModifiedBy(8);
            timer = 0.0;
            secondary = false;
            reaction=0.0;
            distance=0.0;
            Zdistance=0.0;
            oldLocation=null;
            oldVelocity=null;
        }
        if (enemy == null) {
            return;
        }
        if (enemy.getId().equals(event.getId())) {
            enemy = null;
            situation.escape = false;
        }
        
    }
    private Location oldLocation;
    private Velocity oldVelocity;
    private int modu=0;
    protected Player enemy = null;
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
        weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, false);
        
        weaponPrefs.newPrefsRange(800)
                .add(UT2004ItemType.FLAK_CANNON, true)
                .add(UT2004ItemType.ROCKET_LAUNCHER, true)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.LINK_GUN, false)
                .add(UT2004ItemType.BIO_RIFLE, false)
                .add(UT2004ItemType.SHOCK_RIFLE, true);

        weaponPrefs.newPrefsRange(1500)
                .add(UT2004ItemType.ROCKET_LAUNCHER, true)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.FLAK_CANNON, true)
                .add(UT2004ItemType.SHOCK_RIFLE, true)
                .add(UT2004ItemType.LINK_GUN, false);  
      	
        weaponPrefs.newPrefsRange(9000)
                .add(UT2004ItemType.LIGHTNING_GUN, true)
                .add(UT2004ItemType.SNIPER_RIFLE, true)
                .add(UT2004ItemType.SHOCK_RIFLE, true);  
    }

    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
    	// By uncommenting line below, you will see all messages that goes trough GB2004 parser (GB2004 -> BOT communication)
    	//bot.getLogger().getCategory("Parser").setLevel(Level.ALL);
        shoot.setChangeWeaponCooldown(3000);
        config.setRotationHorizontalSpeed(config.getRotationSpeed().yaw * horizontalSpeed);
        //itemsToIgnore = new TabooSet<Item>(bot);
        
        boolean fastTrace = true;        
        boolean floorCorrection = false; 
        boolean traceActor = false;

        getAct().act(new RemoveRay("All"));
        
        raycasting.createRay(BACK,   new Vector3d(-1, 0, 0), longRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(LEFT_BACK,  new Vector3d(-1, -1, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(RIGHT_BACK, new Vector3d(-1, 1, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(LEFT_FRONT,  new Vector3d(1, -1, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(RIGHT_FRONT, new Vector3d(1, 1, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(LEFT,  new Vector3d(0, -1, 0), shortRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(RIGHT, new Vector3d(0, 1, 0), shortRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(BEHIND, new Vector3d(-1, 0, -2), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(FRONTSHOT,   new Vector3d(1, 0, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(UNDERSHOT,   new Vector3d(1, 0, -0.03), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(UPSHOT,   new Vector3d(1, 0, 0.03), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(LEFTSHOT,   new Vector3d(1, -0.03, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(RIGHTSHOT,   new Vector3d(1, 0.03, 0), mediumRay, fastTrace, floorCorrection, traceActor);
        
        raycasting.createRay(FRONT, new Vector3d(1, 0, 0), veryLongRay, false, floorCorrection, traceActor);
        raycasting.createRay(UP_FRONT, new Vector3d(1, 0, 0.5), mediumRay, false, floorCorrection, traceActor);
        raycasting.createRay(DOWN_FRONT, new Vector3d(1, 0, -1), shortRay, fastTrace, floorCorrection, traceActor);
        raycasting.createRay(FRONTAL,   new Vector3d(1, 0, 0), shortRay, fastTrace, floorCorrection, traceActor);

         // register listener called when all rays are set up in the UT engine
        raycasting.getAllRaysInitialized().addListener(new FlagListener<Boolean>() {

	    @Override
            public void flagChanged(Boolean changedValue) {
                // once all rays were initialized store the AutoTraceRay objects
                // that will come in response in local variables, it is just
                // for convenience
                leftBack = raycasting.getRay(LEFT_BACK);
                left = raycasting.getRay(LEFT);
                leftFront = raycasting.getRay(LEFT_FRONT);
                back = raycasting.getRay(BACK);
                rightBack = raycasting.getRay(RIGHT_BACK);
                right = raycasting.getRay(RIGHT);
                rightFront = raycasting.getRay(RIGHT_FRONT);
                behind = raycasting.getRay(BEHIND);
                frontshot = raycasting.getRay(FRONTSHOT);
                undershot=raycasting.getRay(UNDERSHOT);
                upshot=raycasting.getRay(UPSHOT);
                leftshot=raycasting.getRay(LEFTSHOT);
                rightshot=raycasting.getRay(RIGHTSHOT);
                front=raycasting.getRay(FRONT);
                upFront=raycasting.getRay(UP_FRONT);
                downFront=raycasting.getRay(DOWN_FRONT);
                frontal=raycasting.getRay(FRONTAL);

            }
        });
        
        raycasting.endRayInitSequence();
        getAct().act(new Configuration().setDrawTraceLines(false).setAutoTrace(true));
        
    }
    
    /**
     * initialisation du bot
     * @return
     */
    @Override
    public Initialize getInitializeCommand() {
        //setDesiredSkill(int skill) -> varie de 1 à 9 pour choisir le "niveau de visée du bot"
        return new Initialize().setName("K-A-C-A").setDesiredSkill(4);
    }

    /**
     * réinitialise la navigation du bot
     */
    protected void reset() {
        item = null;
        savedItem = null;
        enemy = null;
        navigation.stopNavigation();
        CollectItems = null;
        situation.injured = false;
        situation.escape = false;
        isLowAmmoShieldGun = false;
        turn = false;
        huntCount = 0;
        escapeCount = 0;
	timer=0.0;
        secondary=false;
        previousChoiceBackward = 0;
        situation.stuck = false;
	hasAvoided = false;
        oldLocation=null;
        oldVelocity=null;
        reaction=0.0;
        distance=0.0;
        Zdistance=0.0;
    }

    @EventListener(eventClass = PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
        log.log(Level.INFO, "J'ai infligé: {0}[{1}]", new Object[]{event.getDamageType(), event.getDamage()});
        
    }

    @EventListener(eventClass = BotDamaged.class)
    public void botDamaged(BotDamaged event) {
        log.log(Level.INFO, "J'ai reçu: {0}[{1}]", new Object[]{event.getDamageType(), event.getDamage()});
        enemy = players.getPlayer(event.getInstigator());
        if(enemy != null)
            move.turnTo(enemy);
        situation.injured = true;
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
        hasAvoided=avoidProjectile();	
        
        if (enemy==null)
            situation.stuck=false;
        situation.situationActualisation(info.getArmor(), players.getVisibleEnemies().size(), info.getHealth());
        situation.injured = senses.isBeingDamaged();        
        actionChoice = profile.decision(situation, actionChoice);
        
        if (actionChoice != Action.ESCAPE) {
           if (distance != 0.0){
                shoot.changeWeapon(weaponPrefs.getWeaponPreference(distance).getWeapon());
           }
           else{
                shoot.changeWeapon(weaponPrefs.getWeaponPreference().getWeapon());
           }
          
           if (actionChoice != Action.FIGHT) {
               
               navigation.setFocus(null);
               if (info.isShooting() || info.isSecondaryShooting() || secondary) {
                    chargeWeaponOrStopShooting();
                    //remise a zero des informations concernant l'ancien enemy visible
                    oldVelocity=null;
                    oldLocation=null;
                    modu=0;
                    reaction=0.0;
                }
               
           }
            
        }
        

        switch (actionChoice) {
            case BASIC_COLLECT:
                collectState();
                return;
            case FIGHT:
                if (situation.engage  && weaponry.getCurrentPrimaryAmmo() != 0) {
                    engageState();
                } else {
                    actionChoice = Action.ESCAPE;
                    escapeState();
                }
                
                return;
            case HUNT:
                if (enemy != null && situation.hunt && weaponry.getCurrentPrimaryAmmo() != 0) {
                    this.huntState();
                    return;
                }
            case ESCAPE:
                if (enemy != null && situation.escape && !hasLowAmmoForWeapon(UT2004ItemType.SHIELD_GUN, 0.1)) {
                    this.escapeState();
                }
                else {
                    situation.escape = false;
                    situation.justEscaped = true;
                    
                    if (situation.engage && players.canSeeEnemies() && weaponry.getCurrentPrimaryAmmo() != 0) {
                        actionChoice = Action.FIGHT;
                        engageState();
                    }
                    else{
                        actionChoice = Action.BASIC_COLLECT;
                        collectState();
                    }
                }
                return;
            case INJURED:
                injuredState();
                return;
            case HEALTH:
                if (situation.healthCollect) {
                    this.healingState();
                }
        }
        
//        if (situation.justEscaped) {
//            ++situation.logicIterationNumber;
//            if (situation.logicIterationNumber > 8){
//                situation.justEscaped = false;
//                situation.logicIterationNumber = 0;
//            }
//        }
//        situation.escape = info.getHealth() < 40 && !situation.justEscaped;
        
        // 1) niveau de vie faible
//        if (enemy != null && situation.escape && !hasLowAmmoForWeapon(UT2004ItemType.SHIELD_GUN, 0.1)) {
//            this.escapeState();
//            return;
//        }
//        else { 
//            situation.escape = false;
//        }
//        
//        //prends une arme de la liste des preferences
//        weaponry.changeWeapon(weaponPrefs.getWeaponPreference().getWeapon());
//        
//        // 2) enemy repéré ? 	-> poursuivre (tirer / suivre)
//        if (situation.engage && players.canSeeEnemies() && weaponry.hasLoadedWeapon()) {
//            initRayToEnnemi();
//            if (secondary)
//                releaseWeapon();
//            engageState();
//            return;
//        }
//        navigation.setFocus(null);
//        // 3) en train de tiré    -> arrête de tiré si l'enemy est perdu de vue
//        if (info.isShooting() || info.isSecondaryShooting()) {
//            chargeWeaponOrStopShooting();
//            //remise a zero des informations concernant l'ancien enemy visible
//            oldVelocity=null;
//            oldLocation=null;
//            modu=0;
//        }
//
//        // 4) dommages reçu ?	-> tourne sur lui même pour chercher l'enemy
//        if (senses.isBeingDamaged()) {
//            this.injuredState();
//            return;
//        }
//        
//        // 5) enemy poursuivis -> va à la dernière position connue de l'enemy
//        if (enemy != null && situation.hunt && weaponry.hasLoadedWeapon()) {
//            this.huntState();
//            return;
//        }
//        
//        // 6) blessé ?			-> cherche des soins
//        if (situation.healthCollect && info.getHealth() < healthNeeded) {
//            this.healingState();
//            return;
//        }
//
//        // 7) rien ... ramasses les items
//        collectState();
        
    }

    protected boolean goToPlayer = false;

   protected void engageState() {
        //log.info("Decision: Engager");
        //config.setName("Hunter [Engager]");

        boolean fire = false;
        huntCount = 0;
        escapeCount = 0;
        
        // 1) choisis un nouvel enemy si le précédent est perdu de vue
        if (enemy == null || !enemy.isVisible()) {
            enemy = players.getNearestVisiblePlayer(players.getVisibleEnemies().values());
            if (enemy == null) {
                log.info("Pas d'enemy en vue !");
                return;
            }
        }
        initRayToEnnemi();
        if (secondary){
            releaseWeapon();
            secondary= false;
        }
        // 2) si l'enemy n'est plus visible -> arrête de tirer
        if (!enemy.isVisible()) {
            if (info.isShooting() || info.isSecondaryShooting()) {
                getAct().act(new StopShooting());
            }
            goToPlayer = false;
        } else {
            // 2) tires sur l'enemy s'il est visible
            coeff=calculCoeff(distance);
            navigation.setFocus(enemy.getLocation().add(enemy.getVelocity().scale(coeff)));
            if (reaction < 0.2){
                reaction = reaction + 0.1;
                return;
            }else {
           
            if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.ROCKET_LAUNCHER) 
                shootRocketLauncher(enemy);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.LINK_GUN)
                shootLinkGun(enemy);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.FLAK_CANNON)
                shootFlakCannon(enemy);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.SHOCK_RIFLE)
                shootShockRifle(enemy);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.BIO_RIFLE)
                shootBioRifle(enemy);
            else if (weaponry.getCurrentWeapon().getType() == UT2004ItemType.MINIGUN)
                shootMiniGun(enemy);
            else if (!weaponry.hasPrimaryLoadedWeapon(UT2004ItemType.ASSAULT_RIFLE) && (weaponry.getCurrentWeapon().getType() == UT2004ItemType.ASSAULT_RIFLE )){
                chargeWeaponOrStopShooting();
                secondary=true;
            }
            else
           	 shoot.shoot(enemy);
	    fire=true;
            }
        }
        
        // 3) Si l'enemys n'est pas visible ou trop loin -> vas vers lui
        int distSuffisante = Math.round(random.nextFloat() * 800) + 200;
        if (!enemy.isVisible() || !fire || distSuffisante < distance) {
            if (!goToPlayer) {
                navigation.navigate(enemy);
                goToPlayer = true;
            }
        } else {
            goToPlayer = false;
            navigation.stopNavigation();
            //move.strafeAlong(enemy.getLocation(), items.getNearestItem().getLocation(), enemy);
           if (!hasAvoided){
                int choixBot = getRandom().nextInt(50);
                if (choixBot < 20 && distance > 400)
                    switchStrafe(enemy, Location.sub(enemy.getLocation(),bot.getLocation()), 6);
                else if (choixBot >= 20 && choixBot < 30)
                    move.strafeLeft(150);
                else if (choixBot >= 30 && choixBot < 40 )
                    move.strafeRight(150);
                else if ( choixBot >= 40 && choixBot < 43){
                    move.strafeLeft(20);
                    getAct().act(new Jump(getRandom().nextBoolean(), 0.3d, 755d));
                }
               else if ( choixBot >= 43 && choixBot < 46){
                    move.strafeRight(20);
                    getAct().act(new Jump(getRandom().nextBoolean(), 0.3d, 755d));
                }
                else if ( choixBot >= 46 && choixBot < 49){
                    move.moveTo(enemy);
                    getAct().act(new Jump(getRandom().nextBoolean(), 0.3d, 755d));
                }
                else if (choixBot >= 49)
                    move.dodgeLeft( enemy, getRandom().nextBoolean());
                else
                    move.dodgeRight( enemy, getRandom().nextBoolean());
            }
        }
        hasAvoided=false;
        item = null;
    }

    ///////////////////
    // Etat Toucher //
    /////////////////
    protected void injuredState() {
        //log.info("Decision is: Toucher");
//        bot.getBotName().setInfo("Toucher");
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
//        log.info("Decision: PURSUE");
            ++huntCount;
            if (huntCount > 30) {
                situation.nb_ennemy_engaged = 0;
                reset();
            }
            if (enemy != null) {
//                bot.getBotName().setInfo("Poursuivre");
                navigation.navigate(enemy);
                item = null;
            } else {
                situation.nb_ennemy_engaged = 0;
                reset();
            }
    }

    
    ///////////////////
    //  Etat Fuite  //
    /////////////////
    protected void escapeState(){
        if (escapeCount == 0) {
            getAct().act(new SetCrouch(false));
        }
        ++escapeCount;
        if(enemy == null){
            situation.justEscaped = true;
            situation.nb_ennemy_engaged = 0;
            reset();
        }
        
//        bot.getBotName().setInfo("Escape");

        if(enemy.isVisible())
            escapeCount = 0;

        if(escapeCount < logic_escape){  
            goBackward();
            protect();
        }
        else{
            situation.justEscaped = true;
            situation.nb_ennemy_engaged = 0;
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
//            bot.getBotName().setInfo("Soins");
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
            if (savedItem == null) {
                proximityItem = items.getPathNearestSpawnedItem();
                if (proximityItem != null && Location.getDistance(proximityItem.getLocation(),bot.getLocation()) < 300){
                    if (proximityItem.getType().getCategory() != UT2004ItemType.Category.AMMO && proximityItem.getType().getCategory() != ItemType.Category.HEALTH) {
                        savedItem = this.item;
                        this.item = proximityItem;
                       // log.log(Level.INFO, "Collecte: {0}", this.item.getType().getName());
//                        bot.getBotName().setInfo("Item: " + this.item.getType().getName() + "");
                        navigation.navigate(this.item);
                        return;
                    }
                }
            }
            navigation.navigate(this.item);
            return;
        }
        
        if (savedItem != null){
            this.item = savedItem;
            savedItem = null;
            //log.log(Level.INFO, "Collecte: {0}", this.item.getType().getName());
//            bot.getBotName().setInfo("Item: " + this.item.getType().getName() + "");
            navigation.navigate(this.item);
            return;
        }
        
        nextItem = items.getPathNearestSpawnedItem();
        if (nextItem != null && nextItem.getType() != UT2004ItemType.HEALTH_PACK && nextItem.getType().getCategory() != ItemType.Category.AMMO){
            Location nItem = nextItem.getLocation();
            if(nItem.getDistance(bot.getLocation()) < 500){
                this.item = nextItem;
              //  log.log(Level.INFO, "Collecte: {0}", this.item.getType().getName());
//                bot.getBotName().setInfo("Item: " + this.item.getType().getName() + "");
                navigation.navigate(this.item);
                return;
            }
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
        // Ajouter Boost
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
//            bot.getBotName().setInfo("Navigation aléatoire");
            navigation.navigate(navPoints.getRandomNavPoint());
            
        } else {
            this.item = newItem;
          //  log.log(Level.INFO, "Collecte: {0}", newItem.getType().getName());
//            bot.getBotName().setInfo("Item: " + newItem.getType().getName() + "");
            navigation.navigate(newItem);
        }
    }
    

    ///////////////
    // Bot Tuer //
    /////////////
    @Override
    public void botKilled(BotKilled event) {
        reset();
        situation.incDeaths();
        situation.nb_ennemy_engaged = 0;
        profile.angerModified(8);
        profile.cautionModifiedBy(-5);
        profile.trustModifiedBy(-7);
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
        
            leftB = leftBack.isResult();
            backB = back.isResult();
            rightB = rightBack.isResult();
            behindG = behind.isResult();
            leftL = left.isResult();
            rightR = right.isResult();
            
            //Si il y a du vide derrière lui
            if(!behindG){
                move.stopMovement();
                System.out.println("Je vais tomber !");
                return;
            }
            
            double rotation = 0;
            
            //si un obstacle detecte pour back
            if (backB){
                turn = true;
                
                //si pas d'obstacle detecte pour left
                if (!leftB) {
                    //move.strafeLeft(pas, enemy);
                    rotation = rotateLeftBack;
                }//si pas d'obstacle detecte pour right
                else if (!rightB) {
                    //move.strafeRight(pas, enemy);
                    rotation = rotateRightBack;
                }//sinon impasse
                else{
                    //cas ou les 3 rayons sont rouges
                    if (rightR && leftL)
                        situation.stuck = true;
                    else if (rightR)
                        rotation = rotateLeftBack;
                    else if (leftL)
                        rotation = rotateRightBack;
                    else {
                        switch (previousChoiceBackward) {
                            case 0:
                                previousChoiceBackward = (int)Math.round(Math.random() * 1) + 1;
                                break;
                            case 1:
                                rotation = rotateLeftBack;
                                break;
                            case 2:
                                rotation = rotateRightBack;
                        }
                    }
                }
            }
            
            
            
        if(turn){
            Location l = Location.sub(bot.getLocation(), enemy.getLocation());
            l = l.rotateXY(rotation);
            l = Location.add(bot.getLocation(), l);
            move.strafeTo(l, enemy);
            turn = false;
        }
        else{
            //direction de l'enemy
            Location vector = Location.sub(bot.getLocation(), enemy.getLocation());
            switchStrafe(enemy, vector,12);
        }
        
    }
    public void protect(){
        //si il possède le shield_gun, il a des munitions et l'enemy est en vue
        weaponSelected = UT2004ItemType.SHIELD_GUN;
        
        
        //System.out.println((double)weaponry.getWeaponAmmo(weapon)/(double)weaponry.getWeaponDescriptor(weapon).getPriMaxAmount());

        if(weaponry.hasWeapon(weaponSelected)){
            boolean hasAmmoForWeapon = weaponry.hasAmmoForWeapon(weaponSelected);
            if(hasAmmoForWeapon && enemy.isVisible() && !isLowAmmoShieldGun){
//                info.getBotName().setInfo("Protect");
                //false => tir secondaire
                shoot.shoot(weaponry.getWeapon(weaponSelected), false, enemy.getId());
            }
            else{
                shoot.stopShooting();
                if(!hasAmmoForWeapon || isLowAmmoShieldGun){
                    isLowAmmoShieldGun = hasLowAmmoForWeapon(weaponSelected, lowAmmoShieldGun);
                    
                    //definir une action si il n'a plus de shield
//                    info.getBotName().setInfo("LowAmmoShieldGun");
                }
            }
        }
        else{
//            info.getBotName().setInfo("Can't Protect");
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
                catch (NumberFormatException e)
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
    double coeff1=1;
    if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.ROCKET_LAUNCHER )
        coeff1 =distance/1350;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.LINK_GUN )
        coeff1 =distance/1500;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.SHOCK_RIFLE )
        coeff1 = distance/1150;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.BIO_RIFLE )
        coeff1 = distance/2000;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.ASSAULT_RIFLE )
        coeff1 = distance/933.33;
    else if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.FLAK_CANNON )
        coeff1 = distance/1200;


    return coeff1;
}
    private void initRayToEnnemi(){
        if (enemy != null){
        distance=enemy.getLocation().getDistance(bot.getLocation());
        coeff =calculCoeff(distance);
        final int rayLength = (int) (enemy.getLocation().add(enemy.getVelocity().scale(coeff).asLocation()).getDistance(bot.getLocation()))-150;
        Zdistance=enemy.getLocation().setZ(enemy.getLocation().getZ()+20).add(enemy.getVelocity()).getDistanceZ(bot.getLocation())/distance ;         
        raycasting.createRay(FRONTSHOT,   new Vector3d(1, 0, Zdistance), rayLength, true, false, false);
        raycasting.createRay(UNDERSHOT,   new Vector3d(1, 0, Zdistance-0.03), rayLength, true, false, false);
        raycasting.createRay(LEFTSHOT,   new Vector3d(1, -0.03, Zdistance), (int)distance, true, false, false);
        raycasting.createRay(UPSHOT,   new Vector3d(1, 0, Zdistance+0.03), (int)distance, true, false, false);
        raycasting.createRay(RIGHTSHOT,   new Vector3d(1, 0.03, Zdistance), (int)distance, true, false, false);
        }
    }
    private IncomingProjectile pickProjectile() {
		return DistanceUtils.getNearest(world.getAll(IncomingProjectile.class).values(), info.getLocation());
                /*if (seeIncomingProjectile()){
                                    proj =pickProjectile();
                                    //sayGlobal(String.valueOf(proj.getSpeed()));
                                }*/
	}
        
      
      private boolean seeIncomingProjectile() {
         
    	for (IncomingProjectile proj : world.getAll(IncomingProjectile.class).values()) {
    		if (proj.isVisible() && proj.getOrigin().getDistance(bot.getLocation())>400 && proj.getLocation().getDistance(bot.getLocation())<1000) return true;

    	}
		return false;
	}
   
    
     private void shootRocketLauncher(Player lastPlayer){
         if (lastPlayer!=null){
            //Si l'enemy est proche on tire directement sur sa position (vers le sol car les rockets explose et font des degats de zones et il est très facile de les esquiver autrement)
            if (distance < 500){
                move.jump();
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-40));
            }
            else {
                //Si l'enemy est dans les airs on ne tire pas
                if ((int)lastPlayer.getVelocity().getZ()!=0){
                    shoot.stopShooting();
                    // //sayGlobal("NO AIR SHOOTIN");
                    return;
                }
                // On met en mémoire l'ancienne vitesse et localisation de l'enemye
                if (modu==0){
                    oldVelocity=lastPlayer.getVelocity().scale(coeff);
                    oldLocation = lastPlayer.getLocation();
                }
                
                modu=(modu+1) %2;
                if (oldVelocity==null || oldLocation == null) 
                     return;
                //Si l'enemy descend une pente on tire dans la direction de son deplacement , legerement plus bas.
                if ((int)oldVelocity.getZ()==0 && (int)lastPlayer.getVelocity().getZ()==0 && oldLocation.getZ()>lastPlayer.getLocation().getZ()){
                    ////sayGlobal("going down STAIRS" );
                    ////sayGlobal(String.valueOf((lastPlayer.getLocation().getZ()-oldLocation.getZ())*3));

                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-(lastPlayer.getLocation().getZ()-oldLocation.getZ())*3).add(lastPlayer.getVelocity().scale(coeff)));
                    jukeTEMP=+1;
                    return;
                }
                
                 //Si l'enemy monte une pente on tire dans la direction de son deplacement , legerement plus haut.
                if ((int)oldVelocity.getZ()==0 && (int)lastPlayer.getVelocity().getZ()==0 && oldLocation.getZ()<lastPlayer.getLocation().getZ()){
                   // //sayGlobal(String.valueOf((lastPlayer.getLocation().getZ()-oldLocation.getZ())*3));
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()+(lastPlayer.getLocation().getZ()-oldLocation.getZ())*3).add(lastPlayer.getVelocity().scale(coeff)));
                   // //sayGlobal("climbing");
                    return;
                } 
                //On regarde le rayon envoyé legerement vers le bas, si il n'est pas interrompu
                sensorFront=undershot.isResult();
                if (!sensorFront){
                    //on regarde si le joueur est en train de zigzagé si oui on tire sur sa localisation 
                    if ((oldVelocity.getX()-lastPlayer.getVelocity().scale(coeff).getX()>150 || oldVelocity.scale(coeff).getX()-lastPlayer.getVelocity().scale(coeff).getX()<-150)|| (oldVelocity.scale(coeff).getY() -lastPlayer.getVelocity().scale(coeff).getY()>150 || oldVelocity.scale(coeff).getY()-lastPlayer.getVelocity().scale(coeff).getY()<-150)){
                        if (jukeTEMP <=1){
                           // //sayGlobal("JUKESHoT");
                            shoot.shoot(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(0.1)));
                        }
                        jukeTEMP =0;
                        return;
                    }
                    //sinon on tire dans sa direction
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-40).add(lastPlayer.getVelocity().scale(coeff)));
                    jukeTEMP=+1;
                    return;
                }
                //Sinon on regarde si le rayon envoyé en face n'est pas interrompu 
                sensorDown=frontshot.isResult();
                if (!sensorDown){
                   // //sayGlobal("FRONT");
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).add(lastPlayer.getVelocity().scale(coeff)));
                    return;
                }
                //Sinon on regarde si le rayon envoyé en haut n'est pas interrompu 
                sensorDown=upshot.isResult();
                if (!sensorDown ){
                    ////sayGlobal("UP" );                             
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()+40).add(lastPlayer.getVelocity().scale(coeff)));
                    return;
                }
                //Sinon on regarde si le rayon envoyé à gauche n'est pas interrompu 
                sensorDown=leftshot.isResult();
                if (!sensorDown){
                    ////sayGlobal("LEFT");
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()-40).add(lastPlayer.getVelocity().scale(coeff)));
                    return;
                }
                //Sinon on regarde si le rayon envoyé à droite n'est pas interrompu
                sensorDown=rightshot.isResult();
                if (!sensorDown){
                    ////sayGlobal("RIGHT");
                    shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()+40).add(lastPlayer.getVelocity().scale(coeff)));
                    return;
                }
                //Si tous les rayons sont interrompus on tire sur la position direct de l'enemy
               // //sayGlobal("POSITION");
                jukeTEMP=+1;
                shoot.shoot(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(0.1)));
                }     
        }
    }

    private void shootLinkGun(Player lastPlayer){
    if (lastPlayer!=null){
        if (distance < 700)
            shoot.shootSecondary(lastPlayer);
        else {

            //Si l'enemy est dans les airs on ne tire pas
            if ((int)lastPlayer.getVelocity().getZ()!=0){
                shoot.stopShooting();
                // //sayGlobal("NO AIR SHOOTIN");
                return;
            }
            // On met en mémoire l'ancienne vitesse et localisation de l'enemye
            if (modu==0){
                oldVelocity = lastPlayer.getVelocity().scale(coeff);
                oldLocation = lastPlayer.getLocation();
            }
            modu=(modu+1) %2;
            if (oldVelocity==null || oldLocation == null) 
                return;
            //Si l'enemy descend une pente on tire dans la direction de son deplacement , legerement plus bas.
            if ((int)oldVelocity.getZ()==0 && (int)lastPlayer.getVelocity().getZ()==0 && oldLocation.getZ()>lastPlayer.getLocation().getZ()){
                ////sayGlobal("going down STAIRS" );
                ////sayGlobal(String.valueOf((lastPlayer.getLocation().getZ()-oldLocation.getZ())*3));

                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-(lastPlayer.getLocation().getZ()-oldLocation.getZ())*3).add(lastPlayer.getVelocity().scale(coeff)));
                jukeTEMP=+1;
                return;
            }

             //Si l'enemy monte une pente on tire dans la direction de son deplacement , legerement plus haut.
            if ((int)oldVelocity.getZ()==0 && (int)lastPlayer.getVelocity().getZ()==0 && oldLocation.getZ()<lastPlayer.getLocation().getZ()){
               // //sayGlobal(String.valueOf((lastPlayer.getLocation().getZ()-oldLocation.getZ())*3));
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()+(lastPlayer.getLocation().getZ()-oldLocation.getZ())*3).add(lastPlayer.getVelocity().scale(coeff)));
                //sayGlobal("climbing");
                return;
            } 
            //On regarde le rayon envoyé legerement vers le bas, si il n'est pas interrompu
            sensorFront=front.isResult();
            if (!sensorFront){
                //on regarde si le joueur est en train de zigzagé si oui on tire sur sa localisation 
                if ((oldVelocity.getX()-lastPlayer.getVelocity().scale(coeff).getX()>150 || oldVelocity.scale(coeff).getX()-lastPlayer.getVelocity().scale(coeff).getX()<-150)|| (oldVelocity.scale(coeff).getY() -lastPlayer.getVelocity().scale(coeff).getY()>150 || oldVelocity.scale(coeff).getY()-lastPlayer.getVelocity().scale(coeff).getY()<-150)){
                    if (jukeTEMP <=1){
                       // //sayGlobal("JUKESHoT");
                        shoot.shoot(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(0.1)));
                    }
                    jukeTEMP =0;
                    return;
                }
                //sinon on tire dans sa direction
                shoot.shoot(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(coeff)));
                jukeTEMP=+1;
                return;
            }
            //Sinon on regarde si le rayon envoyé en face n'est pas interrompu 
            sensorDown=undershot.isResult();
            if (!sensorDown){
               // //sayGlobal("FRONT");
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-40).add(lastPlayer.getVelocity().scale(coeff)));
                return;
            }
            //Sinon on regarde si le rayon envoyé en haut n'est pas interrompu 
            sensorDown=upshot.isResult();
            if (!sensorDown ){
                ////sayGlobal("UP" );                             
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()+40).add(lastPlayer.getVelocity().scale(coeff)));
                return;
            }
            //Sinon on regarde si le rayon envoyé à gauche n'est pas interrompu 
            sensorDown=leftshot.isResult();
            if (!sensorDown){
                ////sayGlobal("LEFT");
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()-40).add(lastPlayer.getVelocity().scale(coeff)));
                return;
            }
            //Sinon on regarde si le rayon envoyé à droite n'est pas interrompu
            sensorDown=rightshot.isResult();
            if (!sensorDown){
                ////sayGlobal("RIGHT");
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).setY(lastPlayer.getLocation().getY()+40).add(lastPlayer.getVelocity().scale(coeff)));
                return;
            }
            //Si tous les rayons sont interrompus on tire sur la position direct de l'enemy
           // //sayGlobal("POSITION");
            jukeTEMP=+1;
            shoot.shoot(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(0.1)));
            }   
        }    
    }

    
          // ROCKET 1350  glue 2000 link gun 1500(fake 1000)flak 2500   
    //secondary machine gun 933.33 secondary flak 1200 secondary shock rifle 1150  secondary glue 2000 secondary ROCKET 1350

    private void shootFlakCannon(Player lastPlayer){
        if (lastPlayer!=null){           
            ////sayGlobal(String.valueOf(Zdistance*distance));
            if (Zdistance*distance<-250 && distance <800 && (int)lastPlayer.getVelocity().getZ()==0){
                move.jump();
                shoot.shootSecondary(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()-150).add(lastPlayer.getVelocity().scale(coeff)));
                return;
            }
            if (distance < 500){
                shoot.shoot(lastPlayer);
            }
            else {
                coeff = distance / 2500;
                shoot.shoot(lastPlayer.getLocation().add(lastPlayer.getVelocity().scale(coeff)));
            }
        }    
    }
    
    private boolean seeShotProjectile() {
         
    	for (IncomingProjectile proj : world.getAll(IncomingProjectile.class).values()) {
    		if (proj.isVisible() && proj.getOrigin().getDistance(bot.getLocation())<400) return true;

    	}
		return false;
	}
      
    private void shootShockRifle(Player lastPlayer){

        if (lastPlayer!=null){
            if (bulleShot && seeShotProjectile()){
                shoot.stopShooting();
                IncomingProjectile proj = pickProjectile();
                if (distance <info.getLocation().getDistance(proj.getLocation())+100 ){             
//                        log.info("in range?: SHOOT");
                    shoot.shoot(proj);   
                    bulleShot=false;
                    return;
                }
            }
            if (distance < 800){
                shoot.shootSecondary(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()+30).add(lastPlayer.getVelocity().scale(coeff)));
                bulleShot=true;
            }
            else {
                if (tempo==4){
                    shoot.shoot(lastPlayer.getLocation());
                    tempo=(tempo+1) %5;
                    return;
                }
                tempo=(tempo+1) %5;
                shoot.stopShooting();
            }
        }
    }
        
    private void shootMiniGun(Player lastPlayer){
        if (lastPlayer!=null){
            if (distance < 1500)
                shoot.shoot(lastPlayer);
            else 
                shoot.shootSecondary(lastPlayer);
        }

    }

 
    private void shootBioRifle(Player lastPlayer){
        if (lastPlayer!=null){
            if (distance > 1000){
               
                if (info.isSecondaryShooting())
                    return;
                else 
                    shoot.shootSecondary();
            }
            int skip = getRandom().nextInt(5);
            if((int)lastPlayer.getVelocity().getZ()==0){
                if (skip==0){
                   shoot.stopShooting();
                   //sayGlobal("SKIPPED");
                   return;
                }
                shoot.shoot(lastPlayer.getLocation().setZ(lastPlayer.getLocation().getZ()).add(lastPlayer.getVelocity().scale(coeff)));  
                //sayGlobal("Shoot");
            }
        }
    }
    
       public void switchStrafe(Player player, Location vector , int coeff ){
        
        int choix = (int)Math.round(Math.random() * 2);
        switch (choix){
            case 1 :
                angular = Math.PI/coeff;
                break;
            case 2 :
                angular = -Math.PI/coeff;
                break;
            default :
                angular = 0;
                break;

        }

        //Location l = Location.sub(player.getLocation(), bot.getLocation());
        
        Location l = vector.rotateXY(angular);
        l = Location.add(bot.getLocation(), l);
        move.strafeTo(l, player);
        
    }
    
    public void switchStrafe(Item item, Location vector){
        
        if (!item.isVisible()) {
            navigation.navigate(item);
            return;
        }
        
        int choix = (int)Math.round(Math.random() * 2);
        switch (choix){
            case 1 :
                angular = Math.PI/12;
                break;
            case 2 :
                angular = -Math.PI/12;
                break;
            default :
                angular = 0;
                break;

        }

        //Location l = Location.sub(player.getLocation(), bot.getLocation());
        
        Location l = vector.rotateXY(angular);
        l = Location.add(bot.getLocation(), l);
        move.strafeTo(l, item);
        
    }
    
   
     public boolean avoidProjectile () {
        
        if (this.seeIncomingProjectile()) {
            IncomingProjectile proj = pickProjectile();
            rightR = right.isResult();
            leftL = left.isResult();
            
            if (proj.getLocation().getDistance(bot.getLocation()) < 1000 && proj.getLocation().getDistance(bot.getLocation()) > 400) {   
                int choix = getRandom().nextInt(20);
                
                    
                if (choix < 12) {
                    
                    if (!rightR)
                        move.strafeLeft(150);
                    else
                        move.strafeRight(150);
                    
                } else if (choix >= 12 && choix < 17) {
                    
                    if (!rightR)
                        move.dodgeLeft(proj, false);
                    else
                        move.dodgeRight(proj, false);
                    
                } else if (choix == 17 ) {
                    
                    if (!rightR)
                        move.strafeRight(150);
                    else
                        move.strafeLeft(150);
                    
                } else if (choix > 17 ) {
                    getAct().act(new Jump(getRandom().nextBoolean(), 0.3d, 755d));
                }
                
            }
          return true;  
        }
        return false;
    }
    
    public void jumpFarAway(Location location){
        //move.dodgeTo(, true);
        downFrontF = downFront.isResult();
        upFrontF = upFront.isResult();
        frontF = front.isResult();
        
        if(!downFrontF){
            if(!frontF){
                move.dodgeTo(front.getTo(), true);
            }
            else if(!upFrontF){
                move.dodgeTo(upFront.getTo(), true);
            }
            else
                //cas où il n'y a pas de platefrome en face
                return;
        }
    }

    public void chargeWeaponOrStopShooting(){
        if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.ROCKET_LAUNCHER || weaponry.getCurrentWeapon().getType()==UT2004ItemType.BIO_RIFLE||weaponry.getCurrentWeapon().getType()==UT2004ItemType.ASSAULT_RIFLE){
                shoot.shootSecondary();
                ////sayGlobal("charging");
                secondary=true;
                if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.ROCKET_LAUNCHER  &&weaponry.getCurrentWeapon().getType()==UT2004ItemType.ASSAULT_RIFLE ){
                    frontalF=frontal.isResult();
                    timer=timer+0.1;
                   // //sayGlobal(String.valueOf(timer));
                    if (timer > 0.7 && frontalF){
                       // //sayGlobal("ILL DIE");
                        move.turnHorizontal(80);
                    }
                    if (timer>0.8){
                        secondary=false;
                        timer=0.0;
                        shoot.stopShooting();
                    }
                }
            }
            else{
               getAct().act(new StopShooting());
               secondary=false;
            }
        }
      public void releaseWeapon(){
        secondary=false;
        if (weaponry.getCurrentWeapon().getType()==UT2004ItemType.ASSAULT_RIFLE){
                    shoot.shootSecondary(enemy.getLocation().setZ(enemy.getLocation().getZ()-150));
                    shoot.stopShooting();
        }
        else{
            shoot.shootSecondary(enemy);
            shoot.stopShooting();
        }
        ////sayGlobal("second"+String.valueOf(info.isSecondaryShooting()));
        timer=0.0;
      }
    
}
