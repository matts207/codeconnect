/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tcvcog.tcvce.application;

import com.tcvcog.tcvce.entities.search.SearchParams;
import com.tcvcog.tcvce.entities.search.SearchParamsCEActionRequests;
import com.tcvcog.tcvce.entities.search.SearchParamsPersons;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * @author sylvia
 */
public class SearchCoordinator extends BackingBeanUtils implements Serializable{

    /**
     * Creates a new instance of SearchCoordinator
     */
    public SearchCoordinator() {
    }
    
    
    
    /**
     * The temporarily hard-coded values for default search parameters for various
     * types of search Param objects
     * 
     * @return an search params object for CEAction requests with default values
     * which amount to requests that aren't attached to a case and were submitted
     * within the past 10 years
     */
    public SearchParamsCEActionRequests getDefaultSearchParamsCEActionRequests(){
        
            System.out.println("CaseCoordinator.configureDefaultSearchParams "
                    + "| found actionrequest param object");
            
            SearchParamsCEActionRequests sps = new SearchParamsCEActionRequests();
            sps.setMuni(getSessionBean().getActiveMuni());

            LocalDateTime pastTenYears = LocalDateTime.now().minusYears(10);
            sps.setStartDate(pastTenYears);
            
            // action requests cannot have a time stamp past the current datetime
            sps.setEndDate(LocalDateTime.now());
            
            sps.setUseAttachedToCase(true);
            sps.setAttachedToCase(false);
            sps.setUseMarkedUrgent(false);
            sps.setUseNotAtAddress(false);
            sps.setUseRequestStatus(false);
        
        return sps;
    }
    
    public SearchParamsPersons getDefaultSearchParamsPersons(){
        SearchParamsPersons spp = new SearchParamsPersons();
        spp.setMuni(getSessionBean().getActiveMuni());
        spp.setFilterByStartEndDate(false);
        spp.setFilterByFirstName(false);
        spp.setFilterByLastName(true);
        spp.setOnlySearchBlobs(false);
        spp.setFilterByPersonTypes(false);
        spp.setFilterByEmail(false);
        spp.setFilterByAddressStreet(false);
        spp.setFilterByActiveSwitch(false);
        spp.setFilterByVerifiedSwitch(false);
       
        return spp;
        
    }
}
