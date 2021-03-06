/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tcvcog.tcvce.entities.search;

import com.tcvcog.tcvce.entities.Municipality;
import com.tcvcog.tcvce.entities.PersonType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores search parameters and switches turning each one on and off 
 * for queries involving Person objects
 * 
 * @author Sylvia Garland
 */
public class SearchParamsPersons extends SearchParams implements Serializable{
     
    
        private String firstNameSS;
        private boolean filterByFirstName;

        private String lastNameSS;
        private boolean filterByLastName;
        private boolean onlySearchBlobs;
    
        private List<PersonType> personTypes; 
        private boolean filterByPersonTypes;
        
        private String emailSS;
        private boolean filterByEmail;
        
        private String addrStreetSS;
        private boolean filterByAddressStreet;
        
        private boolean activeSwitch;
        private boolean filterByActiveSwitch;
        
        private boolean verifiedSwitch;
        private boolean filterByVerifiedSwitch;
        
        
        
   public SearchParamsPersons(){
       personTypes = new ArrayList<>();
   }

    /**
     * @return the firstNameSS
     */
    public String getFirstNameSS() {
        return firstNameSS;
    }

    /**
     * @return the filterByFirstName
     */
    public boolean isFilterByFirstName() {
        return filterByFirstName;
    }

    /**
     * @return the lastNameSS
     */
    public String getLastNameSS() {
        return lastNameSS;
    }

    /**
     * @return the filterByLastName
     */
    public boolean isFilterByLastName() {
        return filterByLastName;
    }

    /**
     * @return the onlySearchBlobs
     */
    public boolean isOnlySearchBlobs() {
        return onlySearchBlobs;
    }

    /**
     * @return the personTypes
     */
    public List<PersonType> getPersonTypes() {
        return personTypes;
    }

    /**
     * @return the filterByPersonTypes
     */
    public boolean isFilterByPersonTypes() {
        return filterByPersonTypes;
    }

    /**
     * @return the emailSS
     */
    public String getEmailSS() {
        return emailSS;
    }

    /**
     * @return the filterByEmail
     */
    public boolean isFilterByEmail() {
        return filterByEmail;
    }

    /**
     * @return the addrStreetSS
     */
    public String getAddrStreetSS() {
        return addrStreetSS;
    }

    /**
     * @return the filterByAddressStreet
     */
    public boolean isFilterByAddressStreet() {
        return filterByAddressStreet;
    }

    /**
     * @return the activeSwitch
     */
    public boolean isActiveSwitch() {
        return activeSwitch;
    }

    /**
     * @return the filterByActiveSwitch
     */
    public boolean isFilterByActiveSwitch() {
        return filterByActiveSwitch;
    }

    /**
     * @return the verifiedSwitch
     */
    public boolean isVerifiedSwitch() {
        return verifiedSwitch;
    }

    /**
     * @return the filterByVerifiedSwitch
     */
    public boolean isFilterByVerifiedSwitch() {
        return filterByVerifiedSwitch;
    }

    /**
     * @param firstNameSS the firstNameSS to set
     */
    public void setFirstNameSS(String firstNameSS) {
        this.firstNameSS = firstNameSS;
    }

    /**
     * @param filterByFirstName the filterByFirstName to set
     */
    public void setFilterByFirstName(boolean filterByFirstName) {
        this.filterByFirstName = filterByFirstName;
    }

    /**
     * @param lastNameSS the lastNameSS to set
     */
    public void setLastNameSS(String lastNameSS) {
        this.lastNameSS = lastNameSS;
    }

    /**
     * @param filterByLastName the filterByLastName to set
     */
    public void setFilterByLastName(boolean filterByLastName) {
        this.filterByLastName = filterByLastName;
    }

    /**
     * @param onlySearchBlobs the onlySearchBlobs to set
     */
    public void setOnlySearchBlobs(boolean onlySearchBlobs) {
        this.onlySearchBlobs = onlySearchBlobs;
    }

    /**
     * @param personTypes the personTypes to set
     */
    public void setPersonTypes(List<PersonType> personTypes) {
        this.personTypes = personTypes;
    }

    /**
     * @param filterByPersonTypes the filterByPersonTypes to set
     */
    public void setFilterByPersonTypes(boolean filterByPersonTypes) {
        this.filterByPersonTypes = filterByPersonTypes;
    }

    /**
     * @param emailSS the emailSS to set
     */
    public void setEmailSS(String emailSS) {
        this.emailSS = emailSS;
    }

    /**
     * @param filterByEmail the filterByEmail to set
     */
    public void setFilterByEmail(boolean filterByEmail) {
        this.filterByEmail = filterByEmail;
    }

    /**
     * @param addrStreetSS the addrStreetSS to set
     */
    public void setAddrStreetSS(String addrStreetSS) {
        this.addrStreetSS = addrStreetSS;
    }

    /**
     * @param filterByAddressStreet the filterByAddressStreet to set
     */
    public void setFilterByAddressStreet(boolean filterByAddressStreet) {
        this.filterByAddressStreet = filterByAddressStreet;
    }

    /**
     * @param activeSwitch the activeSwitch to set
     */
    public void setActiveSwitch(boolean activeSwitch) {
        this.activeSwitch = activeSwitch;
    }

    /**
     * @param filterByActiveSwitch the filterByActiveSwitch to set
     */
    public void setFilterByActiveSwitch(boolean filterByActiveSwitch) {
        this.filterByActiveSwitch = filterByActiveSwitch;
    }

    /**
     * @param verifiedSwitch the verifiedSwitch to set
     */
    public void setVerifiedSwitch(boolean verifiedSwitch) {
        this.verifiedSwitch = verifiedSwitch;
    }

    /**
     * @param filterByVerifiedSwitch the filterByVerifiedSwitch to set
     */
    public void setFilterByVerifiedSwitch(boolean filterByVerifiedSwitch) {
        this.filterByVerifiedSwitch = filterByVerifiedSwitch;
    }
   
   
   
    
}
