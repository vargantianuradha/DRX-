package com.ys.drx.ui.web.refill.actions;

import static com.ys.drx.Constants.ACTIVE_MED_FOR_NEWRX_FLAG;
import static com.ys.drx.Constants.CONFIGURATION_BEAN_MAP_OBJECT_SUBSTITUTIONS;
import static com.ys.drx.Constants.DOCTOR_ADDRESS_SELECTED_FLAG;
import static com.ys.drx.Constants.MEDICATION_ENDED;
import static com.ys.drx.Constants.NO_OF_DAYS_FOR_SEARCH;
import static com.ys.drx.Constants.REFILLRX_ENTRY;
import static com.ys.drx.Constants.REFILL_MODE;
import static com.ys.drx.Constants.REQUESTED_BY_DOCTOR_FOR_PATIENT;
import static com.ys.drx.Constants.REQUESTED_PAGE;
import static com.ys.drx.Constants.RX_APPROVED_BY_DOCTOR;
import static com.ys.drx.Constants.RX_DAYS_SEARCH_TYPE;
import static com.ys.drx.Constants.RX_DENIED_NEW_RX;
import static com.ys.drx.Constants.SEARCH_HANDLE;
import static com.ys.drx.surescripts.common.SIGConstants.FAX;
import static com.ys.drx.surescripts.common.SIGConstants.STATUS_RES;
import static com.ys.drx.surescripts.common.SIGConstants.WORKPHONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import com.ys.drx.Auditor;
import com.ys.drx.Constants;
import com.ys.drx.business.IdentificationLocal;
import com.ys.drx.business.PersonLocal;
import com.ys.drx.business.facade.ApplicationContextLocal;
import com.ys.drx.business.facade.IndividualLocal;
import com.ys.drx.business.facade.NewDrugInformationBridgeLocal;
import com.ys.drx.business.facade.PersonFacadeLocal;
import com.ys.drx.business.facade.PrescriptionFacadeLocal;
import com.ys.drx.business.facade.SearchFacadeLocal;
import com.ys.drx.business.facade.ShieldLocal;
import com.ys.drx.business.facade.StatelessSearchFacadeLocal;
import com.ys.drx.business.facade.model.DigitalRxParameterLoader;
import com.ys.drx.business.facade.model.IndividualBean;
import com.ys.drx.business.model.PrimaryKeyGenerator;
import com.ys.drx.dao.DAOException;
import com.ys.drx.dao.rxpad.PrescriptionDAO;
import com.ys.drx.delegate.ConfigurationDelegate;
import com.ys.drx.dto.DoctorInformationDTO;
import com.ys.drx.dto.LabRecordsSearchResultsImpl;
import com.ys.drx.dto.OnBehalfRefillPhysicianResultsImpl;
import com.ys.drx.dto.PersonSpecimenSearchResultsImpl;
import com.ys.drx.dto.RefillsSearchCriteria;
import com.ys.drx.dto.RxAlertSearchResultsImpl;
import com.ys.drx.dto.RxhubMemberIdDTO;
import com.ys.drx.dto.UserLoginInfoDTO;
import com.ys.drx.dto.entity.IdentificationDTO;
import com.ys.drx.dto.entity.PersonAddressDTO;
import com.ys.drx.dto.entity.PersonAllergyDTO;
import com.ys.drx.dto.entity.PersonContactDTO;
import com.ys.drx.dto.entity.PersonDTO;
import com.ys.drx.dto.entity.PersonDrugDTO;
import com.ys.drx.dto.entity.PersonPrescriptionDTO;
import com.ys.drx.dto.entity.PersonRefillDTO;
import com.ys.drx.dto.entity.PersonSubscriptionLevelDTO;
import com.ys.drx.dto.entity.PharmacyDTO;
import com.ys.drx.dto.entity.PrescriptionRefillDTO;
import com.ys.drx.dto.entity.RxAlertDTO;
import com.ys.drx.dto.entity.UserDTO;
import com.ys.drx.surescripts.common.SIGStatusConstants;
import com.ys.drx.surescripts.sig.common.to.PrescriptionTO;
import com.ys.drx.surescripts.sig.common.to.RefillRequestTO;
import com.ys.drx.surescripts.sig.validator.SIGValidator;
import com.ys.drx.ui.web.AbstractAction;
import com.ys.drx.ui.web.AbstractActionForm;
import com.ys.drx.ui.web.AjaxHelper;
import com.ys.drx.ui.web.DoctorInfoForm;
import com.ys.drx.ui.web.PatientInfoForm;
import com.ys.drx.ui.web.eligibility.actions.EligibilityResponse;
import com.ys.drx.ui.web.eligibility.forms.EligibilityResponseForm;
import com.ys.drx.ui.web.profile.forms.AllergiesInfoObject;
import com.ys.drx.ui.web.refill.actions.utils.RefillUtils;
import com.ys.drx.ui.web.refill.forms.RefillAuthorizeSearchForm;
import com.ys.hipaa.elig.to.EligBenInqRespTO;
import com.ys.hipaa.elig.to.EligResponseTO;

@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//@RunWith(MockitoJUnitRunner.class)
class RefillAuthorizeSearchActionTest {
	 
	 


	@InjectMocks
	private RefillAuthorizeSearchAction i_refillAuthorizeSearchAction;
	
//	@InjectMocks
//	private DoctorInfoForm m_doctorInfoForm;
	
	@Mock
	private RefillAuthorizeSearchForm m_form;
	
	@Mock
	private RxhubMemberIdDTO m_rxhubMemberIdDTO;
	
	@InjectMocks
	private DoctorInformationDTO m_doctorInformationDTO;
	
	@Mock
	private ShieldLocal m_shieldLocal;
	
	@Mock
	private PrescriptionFacadeLocal m_prescriptionFacadeLocal;
	
	@Mock
	private NewDrugInformationBridgeLocal m_newDrugInformationBridgeLocal;
	
	@Mock
	private PersonLocal m_personLocal;
	
	@Mock
	private StatelessSearchFacadeLocal m_statelessSearchFacadeLocal;
	
	@Mock
	private IndividualLocal m_individualLocal;
	
	@Mock
	private Constants m_Constants;
	
	@Mock
	private RefillUtils m_refillUtils;
	
	@Mock
	private HttpServletRequest m_request;
	
	@Mock
	private HttpServletResponse m_response;
	
	@Mock
	 private HttpSession m_session;
	
	@Mock
	private UserLoginInfoDTO m_userLoginInfoDTO;
	
	@Mock
	private PersonRefillDTO m_personRefillDTO;
	
	@Mock
	private IdentificationDTO m_identificationDTO;
	
	@Mock
	private RefillsSearchCriteria m_refillsSearchCriteria;
	
	@Mock
	private ConfigurationDelegate m_configurationDelegate;
	
	@Mock
	private SearchFacadeLocal m_searchFacadeLocal;
	
	@Mock
	private  PrescriptionDAO m_prescriptionDAOImpl;
	
	@Mock
	private DoctorInfoForm m_doctorInfoForm;
	
	@Mock
	private Auditor m_auditor;
	
	@Mock
	private PersonPrescriptionDTO m_personPrescriptionDTO;
	
	@Mock
	private PersonFacadeLocal m_personFacadeLocal;
	
	@Mock
	private AbstractAction m_abstractAction;
	
	@Mock
	private PrescriptionRefillDTO m_prescriptionRefillDTO;
	
	@Mock
	private AbstractActionForm m_abstractActionForm;
	
	@Mock
	private PersonSubscriptionLevelDTO m_personSubscriptionLevelDTO;
	
	@Mock
	private PersonDrugDTO m_personDrugDTO;
	
	@Mock
	private PatientInfoForm m_patientInfoForm;
	
	@Mock
	private PharmacyDTO m_pharmacyDTO;
	
	@Mock
	private PersonAllergyDTO m_PersonAllergyDTO;
	
	private ModelAndView m_model = new ModelAndView();
	private AjaxHelper m_ajaxHelper = new AjaxHelper();
  
	@Mock
	private BindingResult m_errors;
	
	@Mock
	private PersonDTO m_personDTO;
	
	@Mock
	private OnBehalfRefillPhysicianResultsImpl m_onBehalfRefillPhysicianResultsImpl;
    
	@Mock
	private IdentificationLocal m_identificationLocal;
	
	@Mock
	private DigitalRxParameterLoader m_digitalRxParameterLoader;
	
	@Mock
	private SIGValidator m_sigValidator;
	
	@Mock
	private RxAlertDTO m_rxAlertDTO;
	
	@Mock
	private EligResponseTO m_eligResponseTO;
	
	@Mock
	private ApplicationContextLocal m_applicationContextLocal;
	
	@Mock
	private AllergiesInfoObject m_allergiesInfoObject;
	
	//@Mock
//	protected SearchFacadeLocal searchFacdeLocal;
	
	@BeforeEach
    public void setup() 
    {
        // this must be called for the @Mock annotations above to be processed.
    	MockitoAnnotations.initMocks( this ); 
    } 
   
	@Test           
	void testInitialize() throws Exception {      
		when(m_request.getSession()).thenReturn(m_session);
	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
	    when(m_session.getAttribute("doctorInfoForm")).thenReturn(m_doctorInfoForm);
	    m_doctorInfoForm.setAddressSelected("");
	    when(m_session.getAttribute("prescriptionRefillReportUrl")).thenReturn(m_session);
	    m_session.removeAttribute("prescriptionRefillReportUrl");
	    // checking null
	    //when(m_session.getAttribute("prescriptionRefillReportUrl")).thenReturn(null);
	         
	    m_searchFacadeLocal.clearCache();         
		m_searchFacadeLocal.clearSearchResult(); 
		m_userLoginInfoDTO.setId("23");
		when(m_userLoginInfoDTO.getId()).thenReturn("23");
		List<OnBehalfRefillPhysicianResultsImpl> doctorsList = m_shieldLocal.getDoctorsListForOnBehalfRefills(m_userLoginInfoDTO.getId());
		
		m_userLoginInfoDTO.setPendingRx(m_shieldLocal.getPendingRxCount(m_userLoginInfoDTO.getId()));
		m_userLoginInfoDTO.setRxAlerts(m_shieldLocal.getRxAlertsPending(m_userLoginInfoDTO.getId()).size());
		m_userLoginInfoDTO.setPendingRefillalert(m_shieldLocal.getPendingRefillAlertCount(m_userLoginInfoDTO.getId()));
		List<PersonRefillDTO> OnBehalfPendingRefills = m_shieldLocal.retrieveOnBehalfPendingRefills(doctorsList);
		//m_userLoginInfoDTO.setOnBehalfPendingRx((OnBehalfPendingRefills != null) ? OnBehalfPendingRefills.size() : 0);
		m_session.setAttribute("onBehalfRefills", OnBehalfPendingRefills);
		 
		List<RxAlertSearchResultsImpl> onBehalfPendingRefillAlertList = new ArrayList<RxAlertSearchResultsImpl>();	
		onBehalfPendingRefillAlertList = m_statelessSearchFacadeLocal.getOnBehalfPendingRefillAlerts(m_userLoginInfoDTO.getId());	
		m_userLoginInfoDTO.setOnBehalfPendingRefillAlert((onBehalfPendingRefillAlertList != null) ? onBehalfPendingRefillAlertList.size() : 0);	
		
		m_userLoginInfoDTO.setOnBehalfRxAlerts(m_shieldLocal.getOnBehalfRxAlertsPending(m_userLoginInfoDTO.getId()).size());
		m_session.removeAttribute(ACTIVE_MED_FOR_NEWRX_FLAG); 
		m_session.setAttribute("statelessSearchFacadeLocal", m_statelessSearchFacadeLocal); 
		m_session.setAttribute("IndividualLocal", m_individualLocal);
		
		when(m_userLoginInfoDTO.getPatientId()).thenReturn("123");
		m_userLoginInfoDTO.setPatientId("123");
	    
	    ModelAndView initialize = i_refillAuthorizeSearchAction.initialize(m_request, m_response, m_form, m_errors);
	    assertNotEquals(initialize,m_model);  
	} 

	@Test  
	void testPharmacyUpdateSS() throws Exception {
		
		when(m_request.getSession()).thenReturn(m_session);
	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
		Map<String, PersonPrescriptionDTO> unprocessed = new TreeMap<String, PersonPrescriptionDTO>();
		 Map<String, PersonPrescriptionDTO> ssMap = new HashMap<>();
		
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype");
		 ssMap.put("One", m_personPrescriptionDTO);
		 ssMap.put("two", m_personPrescriptionDTO);
		String type = null;
		PersonPrescriptionDTO personPrescriptionDTO = null; 
		String rxReferenceNumber = "";
		String relatesId = ""; 
		PrescriptionTO prescriptionTO = null;
		String refillRenewalXMLString = null;
		String xmlResponse = null;
		String rxMessageNumber = null; 
		String doctorId = "124";
		String onBehalfId = null;
		String prescriberAgentFirstName = null;
		String prescriberAgentLastName = null;
		String supervisorSegmentId = "";
		PersonDTO supervisorDTO = null;
	
		@SuppressWarnings("unused")
		PersonPrescriptionDTO personPrescriptionDTO1 = new PersonPrescriptionDTO();
		personPrescriptionDTO1.setNotes("notes");
		//when(m_userLoginInfoDTO.getId()).thenReturn("789");
	//	when
		//personPrescriptionDTO1.setDoctorId("124");
		when(m_userLoginInfoDTO.getSupervisorSegmentId()).thenReturn(supervisorSegmentId);
		String doctorFirstName ="firstname";
				when(m_individualLocal.getDoctorProfile(null)).thenReturn(m_personDTO);
				when(m_personDTO.getFirstName()).thenReturn(doctorFirstName);
		      String doctorLastName = "lastname";
				when(m_individualLocal.getDoctorProfile(null)).thenReturn(m_personDTO);
				when(m_personDTO.getLastName()).thenReturn(doctorLastName);
				//when(personPrescriptionDTO1.getNotes() ).thenReturn("notes");
				 
		
		i_refillAuthorizeSearchAction.pharmacyUpdateSS(m_form, m_request, m_response, ssMap, m_personRefillDTO, m_errors);
	}
	
	// 70% done pending due to protected methods 
	
	@Test  
	void testAlterrx() throws Exception { 
		boolean validationErrors=false; 
		when(m_request.getSession()).thenReturn(m_session); 
		when(m_session.getAttribute("doctorInfoForm")).thenReturn(m_doctorInfoForm);
		m_doctorInfoForm.setAddressSelected(DOCTOR_ADDRESS_SELECTED_FLAG);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 

		 m_userLoginInfoDTO.setId("124");
		 List<OnBehalfRefillPhysicianResultsImpl> doctorsListForOnBehalfRefills = new  ArrayList<OnBehalfRefillPhysicianResultsImpl> ();
		 m_onBehalfRefillPhysicianResultsImpl .setDoctorId("66666");
		 when (m_shieldLocal.getDoctorsListForOnBehalfRefills(m_userLoginInfoDTO.getId())).thenReturn(doctorsListForOnBehalfRefills);

		 doctorsListForOnBehalfRefills.add(m_onBehalfRefillPhysicianResultsImpl);
		 List<PersonRefillDTO> behalfPendingRefills = new ArrayList<PersonRefillDTO>();
		when(m_shieldLocal.retrieveOnBehalfPendingRefills(doctorsListForOnBehalfRefills)).thenReturn(behalfPendingRefills);
		 PersonRefillDTO prd = new PersonRefillDTO();
		   
		prd.setDoctorId("1234");
		 prd.setDoctorName("jj");  
		 behalfPendingRefills.add(prd);
		 
		 @SuppressWarnings("unchecked")
		Map<String, PersonPrescriptionDTO> rxMap = (Map<String, PersonPrescriptionDTO>) m_searchFacadeLocal
					.getFromCache(REFILLRX_ENTRY);
	
		// when((m_userLoginInfoDTO.getPatientId() )).thenReturn("12345");
		// when(m_form.getPatientId()).thenReturn(""); 
		// String patientId = m_userLoginInfoDTO.getPatientId();	
		 when(m_form.getPatientId()).thenReturn(null); 
		    validationErrors = true;
			m_errors.reject("messages.rx.required");
			m_request.setAttribute("clicked", " "); 
			ModelAndView model= new ModelAndView("refill.refillauthorizesearch.page","refillAuthorizeSearchForm", m_form);
          List<PersonRefillDTO> list = new  ArrayList<PersonRefillDTO>();
        		 when (m_statelessSearchFacadeLocal.searchAllRefillsForDoctor(m_userLoginInfoDTO.getId())).thenReturn(list);
			list.addAll( behalfPendingRefills);
			
			when(m_userLoginInfoDTO.isControlledSubstances()).thenReturn(true);
			when(m_session.getAttribute("setOnBehalfId")).thenReturn(null);
			m_request.setAttribute("isUserCSEnabled", true);
			
			List<IdentificationDTO> doctorIdentificationList = new ArrayList<>();
			m_userLoginInfoDTO.setDoctorId("99999");
			when(m_identificationLocal.findByPersonId(m_userLoginInfoDTO.getDoctorId())).thenReturn(doctorIdentificationList);
			m_identificationDTO.setType("type");
			m_identificationDTO.setConfirmRxType("rxtype");
			m_identificationDTO.setIdentification("identification");
			doctorIdentificationList.add(m_identificationDTO);
			for (IdentificationDTO identificationDTO : doctorIdentificationList) {
				when(identificationDTO.getType()).thenReturn("DH");
				when(identificationDTO.getIdentification()).thenReturn("identification");
				String deaNumber = identificationDTO.getIdentification();
				   m_request.setAttribute("DoctorDEA",deaNumber);
				   
			}
			int count = 1;
			for (PersonRefillDTO refillDTO : list) {
				
				PersonPrescriptionDTO prescriptionDTO =null;
				refillDTO.setFollowUpCount("4r5t6");
				RefillRequestTO refillRequestTOn=null;
				 boolean poncount=true;
				  String messageId=null;
				  String orginalMessageId=null; 
				  String receivedTime=null;
				  orginalMessageId =refillDTO.getRxId();
				  boolean PONMatch=false;
			      when(m_sigValidator.validatePON(orginalMessageId,refillRequestTOn, messageId, receivedTime)).thenReturn(false);
				  PersonRefillDTO refilldto = new PersonRefillDTO();
				  refilldto.setConfirmRxType("RxType");
				  when(m_prescriptionFacadeLocal.getPersonRefillDtoByRelatedMessageId(orginalMessageId)).thenReturn(refilldto);
				  refilldto.setRxId("Rxid"); 
				  orginalMessageId = refillDTO.getRxId(); 
				  
//				  PersonRefillDTO refilldto = null;
//				  when(m_prescriptionFacadeLocal.getPersonRefillDtoByRelatedMessageId(orginalMessageId)).thenReturn(refilldto);
//				  when(m_prescriptionFacadeLocal.getRxaletDtoByRelatedMessageId(orginalMessageId)).thenReturn(m_rxAlertDTO);
//				  m_rxAlertDTO.setRelatedMessageId("relatedMessageID");
//				  orginalMessageId =m_rxAlertDTO.getRelatedMessageId();
				  
				 // when(m_sigValidator.validatePON(orginalMessageId,refillRequestTOn, messageId, receivedTime)).thenReturn(true);
				 when(m_statelessSearchFacadeLocal.getPrescriptionFromMessageId(orginalMessageId)).thenReturn(m_personPrescriptionDTO);
				// when(m_statelessSearchFacadeLocal.getPrescriptionFromMessageId(refillDTO.getRxId())).thenReturn(m_personPrescriptionDTO);
				  
			} 
			
			
			 i_refillAuthorizeSearchAction.alterrx(m_form, m_errors, m_request, m_response, m_model);
			
		  
	}  
	
	 // pending due to rxentry
	@Test
	void testResetrx() throws Exception { 
		m_form.setPrescriptionMode(REFILL_MODE);
		ModelAndView resetrx = i_refillAuthorizeSearchAction.resetrx(m_form, m_errors,m_request, m_response, m_model);
	    
		assertEquals(resetrx,m_model);
	
	}
//pending due to protected methods
	@Test 
	void testRxreset() throws Exception {
		TreeMap<Object, Object> m_rxmap = new TreeMap<>();
		when(m_request.getSession()).thenReturn(m_session);
		when(m_session.getAttribute(REFILLRX_ENTRY)).thenReturn(m_rxmap);
		m_request.setAttribute(REFILLRX_ENTRY, (Serializable) m_rxmap);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
        
	 	//m_abstractAction.setPatientInfoInSession(m_form.getPatientId(), m_request);
		//i_refillAuthorizeSearchAction.setPatientDoctorNames(m_request, m_form);
		i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form);
		when(m_userLoginInfoDTO.getDoctorId()).thenReturn("124");
	    when(m_individualLocal.getDoctorProfile("124")).thenReturn(m_personDTO);
	    when(m_personDTO.getFirstName()).thenReturn("hh");
	    m_form.setDoctorFirstName("jj"); 
	    
	    when(m_personDTO.getLastName()).thenReturn("hh"); 
	    m_form.setDoctorLastName("rii");
	   
	    when(m_userLoginInfoDTO.getPatientId()).thenReturn("124");
	    when(m_individualLocal.getPatientProfile("124")).thenReturn(m_personDTO);
	    when(m_personDTO.getFirstName()).thenReturn("hh"); 
	    m_form.setPatientFirstName("jj");
	    
	    when(m_personDTO.getLastName()).thenReturn("hh");
	    m_form.setPatientLastName("rii");
		i_refillAuthorizeSearchAction.setRxhubPBMUniqueId(m_form, m_request);
		m_form.setInitialized(true);
		m_request.setAttribute("alterrx", " ");  
		
		m_model.addObject("refillAuthorizeSearchForm",m_form);
		m_model.setViewName("refill.rxentry.page");
		ModelAndView rxreset = i_refillAuthorizeSearchAction.rxreset(m_form, m_errors, m_request, m_response, m_model);
	   assertEquals(rxreset,m_model);
	
	}

	
	// completed
 
	@Test
	void testValidateNewRefillForm() throws Exception {
		Map<String, PersonPrescriptionDTO> m_refillMap = new TreeMap<>();
	    //String key = "123";
		PersonPrescriptionDTO dto = new PersonPrescriptionDTO();
		dto.setStatus("APPROVED"); 
	    dto.setDisplayRefills(-1); 
		m_refillMap.put("3", dto);
		for (String key: m_refillMap.keySet()) { 
		   dto = m_refillMap.get(key);
		   dto.setStatus("APPROVED");
		   dto.setDisplayRefills(-1);
		   m_errors.reject("messages.refills.invalid");

	}
		i_refillAuthorizeSearchAction.validateNewRefillForm(m_request, m_refillMap, m_errors);
	}
	
	
	// pending due to protected method in AbstractAction class 
	 @Test
	void testGetRxList() throws Exception {
	//	m_personFacadeLocal = m_abstractAction.getPersonFacadeBean();
		 Map<String, PersonPrescriptionDTO> m_rxMap = new TreeMap<>();
		 List<PersonPrescriptionDTO> personPrescriptionList = new ArrayList<PersonPrescriptionDTO>();
		 for (Map.Entry<String, PersonPrescriptionDTO> prescriptionEntry : m_rxMap.entrySet()) {
			 PersonPrescriptionDTO prescriptionDTO = prescriptionEntry.getValue();
			 String pharmacyId = "12345";
			when( prescriptionDTO.getPharmacyId()).thenReturn(pharmacyId);
			prescriptionDTO.setPharmacyName(m_personFacadeLocal.getPharmacyNameByPharmacyCode(pharmacyId));
	 		personPrescriptionList.add(prescriptionDTO);
		
					 }
		//	List<PersonPrescriptionDTO> rxList = i_refillAuthorizeSearchAction.getRxList(m_rxMap);
			// assertNotNull(rxList);
	  
	 }

	 
	 @Test
		void testSetAttributesForPreviousNextButtons() {
		 int rxNumber=0;
		 Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype");
		 rxMap.put("One", m_personPrescriptionDTO);
		 i_refillAuthorizeSearchAction.setAttributesForPreviousNextButtons(rxNumber, rxMap, m_request);
		 
		}
	 
	 @Test
		void testSetAttributesForPreviousNextButtons_01() {
		 int rxNumber=3;
		 Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype");
		 rxMap.put("One", m_personPrescriptionDTO);
		 i_refillAuthorizeSearchAction.setAttributesForPreviousNextButtons(rxNumber, rxMap, m_request);
		 
		}
	 
	 @Test
		void testSetAttributesForPreviousNextButtons_02() {
		 int rxNumber=1;
		 Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype");
		 rxMap.put("One", m_personPrescriptionDTO);
		 rxMap.put("two",m_personPrescriptionDTO);
		 i_refillAuthorizeSearchAction.setAttributesForPreviousNextButtons(rxNumber, rxMap, m_request);
		 
		} 
	 
	 @Test
		void testSetAttributesForPreviousNextButtons_03() {
		 int rxNumber=2;
		 Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype");
		 rxMap.put("One", m_personPrescriptionDTO);
		 rxMap.put("two",m_personPrescriptionDTO);
		 i_refillAuthorizeSearchAction.setAttributesForPreviousNextButtons(rxNumber, rxMap, m_request);
		 
		}
	 
	 
	 @Test
	void testUpdatePrescriptions() throws Exception {
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		 
		 Map<String, PersonPrescriptionDTO> inputMap = new HashMap<>();
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype"); 
		 inputMap.put("One", m_personPrescriptionDTO);
		 inputMap.put("two",m_personPrescriptionDTO);
		 
		 String updateType = "TO_PHARMACY";
		 //String updateType = "PHARMACY";
		 
		 StringBuffer rxIdsToBePrinted = new StringBuffer();
		 m_userLoginInfoDTO.setPatientId("1234");
		 m_userLoginInfoDTO.setDoctorId("456");
		 
		 for (Map.Entry<String, PersonPrescriptionDTO> inputMapEntry : inputMap.entrySet()) {
				PersonPrescriptionDTO personPrescriptionDTO = inputMapEntry.getValue();
				personPrescriptionDTO.setPatientId(m_userLoginInfoDTO.getPatientId());
				personPrescriptionDTO.setDoctorId(m_userLoginInfoDTO.getDoctorId());
				personPrescriptionDTO.setUserId(m_userLoginInfoDTO.getDoctorId());
				rxIdsToBePrinted.append("'" + personPrescriptionDTO.getRxId() + "',");
				
				when(personPrescriptionDTO.getStatus()).thenReturn("DENIED");
				when(personPrescriptionDTO.getStatus()).thenReturn("APPROVED");
				personPrescriptionDTO.setRxId("123");
				PersonPrescriptionDTO tempPersonPrescriptionDTO = new PersonPrescriptionDTO(); 
				when(m_statelessSearchFacadeLocal.getPrescriptionFromRxId(personPrescriptionDTO.getRxId())).thenReturn(tempPersonPrescriptionDTO);
				personPrescriptionDTO.setDaysSupply(30);
				personPrescriptionDTO.setRefills(4);
				int totalDaysPrescribedFor = personPrescriptionDTO.getDaysSupply() * personPrescriptionDTO.getRefills();
				Calendar cal = Calendar.getInstance();
				tempPersonPrescriptionDTO.setTentativeEndDt(new Date());
				cal.setTime(tempPersonPrescriptionDTO.getTentativeEndDt());
				cal.add(Calendar.DATE, totalDaysPrescribedFor);
				Date tentativeEndDt = cal.getTime(); 
				personPrescriptionDTO.setTentativeEndDt(tentativeEndDt);
				personPrescriptionDTO.setActiveF(true);
				
				PrescriptionRefillDTO saveReadyPrescriptionRefillDTO = m_refillUtils.generateSaveReadyPrescriptionRefill(personPrescriptionDTO);
				m_prescriptionFacadeLocal.updateRefillInformation(personPrescriptionDTO,
						saveReadyPrescriptionRefillDTO, updateType);
				
				personPrescriptionDTO.setSendRefillRx(true); 
				//personPrescriptionDTO.setSendRefillRx(false); 
				
				m_userLoginInfoDTO.setUserId("777");
			
				personPrescriptionDTO.setCreatedBy(m_userLoginInfoDTO.getUserId());
				personPrescriptionDTO.setCreatedFrom(m_request.getRemoteAddr());
				personPrescriptionDTO.setCreatedAt(new Date(System.currentTimeMillis()));
				personPrescriptionDTO.setLastUpdatedFrom(m_session.getId());
				//m_auditor.audit(personPrescriptionDTO);
		 }		
				//inputMap.clear(); 
			//m_searchFacdeLocal.clearSearchResult();
			//m_searchFacdeLocal.clearCache();
			//rxIdsToBePrinted.substring(0, rxIdsToBePrinted.lastIndexOf(","));
				
		 
		 i_refillAuthorizeSearchAction.updatePrescriptions(m_request, m_form, updateType, inputMap);
			
		} 
	 
	  
	  
	@Test
	void testConfirm() throws Exception {
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype"); 
		 rxMap.put("One", m_personPrescriptionDTO);
		 rxMap.put("two",m_personPrescriptionDTO);
		 i_refillAuthorizeSearchAction.storeMapInCache(rxMap, m_request, m_form);
		 PersonPrescriptionDTO dto = new PersonPrescriptionDTO(); 
			dto.setStatus("APPROVED");   
		    dto.setDisplayRefills(-1); 
		    when(m_errors.getErrorCount()).thenReturn(3);
		// i_refillAuthorizeSearchAction.validateNewRefillForm(m_request, rxMap,m_errors);
		    i_refillAuthorizeSearchAction.setRxhubPBMUniqueId(m_form, m_request);
		    i_refillAuthorizeSearchAction.setAttributesForPreviousNextButtons(m_form.getRxNumber(), rxMap, m_request);
		     m_form.setPrescriptionMode(REFILL_MODE);
		  
		     when(m_userLoginInfoDTO.getDoctorId()).thenReturn("124");
		    when(m_individualLocal.getDoctorProfile("124")).thenReturn(m_personDTO);
		    when(m_personDTO.getFirstName()).thenReturn("hh");
		    m_form.setDoctorFirstName("jj"); 
		    
		    when(m_personDTO.getLastName()).thenReturn("hh"); 
		    m_form.setDoctorLastName("rii");
		   
		    when(m_userLoginInfoDTO.getPatientId()).thenReturn("124");
		    when(m_individualLocal.getPatientProfile("124")).thenReturn(m_personDTO);
		    when(m_personDTO.getFirstName()).thenReturn("hh"); 
		    m_form.setPatientFirstName("jj");
		    
		    when(m_personDTO.getLastName()).thenReturn("hh");
		    m_form.setPatientLastName("rii");

			//setPatientDoctorNames(request, refillAuthorizeSearchForm);
		    i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form);
		    i_refillAuthorizeSearchAction.setRxhubPBMUniqueId(m_form, m_request);
		    List<PersonPrescriptionDTO> rxList  = new ArrayList<>();
		    PersonPrescriptionDTO pd = new PersonPrescriptionDTO();
		    pd.setCodifiedSig("dc");
		    pd.setSig("did");
		    rxList.add(pd);
		   // when(m_refillUtils.getRxList(rxMap)).thenReturn(rxList);
		    m_form.setSearchResult(rxList);
			m_searchFacadeLocal.cacheSearchResult(rxList, 5);
			m_request.setAttribute(REQUESTED_PAGE, 1);
			m_request.setAttribute("clicked", "");
			
			when(m_form.getDeaClassCode()).thenReturn("[Schedule II Drug]");
			
		    i_refillAuthorizeSearchAction.confirm(m_request, m_response, m_model, m_form, m_errors);

	}
	

	@Test
	void testPharmacyupdate() throws Exception {
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		Map<String, PersonPrescriptionDTO> unprocessedMap= null;
		Map<String, PersonPrescriptionDTO> rxMap = null; 
		String doctorId=null;
		UserDTO userDisablePhysicanDTO = null; 
	  
		PersonRefillDTO prRefillDTO = new PersonRefillDTO();
		when(m_session.getAttribute("isScheduleDrug")).thenReturn(true);
		when(m_session.getAttribute("refillAuthorizeSearchForm")).thenReturn(m_form);
		//when(m_session.getAttribute("isScheduleDrug")).thenReturn(null);
		String sRefillCount= "4";
		when(m_form.getValue("RefillCount")).thenReturn(sRefillCount); 
		int refillCount = 0;
    	refillCount = Integer.parseInt(sRefillCount);
    	 Map<String,String>  refillMap = new HashMap<String,String>();  
		  
			for (int i = 0; i < refillCount; i++) {
//				when(m_session.getAttribute("isScheduleDrug")).thenReturn(null);
//				m_form.setValue("RefillAction"+i,m_request.getParameter("value(RefillAction"+i+")"));
//				m_form.setValue("DenialReason"+i, m_request.getParameter("value(DenialReason"+i+")"));
//				 m_form.setValue("Refill"+i,m_request.getParameter("value(Refill"+i+")"));
//			    m_form.setValue("Notes"+i,m_request.getParameter("value(Notes"+i+")"));
				when(m_request.getParameter("value(RefillAction"+i+")")).thenReturn("value");
			
				m_form.setValue("RefillAction"+i,m_request.getParameter("value(RefillAction"+i+")"));
				 m_form.setValue("DenialReason"+i, m_request.getParameter("value(DenialReason"+i+")"));
				 m_form.setValue("Refill"+i,m_request.getParameter("value(Refill"+i+")"));
				m_form.setValue("Notes"+i,m_request.getParameter("value(Notes"+i+")"));
				
			String action = (String)m_form.getValue("RefillAction" + i);
    		String reason = (String)m_form.getValue("DenialReason" + i);
    		String Refill=(String)m_form.getValue("Refill"+i);
    		String Notes=(String)m_form.getValue("Notes"+i);
			
			refillMap.put("RefillAction" + i, action);
    		refillMap.put("DenialReason" + i, reason);
    		refillMap.put("Refill" + i, Refill);
    		refillMap.put("notes" + i, Notes); 
    		m_request.setAttribute("RefillMap", refillMap);
			}
		 
		     rxMap = new HashMap<>();
			 m_personPrescriptionDTO.setApplnMode("Inmode");
			 m_personPrescriptionDTO.setConfirmRxType("Rxtype"); 
			 rxMap.put("One", m_personPrescriptionDTO);
			 rxMap.put("two",m_personPrescriptionDTO);
			 when(m_userLoginInfoDTO.getDoctorId()).thenReturn("124");
			    when(m_individualLocal.getDoctorProfile("124")).thenReturn(m_personDTO);
			    when(m_personDTO.getFirstName()).thenReturn("hh");
			    m_form.setDoctorFirstName("jj"); 
			    
			    when(m_personDTO.getLastName()).thenReturn("hh"); 
			    m_form.setDoctorLastName("rii");
			   
			    when(m_userLoginInfoDTO.getPatientId()).thenReturn("124");
			    when(m_individualLocal.getPatientProfile("124")).thenReturn(m_personDTO);
			    when(m_personDTO.getFirstName()).thenReturn("hh"); 
			    m_form.setPatientFirstName("jj");
			    
			    when(m_personDTO.getLastName()).thenReturn("hh");
			    m_form.setPatientLastName("rii");
			 Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("rxentry" , HttpServletRequest.class, HttpServletResponse.class,Map.class, RefillAuthorizeSearchForm.class, BindingResult.class,ModelAndView.class);
				method.setAccessible(true);
				 ModelAndView view = (ModelAndView) method.invoke(i_refillAuthorizeSearchAction,  m_request,m_response, rxMap, m_form, m_errors, m_model );

			
			when(m_userLoginInfoDTO.isControlledSubstances()).thenReturn(true);
			when(m_session.getAttribute("setOnBehalfId")).thenReturn(null);
			m_request.setAttribute("isUserCSEnabled", true);
			
			//when(m_form.getSigResponse()).thenReturn("resposnse");
			//String duoIntegrationKey = "integrationkey";
			//when(DigitalRxParameterLoader.getInstance().getDigitalrxParameter("DUO_SECRET_KEY")).thenReturn(duoIntegrationKey);
			
		i_refillAuthorizeSearchAction.pharmacyupdate(m_form, m_errors, m_request, m_response, m_model);
	   
		 
	     
	
	}

	@Test
	void testFrontdeskupdate() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
	    m_form.setPrescriptionMode(REFILL_MODE);
	    when(m_session.getAttribute("prescriptionRefillReportUrl")).thenReturn(null);
	    m_model.setViewName("refill.refillauthorizesearch.page");
	    ModelAndView frontdeskupdate = i_refillAuthorizeSearchAction.frontdeskupdate(m_form, m_errors, m_request, m_response, m_model);
	
	    assertEquals(frontdeskupdate.getViewName(),m_model.getViewName());
	
	}
     
	
	
	@Test  
	void testRefilldenied() throws Exception {
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
	    m_form.setPrescriptionMode(REFILL_MODE);

		i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form); 
		i_refillAuthorizeSearchAction.setRxhubPBMUniqueId(m_form, m_request);
		m_request.setAttribute("denied", "");
		m_model.setViewName("refill.rxentry.page");
		ModelAndView refilldenied = i_refillAuthorizeSearchAction.refilldenied(m_form, m_errors, m_request, m_response, m_model);
		assertEquals(m_model.getViewName(),refilldenied.getViewName());
	} 
       
	
	@Test
	void testBacktorxrefill() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		 i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form); 
			i_refillAuthorizeSearchAction.setRxhubPBMUniqueId(m_form, m_request);
			ModelAndView backtorxrefill = i_refillAuthorizeSearchAction.backtorxrefill(m_form, m_errors, m_request, m_response, m_model);
	        assertNotNull(backtorxrefill);
	  
	} 

	                    
	@Test 
	void testLabrecords() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		 i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form);
		 m_form.setPrescriptionMode(REFILL_MODE);
		 i_refillAuthorizeSearchAction.addMapValues(m_request, m_form);
		 
		 Collection<LabRecordsSearchResultsImpl> labRecords = m_statelessSearchFacadeLocal.labRecords(m_userLoginInfoDTO.getDoctorId(), m_userLoginInfoDTO.getPatientId());
		 m_form.setSearchResult(labRecords);
		 m_searchFacadeLocal.cacheSearchResult((List<LabRecordsSearchResultsImpl>) labRecords, 15);
		 m_request.setAttribute(REQUESTED_PAGE, 1);
			m_request.setAttribute("clicked", "");
			ModelAndView labrecords2 = i_refillAuthorizeSearchAction.labrecords(m_form, m_errors, m_request, m_response, m_model);
			assertNotNull(labrecords2);
	}
	
       
	@Test 
	void testLabrecordresult() throws Exception {
		m_form.setPrescriptionMode(REFILL_MODE);
		Collection<PersonSpecimenSearchResultsImpl> personSpecimen = m_statelessSearchFacadeLocal.personSpecimen(m_form.getPersonLabRecordId());
		m_form.setSearchResult(personSpecimen);
		ModelAndView model= new ModelAndView("refill.labrecords.page","refillAuthorizeSearchForm", m_form);
		ModelAndView labrecordresult = i_refillAuthorizeSearchAction.labrecordresult(m_form, m_errors, m_request, m_response, model);
		assertNotEquals(labrecordresult,model);
	
	} 
   /**
	@throws Exception 
 * @Test
	void testPharmacyUpdateSS() {
		fail("Not yet implemented");
	}
*/                
	@Test
	void testBack() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		 i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form);
		 m_form.setPrescriptionMode(REFILL_MODE);
		 i_refillAuthorizeSearchAction.addMapValues(m_request, m_form);
		 
		 Collection<LabRecordsSearchResultsImpl> labRecords = m_statelessSearchFacadeLocal.labRecords(m_userLoginInfoDTO.getDoctorId(), m_userLoginInfoDTO.getPatientId());
		 m_form.setSearchResult(labRecords);
		 m_searchFacadeLocal.cacheSearchResult((List<LabRecordsSearchResultsImpl>) labRecords, 15);
		 m_request.setAttribute(REQUESTED_PAGE, 1);
			m_request.setAttribute("clicked", "");
			ModelAndView back = i_refillAuthorizeSearchAction.back (m_request, m_response, m_form);
			assertNotNull(back);
	} 
	
	// pending
	 @Test 
		void testUpdatePrescriptionsForSSMessages() throws Exception {
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		    String rxMessageNumber ="message"; 
			String statusCode ="TRANSACTION_SUCCESSFUL_ACCEPTED_BY_ULTIMATE";
			System.out.println(statusCode);
			//String statusCode = "010";
			System.out.println(statusCode.substring(7,10).equalsIgnoreCase(SIGStatusConstants.TRANSACTION_SUCCESSFUL_ACCEPTED_BY_ULTIMATE));
			//String statusCode    = "TransactionSuccessfulAcceptedByUltimate";
			String rawSSMessageId = "rawmessage";  
			String refillAction  = "refil"; 
			String rxReferenceNumber = "1234";  
			String relatesId = "567";   
			
		 m_personPrescriptionDTO.setAuthorizedRefillsId("1234");
		when(m_statelessSearchFacadeLocal.getPersonRefillByRefillId(m_personPrescriptionDTO.getAuthorizedRefillsId())).thenReturn(m_personRefillDTO);
		m_personPrescriptionDTO.setStatus("Sataus");
		m_personPrescriptionDTO.setRefillRequestDenialReason("DenialReason");
		m_personPrescriptionDTO.setInstructions("Instructions");
	   
		m_personRefillDTO.setApprovalType(m_personPrescriptionDTO.getStatus());
		m_personRefillDTO.setDenialReason(m_personPrescriptionDTO.getRefillRequestDenialReason());
		m_personRefillDTO.setNotes(m_personPrescriptionDTO.getInstructions());
		
		when(m_personPrescriptionDTO.getStatus()).thenReturn("APPROVED");
		m_personPrescriptionDTO.setDisplayRefills(4);
		m_personRefillDTO.setRefillsApproved(m_personPrescriptionDTO.getDisplayRefills());
		
		m_personRefillDTO.setRefillResponseStatusCode(statusCode);
		m_personRefillDTO.setRxSSMsgNumber(rxMessageNumber);
		
		//when(i_refillAuthorizeSearchAction.generateSavePersonDrugDTO(m_personPrescriptionDTO,rxMessageNumber)).thenReturn(m_personDrugDTO);
		//m_personDrugDTO.setDrugId("234");
		when(m_personPrescriptionDTO.getDoctorNote()).thenReturn("fff");
		i_refillAuthorizeSearchAction.updatePrescriptionsForSSMessages(m_request, rxReferenceNumber, relatesId, m_personPrescriptionDTO, rxMessageNumber, statusCode, rawSSMessageId, m_personRefillDTO, refillAction);
	 }
	 

	 
    // completed
	@Test
	void testGenerateSavePersonDrugDTO() throws Exception {
		Date currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(
				System.currentTimeMillis())));
		
		String rxMessageNumber = "567"; 
		
		m_personPrescriptionDTO.setDoctorId("123");
		m_personPrescriptionDTO.setPatientId("234");
		m_personPrescriptionDTO.setDescription("Desc");
		m_personPrescriptionDTO.setDosageForm("Dosage");
		m_personPrescriptionDTO.setDispensibleDrugId(4);
		m_personPrescriptionDTO.setDrugName("drug"); 
		m_personPrescriptionDTO.setFormularyStatus("staus");
		m_personPrescriptionDTO.setInstructions("instr");
		m_personPrescriptionDTO.setPersonId("789");
		m_personPrescriptionDTO.setRoute("route");
		m_personPrescriptionDTO.setStrength("strength");
		m_personPrescriptionDTO.setStrengthUnit("Unit");
		m_personPrescriptionDTO.setSig("sig");
		m_personPrescriptionDTO.setQuantity(10);
		m_personPrescriptionDTO.setDaysSupply(5);
		m_personPrescriptionDTO.setSubstitutionCode("code");
		
		
		m_personDrugDTO.setId(PrimaryKeyGenerator.getKey());		
		m_personDrugDTO.setDoctorId(m_personPrescriptionDTO.getDoctorId());
		m_personDrugDTO.setPersonId(m_personPrescriptionDTO.getPatientId());
		m_personDrugDTO.setDescription(m_personPrescriptionDTO.getDescription());
		m_personDrugDTO.setDosageForm(m_personPrescriptionDTO.getDosageForm());
		Integer dispensableDrugId = m_personPrescriptionDTO.getDispensibleDrugId();
		m_personDrugDTO.setDrugId(dispensableDrugId.toString());
		m_personDrugDTO.setDrugName(m_personPrescriptionDTO.getDrugName());
		m_personDrugDTO.setFormularyStatus(m_personPrescriptionDTO.getFormularyStatus());
		m_personDrugDTO.setInstructions(m_personPrescriptionDTO.getInstructions());
		m_personDrugDTO.setPersonId(m_personPrescriptionDTO.getPersonId());
		m_personDrugDTO.setRoute(m_personPrescriptionDTO.getRoute());
		m_personDrugDTO.setStrength(m_personPrescriptionDTO.getStrength());
		m_personDrugDTO.setStrengthUnit(m_personPrescriptionDTO.getStrengthUnit());
		m_personDrugDTO.setSig(m_personPrescriptionDTO.getSig());
		m_personDrugDTO.setCreatedDate(currentDate);
		m_personDrugDTO.setQuantity(m_personPrescriptionDTO.getQuantity());
		m_personDrugDTO.setDaysSupply(m_personPrescriptionDTO.getDaysSupply());
		m_personDrugDTO.setSubstitutionCode(m_personPrescriptionDTO.getSubstitutionCode());
	             // if condition check
				when(m_personPrescriptionDTO.getDisplayRefills()).thenReturn(3);
				 m_personDrugDTO.setRefills(m_personPrescriptionDTO.getDisplayRefills());
				
				 // else condition
				// when(m_personPrescriptionDTO.getDisplayRefills()).thenReturn(0);
				// m_personPrescriptionDTO.setRefills(4);
				// m_personDrugDTO.setRefills(m_personPrescriptionDTO.getRefills());
				
				    m_personDrugDTO.setCodifiedSig(m_personPrescriptionDTO.getCodifiedSig());
					m_personDrugDTO.setActive(true);
					m_personDrugDTO.setRxMode(m_personPrescriptionDTO.getPrescriptionDeliveryMode());
					m_personDrugDTO.setRxStatus(m_personPrescriptionDTO.getRxStatus());
					m_personDrugDTO.setRxType(m_personPrescriptionDTO.getMessageType());
					m_personDrugDTO.setRxMessageNumber(rxMessageNumber);
					m_personDrugDTO.setRoute(m_personPrescriptionDTO.getRoute());
					m_personDrugDTO.setFormularyStatus(m_personPrescriptionDTO.getFormularyStatus());
					m_personDrugDTO.setDosageForm(m_personPrescriptionDTO.getDosageForm());
					m_personDrugDTO.setCodifiedSig(m_personPrescriptionDTO.getCodifiedSig());
					m_personDrugDTO.setPharmacyId(m_personPrescriptionDTO.getPharmacyId());
					m_personDrugDTO.setPrescriberAgentId(m_personPrescriptionDTO.getPrescriberAgentId());
					m_personDrugDTO.setSupervisorSegmentId(m_personPrescriptionDTO.getSupervisorSegmentId());	
					
					when(m_personPrescriptionDTO.getDiagnosisQualifier()).thenReturn("qualifier");
					m_personDrugDTO.setDiagnosisQualifier(m_personPrescriptionDTO.getDiagnosisQualifier());
					
					when(m_personPrescriptionDTO.getDiagnosisValue()).thenReturn("qualifier");
					m_personDrugDTO.setDiagnosisValue(m_personPrescriptionDTO.getDiagnosisValue());
					
					when(m_personPrescriptionDTO.getDiagnosisIcd()).thenReturn("7");
					m_personDrugDTO.setDiagnosisIcd(m_personPrescriptionDTO.getDiagnosisIcd());
					
					when(m_personPrescriptionDTO.getDisplayRefills()).thenReturn(3);
					 m_personDrugDTO.setRefills(m_personPrescriptionDTO.getDisplayRefills());
					 
					 when(m_personPrescriptionDTO.getDoctorNote()).thenReturn("note");
		 			 m_personDrugDTO.setDoctorNote(m_personPrescriptionDTO.getDoctorNote());
					
					
	PersonDrugDTO generateSavePersonDrugDTO = i_refillAuthorizeSearchAction.generateSavePersonDrugDTO(m_personPrescriptionDTO, rxMessageNumber);
	assertNotNull(generateSavePersonDrugDTO);
	}
     
// still pending 80% done
	@Test                                        
	void testBacktorxsearch() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form);
		i_refillAuthorizeSearchAction.setPharmacyAndSubstitution(m_form, m_request);
		ModelAndView backtorxsearch = i_refillAuthorizeSearchAction.backtorxsearch(m_model, m_form, m_request, m_response);
		assertNotNull(backtorxsearch);
	}
       
	   
	@Test   
	void testRxsearch() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		m_session.removeAttribute(ACTIVE_MED_FOR_NEWRX_FLAG);
		when(m_session.getAttribute("doctorInfoForm")).thenReturn(m_doctorInfoForm);
		m_doctorInfoForm.setAddressSelected(DOCTOR_ADDRESS_SELECTED_FLAG);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		 when(m_form.getSearchCriteria(m_userLoginInfoDTO.getDoctorId())).thenReturn(m_refillsSearchCriteria);
		 m_refillsSearchCriteria.setNumberOfDays(NO_OF_DAYS_FOR_SEARCH);
		 m_refillsSearchCriteria.setSearchType(RX_DAYS_SEARCH_TYPE);
		 ModelAndView rxsearch = i_refillAuthorizeSearchAction.rxsearch(m_model, m_form, m_request, m_response);
	     assertNotNull(rxsearch);
	}
      
	@Test
	void testSearch() throws Exception {
		m_searchFacadeLocal.searchRefills(m_refillsSearchCriteria); 
		when(m_request.getSession()).thenReturn(m_session); 
		m_session.setAttribute(SEARCH_HANDLE, m_searchFacadeLocal);
		m_request.setAttribute(REQUESTED_PAGE, 1);
		m_request.setAttribute("clicked", " "); 
		m_model.setViewName("refill.refillauthorizesearch.page");
		ModelAndView search = i_refillAuthorizeSearchAction.search(m_model, m_form, m_request, m_response, m_refillsSearchCriteria);
	    assertEquals(m_model,search);
	} 

	
	@Test 
	void testSetPharmacyAndSubstitution() {
		m_form.setPharmacyName(m_personRefillDTO.getPharmacyName());
		m_form.setPharmacyId(m_personRefillDTO.getPharmacyId());
		m_form.setPharmacyFaxNumber("");
		m_personPrescriptionDTO.setPharmacyId(m_form.getPharmacyId());
		m_personPrescriptionDTO.setPharmacyName(m_form.getPharmacyName());
		m_personPrescriptionDTO.setPharmacyFaxNumber(m_form.getPharmacyFaxNumber());
 
		m_personPrescriptionDTO.setSubstitutionCode(m_form.getSubstitutionCode());
		i_refillAuthorizeSearchAction.setPharmacyAndSubstitution(m_form, m_personRefillDTO, m_personPrescriptionDTO);
	}
     
	@Test  
	void testSetPharmacyAndSubstitution_01() throws Exception {
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		 m_form.setPatientId("4");
		 m_userLoginInfoDTO.setPatientId("123");
		 m_userLoginInfoDTO.setPraticeId("67");
		 
		m_form.setPharmacyOptions(m_individualLocal.currentPharmacyInfo(m_form.getPatientId()));
		m_form.setSubstitutionCodeOptions(m_abstractAction.getSubstitutionValues());
		m_form.setRxhubPBMUniqueIdOptions(m_statelessSearchFacadeLocal
		.getRxHubMemberId(m_userLoginInfoDTO.getPatientId(), m_userLoginInfoDTO.getPracticeId()));
	
		i_refillAuthorizeSearchAction.setPharmacyAndSubstitution(m_form, m_request);
		
	}
	
	
	@Test
	void testAllergies() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		 m_form.setPrescriptionMode(REFILL_MODE);
		 
		 when(m_userLoginInfoDTO.getDoctorId()).thenReturn("124");
		    when(m_individualLocal.getDoctorProfile("124")).thenReturn(m_personDTO);
		    when(m_personDTO.getFirstName()).thenReturn("hh");
		    m_form.setDoctorFirstName("jj"); 
		    
		    when(m_personDTO.getLastName()).thenReturn("hh"); 
		    m_form.setDoctorLastName("rii");
		   
		    when(m_userLoginInfoDTO.getPatientId()).thenReturn("124");
		    when(m_individualLocal.getPatientProfile("124")).thenReturn(m_personDTO);
		    when(m_personDTO.getFirstName()).thenReturn("hh"); 
		    m_form.setPatientFirstName("jj");
		    
		    when(m_personDTO.getLastName()).thenReturn("hh");
		    m_form.setPatientLastName("rii");
		 // i_refillAuthorizeSearchAction.setPatientDoctorNames(m_request, m_form);
		   i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form);
		   i_refillAuthorizeSearchAction.addMapValues(m_request, m_form);
		  
		   
			List<PersonAllergyDTO> allergiesInformationList = new ArrayList<>();
			m_PersonAllergyDTO.setDrugId("124");
			m_PersonAllergyDTO.setType("type");
			m_PersonAllergyDTO.setAllergyId("allergyId");
			m_PersonAllergyDTO.setDrugName("drugname");
			allergiesInformationList.add(m_PersonAllergyDTO);
			m_userLoginInfoDTO.setPatientId("124");
			when(m_userLoginInfoDTO.getPatientId()).thenReturn("124");
		when(m_individualLocal.allergiesInfo(m_userLoginInfoDTO.getPatientId())).thenReturn(allergiesInformationList);

		Map<String, AllergiesInfoObject> allergiesMap = new TreeMap<String, AllergiesInfoObject>();
		m_allergiesInfoObject.setDrugId("678");
		allergiesMap.put("drugID", m_allergiesInfoObject);
		
		for (PersonAllergyDTO personAllergyDTO : allergiesInformationList) { 
			String drugId = "124";
			when(personAllergyDTO.getDrugId()).thenReturn(drugId);
			//when(allergiesMap.containsKey(drugId)).thenReturn(true);
			AllergiesInfoObject allergiesInfoObject = new AllergiesInfoObject();
			allergiesInfoObject.setAllergyId((personAllergyDTO.getAllergyId()));
			allergiesInfoObject.setDrugId(personAllergyDTO.getDrugId());
			allergiesInfoObject.setDrugName(personAllergyDTO.getDrugName());
			allergiesInfoObject.setId(personAllergyDTO.getId());

			allergiesInfoObject.getAllergies().add(personAllergyDTO.getType());
			allergiesMap.put(drugId, allergiesInfoObject); 
		}
		Collection<AllergiesInfoObject> allergiesInfoValuesSet = allergiesMap.values();
		List<Object> allergiesInfoList = new ArrayList<Object>();
		for (Object o : allergiesInfoValuesSet) {
			allergiesInfoList.add(o);
		}
		m_searchFacadeLocal.cacheSearchResult(allergiesInfoList, 5);
		m_session.setAttribute(SEARCH_HANDLE,m_searchFacadeLocal);
		m_request.setAttribute(REQUESTED_PAGE, 1);
		m_model.addObject("refillAuthorizeSearchForm",m_form);
		m_model.setViewName("refill.allergies.page");
		 ModelAndView allergies = i_refillAuthorizeSearchAction.allergies(m_request, m_response, m_form, m_model);
        assertEquals(allergies,m_model);		   

	}
        
	@Test 
	void testPaginate() throws Exception {
		m_request.setAttribute("clicked", " ");
		m_form.setPageNumber(10);
		m_request.setAttribute(REQUESTED_PAGE, m_form.getPageNumber());
		m_model.addObject("refillAuthorizeSearchForm",m_form);
		m_model.setViewName("refill.allergies.page");
		ModelAndView paginate = i_refillAuthorizeSearchAction.paginate(m_request, m_response, m_form, m_errors, m_model);
		assertEquals(paginate,m_model);
	}

	/**
	@throws Exception 
	 * @throws SecurityException  
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 *             
	}   
*/          
	 
	 @Test
	 void testAddMapValues() throws Exception {
		 Map<String, PersonPrescriptionDTO> rxMap =null;
		 rxMap = new TreeMap<String, PersonPrescriptionDTO>();
	    i_refillAuthorizeSearchAction.storeMapInCache(rxMap, m_request, m_form);
	    i_refillAuthorizeSearchAction.addMapValues(m_request, m_form);
	 }  
		
	 // pending
	 @Test
	 void testAddMapValues_01() throws Exception {
		 Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		 PersonPrescriptionDTO dto = new PersonPrescriptionDTO();
		 dto.setApplnMode("Inmode");
		dto.setCopay("copay");;
		 rxMap.put("One", dto); 
		 m_form.setRxNumber(6); 
		 int rxNumber = m_form.getRxNumber(); 
		 when(m_form.getRxNumber()).thenReturn(6);
		 when(m_form.getPatientId()).thenReturn("67");
		// Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
				// i_refillAuthorizeSearchAction.getRxMap(m_form, m_request);
		
//		m_personPrescriptionDTO.setApplnMode("Inmode");
//	     m_personPrescriptionDTO.setConfirmRxType("Rxtype");
//		 rxMap.put("One", m_personPrescriptionDTO);
//	     rxMap.put("two",m_personPrescriptionDTO);
		 
		
		 rxMap.put(String.valueOf(m_form.getRxNumber()),
					m_form.getRxDTO(m_form.getPatientId())); 
		 
		 
		 i_refillAuthorizeSearchAction.addMapValues(m_request, m_form);
	 } 
			
	
			
	@Test
	void testSetDoctorAndPatientId()  {  
		
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		m_form.setInitialized(true); 
		m_userLoginInfoDTO.setPatientId("123");
		m_userLoginInfoDTO.setDoctorId("11");
	    m_form.setPatientId(m_userLoginInfoDTO.getPatientId()); 
		m_form.setDoctorId(m_userLoginInfoDTO.getDoctorId());
		i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form);
			
	}                                            

	@Test
	void testSetPatientDoctorNames() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO); 
		
	   
	    when(m_userLoginInfoDTO.getDoctorId()).thenReturn("124");
	    when(m_individualLocal.getDoctorProfile("124")).thenReturn(m_personDTO);
	    when(m_personDTO.getFirstName()).thenReturn("hh");
	    m_form.setDoctorFirstName("jj"); 
	    
	    when(m_personDTO.getLastName()).thenReturn("hh"); 
	    m_form.setDoctorLastName("rii");
	   
	    when(m_userLoginInfoDTO.getPatientId()).thenReturn("124");
	    when(m_individualLocal.getPatientProfile("124")).thenReturn(m_personDTO);
	    when(m_personDTO.getFirstName()).thenReturn("hh"); 
	    m_form.setPatientFirstName("jj");
	    
	    when(m_personDTO.getLastName()).thenReturn("hh");
	    m_form.setPatientLastName("rii");
	    
		i_refillAuthorizeSearchAction.setPatientDoctorNames(m_request, m_form);
	}
	

	

	 // pending due to single ton class
	
	@Test
	void testSetSubstitution() {
		String substitutionCode = "kkkkkkhhhhh";
        m_form.setSubstitutionCode(substitutionCode);
//        Map<String, String> objectMap = new TreeMap<>();
//       objectMap.put("kkkkkkhhhhh", "hhhhhh");
//        objectMap.put("aaaa", "kkkkk");
        Map<String, String> substitutionCodeOptionsMap = new TreeMap<>();
        substitutionCodeOptionsMap.put("kkkkkkhhhhh", "hhhhhh");
        substitutionCodeOptionsMap.put("aaaa", "kkkkk");
      
        String substitutionCodeDescription =  substitutionCodeOptionsMap.get("kkkkk");
        m_form.setSubstitutionCodeDesc(substitutionCodeDescription);
        when(m_form.getSubstitutionCode()).thenReturn(substitutionCode);
        i_refillAuthorizeSearchAction.setSubstitution(m_form, substitutionCode);
	} 
     
	
	@Test  
	void testSetRxhubPBMUniqueId() throws Exception {
		when(m_request.getSession()).thenReturn(m_session);
		when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
		m_form.setRxhubPBMUniqueIdOptions(m_statelessSearchFacadeLocal
				.getRxHubMemberId(m_userLoginInfoDTO.getPatientId(), m_userLoginInfoDTO.getPracticeId()));
	
		i_refillAuthorizeSearchAction.setRxhubPBMUniqueId(m_form, m_request);
	}

	// pending due to private method
	@Test
	void testGetRxMapRefillAuthorizeSearchFormHttpServletRequest() {
	//	Map<String, PersonPrescriptionDTO> rxMap = (Map<String, PersonPrescriptionDTO>) m_searchFacadeLocal
		//	.getFromCache(getRxMapName(m_form, m_request));
	}
	/** 
	@throws Exception 
	 * @Test
	void testStoreMapInCacheMapOfStringPersonPrescriptionDTOHttpServletRequestRefillAuthorizeSearchForm() {
		fail("Not yet implemented");
	}
      **/   
	@Test
	void testCheckForEligibilityBenchMark() throws Exception {
		 boolean checkEligibility = false;
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.HOUR, -72); 

			Date eligibilityCheckBenchmarkTime = calendar.getTime();
			String patientId ="124"; 
			String practiceId = "234";
			Date lastEligibilityCheckTime = new Date();
			when(m_statelessSearchFacadeLocal.getEligibilityTime(patientId, practiceId)).thenReturn(null);		
	
	boolean checkForEligibilityBenchMark = i_refillAuthorizeSearchAction.checkForEligibilityBenchMark(m_request, patientId, practiceId);
	assertEquals(checkForEligibilityBenchMark,true);
	}
    
//	@Test
//	void testGetPrescriptionFacade() throws NamingException {
//		
//		PrescriptionFacadeLocal prescriptionFacade = i_refillAuthorizeSearchAction.getPrescriptionFacade(m_request);
//		assertEquals(m_prescriptionFacadeLocal, prescriptionFacade); 
//		//assertEquals(null,prescriptionFacade);
//	}
//	@Test
//	void testGetDrugInformation() throws Exception {
//	NewDrug drugInformation = m_newDrugInformationBridgeLocal.getDrugInformation("100", "101", "102", "103");
//  NewDrug drugInformation1 = i_refillAuthorizeSearchAction.getDrugInformation("122", "2", "3", "4");	
//	 assertEquals(drugInformation1,drugInformation);
//	}

	
//	@Test
//	void testGetPersonInformation() throws Exception {
//	//	PersonDTO personDTO = m_personLocal.getDetailedPersonDTO("11");
//	//	PersonDTO personDTO2 = i_refillAuthorizeSearchAction.getPersonInformation("151");
//		//assertEquals(personDTO,personDTO2);
//	}

	
  
//	@Test
//	void testSetSubstitutionCodes_01() {
//		String substitutionCode = null;
//		 RefillAuthorizeSearchAction.setSubstitutionCodes(substitutionCode);
//	}
//	@Test
//	void testSetSubstitutionCodes_02() {
//		String substitutionCode = "0";
//		 RefillAuthorizeSearchAction.setSubstitutionCodes(substitutionCode);
//	}
//	@Test
//	void testSetSubstitutionCodes_03() {
//		String substitutionCode = "1";
//		 RefillAuthorizeSearchAction.setSubstitutionCodes(substitutionCode);
//	}
//	@Test
//	void testSetSubstitutionCodes_04() {
//		String substitutionCode = " ";
//		 RefillAuthorizeSearchAction.setSubstitutionCodes(substitutionCode);
//	}
	
//	@Test                                                                          
//	void testEpcsRefillsAuthentication() throws Exception {
//		String type = m_request.getParameter("type"); 
//		String signed_dt = m_request.getParameter("signed_dt");
//		boolean isLogout=true;
//		when(m_request.getSession()).thenReturn(m_session);
//		m_session.setAttribute("isLogout", isLogout);
//		m_session.setAttribute("type", type);
//		m_session.setAttribute("signed_dt", signed_dt); 
//		 Map<String,Object> m_items = new HashMap<String, Object>();
//		 when(m_session.getAttribute("items")).thenReturn(m_items);
//		 for(Map.Entry<String, Object> m_values:m_items.entrySet()){
//			m_form.setValue(m_values.getKey(), m_values.getValue());
//		 }
//		 String sRefillCount = "123";
//		 when(m_form.getValue("RefillCount")).thenReturn(sRefillCount);
//		 int refillCount = Integer.parseInt(sRefillCount);
//		 for (int i = 0; i < refillCount; i++) {
//	    		
//	    		m_form.setValue("RefillAction"+i,m_request.getParameter("value(RefillAction"+i+")"));
//	    		m_form.setValue("DenialReason"+i, m_request.getParameter("value(DenialReason"+i+")"));
//		    	m_form.setValue("Refill"+i,m_request.getParameter("value(Refill"+i+")"));
//		    	m_form.setValue("Notes"+i,m_request.getParameter("value(Notes"+i+")"));
//	     }
//		 m_session.setAttribute("refillAuthorizeSearchForm", m_form);
//		 String domain = "upgrade";
//		 m_response.sendRedirect("https://epcsprod.h2hdigitalrx.com/sso/samlLogin?domain="+domain);
//		// ModelAndView epcsRefillsAuthentication = i_refillAuthorizeSearchAction.epcsRefillsAuthentication(m_request, m_response, m_form);
//		 //assertEquals(epcsRefillsAuthentication,null);
//	}
	
//	@Test
//	void testUpdateRefillInformation() throws DAOException {
//		m_prescriptionDAOImpl.updatePersonPrescription(m_personPrescriptionDTO);
//		String operationType ="TO_PHARMACY";
//		operationType.equalsIgnoreCase("TO_PHARMACY"); 
//		//when(operationType.equalsIgnoreCase("TO_PHARMACY")).thenReturn(true);
//		m_prescriptionDAOImpl.savePrescriptionRefill(m_prescriptionRefillDTO);
//		//i_refillAuthorizeSearchAction.updateRefillInformation(m_personPrescriptionDTO, m_prescriptionRefillDTO, operationType);
//	}
//	@Test
//	void testIsRefillAuthorized() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("isRefillAuthorized" ,UserLoginInfoDTO.class);
//		method.setAccessible(true);
//	    m_userLoginInfoDTO.setDoctorId("12345");
//		when(m_personFacadeLocal.retrieveDoctorInformation(m_userLoginInfoDTO.getDoctorId())).thenReturn(m_doctorInformationDTO);
//	 when(m_personSubscriptionLevelDTO.isRefill()).thenReturn(true);
//	  Object invoke = method.invoke(i_refillAuthorizeSearchAction,m_userLoginInfoDTO);
//	   assertNotEquals(false,invoke);
//	     
//	}     
	
//	@Test 
//	void testGenerateSaveReadyPrescriptionRefill() throws NoSuchMethodException, SecurityException, ParseException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("generateSaveReadyPrescriptionRefill", PersonPrescriptionDTO.class);
//		method.setAccessible(true);
//		Date currentDate = new Date(System.currentTimeMillis());
//		currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentDate));
// 
//		//PrescriptionRefillDTO prescriptionRefillDTO = new PrescriptionRefillDTO();
//		m_prescriptionRefillDTO.setId(PrimaryKeyGenerator.getKey());
//		m_prescriptionRefillDTO.setRxId(m_personPrescriptionDTO.getRxId());
//		m_prescriptionRefillDTO.setStrength(m_personPrescriptionDTO.getStrength());
//		m_prescriptionRefillDTO.setStrengthUnit(m_personPrescriptionDTO.getStrengthUnit());
//		m_prescriptionRefillDTO.setRoute(m_personPrescriptionDTO.getRoute());
//		m_prescriptionRefillDTO.setFormularyStatus(m_personPrescriptionDTO.getFormularyStatus());
//		m_prescriptionRefillDTO.setCopay(m_personPrescriptionDTO.getCopay());
//		m_prescriptionRefillDTO.setCoverage(m_personPrescriptionDTO.getCoverage());
//		m_prescriptionRefillDTO.setSig(m_personPrescriptionDTO.getSig());
//		m_prescriptionRefillDTO.setDosageForm(m_personPrescriptionDTO.getDosageForm());
//		m_prescriptionRefillDTO.setDaysOfSupply(m_personPrescriptionDTO.getDaysSupply());
//		m_prescriptionRefillDTO.setQuantity(m_personPrescriptionDTO.getQuantity());
//		m_prescriptionRefillDTO.setInstructions(m_personPrescriptionDTO.getInstructions());
//		m_prescriptionRefillDTO.setSubstitution(m_personPrescriptionDTO.getSubstitutionCode());
//		m_prescriptionRefillDTO.setPickUpDate(currentDate);
//		m_prescriptionRefillDTO.setDoctorId(m_personPrescriptionDTO.getDoctorId());
//		m_prescriptionRefillDTO.setPharmacyId(m_personPrescriptionDTO.getPharmacyId());
//		m_prescriptionRefillDTO.setPharmacyName(m_personPrescriptionDTO.getPharmacyName());
//		m_prescriptionRefillDTO.setRequestedBy(REQUESTED_BY_DOCTOR_FOR_PATIENT);
//		m_prescriptionRefillDTO.setRequestedDate(currentDate);
//		m_prescriptionRefillDTO.setFilledDate(currentDate);
//		m_prescriptionRefillDTO.setRxHubPBMUniqueId(m_personPrescriptionDTO.getRxHubPBMUniqueId());
//		  Object invoke = method.invoke(i_refillAuthorizeSearchAction, m_personPrescriptionDTO);
//       assertEquals(m_prescriptionRefillDTO, invoke);
//		
//		
//		
//	}

     
	@Test
	void testDrugFoodInteraction() throws Exception {
		
		m_request.setAttribute("personId", m_form.getPatientId());
		m_request.setAttribute("drugs", m_form.getDrugs());
		when(m_request.getSession()).thenReturn(m_session);
		m_session.setAttribute("statelessSearchFacadeLocal", m_statelessSearchFacadeLocal);
		m_session.setAttribute("newDrugInformationBridgeLocal", m_newDrugInformationBridgeLocal);
		m_session.setAttribute("individualLocal", m_individualLocal);
		m_model.setViewName("refill.refillInteraction.page");
	  ModelAndView modelAndView = i_refillAuthorizeSearchAction.drugFoodInteraction(m_form, m_request, m_response, m_model);
	  
	    assertEquals("refill.refillInteraction.page",modelAndView.getViewName());
	}
	 
	@Test 
	void testShowPrescribedMedication() throws Exception {
		String requestId = m_request.getParameter("messageId");
		PersonRefillDTO personRefillDTO = m_statelessSearchFacadeLocal.getPersonRefillByRefillResponseMessageId(requestId);
	    m_ajaxHelper.createXMLForPrescribedDrugInfoFromRefills(m_response,personRefillDTO);
	    ModelAndView showPrescribedMedication = i_refillAuthorizeSearchAction.showPrescribedMedication(m_form, m_request, m_response);
	    assertEquals(null,showPrescribedMedication);
	}
 
	    
	@Test
	void testRemovePendingRefill() throws Exception { 
   
	when(m_request.getParameter("selectedRefill")).thenReturn("selectedRefill");
	when(m_request.getSession()).thenReturn(m_session);
    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
	when(m_individualLocal.getPendingRefillRecord(m_request.getParameter("selectedRefill"))).thenReturn(m_personRefillDTO);

	m_personRefillDTO.setRefillResponseStatusCode("Closed");
	m_individualLocal.updatePendingRefill(m_personRefillDTO);
    m_request.setAttribute("clicked", " ");
    m_userLoginInfoDTO.setPendingRx(m_shieldLocal.getPendingRxCount(m_userLoginInfoDTO.getId()));
    ModelAndView removePendingRefill = i_refillAuthorizeSearchAction.removePendingRefill(m_form, m_errors, m_request, m_response, m_model);
		assertEquals(removePendingRefill, m_model);
 
	}
    
	@Test  
	void testRemovePendingRefill_01() throws Exception { 
		when(m_request.getParameter("selectedRefill")).thenReturn(null);
	    ModelAndView removePendingRefill = i_refillAuthorizeSearchAction.removePendingRefill(m_form, m_errors, m_request, m_response, m_model);
     assertNotNull(removePendingRefill);
	}
	
	@Test
	void testRemovePendingRefill_02() throws Exception { 
		when(m_request.getParameter("selectedRefill")).thenReturn("selectedRefill");
		when(m_request.getSession()).thenReturn(m_session);
	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
		when(m_individualLocal.getPendingRefillRecord(m_request.getParameter("selectedRefill"))).thenReturn(null);
//ModelAndView alterrx = i_refillAuthorizeSearchAction.alterrx(m_form, m_errors, m_request, m_response, m_model);
	    ModelAndView removePendingRefill = i_refillAuthorizeSearchAction.removePendingRefill(m_form, m_errors, m_request, m_response, m_model);
     assertNotNull(removePendingRefill); 
	}
	
      @Test
	void testAuditEPCSRefillRxListByAjax() throws Exception {
		when(m_request.getParameter("type")).thenReturn("fromRefill"); 
		//when(i_refillAuthorizeSearchAction.auditEPCSRefillRxListByAjax(m_request, m_response, m_form)).thenReturn(i_refillAuthorizeSearchAction.epcsRefillsAuthentication(m_request, m_response, m_form));
		//ModelAndView auditEPCSRefillRxListByAjax = i_refillAuthorizeSearchAction.auditEPCSRefillRxListByAjax(m_request, m_response, m_form);
      //  ModelAndView epcsRefillsAuthentication = i_refillAuthorizeSearchAction.epcsRefillsAuthentication(m_request, m_response, m_form);
      // assertEquals(auditEPCSRefillRxListByAjax,epcsRefillsAuthentication);
       // assertNotNull(epcsRefillsAuthentication);
      } 
	
      @Test
  	void testAuditEPCSRefillRxListByAjax_01() throws Exception {
  		 
  		when(m_request.getSession()).thenReturn(m_session);
  		List<PersonRefillDTO> auditEPCSRefillRxList = new ArrayList<>();
  		m_personRefillDTO.setApplnMode("InMode"); 
  		m_personRefillDTO.setConfirmRxType("rxType");
  		auditEPCSRefillRxList.add(m_personRefillDTO);
  		when(m_session.getAttribute("auditEPCSRefillRxList")).thenReturn(auditEPCSRefillRxList);
  		String auditStatus = "auditStatusEPCSisSigned";
  		when(m_request.getParameter("auditStatus")).thenReturn(auditStatus);
  		String refillNumber = "99";
  		when(m_request.getParameter("refillNumber")).thenReturn(refillNumber);
  		when(m_session.getAttribute("epcsSignedRxIndexlist")).thenReturn("epcsSignedRxIndexlist");
  		m_session.removeAttribute("epcsSignedRxIndexlist");
  		
  		String[] split = refillNumber.split(":"); 
   
  		for(int i=0;i<split.length; i++)   
		{
		//	when(auditEPCSRefillRxList.get(Integer.parseInt(split[i]))).thenReturn(m_personRefillDTO);
		//	m_personRefillDTO.setRefillResponseStatusDescription(auditStatus);
		//	Auditor.audit(m_personRefillDTO);
		}
  		i_refillAuthorizeSearchAction.auditEPCSRefillRxListByAjax(m_request, m_response, m_form);
      }
	 
	  
	//  few lines missing
	@Test
	void testEpcsRefillsIdMeAuthentication() throws Exception {
		
		String urlString  = "http://idme.h2hdigitalrx.com/idme/sendToIdMe";
		String domain = "prod";
		String type = m_request.getParameter("type"); 
		String signed_dt = m_request.getParameter("signed_dt");
		boolean isLogout=true; 
		when(m_request.getSession()).thenReturn(m_session); 
		m_session.setAttribute("isLogout", isLogout);
		m_session.setAttribute("type", type);
		m_session.setAttribute("signed_dt", signed_dt); 
		
		 Map<String,Object> m_items = new HashMap<String, Object>(); 
		 m_items.put("a", 1);
		 m_items.put("b", 2);
		 when(m_session.getAttribute("items")).thenReturn(m_items);
		 for(Map.Entry<String, Object> m_values:m_items.entrySet()){
			m_form.setValue(m_values.getKey(), m_values.getValue());
		 } 
		 String sRefillCount = "123";
		 when(m_form.getValue("RefillCount")).thenReturn(sRefillCount);
		 int refillCount = Integer.parseInt(sRefillCount);
		 for (int i = 0; i < refillCount; i++) {
	    		
	    		m_form.setValue("RefillAction"+i,m_request.getParameter("value(RefillAction"+i+")"));
	    		m_form.setValue("DenialReason"+i, m_request.getParameter("value(DenialReason"+i+")"));
		    	m_form.setValue("Refill"+i,m_request.getParameter("value(Refill"+i+")"));
		    	m_form.setValue("Notes"+i,m_request.getParameter("value(Notes"+i+")"));
	     }
		 m_session.setAttribute("refillAuthorizeSearchForm", m_form);
		 m_response.sendRedirect(urlString + "?domain="+domain);
		
	
		
		 
		 ModelAndView epcsRefillsIdMeAuthentication = i_refillAuthorizeSearchAction.epcsRefillsIdMeAuthentication(m_form, m_request, m_response);
	     assertEquals(null,epcsRefillsIdMeAuthentication);
	}
	
	   
	

	
	//	private methods starts here
	 // completed
	@Test   
	void testInsertRefillInMedicationHistory() throws Exception {
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("insertRefillInMedicationHistory", PersonDrugDTO.class, HttpServletRequest.class);
		method.setAccessible(true);
		m_personDrugDTO.setPersonId("123"); 
		m_personDrugDTO.setDrugId("667");
		List<PersonDrugDTO> allDuplicateDrugs = new ArrayList<>();
		when(m_personFacadeLocal.getAllDuplicateDrugs(m_personDrugDTO.getPersonId(),m_personDrugDTO.getDrugId())).thenReturn(allDuplicateDrugs);
		when(m_request.getSession()).thenReturn(m_session);
	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
       PersonDrugDTO pd = new PersonDrugDTO();
	   pd.setId("123");
       pd.setDrugId("678");  
       pd.setActive(false); 
       pd.setReasonForInactivation(MEDICATION_ENDED); 
       allDuplicateDrugs.add(pd);
		for(PersonDrugDTO m_PersonDrugDTO : allDuplicateDrugs){ 
			
			m_PersonDrugDTO.setActive(false); 
			m_PersonDrugDTO.setReasonForInactivation(MEDICATION_ENDED);
			m_PersonDrugDTO.setEndDate(new Date());
			m_PersonDrugDTO.setInactivatedBy(m_userLoginInfoDTO.getDoctorId());
			m_personFacadeLocal.deactivateMedicationHistoryDrug(m_PersonDrugDTO);
       
		}
		m_prescriptionFacadeLocal.savePersonDrugInformation(m_personDrugDTO);
		  //Object invoke = 
		method.invoke(i_refillAuthorizeSearchAction, m_personDrugDTO, m_request);
          //assertEquals(m_prescriptionRefillDTO, invoke);
	}
	
	@Test
	void testGetRxMapName() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("getRxMapName", RefillAuthorizeSearchForm.class, HttpServletRequest.class);
		method.setAccessible(true);
		String rxMapName = "";
	   when(m_form.getPrescriptionMode()).thenReturn(REFILL_MODE);
	   rxMapName = "RefillEntry";
		Object invoke = method.invoke(i_refillAuthorizeSearchAction, m_form, m_request);
		assertEquals(invoke, rxMapName);
		
		
	}
	
	@Test
	void testGetRxMapName_01() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("getRxMapName", RefillAuthorizeSearchForm.class, HttpServletRequest.class);
		method.setAccessible(true);
		String rxMapName = "";
	   when(m_form.getPrescriptionMode()).thenReturn(null);
	   rxMapName = "RxEntry";
		Object invoke = method.invoke(i_refillAuthorizeSearchAction, m_form, m_request);
		assertEquals(invoke, rxMapName);
		
		 
	}
	
	
	@Test 
	void testCheckEligibility() throws Exception {
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("checkEligibility" , HttpServletRequest.class);
		method.setAccessible(true);
		when(m_request.getSession()).thenReturn(m_session);
	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
	    EligResponseTO eligResponseTO = new EligResponseTO();
	    List<EligBenInqRespTO> list = new ArrayList<>();
	    EligBenInqRespTO ben = new EligBenInqRespTO();
	    ben.setCity("Hyd");
	    ben.setGroupId("234");
	    list.add(ben);
	    eligResponseTO.setEligInqRespTOList(list);
		when(m_individualLocal.rxhubRxEligibilityInfo(m_userLoginInfoDTO, null, false)).thenReturn(m_eligResponseTO);
		List<EligibilityResponseForm> responseList = new LinkedList<EligibilityResponseForm>();

	    when(m_eligResponseTO.getEligInqRespTOList()).thenReturn(list);
	    EligibilityResponse eligResponse = new EligibilityResponse();
		responseList = eligResponse.getResponseList(eligResponseTO);
	    
		m_request.setAttribute("requestSubmitted", responseList);
		
	   method.invoke(i_refillAuthorizeSearchAction,  m_request);
	
	}
  
	@Test 
	void testBuildPrescription() throws Exception {
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("buildPrescription" , HttpServletRequest.class, String.class,  String.class, int.class, RefillAuthorizeSearchForm.class);
		method.setAccessible(true);
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
		
		String personRefillId = "RefillId";
	   String personId = "personID";
	   int rxNumber = 345;
	  // PersonRefillDTO m_personRefillDTO = new PersonRefillDTO();
	   when(m_statelessSearchFacadeLocal.getPersonRefillByRefillId(personId)).thenReturn(m_personRefillDTO);
	   PersonPrescriptionDTO original=new PersonPrescriptionDTO();
	   
	   m_personRefillDTO.setFollowUpCount("Count");
	   when(m_personRefillDTO.getFollowUpCount()).thenReturn("count");
	   RefillRequestTO refillRequestTO=null;
	   boolean poncount=true;
		  String messageId=null;     
		  String orginalMessageId=null;
		  String receivedTime=null;
		  orginalMessageId =m_personRefillDTO.getRxId();
		  boolean PONMatch=false; 
		 when(m_sigValidator.validatePON(orginalMessageId,refillRequestTO, messageId, receivedTime)).thenReturn(false);
		  PersonRefillDTO refilldto = new PersonRefillDTO();
		  refilldto.setConfirmRxType("RxType");
		 // when(m_prescriptionFacadeLocal.getPersonRefillDtoByRelatedMessageId(orginalMessageId)).thenReturn(refilldto);
		  refilldto.setRxId("Rxid"); 
		  orginalMessageId = refilldto.getRxId();
		 // when(m_prescriptionFacadeLocal.getRxaletDtoByRelatedMessageId(orginalMessageId)).thenReturn(m_rxAlertDTO);
		  m_rxAlertDTO.setRelatedMessageId("relatedMessageID");
		  orginalMessageId =m_rxAlertDTO.getRelatedMessageId();
	   
		 // making ponmatch =true and else condition
		  when(m_sigValidator.validatePON(orginalMessageId,refillRequestTO, messageId, receivedTime)).thenReturn(true);
		 
	   when(m_statelessSearchFacadeLocal.getPrescriptionFromMessageId(orginalMessageId)).thenReturn(original);
          //when(m_statelessSearchFacadeLocal.getPrescriptionFromMessageId(personRefillDTO.getRxId()));
	     m_personPrescriptionDTO = new PersonPrescriptionDTO(original);
//	   Method method1 = RefillAuthorizeSearchAction.class.getDeclaredMethod("getCurrentRxHubUniqueId" ,  HttpServletRequest.class,PersonPrescriptionDTO.class, String.class);
//		method1.setAccessible(true);
//		//String personId = "123"; 
//		String rxHubPBMUniqueIda = (String) method.invoke(i_refillAuthorizeSearchAction,  m_request, personPrescriptionDTO, personId);

	  // personPrescriptionDTO.setRxHubPBMUniqueId(rxHubPBMUniqueIda);
	   m_personPrescriptionDTO.setRxNumber(rxNumber);
       m_personPrescriptionDTO.setReason(m_personRefillDTO.getReason());
       m_personPrescriptionDTO.setRefillsRequestedDate(m_personRefillDTO.getRequestedDate());
       m_personPrescriptionDTO.setAuthorizedRefillsId(m_personRefillDTO.getId());
       m_personPrescriptionDTO.setMessageId(m_personRefillDTO.getMessageId());
       m_personPrescriptionDTO.setRefills(m_personRefillDTO.getRefills());
       m_personPrescriptionDTO.setDisplayRefillsRequested(Integer.toString(m_personRefillDTO.getRefillsRequested()));
       m_personPrescriptionDTO.setAuthorizationRequestedBy(m_personRefillDTO.getRequestedBy());
       m_personPrescriptionDTO.setPickupDateTime(null);
       m_personPrescriptionDTO.setPharmacyType(m_personRefillDTO.getPharmacyType());
       i_refillAuthorizeSearchAction.setPharmacyAndSubstitution(m_form, m_personRefillDTO, m_personPrescriptionDTO);
       m_form.setRefillStatus(RX_APPROVED_BY_DOCTOR);
       m_personPrescriptionDTO.setStatus(RX_APPROVED_BY_DOCTOR);
       m_personPrescriptionDTO.setDaysSupply(m_personRefillDTO.getDaysOfSupply());
       m_form.setDaysSupply(String.valueOf(m_personPrescriptionDTO.getDaysSupply()));	
       m_personPrescriptionDTO.setPersonRefillId(m_personRefillDTO.getId());
       m_personPrescriptionDTO.setSubstitutionCode(m_personRefillDTO.getSubstitution());
       
       m_form.setPatientId(personId); 
       m_form.setPatientId("patient");
       m_form.setPracticeId("practice");
       m_refillUtils.setDespensedMedication(m_personRefillDTO.getMessageId(),m_personPrescriptionDTO, m_form.getPatientId(),m_form.getPracticeId());
       String deaClassCode ="[Schedule II Drug]";
       m_personPrescriptionDTO.setDeaClassCode(deaClassCode);
    	//	  when(m_personPrescriptionDTO.getDeaClassCode()).thenReturn(deaClassCode);
     //  when(m_personPrescriptionDTO.getDiagnosisQualifier()).thenReturn("qualifier");
		  method.invoke(i_refillAuthorizeSearchAction,  m_request, personRefillId, personId, rxNumber, m_form );
 	 
	}
	  
	

	
	@Test 
	void testRxentry() throws Exception{ 
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("rxentry" , HttpServletRequest.class, HttpServletResponse.class,Map.class, RefillAuthorizeSearchForm.class, BindingResult.class,ModelAndView.class);
		method.setAccessible(true);
		 Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setConfirmRxType("Rxtype");
		 rxMap.put("One", m_personPrescriptionDTO);
		 when(m_request.getSession()).thenReturn(m_session);
		 when(m_session.getAttribute("isScheduleDrug")).thenReturn(true);
		 when(m_session.getAttribute("refillAuthorizeSearchForm")).thenReturn(m_form);
		 when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);  
		 m_userLoginInfoDTO.setDoctorId("124");
		 List<IdentificationDTO> doctorIdentificationList = new ArrayList<>();
		 m_identificationDTO.setConfirmRxType("rstype");
		 m_identificationDTO.setCreatedBy("created");  
		 doctorIdentificationList.add(m_identificationDTO);  
		 when(m_userLoginInfoDTO.getDoctorId()).thenReturn("124");
		 when(m_identificationLocal.findByPersonId(m_userLoginInfoDTO.getDoctorId())).thenReturn(doctorIdentificationList);
		 for (IdentificationDTO identificationDTO : doctorIdentificationList) {
			 identificationDTO.setType("DH");
			 when(identificationDTO.getType()).thenReturn("DH");
			 when(identificationDTO.getIdentification()).thenReturn("identi");
			 String deaNumber = identificationDTO.getIdentification();
			   m_request.setAttribute("DoctorDEA",deaNumber); 
		 }
//		 String argPatientId = "aa";
//		 m_form.setPatientId("124");
//		 m_userLoginInfoDTO.setPatientId("hh");
//		 when(m_form.getPatientId()).thenReturn("123456789");
//		 m_abstractAction.setPatientInfoInSession(m_form.getPatientId(), m_request);
//		 PatientInfoForm patientInfoForm = new PatientInfoForm();
//		 patientInfoForm.setPatientId("123456789");
//		 when(m_statelessSearchFacadeLocal.getPatientHostId(patientInfoForm.getPatientId())).thenReturn("123456789");
//		// PersonDTO m_patientDTO = newPerosn
//		 List<PersonAddressDTO> list = new ArrayList<>();
//		 PersonAddressDTO pad = new PersonAddressDTO();
//		 pad.setAddress("hyd");
//		 list.add(pad);
//		 when(m_personDTO.getPersonAddresses()).thenReturn(list);
//		//PersonDTO m_patientDTO = new PersonDTO();
//		m_personDTO.setPersonAddresses(list);
//		 when(m_personDTO.getPersonAddresses()).thenReturn(list);
		 
		  when(m_userLoginInfoDTO.getDoctorId()).thenReturn("124");
		    when(m_individualLocal.getDoctorProfile("124")).thenReturn(m_personDTO);
		    when(m_personDTO.getFirstName()).thenReturn("hh");
		    m_form.setDoctorFirstName("jj");
		    
		    when(m_personDTO.getLastName()).thenReturn("hh");
		    m_form.setDoctorLastName("rii");
		   
		    when(m_userLoginInfoDTO.getPatientId()).thenReturn("124");
		    when(m_individualLocal.getPatientProfile("124")).thenReturn(m_personDTO);
		    when(m_personDTO.getFirstName()).thenReturn("hh");
		    m_form.setPatientFirstName("jj");
		    
		    when(m_personDTO.getLastName()).thenReturn("hh");
		    m_form.setPatientLastName("rii");
		    i_refillAuthorizeSearchAction.setDoctorAndPatientId(m_request, m_form); 
		    i_refillAuthorizeSearchAction.setRxhubPBMUniqueId(m_form, m_request);
			m_form.setInitialized(true);
			m_request.setAttribute("alterrx", "");
			m_session.removeAttribute("refillAuthorizeSearchForm");
			m_session.removeAttribute("isScheduleDrug");
			m_model.setViewName("refill.rxentry.page");
		 method.invoke(i_refillAuthorizeSearchAction,  m_request,m_response, rxMap, m_form, m_errors, m_model );
		
	}  
	 
	@Test
	void testIsPharmacyEnabledForSS() throws Exception {
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("isPharmacyEnabledForSS" , Map.class, HttpServletRequest.class, BindingResult.class);
		method.setAccessible(true);
		boolean retval = true;
		 Map<String, PersonPrescriptionDTO> rxMap = new HashMap<>();
		
		 m_personPrescriptionDTO.setApplnMode("Inmode");
		 m_personPrescriptionDTO.setCopay("copay");
		 m_personPrescriptionDTO.setStatus("");
		 m_personPrescriptionDTO.setPharmacyType("ooo");
		 rxMap.put("One", m_personPrescriptionDTO); 
		// rxMap.put("two", dto); 
		 
	 for (String rxMapKey : rxMap.keySet()) {
//			  m_personPrescriptionDTO = rxMap.get(rxMapKey);
//			 //when(rxMap.get(rxMapKey)).thenReturn(m_personPrescriptionDTO);
		// m_personPrescriptionDTO.setStatus(null);
		 when(m_personPrescriptionDTO.getStatus()).thenReturn("");     
		 
		//String pharmacyType = "ooo";
		when(m_personPrescriptionDTO.getPharmacyType()).thenReturn("ooo");
			// m_personPrescriptionDTO.setPharmacyId("pharmacy");
			// when(m_personPrescriptionDTO.getPharmacyId()).thenReturn("pharmacy");
		// when(m_refillUtils.isPharmacyEnabledForSS(m_personPrescriptionDTO.getPharmacyId())).thenReturn(false);
		 m_personPrescriptionDTO.setPharmacyName("Name");
		 m_errors.reject("errors.refill.pharmacy.disabled",m_personPrescriptionDTO.getPharmacyName());
			retval = false;
		 }
		  method.invoke(i_refillAuthorizeSearchAction, rxMap, m_request , m_errors );
		// assertEquals(invoke,false);
	}

	@Test
	void testIsMultipleDeniedNewRx() throws Exception {
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("isMultipleDeniedNewRx" , RefillAuthorizeSearchForm.class );
		method.setAccessible(true);
		boolean retval = true;
		int count = 0;
		String sRefillCount = "76";
		when(m_form.getValue("RefillCount")).thenReturn(sRefillCount);
		int refillCount = Integer.parseInt(sRefillCount);
		
		System.out.println(refillCount);
	//	for (int i = 0; i < refillCount; i++) {  
			String action = RX_DENIED_NEW_RX; 
			System.out.println(action.trim().equals(RX_DENIED_NEW_RX));
			// when(action.trim().equals("RX_DENIED_NEW_RX")).thenReturn(true);
			when(m_form.getValue("RefillCount" )).thenReturn(action);
			// m_form.setValue(sRefillCount, 2);
			   // action=  (String) m_form.getValue("RefillAction" + i);
			
	//	}
		
		method.invoke(i_refillAuthorizeSearchAction,  m_form);
		
	}
	 
	@Test
	void testBuildMap() throws Exception{
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("buildMap" , RefillAuthorizeSearchForm.class, HttpServletRequest.class);
		method.setAccessible(true);	
		String sRefillCount = "4";
		when(m_form.getValue("RefillCount")).thenReturn(sRefillCount);
		int refillCount = 6;
		String personRefillId = "12";
		String personId = "34";
	    refillCount = Integer.parseInt(sRefillCount);
	    for (int i = 1; i < refillCount; i++) { 
			int refill = 3;  
			String refillString = "";
			when(m_form.getValue("Refill" +i)).thenReturn(refill); 
			refill = Integer.parseInt(m_form.getValue("Refill" + i).toString()); 
			
			String notes = (String)m_form.getValue("Notes" + i);
			String action = (String) m_form.getValue("RefillAction" + i);
			String denialReason = (String) m_form.getValue("DenialReason" + i);
			//String personRefillId = (String) m_form.getValue("PersonRefillId" + i);			 
			//String personId = (String) m_form.getValue("PersonId");			
			//PersonPrescriptionDTO personPrescriptionDTO = i_refillAuthorizeSearchAction.buildPrescription(m_request, personId, 
			//personRefillId, i, m_form);	
			m_form.setActiveFlag("flag");
			m_form.setAge("45");
			m_form.setCity("hyd");
			m_personRefillDTO.setRxId("rs");
			when(m_statelessSearchFacadeLocal.getPersonRefillByRefillId(personId)).thenReturn(m_personRefillDTO);
			  
			   
			   m_personRefillDTO.setFollowUpCount("Count");
			   when(m_personRefillDTO.getFollowUpCount()).thenReturn("count");
			 //  String orginalMessageId = "messageId";
			//   m_rxAlertDTO.setRelatedMessageId(orginalMessageId);
			 //  when(m_rxAlertDTO.getRelatedMessageId()).thenReturn( orginalMessageId);

			Method method1 = RefillAuthorizeSearchAction.class.getDeclaredMethod("buildPrescription" , HttpServletRequest.class, String.class,  String.class, int.class, RefillAuthorizeSearchForm.class);
			method1.setAccessible(true);
	
			 method1.invoke(i_refillAuthorizeSearchAction,  m_request, personRefillId, personId, i, m_form );

		
			m_personPrescriptionDTO.setDisplayRefills(refill);
			m_personPrescriptionDTO.setStatus(action); 
	    }
		//Map<String, PersonPrescriptionDTO> rxMap = new TreeMap<String, PersonPrescriptionDTO>();
	    
		
		method.invoke(i_refillAuthorizeSearchAction, m_form, m_request);
	}
	
	@Test
	void testSetControlledSubstanceFlag() throws Exception {
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("setControlledSubstanceFlag" , PersonPrescriptionDTO.class, HttpServletRequest.class);
		method.setAccessible(true);
		when(m_request.getSession()).thenReturn(m_session);
	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
		String deaClassCode = "SCHEDULE_III_DRUG";

		String userState = "VERMONT_STATE";  
		PharmacyDTO pharmacyDTO = null;
		//String deaClassCode = "classcode"; 
		when(m_personPrescriptionDTO.getDeaClassCode()).thenReturn(deaClassCode);
		List<PersonAddressDTO> prd = new ArrayList<>();
		PersonAddressDTO dto = new PersonAddressDTO();
		dto.setAddress("Hyderabad");
		dto.setId("Hyderabad");
		prd.add(dto); 
		m_userLoginInfoDTO.setAddressList(prd);
		when( m_userLoginInfoDTO.getAddressList()).thenReturn(prd);
		for (PersonAddressDTO personAddressDTO : m_userLoginInfoDTO.getAddressList()) {
		//when(personAddressDTO.getId()).thenReturn("Hyderabad");
		when(m_userLoginInfoDTO.getSelectedAddress()).thenReturn("Hyderbad");
		//when(personAddressDTO.getId().equalsIgnoreCase((String) m_userLoginInfoDTO.getSelectedAddress())).thenReturn(true);
	  //when(personAddressDTO.getId()).thenReturn("id");
	  when(m_userLoginInfoDTO.getSelectedAddress()).thenReturn("hyd");
	   List<IdentificationDTO> doctorIdentificationList = new ArrayList<>();
	   m_identificationDTO.setConfirmRxType("rx");
	   m_identificationDTO.setCreatedBy("by");
	   m_identificationDTO.setType("DH");
	   m_identificationDTO.setIdentification("identity");
	   when(m_userLoginInfoDTO.getDoctorId()).thenReturn("345");
	   doctorIdentificationList.add(m_identificationDTO);
	   when(m_identificationLocal.findByPersonId(m_userLoginInfoDTO.getDoctorId())).thenReturn(doctorIdentificationList);

	   for (IdentificationDTO m_identificationDTO : doctorIdentificationList) {
	   when(m_identificationDTO.getType()).thenReturn("DH");
	   when(m_identificationDTO.getIdentification()).thenReturn("identity");
	   String deaNumber = m_identificationDTO.getIdentification();
	   m_request.setAttribute("DoctorDEA",deaNumber);
	   }
		}
		
		when(m_personPrescriptionDTO.getPharmacyId()).thenReturn("2050");
		when(m_individualLocal.getPharmacyInfoFromPharmacyCode(m_personPrescriptionDTO.getPharmacyId())).thenReturn(m_pharmacyDTO);
		int pharmacyServicelevel=0;
		//pharmacyDTO= new PharmacyDTO();
		//pharmacyDTO.setServiceLevel("level");
		when(m_pharmacyDTO.getServiceLevel()).thenReturn("2050");
		pharmacyServicelevel = Integer.parseInt(m_pharmacyDTO.getServiceLevel());
		when(m_personPrescriptionDTO.getDeaClassCode()).thenReturn("SCHEDULE_III_DRUG");
		when(m_userLoginInfoDTO.isControlledSubstances()).thenReturn(true);
		//m_session.setAttribute(null, null); 
		when(m_session.getAttribute("setOnBehalfId")).thenReturn(null);
		method.invoke(i_refillAuthorizeSearchAction, m_personPrescriptionDTO, m_request);
	}
	
	@Test
	void testConstructBIRTReportURL() throws Exception {
		Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("constructBIRTReportURL" ,  HttpServletRequest.class,RefillAuthorizeSearchForm.class, String.class);
		method.setAccessible(true);
		String generatedRxIds = "";
		 
		
		String PRES_REP_NAME = "PrintNewPrescription.rptdesign";
		String PRES_WITH_WATERMARK = "PrintPrescriptionWithWatermark.rptdesign";
		String AMP = "&";
		String FORMAT = "__format=pdf";

		
		String DOC_NAME = "p_doctorName=";
		String DOC_ADDRESS = "p_doctorAddress="; 
		String DOC_CITY_STATE_ZIP = "p_doctorCityStateAndZip=";
		String DOC_PHONE = "p_doctorPhone=";
		String DOC_FAX = "p_doctorFax=";  
		String DOC_DEA = "p_doctorDEA="; 
		String DOC_NPI = "p_doctorNPI="; 
		String DOC_CLINIC_NAME = "p_clinicName=";
		String PAT_NAME = "p_patientName=";
		String PAT_DOB = "p_patientDOB=";
		String RX_IDS = "p_generatedRxIds=";
		String rxId = "AND pp.RX_SSMSGNUMBER_X in (" + generatedRxIds + ")";	    	
        
        StringBuffer newPrescriptionBIRTUrl = new StringBuffer();
		PersonContactDTO doctorWorkPhoneDTO = new PersonContactDTO() ;
		doctorWorkPhoneDTO.setContact("contact");
		PersonContactDTO doctorFaxDTO = new PersonContactDTO();
		doctorFaxDTO.setCreatedBy("by");
		newPrescriptionBIRTUrl.append("/" + m_applicationContextLocal.getReportContext() + "/run?__report=report/");
         when(m_request.getParameter("watermark")).thenReturn("watermark");
         newPrescriptionBIRTUrl.append(PRES_WITH_WATERMARK);
         
         when(m_request.getSession()).thenReturn(m_session);
 	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
 		when(m_session.getAttribute("patientInfoForm")).thenReturn(m_patientInfoForm);
 		String doctorId ="123"; 
 		when(m_userLoginInfoDTO.getDoctorId()).thenReturn(doctorId);
 		when(m_refillUtils.getPersonInformation(doctorId)).thenReturn(m_personDTO);
 		when(m_personFacadeLocal.getPrimaryContact(m_userLoginInfoDTO.getDoctorId(), WORKPHONE)).thenReturn(doctorWorkPhoneDTO);
 		when(m_personFacadeLocal.getPrimaryContact(m_userLoginInfoDTO.getDoctorId(), FAX)).thenReturn(doctorFaxDTO);
 		
 		List<PersonAddressDTO> addressList = m_userLoginInfoDTO.getAddressList();
		
		
		PersonAddressDTO dto = new PersonAddressDTO();
		dto.setAddress("Hyderabad");
		dto.setId("id");
		dto.setAddressLineOne("Hyd");
		dto.setAddressLineTwo("nagar");
		dto.setCity("Hubli");
		dto.setState("ts");
		dto.setZip("34567");
		dto.setLocation("hyderabad");
		addressList.add(dto); 
		m_userLoginInfoDTO.setAddressList(addressList);
		when( m_userLoginInfoDTO.getAddressList()).thenReturn(addressList);
		
		String docAddLine1 = null;
		String docAddLine2 = null; 
		String docClinicName = null;
		
		for (PersonAddressDTO personAddressDTO : addressList) {
		when(m_userLoginInfoDTO.getSelectedAddress()).thenReturn("id");	
		docAddLine1 = personAddressDTO.getAddressLineOne() + " "
				+ personAddressDTO.getAddressLineTwo();
		docAddLine2 = personAddressDTO.getCity() + ", "
				+ personAddressDTO.getState() + " " 
						+ personAddressDTO.getZip() ;
		docClinicName = personAddressDTO.getLocation();
		}
		
		
		List<IdentificationDTO> doctorIdentificationList = new ArrayList<>();
				when(m_personDTO.getIdentifications()).thenReturn(doctorIdentificationList);
		IdentificationDTO dtos = new IdentificationDTO();
		
		dtos.setId("ide");  
         dtos.setType("HPI");
         dtos.setIdentification("identi");
        doctorIdentificationList.add(dtos);
        
		for (IdentificationDTO identificationDTO : doctorIdentificationList) {
			when(m_identificationDTO.getType()).thenReturn("ID_QUAL_DEA_NUMBER");
			when(m_identificationDTO.getIdentification()).thenReturn("identi");
			newPrescriptionBIRTUrl.append(AMP + DOC_DEA + URLEncoder.encode(""
					+m_identificationDTO.getIdentification(), "UTF-8"));
			
		 	when(m_identificationDTO.getType()).thenReturn("ID_QUAL_NPI");
			when(m_identificationDTO.getIdentification()).thenReturn("identi");
		}
		method.invoke(i_refillAuthorizeSearchAction,  m_request, m_form, generatedRxIds);
       
	}
	 
	 @Test 
	   void testGetCurrentRxHubUniqueId() throws Exception {
		 Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("getCurrentRxHubUniqueId" ,  HttpServletRequest.class,PersonPrescriptionDTO.class, String.class);
			method.setAccessible(true);
			 when(m_request.getSession()).thenReturn(m_session);
		 	    when(m_session.getAttribute("session.user.HTTPSession")).thenReturn(m_userLoginInfoDTO);
			
			String patientId = "123";
			String rxHubPBMUniqueId = "";
			List<RxhubMemberIdDTO> rxHubPBMUniqueIdList = new ArrayList<>();
			m_rxhubMemberIdDTO.setMemberUniqueId("4567");	
			rxHubPBMUniqueIdList.add(m_rxhubMemberIdDTO);
			when(m_userLoginInfoDTO.getPracticeId()).thenReturn("67");
			when(m_statelessSearchFacadeLocal.getRxHubMemberId(patientId, m_userLoginInfoDTO.getPracticeId())).thenReturn(rxHubPBMUniqueIdList);
			m_userLoginInfoDTO.setPatientId(patientId); 
			for (RxhubMemberIdDTO rxhubMemberIdDTO : rxHubPBMUniqueIdList) {
				when(m_personPrescriptionDTO.getRxHubPBMUniqueId()).thenReturn("4567");
				when(rxhubMemberIdDTO.getMemberUniqueId()).thenReturn("4567");
				rxHubPBMUniqueId = m_personPrescriptionDTO.getRxHubPBMUniqueId();
			}
			m_userLoginInfoDTO.setRxHubMemberID(rxHubPBMUniqueId);

			
		String rxHubPBMUniqueIda = (String) method.invoke(i_refillAuthorizeSearchAction,  m_request, m_personPrescriptionDTO, patientId);
		assertEquals(rxHubPBMUniqueIda,"4567");   
	   }
	 
	
	 @Test 
	 void  testCheckTypeAndShowStatus() throws Exception {
		 Method method = RefillAuthorizeSearchAction.class.getDeclaredMethod("checkTypeAndShowStatus" ,  HttpServletRequest.class, Map.class, ShieldLocal.class, UserLoginInfoDTO.class,
			String.class, PersonPrescriptionDTO.class, String.class, String.class,String.class, int.class, String.class, PersonDTO.class,String.class, ArrayList.class, String.class, PersonRefillDTO.class);
			method.setAccessible(true);
			Map<String, PersonPrescriptionDTO> unprocessed = new HashMap<String, PersonPrescriptionDTO>();
			unprocessed.put("one", m_personPrescriptionDTO);
			String type = "status_res";
			System.out.println(type.substring(0, 6).equalsIgnoreCase(STATUS_RES));
			String rxReferenceNumber = "34";
			String relatesId = "44";
			String rxMessageNumber = "1"; 
			int i = 7;
			String refillAction = "RX_DENIED_NEW_RX";
			String rawSSMessageId = "id";
			ArrayList<String> refillList = new ArrayList<String>(); 
			refillList.add("aa");
			String pharmacyType = "pharmacy"; 
			String[] errorDesc = {"one","two"};
			String statusCode = type.substring(0, 6).concat(" ").concat(type.substring(6, 9));
			m_request.setAttribute("sendRefillElectronically", refillList);
			unprocessed.put(String.valueOf(i + 1), m_personPrescriptionDTO);
			//when(m_personPrescriptionDTO.getAuthorizedRefillsId()).thenReturn("value");
			// when(m_statelessSearchFacadeLocal.getPersonRefillByRefillId(m_personPrescriptionDTO.getAuthorizedRefillsId())).thenReturn(m_personRefillDTO);
			//Identify the person_refill_p in the case of DENIED_NEW_RX
		
				//m_personRefillDTO.setId(m_personRefillDTO.getId());
			method.invoke(i_refillAuthorizeSearchAction, m_request, unprocessed, m_shieldLocal, m_userLoginInfoDTO, type, m_personPrescriptionDTO, rxReferenceNumber,relatesId,
					rxMessageNumber,i,refillAction, m_personDTO, rawSSMessageId, refillList, pharmacyType, m_personRefillDTO );
	 }
	
	}

