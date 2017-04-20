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
package outils;

/**
 *
 * @author Anthony BORDEAU
 */
public class Profil {
    
    private int confiance;
    private int peur;
    private int skill;
    private int exitation;
    private int reactivite;
    
    public Profil() {
        this.confiance = 50;
        this.peur = 0;
        this.skill = 50;
        this.exitation = 0;
        this.reactivite = 30;
    }
    
    public int getConfiance () {
        return this.confiance;
    }
    
    public int getPeur () {
        return this.peur;
    }
    
    public int getSkill () {
        return this.skill;
    }
    
    public int getExitation () {
        return this.exitation;
    }
    
    public int getReactivite () {
        return this.reactivite;
    }
    
    public Action decision (Situation sit) {
        return Action.DEPLACEMENT_SANS_BUT;
    }
    
}
