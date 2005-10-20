/*
 * Created on 30.09.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.ingrid.ibus.cswinterface.analyse;

import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPMessage;

import de.ingrid.ibus.cswinterface.TestRequests;
import de.ingrid.ibus.cswinterface.exceptions.CSWInvalidParameterValueException;
import de.ingrid.ibus.cswinterface.exceptions.CSWMissingParameterValueException;
import de.ingrid.ibus.cswinterface.exceptions.CSWOperationNotSupportedException;
import de.ingrid.ibus.cswinterface.tools.AxisTools;
import junit.framework.TestCase;

/**
 * @author rschaefer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class GetRecAnalyserTest extends TestCase {

    /*
     * Class under test for boolean analyse(SOAPBodyElement)
     */
    public final void testAnalyseSOAPBodyElement() throws Exception {
      
        
        boolean getRecRequestValid = false;
        
        SOAPMessage soapMessageRequest = null;
        
        SessionParameters sessionParameters = new SessionParameters();
        
        GetRecAnalyser getRecAnalyser = new GetRecAnalyser(sessionParameters);
        
        //TEST1
        soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETREC1);
        
        getRecRequestValid = getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));
       
        assertTrue(getRecRequestValid);
        
        assertEquals("results", sessionParameters.getResultType());
        
        assertEquals("csw:profile", sessionParameters.getOutputSchema());
        
        assertEquals(2, sessionParameters.getStartPosition());
        
        assertEquals(20, sessionParameters.getMaxRecords());
        
        assertEquals("csw:dataset", sessionParameters.getTypeNames());
        
        assertEquals("brief", sessionParameters.getElementSetName());
        
        assertNotNull(sessionParameters.getSoapElementFilter());
        
       
        
        //TEST2
        soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETREC2);
        
        getRecRequestValid = getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));
       
        assertTrue(getRecRequestValid);
        
       
  
        
        //TEST3
        soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETRECINVALID1);
        
        try {
            
           getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));
       
           fail("CSWOperationNotSupportedException expected.");
        
      } catch (CSWOperationNotSupportedException e) {
          //expected
          
          assertEquals("GetRecord", e.getLocator());
        
      }
        
      
      //TEST4
      soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETRECINVALID2);
      
      try {
          
         getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));
     
         fail("CSWInvalidParameterValueException expected.");
      
    } catch (CSWInvalidParameterValueException e) {
        //expected
      
        assertEquals("service", e.getLocator());
    }
      
        
    
//  TEST5
    soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETRECINVALID3);
    
    try {
        
       getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));
   
       fail("CSWInvalidParameterValueException expected.");
    
  } catch (CSWInvalidParameterValueException e) {
      //expected
    
      assertEquals("version", e.getLocator());
  }
    
  
//TEST6
  soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETRECINVALID4);
  
  try {
      
     getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));
 
     fail("CSWMissingParameterValueException expected.");
  
} catch (CSWMissingParameterValueException e) {
    //expected
  
    assertEquals("Query", e.getLocator());
} 
    
    

//TEST7
soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETRECINVALID5);

try {
    
   getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));

   fail("CSWMissingParameterValueException expected.");

} catch (CSWMissingParameterValueException e) {
  //expected
    assertEquals("typeNames", e.getLocator());
    
} 


//TEST7
soapMessageRequest = AxisTools.createSOAPMessage(TestRequests.GETRECINVALID6);

try {
    
   getRecAnalyser.analyse(((SOAPBodyElement) soapMessageRequest.getSOAPBody().getFirstChild()));

   fail("CSWMissingParameterValueException expected.");

} catch (CSWMissingParameterValueException e) {
  //expected

    assertEquals("Filter", e.getLocator());
} 


        
}

    /*
     * Class under test for boolean analyse(String)
     */
    public final void testAnalyseKVPString() throws Exception {
        //TODO Implement analyse().
    }

}