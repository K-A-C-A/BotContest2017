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
 * @author Anthony BORDEAU
 */

public class Profile {
    
    //paramètre principaux du profil
    private int trust = 50;
    private int caution = 30;
    private int anger = 0;
    
    //booleén servant à savoir dans quel type d'action il est en train de faire
    private boolean fight = false;
    private boolean escape = false;
    private boolean basic = true;
    
    public int getTrust () {
        return this.trust;
    }
    
    public int getCaution () {
        return this.caution;
    }
    
    public int getHunger () {
        return this.anger;
    }
    
    public void trust_actualisation (int val) {
        int tmp = this.trust + val;
        if ((tmp >= 0) && (tmp <= 100))
            this.trust = tmp;
        else if (tmp < 0)
            this.trust = 0;
        else
            this.trust = 100;
    }
    
    public void caution_actualisation (int val) {
        int tmp = this.caution + val;
        if ((tmp >= 0) && (tmp <= 100))
            this.caution = tmp;
        else if (tmp < 0)
            this.caution = 0;
        else
            this.caution = 100;
    }
    
    public void hunger_actualisation (int val) {
        int tmp = this.anger + val;
        if ((tmp >= 0) && (tmp <= 100))
            this.anger = tmp;
        else if (tmp < 0)
            this.anger = 0;
        else
            this.anger = 100;
    }
    
    private int randomPick (int nb_choix) {
        return ((int)Math.round(Math.random() * (nb_choix - 1)) + 1);
    }
    
    public Action decision (Situation sit) {
        
        //si le bot est en mode passif
        if (basic) {
            
            //si il voit des ennemis
            if (sit.getNbVisibleEnnemies() > 0) {
                
                //condition d'engagement
                if (((sit.getHealthLevel() > 30) && (sit.getHealthLevel() <= 50) && (sit.getArmourLevel() > 50) && ((this.caution < 20) || (this.trust > 80)))
                    || ((sit.getHealthLevel() > 30) && (sit.getHealthLevel() <= 50) && (sit.getArmourLevel() < 50) && ((this.caution < 10) || (this.trust > 90)))
                    || ((sit.getHealthLevel() > 50) && (sit.getHealthLevel() <= 70) && (sit.getArmourLevel() > 50) && ((this.caution < 40) || (this.trust > 60)))
                    || ((sit.getHealthLevel() > 50) && (sit.getHealthLevel() <= 70) && (sit.getArmourLevel() < 50) && ((this.caution < 30) || (this.trust > 70)))
                    || (sit.getHealthLevel() > 70) || (this.anger > 90)) {
                    
                    //changer l'arme si nécessaire sinon combat
                    if ((float)sit.getNbWeaponAmmo() < 0.1 * (float)sit.getNbMaxWeaponAmmo())
                        return Action.SWITCH_WEAPON;
                    else {
                        this.fight = true;
                        this.basic = false;
                        return Action.FIGHT;
                    }
                
                } else { //sinon fuite
                    
                    this.escape = true;
                    this.basic = false;
                    return Action.ESCAPE;
                    
                }     
                
            } //sinon si pas d'ennemi et pv inférieur a 70 il recherche de la vie en priorité
            else if (sit.getHealthLevel() < 70)
                
                return Action.HEALTH;
            
            //sinon on continue le mode passif ou il n'a pas d'instruction en priorité (recherche d'objet/vie/ennemi)
            else
                
                return Action.BASIC_COLLECT;
            
        }
        
        if (fight) {
            
            if ((float)sit.getNbWeaponAmmo() < 0.1 * (float)sit.getNbMaxWeaponAmmo())
                return Action.SWITCH_WEAPON;
            else
                return Action.FIGHT;
            
        }
        
        return Action.BASIC_COLLECT;
    }
    
    //fonction servant a savoir si on nargue un ennemi ou pas après l'avoir tuer.
    public boolean humiliation (Situation sit) {
        
        if (sit.getNbVisibleEnnemies() == 0) {
            
            if ((this.trust >= 70) && (this.caution <= 50)) {
                int choix = this.randomPick(2);
                switch (choix) {
                    case 1:
                        return true;
                    case 2:
                        return false;
                    default:
                }
            } else if ((this.anger >= 70) && (this.anger < 95)) {
                int choix = this.randomPick(3);
                switch (choix) {
                    case 1 :
                    case 2 :
                        return false;
                    case 3 :
                        return true;
                    default :
                }
            } else if (this.anger >= 95)
                return true;
            
        }
        
        return false;
        
    }
    
}
