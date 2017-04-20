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
public class Situation {
    
    private int nb_enemis_visibles;
    private int nb_objets_visibles;
    private int nb_munitions_arme;
    private int nb_pv;
    private int nb_armure;
    
    public Situation (int enemi, int objet, int mun_arme, int pv, int armure) {
        this.nb_enemis_visibles = enemi;
        this.nb_objets_visibles = objet;
        this.nb_munitions_arme = mun_arme;
        this.nb_pv = pv;
        this.nb_armure = armure;
    }
    
    public int getNbEnemiVisible () {
        return this.nb_enemis_visibles;
    }
    
    public int getObjetVisible () {
        return this.nb_objets_visibles;
    }
    
    public int getNbMunitionArme () {
        return this.nb_munitions_arme;
    }
    
    public int getPV () {
        return this.nb_pv;
    }
    
    public int getArmure () {
        return this.nb_armure;
    }
    
}
