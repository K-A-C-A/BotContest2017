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
 *
 * @author Anthony BORDEAU
 */
public class Situation {
    
    private int nb_visible_enemies = 0;
    private int nb_visible_objects;
    private int nb_weapon_ammo = 100;
    private int nb_max_weapon_ammo;
    private int health_level = 100;
    private int armor_level = 0;
    private int deaths = 0;
    private int kills = 0;
    
    public Situation (int object, int weapon_ammo, int max_weapon_ammo) {
        this.nb_visible_objects = object;
        this.nb_weapon_ammo = weapon_ammo;
        this.nb_max_weapon_ammo = max_weapon_ammo;
    }
    
    public int getNbVisibleEnnemies () {
        return this.nb_visible_enemies;
    }
    
    public int getVisibleObjects () {
        return this.nb_visible_objects;
    }
    
    public int getNbWeaponAmmo () {
        return this.nb_weapon_ammo;
    }
    
    public int getNbMaxWeaponAmmo () {
        return this.nb_max_weapon_ammo;
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
    
    public void SituationActualisation (int armor, int enemy, int weapon, int max_weapon, int object, int health) {
        this.armor_level = armor;
        this.nb_visible_enemies = enemy;
        this.nb_weapon_ammo = weapon;
        this.nb_max_weapon_ammo = max_weapon;
        this.nb_visible_objects = object;
        this.health_level = health;
    }
    
}
