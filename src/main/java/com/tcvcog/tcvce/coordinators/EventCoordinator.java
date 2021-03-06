/*
 * Copyright (C) 2017 Turtle Creek Valley
Council of Governments, PA
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
package com.tcvcog.tcvce.coordinators;

import com.tcvcog.tcvce.application.BackingBeanUtils;
import com.tcvcog.tcvce.domain.CaseLifecyleException;
import com.tcvcog.tcvce.domain.EventException;
import com.tcvcog.tcvce.domain.IntegrationException;
import com.tcvcog.tcvce.domain.ViolationException;
import com.tcvcog.tcvce.entities.CECase;
import com.tcvcog.tcvce.entities.CasePhase;
import com.tcvcog.tcvce.entities.CodeViolation;
import com.tcvcog.tcvce.entities.EventCECase;
import com.tcvcog.tcvce.entities.EventCategory;
import com.tcvcog.tcvce.entities.EventType;
import com.tcvcog.tcvce.entities.Person;
import com.tcvcog.tcvce.integration.EventIntegrator;
import java.io.Serializable;
import com.tcvcog.tcvce.util.Constants;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.faces.application.FacesMessage;

/**
 *
 * Object playing the role of coordinator in the MVC framework by interfacing between
 * the JSF backing beans and the database integration classes.
 * 
 * This role involves several duties:
 * 
 * <ol>
 * <li>Generating new events for Code Enforcement and occupancy</li>
 * <li>Checking event creation permissions before allowing events of
 * restricted categories to be attached to other system objects.</li>
 * </ol>
 * 
 * 
 * @author Eric C. Darsow
 */
public class EventCoordinator extends BackingBeanUtils implements Serializable{

    /**
     * Creates a new instance of EventCoordinator
     */
    public EventCoordinator() {
        
    }
    
    /**
     * TODO: finish
     * @return 
     */
    public EventType[] getUserAdmnisteredEventTypeList(){
        EventType[] eventTypeList = EventType.values();
            
        
        return eventTypeList;
        
    }
    
    /**
     * Core coordinator method called by all other classes who want to 
     * create their own event. Restricts the event type based on current
     * case phase (closed cases cannot have action, origination, or compliance events.
     * Includes the instantiation of Event objects
     * 
     * @param c the case to which the event should be attached
     * @param ec the type of event to attach to the case
     * @return an initialized event with basic properties set
     * @throws CaseLifecyleException thrown if the case is in an improper state for proposed event
     */
    public EventCECase getInitializedEvent(CECase c, EventCategory ec) throws CaseLifecyleException{
        
        // check to make sure the case isn't closed before allowing event into the switched blocks
        if(c.getCasePhase() == CasePhase.Closed && 
                (
                    ec.getEventType() == EventType.Action
                    || 
                    ec.getEventType() == EventType.Origination
                    ||
                    ec.getEventType() == EventType.Compliance
                )
        ){
            
            throw new CaseLifecyleException("This event cannot be attached to a closed case");
            
        }
        
        // the moment of event instantiaion!!!!
        EventCECase event = new EventCECase();
        event.setCategory(ec);
        event.setActiveEvent(true);
        event.setHidden(false);
        System.out.println("EventCoordinator.getInitalizedEvent | eventCat: " 
                + event.getCategory().getEventCategoryTitle());
        return event;
    }
    
    /**
     * For use by the public messaging system which attaches events to code enforcement
     * cases without having access to the entire CECase object--only the caseid
     * @param caseID to which the event should be attached
     * @return an instantiated EventCECase object ready to be configured
     */
    public EventCECase getInitializedEvent(int caseID){
        EventCECase event = new EventCECase();
        event.setCaseID(caseID);
        return event;
        
    }
    
    
    /**
     * Factory method for event categories.
     * Whoever calls this method will still need to do basic setup of the event 
     * before sending to the CaseCoordinator processEvent(CEEvent e) method
     * @return an EventCategory container with basic properties set
     */
    public EventCategory getInitializedEventCateogry(){
        EventCategory ec =  new EventCategory();
        ec.setUserdeployable(true);
        ec.setHidable(true);
        // TODO: finishing autoconfiguring these 
        return ec;
    }
    
    /**
     * Factory method for creating event categories when only the categoryID
     * is available. This is handy since the insertEvent method on the integrator
     * really only needs the categoryID for storing in the database
     * @param catID the categoryID of the EventCategory you want
     * @return an instantiated EventCategory object
     */
    public EventCategory getInitiatlizedEventCategory(int catID) throws IntegrationException{
        EventIntegrator ei = getEventIntegrator();
        EventCategory ec =  ei.getEventCategory(catID);
        return ec;
    }
    
    /**
     * Takes in a well-formed event message from the CaseCoordinator and
     * initializes the appropriate properties on the event before insertion
     * @param caseID id of the case to which the event should be attached
     * @param message the text of the event description message
     * @throws IntegrationException in the case of broken integration process
     */
    public void attachPublicMessagToCECase(int caseID, String message) throws IntegrationException{
        EventIntegrator ei = getEventIntegrator();
        
        
        int publicMessageEventCategory = Integer.parseInt(getResourceBundle(Constants.EVENT_CATEGORY_BUNDLE).getString("publicCECaseMessage"));
        EventCategory ec = getInitiatlizedEventCategory(publicMessageEventCategory);
        
        // setup all the event properties
        EventCECase event = getInitializedEvent(caseID);
        event.setCategory(ec);
        event.setDateOfRecord(LocalDateTime.now());
        event.setEventDescription(message);
        event.setEventOwnerUser(getSystemRobotUser());
        event.setDiscloseToMunicipality(true);
        event.setDiscloseToPublic(true);
        event.setActiveEvent(true);
        event.setHidden(false);
        event.setNotes("Event created by a public user");
        
        
        // sent the built event to the integrator!
        ei.insertEvent(event);
        
    }
    
    

    /**
     * Creates a populated event to log the change of a code violation update. 
     * The event is coming to us from the violationEditBB with the description and disclosures flags
     * correct. This method needs to set the description from the resource bundle, and 
     * set the date of record to the current date
     * @param ceCase the CECase whose violation was updated
     * @param cv the code violation being updated
     * @param event An initialized event
     * @throws IntegrationException bubbled up from the integrator
     * @throws EventException 
     */
    public void generateAndInsertCodeViolationUpdateEvent(CECase ceCase, CodeViolation cv, EventCECase event) throws IntegrationException, EventException{
        EventIntegrator ei = getEventIntegrator();
        String updateViolationDescr = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("violationChangeEventDescription");
        // fetch the event category id from the event category bundle under the key updateViolationEventCategoryID
        // now we're ready to log the event
        EventCategory ec = new EventCategory();
        ec.setCategoryID(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("updateViolationEventCategoryID")));
        event.setCategory(ec); 
       
        // hard coded for now
//        event.setCategory(ei.getEventCategory(117));
        event.setCaseID(ceCase.getCaseID());
        event.setDateOfRecord(LocalDateTime.now());
        event.setEventDescription(updateViolationDescr);
        //even descr set by violation coordinator
        event.setEventOwnerUser(getFacesUser());
        // disclose to muni from violation coord
        // disclose to public from violation coord
        event.setActiveEvent(true);
        
        ei.insertEvent(event);
    }
    
    
    /**
     * Configures an event which represents the moment of compliance with
     * a code violation attached to a code enforcement case
     * 
     * @param violationList
     * @return a partially-baked event ready for inserting
     * @throws IntegrationException 
     */
    public EventCECase generateViolationComplianceEvent(List<CodeViolation> violationList) throws IntegrationException{
        EventCECase e = new EventCECase();
        EventIntegrator ei = getEventIntegrator();
          e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("complianceEvent"))));
        e.setEventDescription("Compliance with municipal code achieved");
        e.setActiveEvent(true);
        e.setDiscloseToMunicipality(true);
        e.setDiscloseToPublic(true);
        
        ListIterator<CodeViolation> li = violationList.listIterator();
        CodeViolation cv;
        StringBuilder sb = new StringBuilder();
        sb.append("Compliance with the following code violations was observed:");
        sb.append("<br/><br/>");
        
        while(li.hasNext()){
            cv = li.next();
            sb.append(cv.getViolatedEnfElement().getCodeElement().getOrdchapterNo());
            sb.append(".");
            sb.append(cv.getViolatedEnfElement().getCodeElement().getOrdSecNum());
            sb.append(".");
            sb.append(cv.getViolatedEnfElement().getCodeElement().getOrdSubSecNum());
            sb.append(":");
            sb.append(cv.getViolatedEnfElement().getCodeElement().getOrdSubSecTitle());
            sb.append("<br/><br/>");
        }
        e.setNotes(sb.toString());
        return e;
    }
    
    /**
     * At its current impelementation, this amounts to a factory for ArrayLists
     * that are populated by the user when creating events
     * @return 
     */
    public ArrayList<Person> getEmptyEventPersonList(){
        return new ArrayList<>();
    }
    
    public String updateEvent(EventCECase event, boolean clearViewConfirmation) throws IntegrationException{
        EventIntegrator ei = getEventIntegrator();
        ei.updateEvent(event, clearViewConfirmation);
        
        return "caseProfile";
    }
    
    
    public void insertEvent(EventCECase e) throws IntegrationException{
        EventIntegrator ei = getEventIntegrator();
        ei.insertEvent(e);
        
    }
    
    /**
     * a pass through method called by the eventAddBB which sends the event and case
     * over to the case coordinator for the meat of the processing cycle. This exists
     * such that the eventAddBB is interacting only with the methods on the EventCoordinator
     * and allows the implementation of event-specific logic before interacting with the
     * CaseCoordinator.
     * @param c the current case
     * @param e the event to be processed which is passed over to the CaseCoordinator
     * @throws IntegrationException
     * @throws CaseLifecyleException
     * @throws ViolationException 
     */
    public void initiateEventProcessing(CECase c, EventCECase e) throws IntegrationException, CaseLifecyleException, ViolationException{
        CaseCoordinator cc = getCaseCoordinator();
        
        cc.processCEEvent(c, e);
        
    }
    
    
    public ArrayList getEventList(CECase currentCase) throws IntegrationException{
        EventIntegrator ei = getEventIntegrator();
        ArrayList<EventCECase> ll = ei.getEventsByCaseID(currentCase.getCaseID());
        return ll;
    }
    
    /**
     * The currentCase argument must be a CECase with the desired case phase set
     * The past phase is passed in separately, allowing for phase changes to
     * any phase from any other phase
     * @param currentCase
     * @param pastPhase
     * @throws IntegrationException
     * @throws CaseLifecyleException 
     */
    public void generateAndInsertPhaseChangeEvent(CECase currentCase, CasePhase pastPhase) throws IntegrationException, CaseLifecyleException{
        
        EventIntegrator ei = getEventIntegrator();
        
        CECase c = getSessionBean().getcECase();
        EventCECase event = getInitializedEvent(c, ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("casePhaseChangeEventCatID"))));
        
        StringBuilder sb = new StringBuilder();
        sb.append("Case phase changed from  \'");
        sb.append(pastPhase.toString());
        sb.append("\' to \'");
        sb.append(currentCase.getCasePhase().toString());
        sb.append("\' by an action event or manual override.");
        event.setEventDescription(sb.toString());
        
        event.setCaseID(currentCase.getCaseID());
        event.setDateOfRecord(LocalDateTime.now());
        // not sure if I can access the session level info for the specific user here in the
        // coordinator bean
        event.setEventOwnerUser(getFacesUser());
        event.setActiveEvent(true);
        
        insertEvent(event);
        
        getFacesContext().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "The case phase has been changed", ""));

    } // close method
    
    public void generateAndInsertManualCasePhaseOverrideEvent(CECase currentCase, CasePhase pastPhase) throws IntegrationException, CaseLifecyleException{
          EventIntegrator ei = getEventIntegrator();
        
        CECase c = getSessionBean().getcECase();
        EventCECase event = getInitializedEvent(c, ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("casePhaseManualOverride"))));
        
        StringBuilder sb = new StringBuilder();
        sb.append("Manual case phase change from  \'");
        sb.append(pastPhase.toString());
        sb.append("\' to \'");
        sb.append(currentCase.getCasePhase().toString());
        sb.append("\' by a a case officer.");
        event.setEventDescription(sb.toString());
        
        event.setCaseID(currentCase.getCaseID());
        event.setDateOfRecord(LocalDateTime.now());
        // not sure if I can access the session level info for the specific user here in the
        // coordinator bean
        event.setEventOwnerUser(getFacesUser());
        event.setActiveEvent(true);
        
        insertEvent(event);
        
        getFacesContext().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "The case phase has been changed", ""));
    }
    
    
    
    /**
     * An unused (but very schnazzy) method for generating the appropriate event that will advance a case
     * to the next phase of its life cycle. Currently called by the method 
     * getEventForTriggeringCasePhaseAdvancement in CaseManageBB
     * @param c
     * @return
     * @throws IntegrationException
     * @throws CaseLifecyleException 
     */
    public EventCECase getActionEventForCaseAdvancement(CECase c) throws IntegrationException, CaseLifecyleException{
        CasePhase cp = c.getCasePhase();
        EventIntegrator ei = getEventIntegrator();
        EventCECase e = new EventCECase();
        
        switch(cp){
            case PrelimInvestigationPending:

                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                   Constants.EVENT_CATEGORY_BUNDLE).getString("advToNoticeDelivery"))));
               return e;
             
            case NoticeDelivery:
                
                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                    Constants.EVENT_CATEGORY_BUNDLE).getString("advToInitialComplianceTimeframe"))));
                return e;
            
            case InitialComplianceTimeframe:
            
               e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
               Constants.EVENT_CATEGORY_BUNDLE).getString("advToSecondaryComplianceTimeframe"))));
               return e;
             
            case SecondaryComplianceTimeframe:
             
                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("advToAwaitingHearingDate"))));
                return e;
         
            case AwaitingHearingDate:
             
                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("advToHearingPreparation"))));
                return e;

            case HearingPreparation:
             
                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("advToInitialPostHearingComplianceTimeframe"))));
                return e;

            case InitialPostHearingComplianceTimeframe:
             
                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("advToSecondaryPostHearingComplianceTimeframe"))));
                return e;
            
            case SecondaryPostHearingComplianceTimeframe:
             
                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("advToAwaitingHearingDate"))));
                return e;
         
            default: 
                // this is a holding default event to allow for debugging without other issues
                e.setCategory(ei.getEventCategory(Integer.parseInt(getResourceBundle(
                Constants.EVENT_CATEGORY_BUNDLE).getString("casePhaseManualOverride"))));
                return e;
                
                //throw new CaseLifecyleException("Cannot determine next action in case protocol");
            
         } // close switch
    } // close method
} // close class
