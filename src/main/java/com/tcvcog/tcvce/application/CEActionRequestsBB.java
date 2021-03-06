/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tcvcog.tcvce.application;

import com.tcvcog.tcvce.coordinators.CaseCoordinator;
import com.tcvcog.tcvce.domain.CaseLifecyleException;
import com.tcvcog.tcvce.domain.IntegrationException;
import com.tcvcog.tcvce.entities.CEActionRequest;
import com.tcvcog.tcvce.entities.CEActionRequestStatus;
import com.tcvcog.tcvce.entities.CECase;
import com.tcvcog.tcvce.entities.Municipality;
import com.tcvcog.tcvce.entities.Property;
import com.tcvcog.tcvce.entities.search.SearchParamsCEActionRequests;
import com.tcvcog.tcvce.integration.CEActionRequestIntegrator;
import com.tcvcog.tcvce.integration.CaseIntegrator;
import com.tcvcog.tcvce.integration.EventIntegrator;
import com.tcvcog.tcvce.integration.PropertyIntegrator;
import com.tcvcog.tcvce.util.Constants;
import com.tcvcog.tcvce.util.MessageBuilderParams;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

/**
 * Primary backing bean for managing all code enforcement action requests.
 * Contains four methods, each of which routes a selected request down 
 * a workflow pathway and adjusts the request object's status accordingly.
 * 
 * Also contains utility methods for manipulating and editing requests.
 * 
 * @author Ellen Baskem
 */
public class CEActionRequestsBB extends BackingBeanUtils implements Serializable {
    
    private CEActionRequest selectedRequest;
    private List<CEActionRequest> requestList;
    private int requestListSize;
    
    private List<CEActionRequestStatus> statusList;
    private CEActionRequestStatus selectedStatus;
    private SearchParamsCEActionRequests searchParams;
    
    private CEActionRequestStatus selectedChangeToStatus;
    private String invalidMessage;
    private String noViolationFoundMessage;

    private String internalMessageText;
    private String muniMessageText;
    private String publicMessageText;
    
    private ArrayList<CECase> caseListForSelectedProperty;
    private String houseNumSearch;
    private String streetNameSearch;
    
    private CECase selectedCaseForAttachment;
    
    private Municipality muniForPropSwitchSearch;
    private Property propertyForPropSwitch;
    private List<Property> propertyList;
    
    private int ceCaseIDForConnection;
    private boolean disablePACCControl;
    private boolean disabledDueToRoutingNotAllowed;
    
    
    public String path1CreateNewCaseAtProperty(){
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();

        if(selectedRequest != null){
            if(selectedRequest.getRequestProperty() != null){
                getSessionBean().setActiveProp(selectedRequest.getRequestProperty());
            }
            
            MessageBuilderParams mbp = new MessageBuilderParams();
            mbp.user = getFacesUser();
            mbp.existingContent = selectedRequest.getPublicExternalNotes();
            mbp.header = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("attachedToCaseHeader");
            mbp.explanation = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("attachedToCaseExplanation");
            mbp.newMessageContent = "";
            
            selectedRequest.setPublicExternalNotes(appendNoteBlock(mbp));
            
            // force the bean to go to the integrator and fetch a fresh, updated
            // list of action requests
            try {
                ceari.updateActionRequestNotes(selectedRequest);
            } catch (IntegrationException ex) {
                getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR
                            ,"Unable to update action request with case attachment notes" , ""));
            }
        } else {
            getFacesContext().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR
                        ,"Please select an action request from the table to open a new case" , ""));
            return "";
        }
        
        updateSelectedRequestStatusWithBundleKey("actionRequestNewCaseStatusCode");
        
// This shelf will be checked by the case creation coordinator
        // and link the request to the new case so we don't lose track of it
        getSessionBean().setCeactionRequestForNewCaseAttachment(selectedRequest);
        
        return "addNewCase";
    }
    
    public void path2UseSelectedCaseForAttachment(CECase c){
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        selectedCaseForAttachment = c;
        try {
            ceari.connectActionRequestToCECase(selectedRequest.getRequestID(), selectedCaseForAttachment.getCaseID(), getFacesUser().getUserID() );
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Successfully connected action request ID " + selectedRequest.getRequestID() 
                                + " to code enforcement case ID " + selectedCaseForAttachment.getCaseID(), ""));
        } catch (CaseLifecyleException | IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Unable to connect request to case.", 
                        getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
        selectedRequest.setCaseID(selectedCaseForAttachment.getCaseID());
        updateSelectedRequestStatusWithBundleKey("actionRequestExistingCaseStatusCode");
        // force a reload of request list
        requestList = null;
    }
    
    public void path3AttachInvalidMessage(ActionEvent ev){
        if(selectedRequest != null){ 
            CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
            updateSelectedRequestStatusWithBundleKey("actionRequestInvalidStatusCode");

            // build message to document change
            MessageBuilderParams mcc = new MessageBuilderParams();
            mcc.user = getFacesUser();
            mcc.existingContent = selectedRequest.getPublicExternalNotes();
            mcc.header = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("invalidActionRequestHeader");
            mcc.explanation = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("invalidActionRequestExplanation");
            mcc.newMessageContent = invalidMessage;
            selectedRequest.setPublicExternalNotes(appendNoteBlock(mcc));
            try {
                ceari.updateActionRequestNotes(selectedRequest);
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                                "Public case note added to action request ID " + selectedRequest.getRequestID() + ".",""));
            } catch (IntegrationException ex) {
                System.out.println(ex);
                getFacesContext().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                                "Unable to write message to The Database", 
                                "This is a system level error that must be corrected by a sys admin--sorries!."));
            }
        } else {
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                            "You just tried to attach a message to a nonexistent request!", 
                            "Choose the request to manage on the left, then click manage"));
        }
        requestList = null;
    }
    
    
    public void path4AttachNoViolationFoundMessage(ActionEvent ev){
        if(selectedRequest != null){

            CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
            
            updateSelectedRequestStatusWithBundleKey("actionRequestNoViolationStatusCode");
            
            // build message to document change
            MessageBuilderParams mbp = new MessageBuilderParams();
            mbp.user = getFacesUser();
            mbp.existingContent = selectedRequest.getPublicExternalNotes();
            mbp.header = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("noViolationFoundHeader");
            mbp.explanation = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("noViolationFoundExplanation");
            mbp.newMessageContent = noViolationFoundMessage;
            
            selectedRequest.setPublicExternalNotes(appendNoteBlock(mbp));
            
            // force the bean to go to the integrator and fetch a fresh, updated
            // list of action requests
            requestList = null;
            try {
                ceari.updateActionRequestNotes(selectedRequest);
                getFacesContext().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, 
                                "Public case note added to action request ID " + selectedRequest.getRequestID() + ".",""));

            } catch (IntegrationException ex) {
                System.out.println(ex);
                getFacesContext().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                                "Unable to write message to The Database", 
                                "This is a system level error that must be corrected by a sys admin--sorries!."));
            }
        } else {
            getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                            "No request selected!", 
                            "Choose the request to manage on the left, then click manage"));
        }
    }
    
    private void updateSelectedRequestStatusWithBundleKey(String newStatusKey){
            CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
            
            try {
                selectedRequest.setRequestStatus(ceari.getRequestStatus(Integer.parseInt(
                        getResourceBundle(Constants.DB_FIXED_VALUE_BUNDLE)
                        .getString(newStatusKey))));
                ceari.updateActionRequestStatus(selectedRequest);
                getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                                "Status changed on request ID " + selectedRequest.getRequestID(),""));
            } catch (IntegrationException ex) {
                getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Unable to change request status", 
                        "This is a system level error that must be corrected by a sys admin--sorries!."));
            }
    }
    
    
    public void updateRequestList(ActionEvent ev){
        requestList = null;
        System.out.println("ActionRequestManagebb.updateRequestList");
    }
    
    public void searchForProperties(ActionEvent ev){
        
        PropertyIntegrator pi = getPropertyIntegrator();
        try {
            propertyList = pi.searchForProperties(houseNumSearch, streetNameSearch, muniForPropSwitchSearch.getMuniCode());
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Your search completed with " + propertyList.size() + " results", ""));
        } catch (IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR
                    , "Unable to complete a property search! Sorry!"
                    , getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
        
    }
    
    public void updateRequestProperty(ActionEvent ev){
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        
        Property formerProp = selectedRequest.getRequestProperty();
        selectedRequest.setRequestProperty(propertyForPropSwitch);
        
        try {
            ceari.updateActionRequestProperty(selectedRequest);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Done! Property udpate for request ID " + selectedRequest.getRequestID(), ""));
        } catch (IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR
                    , "Unable to change request property, sorry!"
                    , getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
        
        StringBuilder sb = new StringBuilder();
        if( formerProp != null){
            sb.append("Previous address: ");
            sb.append(formerProp.getAddress());
            sb.append(" (");
            sb.append(formerProp.getMuni().getMuniName());
            sb.append(")");
            sb.append("New address: ");
            sb.append(selectedRequest.getRequestProperty().getAddress());
            sb.append(" (");
            sb.append(selectedRequest.getRequestProperty().getMuni().getMuniName());
            sb.append(")");
        } else {
            sb.append(getResourceBundle(Constants.MESSAGE_BUNDLE).getString("noPreviousAddress"));
        }
        
        MessageBuilderParams mbp = new MessageBuilderParams();
        mbp.user = getFacesUser();
        mbp.existingContent = selectedRequest.getPublicExternalNotes();
        mbp.header = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("propertyChangedHeader");
        mbp.explanation = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("propertyChangedExplanation");
        mbp.newMessageContent = sb.toString();
        
        selectedRequest.setPublicExternalNotes(appendNoteBlock(mbp));
        try {
            ceari.updateActionRequestNotes(selectedRequest);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Automatic case note generated for property update", ""));
        } catch (IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR
                    , "Unable to add property change note to public listing, sorry!"
                    , getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
        // force table reload to show changes
        requestList = null;
    }
    
    public void changePACCAccess(){
        System.out.println("CEActionRequestsBB.changePACCAccess");
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        
        try {
            ceari.updatePACCAccess(selectedRequest);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Done! Public access status is now: " + String.valueOf(selectedRequest.isPaccEnabled())
                    + " for action request ID: " + selectedRequest.getRequestID(), ""));
        } catch (IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR
                    , "Unable to add change public access code status"
                    , getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
    }
    
    
    public void attachInternalMessage(ActionEvent ev){
        
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        MessageBuilderParams mbp = new MessageBuilderParams();
        mbp.user = getFacesUser();
        mbp.existingContent = selectedRequest.getCogInternalNotes();
        mbp.header = getResourceBundle(Constants.EVENT_CATEGORY_BUNDLE).getString("internalNote");
        mbp.explanation = "";
        mbp.newMessageContent = internalMessageText;
        String newNotes = appendNoteBlock(mbp);
        System.out.println("CEActionRequestsBB.attachInternalMessage | msg before adding to request: " + newNotes);
        selectedRequest.setCogInternalNotes(newNotes);
        try {
            ceari.updateActionRequestNotes(selectedRequest);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO
                    , "Done: added internal note to request ID " + selectedRequest.getRequestID() ,"" ));
        } catch (IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR
                    , "Unable to update notes, sorry!"
                    , getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
        
    }
    
    public void attachMuniMessage(ActionEvent ev){
        
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        MessageBuilderParams mbp = new MessageBuilderParams();
        mbp.user = getFacesUser();
        mbp.existingContent = selectedRequest.getMuniNotes();
        mbp.header = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("muniNote");
        mbp.explanation = "";
        mbp.newMessageContent = muniMessageText;
        
        selectedRequest.setMuniNotes(appendNoteBlock(mbp));
        try {
            ceari.updateActionRequestNotes(selectedRequest);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Done: Added a municipal-only notes to request ID " + selectedRequest.getRequestID(), ""));
        } catch (IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR
                    , "Unable to update notes, sorry!"
                    , getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
        
    }
    
    public void attachPublicMessage(ActionEvent ev){
        
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        MessageBuilderParams mbp = new MessageBuilderParams();
        mbp.user = getFacesUser();
        mbp.existingContent = selectedRequest.getPublicExternalNotes();
        mbp.header = getResourceBundle(Constants.MESSAGE_BUNDLE).getString("externalNote");
        mbp.explanation = "";
        mbp.newMessageContent = publicMessageText;
        
        selectedRequest.setPublicExternalNotes(appendNoteBlock(mbp));
        try {
            ceari.updateActionRequestNotes(selectedRequest);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Done: Added a public note to request ID " + selectedRequest.getRequestID(), ""));
        } catch (IntegrationException ex) {
            System.out.println(ex);
            getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR
                    , "Unable to update notes, sorry!"
                    , getResourceBundle(Constants.MESSAGE_BUNDLE).getString("systemLevelError")));
        }
        
    }
    
    public void manageActionRequest(CEActionRequest req){
        System.out.println("ActionRequestManagebb.manageActionRequest req: " + req.getRequestID());
        selectedRequest = req;
        
    }
    

    /**
     * @return the ceCaseIDForConnection
     */
    public int getCeCaseIDForConnection() {
        return ceCaseIDForConnection;
    }

    /**
     * @param ceCaseIDForConnection the ceCaseIDForConnection to set
     */
    public void setCeCaseIDForConnection(int ceCaseIDForConnection) {
        this.ceCaseIDForConnection = ceCaseIDForConnection;
    }

    /**
     * @return the requestList
     */
    public List<CEActionRequest> getRequestList() {
        System.out.println("ActionRequestManageBB.getRequestList");
        
        CEActionRequestIntegrator ari = getcEActionRequestIntegrator();
        SearchParamsCEActionRequests spcear = getSearchParams();
        if(requestList == null || requestList.isEmpty()){
            System.out.println("CeActionRequestsBB.getUnlinkedRequestList | unlinkedrequests is null");
            try {
                requestList = ari.getCEActionRequestList(spcear);
            } catch (IntegrationException ex) {
                System.out.println(ex);
                getFacesContext().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                                "Unable to load action requests due to an error in the Integration Module", ""));
            }
        }
        return requestList;
    }

    /**
     * @param requestList the requestList to set
     */
    public void setRequestList(List<CEActionRequest> requestList) {
        this.requestList = requestList;
    }


    /**
     * Creates a new instance of ActionRequestManageBB
     */
    public CEActionRequestsBB() {
    
    }
    
    public void updateActionRequestStatus(ActionEvent ev){
        System.out.println("updateStatus");
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        if(selectedChangeToStatus != null){
            
            selectedRequest.setRequestStatus(selectedChangeToStatus);
            try {
                ceari.updateActionRequestStatus(selectedRequest);
                getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                            "Successfully changed request status for request ID: " + selectedRequest.getCaseID(), ""));
            } catch (IntegrationException ex) {
                getFacesContext().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, 
                            "Unable to update request ID : " + selectedRequest.getCaseID(), ""));

            } 
        } else {
            getFacesContext().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR
                        ,"Please select a request status from the drop-down box to proceed" , ""));
            
        }
        // force a reload of request list
        requestList = null;
    
    }
    
    

    /**
     * @return the selectedRequest
     */
    public CEActionRequest getSelectedRequest() {

        return selectedRequest;
    }

    /**
     * @param selectedRequest the selectedRequest to set
     */
    public void setSelectedRequest(CEActionRequest selectedRequest) {
        this.selectedRequest = selectedRequest;
    }

    /**
     * @return the statusList
     */
    public List<CEActionRequestStatus> getStatusList() {
        CEActionRequestIntegrator ceari = getcEActionRequestIntegrator();
        if(statusList == null){
            try {
                statusList = ceari.getRequestStatusList();
            } catch (IntegrationException ex) {
                System.out.println(ex);
            }
        }
        return statusList;
    }

    /**
     * @return the selectedStatus
     */
    public CEActionRequestStatus getSelectedStatus() {
        return selectedStatus;
    }

    /**
     * @param statusList the statusList to set
     */
    public void setStatusList(List<CEActionRequestStatus> statusList) {
        this.statusList = statusList;
    }

    /**
     * @param selectedStatus the selectedStatus to set
     */
    public void setSelectedStatus(CEActionRequestStatus selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    /**
     * @return the searchParams
     */
    public SearchParamsCEActionRequests getSearchParams() {
        System.out.println("ActionRequestManageBB.getSearchparams");
        if(searchParams == null){
            System.out.println("ActionRequestManageBB.getSearchparams | params is null");
            SearchCoordinator sc = getSearchCoordinator();
            searchParams = sc.getDefaultSearchParamsCEActionRequests();
        }
        return searchParams;
    }

    /**
     * @param searchParams the searchParams to set
     */
    public void setSearchParams(SearchParamsCEActionRequests searchParams) {
        this.searchParams = searchParams;
    }

    /**
     * @return the selectedChangeToStatus
     */
    public CEActionRequestStatus getSelectedChangeToStatus() {
        return selectedChangeToStatus;
    }

    /**
     * @param selectedChangeToStatus the selectedChangeToStatus to set
     */
    public void setSelectedChangeToStatus(CEActionRequestStatus selectedChangeToStatus) {
        this.selectedChangeToStatus = selectedChangeToStatus;
    }

    /**
     * @return the requestListSize
     */
    public int getRequestListSize() {
        requestList = null;
        int ls = 0;
        if(!(getRequestList() == null)){
         ls = getRequestList().size();
        } 
        requestListSize = ls;
        return requestListSize;
    }

    /**
     * @param requestListSize the requestListSize to set
     */
    public void setRequestListSize(int requestListSize) {
        this.requestListSize = requestListSize;
    }

    /**
     * @return the invalidMessage
     */
    public String getInvalidMessage() {
        return invalidMessage;
    }

    /**
     * @param invalidMessage the invalidMessage to set
     */
    public void setInvalidMessage(String invalidMessage) {
        this.invalidMessage = invalidMessage;
    }

    /**
     * @return the caseListForSelectedProperty
     */
    public ArrayList<CECase> getCaseListForSelectedProperty() {
        CaseIntegrator ci = getCaseIntegrator();
        if(selectedRequest != null){
            try {
                caseListForSelectedProperty = ci.getCECasesByProp(selectedRequest.getRequestProperty());
                System.out.println("CEActionRequestsBB.getCaseListForSelectedProperty | case list size: " + caseListForSelectedProperty.size());
            } catch (IntegrationException ex) {
                System.out.println(ex);
            }
        }
        return caseListForSelectedProperty;
    }

    /**
     * @param caseListForSelectedProperty the caseListForSelectedProperty to set
     */
    public void setCaseListForSelectedProperty(ArrayList<CECase> caseListForSelectedProperty) {
        this.caseListForSelectedProperty = caseListForSelectedProperty;
    }

    /**
     * @return the selectedCaseForAttachment
     */
    public CECase getSelectedCaseForAttachment() {
        return selectedCaseForAttachment;
    }

    /**
     * @param selectedCaseForAttachment the selectedCaseForAttachment to set
     */
    public void setSelectedCaseForAttachment(CECase selectedCaseForAttachment) {
        this.selectedCaseForAttachment = selectedCaseForAttachment;
    }

    /**
     * @return the noViolationFoundMessage
     */
    public String getNoViolationFoundMessage() {
        return noViolationFoundMessage;
    }

    /**
     * @param noViolationFoundMessage the noViolationFoundMessage to set
     */
    public void setNoViolationFoundMessage(String noViolationFoundMessage) {
        this.noViolationFoundMessage = noViolationFoundMessage;
    }

    /**
     * @return the disabledDueToRoutingNotAllowed
     */
    
    public boolean getIsDisabledDueToRoutingNotAllowed() {
        CaseCoordinator cc = getCaseCoordinator();
        
        disabledDueToRoutingNotAllowed = 
                !(cc.determineCEActionRequestRoutingActionEnabledStatus(
                        selectedRequest,
                        getSessionBean().getAccessKeyCard()));
        System.out.println("CEACtionRequestsBB.isRoutingAllowedOnSelectedRequest | Status: " 
                + disabledDueToRoutingNotAllowed);
        return disabledDueToRoutingNotAllowed;
    }

    /**
     * @param disabledDueToRoutingNotAllowed the disabledDueToRoutingNotAllowed to set
     */
    public void setDisabledDueToRoutingNotAllowed(boolean disabledDueToRoutingNotAllowed) {
        this.disabledDueToRoutingNotAllowed = disabledDueToRoutingNotAllowed;
    }

    /**
     * @return the internalMessageText
     */
    public String getInternalMessageText() {
        return internalMessageText;
    }

    /**
     * @return the muniMessageText
     */
    public String getMuniMessageText() {
        return muniMessageText;
    }

    /**
     * @return the publicMessageText
     */
    public String getPublicMessageText() {
        return publicMessageText;
    }

    /**
     * @param internalMessageText the internalMessageText to set
     */
    public void setInternalMessageText(String internalMessageText) {
        this.internalMessageText = internalMessageText;
    }

    /**
     * @param muniMessageText the muniMessageText to set
     */
    public void setMuniMessageText(String muniMessageText) {
        this.muniMessageText = muniMessageText;
    }

    /**
     * @param publicMessageText the publicMessageText to set
     */
    public void setPublicMessageText(String publicMessageText) {
        this.publicMessageText = publicMessageText;
    }

    /**
     * @return the muniForPropSwitchSearch
     */
    public Municipality getMuniForPropSwitchSearch() {
        return muniForPropSwitchSearch;
    }

    /**
     * @return the propertyForPropSwitch
     */
    public Property getPropertyForPropSwitch() {
        return propertyForPropSwitch;
    }

    /**
     * @param muniForPropSwitchSearch the muniForPropSwitchSearch to set
     */
    public void setMuniForPropSwitchSearch(Municipality muniForPropSwitchSearch) {
        this.muniForPropSwitchSearch = muniForPropSwitchSearch;
    }

    /**
     * @param propertyForPropSwitch the propertyForPropSwitch to set
     */
    public void setPropertyForPropSwitch(Property propertyForPropSwitch) {
        this.propertyForPropSwitch = propertyForPropSwitch;
    }

    /**
     * @return the propertyList
     */
    public List<Property> getPropertyList() {
        return propertyList;
    }

    /**
     * @param propertyList the propertyList to set
     */
    public void setPropertyList(List<Property> propertyList) {
        this.propertyList = propertyList;
    }

    /**
     * @return the houseNumSearch
     */
    public String getHouseNumSearch() {
        return houseNumSearch;
    }

    /**
     * @return the streetNameSearch
     */
    public String getStreetNameSearch() {
        return streetNameSearch;
    }

    /**
     * @param houseNumSearch the houseNumSearch to set
     */
    public void setHouseNumSearch(String houseNumSearch) {
        this.houseNumSearch = houseNumSearch;
    }

    /**
     * @param streetNameSearch the streetNameSearch to set
     */
    public void setStreetNameSearch(String streetNameSearch) {
        this.streetNameSearch = streetNameSearch;
    }

    /**
     * @return the disablePACCControl
     */
    public boolean isDisablePACCControl() {
        disablePACCControl = false;
        if(getSessionBean().getAccessKeyCard().isHasMuniStaffPermissions() == false){
            disablePACCControl = true;
        }
        return disablePACCControl;
    }

    /**
     * @param disablePACCControl the disablePACCControl to set
     */
    public void setDisablePACCControl(boolean disablePACCControl) {
        this.disablePACCControl = disablePACCControl;
    }

   
    
}
