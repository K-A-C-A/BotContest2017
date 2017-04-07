/**
 *
 * @author Alexandre
 */

package com.mycompany.botcontest01;

import cz.cuni.amis.introspection.java.JProp;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
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


@AgentScoped
public class ResponsiveBotTest extends UT2004BotModuleController<UT2004Bot> {
    
    /**
     * booléen pour engager le combat avec un ennemi
     */
    @JProp
    public boolean engager = true;
    /**
     * booléen pour poursuivre un ennemi
     */
    @JProp
    public boolean poursuivre = true;
    /**
     * booléen pour savoir s'il doit se réarmer
     */
    @JProp
    public boolean rearmer = true;
    /**
     * booléen pour aller chercher de la vie
     */
    @JProp
    public boolean collecterSoins = true;
    /**
     * niveau de vie avant de chercher à en récupérer
     */
    @JProp
    public int niveauVie = 60;
    /**
     * nombre de tuer du bot
     */
    @JProp
    public int tuers = 0;
    /**
     * nombre de morts du bot
     */
    @JProp
    public int morts = 0;

    /**
     * {@link PlayerKilled} compteur pour le nombre de tuer.
     * @param event
     */
    @EventListener(eventClass = PlayerKilled.class)
    public void joueurTuer(PlayerKilled event) {
        if (event.getKiller().equals(info.getId())) {
            ++tuers;
        }
        if (ennemi == null) {
            return;
        }
        if (ennemi.getId().equals(event.getId())) {
            ennemi = null;
        }
    }
    
    protected Player ennemi = null;
    /**
     * Item visé
     */
    protected Item item = null;
   
    protected TabooSet<Item> ItemsInutiles = null;
    private UT2004PathAutoFixer autoFixer;
    private static int instanceCount = 0;

    /**
     * Preparation du bot avant son entrer sur le serveur.
     * @param bot
     */
    @Override
    public void prepareBot(UT2004Bot bot) {
        ItemsInutiles = new TabooSet<Item>(bot);

        autoFixer = new UT2004PathAutoFixer(bot, navigation.getPathExecutor(), fwMap, aStar, navBuilder);

        // listeners        
        navigation.getState().addListener(new FlagListener<NavigationState>() {

            @Override
            public void flagChanged(NavigationState changedValue) {
                switch (changedValue) {
                    case PATH_COMPUTATION_FAILED:
                    case STUCK:
                        if (item != null) {
                            ItemsInutiles.add(item, 10);
                        }
                        reinitialisation();
                        break;

                    case TARGET_REACHED:
                        reinitialisation();
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
    protected void reinitialisation() {
        item = null;
        ennemi = null;
        navigation.stopNavigation();
        itemsACollecter = null;
    }

    @EventListener(eventClass = PlayerDamaged.class)
    public void JoueurTouche(PlayerDamaged event) {
        log.log(Level.INFO, "J'ai infligé: {0}[{1}]", new Object[]{event.getDamageType(), event.getDamage()});
    }

    @EventListener(eventClass = BotDamaged.class)
    public void botTouche(BotDamaged event) {
        log.log(Level.INFO, "J'ai reçu: {0}[{1}]", new Object[]{event.getDamageType(), event.getDamage()});
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
        // 1) ennemi repéré ? 	-> poursuivre (tirer / suivre)
        if (engager && players.canSeeEnemies() && weaponry.hasLoadedWeapon()) {
            etatEngager();
            return;
        }

        // 2) en train de tiré    -> arrête de tiré si l'ennemi est perdu de vue
        if (info.isShooting() || info.isSecondaryShooting()) {
            getAct().act(new StopShooting());
        }

        // 3) dommages reçu ?	-> tourne sur lui même pour chercher l'ennemi
        if (senses.isBeingDamaged()) {
            this.etatTouche();
            return;
        }

        // 4) ennemi poursuivis -> va à la dernière position connue de l'ennemi
        if (ennemi != null && poursuivre && weaponry.hasLoadedWeapon()) {
            this.etatPoursuite();
            return;
        }

        // 5) blessé ?			-> cherche des soins
        if (collecterSoins && info.getHealth() < niveauVie) {
            this.etatSoins();
            return;
        }

        // 6) rien ... ramasses les items
        etatCollecterItem();
    }

    protected boolean AllerAuJoueur = false;

    protected void etatEngager() {
        //log.info("Decision: Engager");
        //config.setName("Hunter [Engager]");

        boolean tirer = false;
        double distance = Double.MAX_VALUE;
        compteurPousuite = 0;

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
            AllerAuJoueur = false;
        } else {
            // 2) tires sur l'ennemi s'il est visible
            distance = info.getLocation().getDistance(ennemi.getLocation());
            if (shoot.shoot(weaponPrefs, ennemi) != null) {
                log.info("Tirer sur l'ennemi!!!");
                tirer = true;
            }
        }

        // 3) Si l'ennemis n'est pas visible ou trop loin -> vas vers lui
        int distSuffisante = Math.round(random.nextFloat() * 800) + 200;
        if (!ennemi.isVisible() || !tirer || distSuffisante < distance) {
            if (!AllerAuJoueur) {
                navigation.navigate(ennemi);
                AllerAuJoueur = true;
            }
        } else {
            AllerAuJoueur = false;
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
    protected void etatTouche() {
        //log.info("Decision is: Toucher");
        bot.getBotName().setInfo("Toucher");
        if (navigation.isNavigating()) {
            navigation.stopNavigation();
            item = null;
        }
        getAct().act(new Rotate().setAmount(32000));
    }

    //////////////////////
    // Etat Poursuite  //
    ////////////////////
    protected void etatPoursuite() {
        //log.info("Decision: PURSUE");
        ++compteurPousuite;
        if (compteurPousuite > 30) {
            reinitialisation();
        }
        if (ennemi != null) {
            bot.getBotName().setInfo("Poursuivre");
            navigation.navigate(ennemi);
            item = null;
        } else {
            reinitialisation();
        }
    }
    protected int compteurPousuite = 0;

    ///////////////////
    // Etat Soins //
    /////////////////
    protected void etatSoins() {
        //log.info("Décision: Soins");
        Item newItem = items.getPathNearestSpawnedItem(ItemType.Category.HEALTH);
        if (newItem == null) {
            log.warning("Pas d'item de vie => Items");
            etatCollecterItem();
        } else {
            bot.getBotName().setInfo("Soins");
            navigation.navigate(newItem);
            this.item = newItem;
        }
    }
    ///////////////////////////
    // Etat Collecter Items //
    /////////////////////////
    protected List<Item> itemsACollecter = null;

    protected void etatCollecterItem() {
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

        Item newItem = MyCollections.getRandom(ItemsInutiles.filter(objetInteressant));
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
        reinitialisation();
    }

//    public static void main(String args[]) throws PogamutException {
//        /**
//         * Instanciation d'un bot.
//         */    
//        new UT2004BotRunner(ResponsiveBotTest.class, "responsiveBot").setMain(true).setLogLevel(Level.INFO).startAgents(1);
//    }
    
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
        
        UT2004BotRunner runner = new UT2004BotRunner(ResponsiveBotTest.class, "K-A-C-A", host, port);
        runner.setMain(true);
        runner.setLogLevel(Level.INFO);
        runner.startAgents(1);
    }
}
