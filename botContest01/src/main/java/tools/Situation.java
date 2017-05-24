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
package tools;

/**
 * @author K-A-C-A-Team
 */

public class Situation {
    
    //booléens décrivant ce que fait le bot
    public boolean basic = true;
    public boolean engage = true; //renommer en fight
    public boolean hunt = true;
    public boolean rearm = true;
    public boolean healthCollect = true;
    public boolean escape = false;
    public boolean injured = false;
    
    //information
    public boolean justEscaped = true;
    public boolean stuck = false;
    public int logicIterationNumber = 0;
    public int nb_ennemy_engaged = 0;
    private int nb_visible_enemies = 0;
    private int health_level = 100;
    private int armor_level = 0;
    private int deaths = 0;
    private int kills = 0;
    
    public int getNbVisibleEnnemies () {
        return this.nb_visible_enemies;
    }
    
    public int getHealthLevel () {
        return this.health_level;
    }
    
    public int getArmourLevel () {
        return this.armor_level;
    }
    
    public int getDeaths () {
        return this.deaths;
    }
    
    public int getKills () {
        return this.kills;
    }
    
    public void incDeaths () {
        this.deaths++;
    }
    
    public void incKills () {
        this.kills++;
    }
    
    public void situationActualisation (int armor, int enemy, int health) {
        this.armor_level = armor;
        this.nb_visible_enemies = enemy;
        this.health_level = health;
    }
    
}
