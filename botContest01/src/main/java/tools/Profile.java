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

public class Profile {
    
    //paramètre principaux du profil
    private int trust = 70;
    private int caution = 30;
    private int anger = 0;
    
    public int getTrust () {
        return this.trust;
    }
    
    public int getCaution () {
        return this.caution;
    }
    
    public int getAnger () {
        return this.anger;
    }
    
    public void trustModifiedBy (int val) {
        int tmp = this.trust + val;
        if ((tmp >= 0) && (tmp <= 100))
            this.trust = tmp;
        else if (tmp < 0)
            this.trust = 0;
        else
            this.trust = 100;
    }
    
    public void cautionModifiedBy (int val) {
        int tmp = this.caution + val;
        if ((tmp >= 0) && (tmp <= 100))
            this.caution = tmp;
        else if (tmp < 0)
            this.caution = 0;
        else
            this.caution = 100;
    }
    
    public void angerModified (int val) {
        int tmp = this.anger + val;
        if ((tmp >= 0) && (tmp <= 100))
            this.anger = tmp;
        else if (tmp < 0)
            this.anger = 0;
        else
            this.anger = 100;
    }
    
    public int randomIntForChoice (int nb_choix) {
        return ((int)Math.round(Math.random() * (nb_choix - 1)) + 1);
    }
    
    private boolean returnBool(Situation sit, int minHealthLevel, int maxHealthLevel, boolean armourInf, int armourLevel, int cautionLevel, int trustLevel){
        boolean result;
        
        if (armourInf)
            result = ((sit.getHealthLevel() > minHealthLevel) && (sit.getHealthLevel() <= maxHealthLevel)
                    && (sit.getArmourLevel() < armourLevel) && ((this.caution < cautionLevel) || (this.trust > trustLevel)));
        else
            result = ((sit.getHealthLevel() > minHealthLevel) && (sit.getHealthLevel() <= maxHealthLevel)
                    && (sit.getArmourLevel() >= armourLevel) && ((this.caution < cautionLevel) || (this.trust > trustLevel)));
        
        
        return result;
                        
    }
    
    public Action decision (Situation sit, Action previous) {
        
        
        if (sit.justEscaped) {
            ++sit.logicIterationNumber;
            if (sit.logicIterationNumber > 8){
                sit.justEscaped = false;
                sit.logicIterationNumber = 0;
            }
        }
        
        
        //si le bot est en mode passif
        if ((previous == Action.BASIC_COLLECT) || (previous == Action.HEALTH) || (previous == Action.INJURED)) {
            
            //si il voit des ennemis
            if (sit.getNbVisibleEnnemies() > 0) {
                
                //condition d'engagement pour 1 ennemi visible
                if (sit.getNbVisibleEnnemies() == 1) {
                    
                    if (this.returnBool(sit,30,50,false,50,40,60)
                        || this.returnBool(sit,30,50,true,50,30,70)
                        || (sit.getHealthLevel() > 70) || (this.anger > 80)) {

                        return Action.FIGHT;

                    } else {
                        sit.escape = true;
                        return Action.ESCAPE;
                    }
                    
                }
                
                //condition d'engagement pour 2 ennemi visible
                if (sit.getNbVisibleEnnemies() == 2) {
                    
                    if (this.returnBool(sit,30,50,false,50,10,90)
                        || this.returnBool(sit,50,70,false,50,30,70)
                        || this.returnBool(sit,50,70,true,50,20,80)
                        || (sit.getHealthLevel() > 70) || (this.anger > 95)) {

                        return Action.FIGHT;

                    } else {
                        sit.escape = true;
                        return Action.ESCAPE;
                    }
                    
                }
                
                //si 3 ennemis ou plus -> fuite
                if (sit.getNbVisibleEnnemies() > 2) { 
                    
                    sit.escape = true;
                    return Action.ESCAPE;
                    
                }     
                
            } //sinon si pas d'ennemi et pv inférieur a 70 il recherche de la vie en priorité
            else if (sit.getHealthLevel() < 70)
                
                return Action.HEALTH;
            
            //si on se fait blesser
            else if (sit.injured) {
                
                return Action.INJURED;
                
            } //sinon on fait une collecte basique
            else
                
                return Action.BASIC_COLLECT;
            
        }
        
        if (previous == Action.ESCAPE) {
            
            if (sit.stuck)
                
                return Action.FIGHT;
            
            if (sit.justEscaped)
                
                return Action.BASIC_COLLECT;
            
            else{
                sit.escape = true;
                return Action.ESCAPE;
            }
        }
        
        if (previous == Action.HUNT) {
            
            if (sit.nb_enemy_engaged == 0)
                
                return Action.BASIC_COLLECT;
            
            if (sit.getNbVisibleEnnemies() > 0) {
                
                if (sit.getNbVisibleEnnemies() <= sit.nb_enemy_engaged)
                    
                    return Action.FIGHT;
                
                else if (sit.getNbVisibleEnnemies() == 2) {
                    
                    if (this.returnBool(sit,30,50,false,50,10,90)
                        || this.returnBool(sit,50,70,false,50,30,70)
                        || this.returnBool(sit,50,70,true,50,20,80)
                        || (sit.getHealthLevel() > 70) || (this.anger > 95)) {

                        return Action.FIGHT;

                    } else {
                        sit.escape = true;
                        return Action.ESCAPE;
                    }
                    
                } else {
                    sit.escape = true;
                    return Action.ESCAPE;
                }
                
            }
            else
                return Action.HUNT;
        }
        
        if (previous == Action.FIGHT) {
            
            if (sit.stuck)
                
                return Action.FIGHT;
            
            if (sit.nb_enemy_engaged < sit.getNbVisibleEnnemies())
                
                sit.nb_enemy_engaged = sit.getNbVisibleEnnemies();
            
            if (sit.nb_enemy_engaged == 0)
                
                return Action.HEALTH;
            
            if (sit.getNbVisibleEnnemies() == 0) {
                
                if ((this.trust > 40) || (this.anger > 65))
                    
                    return Action.HUNT;
                
                else
                    
                    return Action.HEALTH;
                
            }
            
            //condition d'engagement pour 1 ennemi
            if (sit.getNbVisibleEnnemies() == 1) {
                    
                if (this.returnBool(sit,30,50,false,50,40,60)
                    || this.returnBool(sit,30,50,true,50,30,70)
                    || (sit.getHealthLevel() > 70) || (this.anger > 80)) {

                    return Action.FIGHT;

                } else {
                    sit.escape = true;
                    return Action.ESCAPE;
                }

            }

            //condition d'engagement pour 2 ennemi visible
            if (sit.getNbVisibleEnnemies() == 2) {

                if (this.returnBool(sit,30,50,false,50,10,90)
                    || this.returnBool(sit,50,70,false,50,30,70)
                    || this.returnBool(sit,50,70,true,50,20,80)
                    || (sit.getHealthLevel() > 70) || (this.anger > 95)) {

                    return Action.FIGHT;

                } else {
                    sit.escape = true;
                    return Action.ESCAPE;
                }

            }

            //si 3 ennemis ou plus -> fuite
            if (sit.getNbVisibleEnnemies() > 2) { 

                sit.escape = true;
                return Action.ESCAPE;

            }
            
        }
        
        return Action.BASIC_COLLECT;
    }
    
    //fonction servant a savoir si on nargue un ennemi ou pas après l'avoir tuer.
    public boolean humiliation (Situation sit) {
        
        if (sit.getNbVisibleEnnemies() == 0) {
            
            if ((this.trust >= 70) && (this.caution <= 50)) {
                int choix = this.randomIntForChoice(2);
                switch (choix) {
                    case 1:
                        return true;
                    case 2:
                        return false;
                    default:
                }
            } else if ((this.anger >= 70) && (this.anger < 95)) {
                int choix = this.randomIntForChoice(3);
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
