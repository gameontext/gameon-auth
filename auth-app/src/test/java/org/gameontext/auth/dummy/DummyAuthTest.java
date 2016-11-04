package org.gameontext.auth.dummy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gameontext.auth.JwtAuth;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

public class DummyAuthTest {

    @Rule
    public TestName test = new TestName();
    
    @Mocked
    JwtAuth jwtAuth;

    @Before
    public void before() {
        System.out.println("\n====== " + test.getMethodName());

    }
    
    @Test
    public void testDevelopmentMode(@Mocked HttpServletRequest request,  @Mocked HttpServletResponse response) throws Exception {
        new Expectations() {{
            request.getParameter("callbackHost"); result = "developmentMode";
        }};

        DummyAuth auth = new DummyAuth();
        
        // CDI injected resources (post constructor)
        Deencapsulation.setField(auth, "callbackSuccess", "/something/#/success");
        Deencapsulation.setField(auth, "developmentMode", "development");

        // @PostConstruct validation
        Deencapsulation.invoke(auth, "verifyInit");
        auth.doGet(request, response);
        
        // Make sure all is well
        new Verifications() {{
            String url;
            Assert.assertEquals("Callback fragment should be set", "#/success", Deencapsulation.getField(auth, "callbackFragment"));
            response.sendRedirect(url = withCapture()); times = 1;
            
            Assert.assertEquals("Redirect string should be assempled from other parts", "developmentMode/#/success/null", url);
        }};
    }

    @Test
    public void testProductionMode(@Mocked HttpServletRequest request,  @Mocked HttpServletResponse response) throws Exception {
        DummyAuth auth = new DummyAuth();
        
        // CDI injected resources (post constructor)
        Deencapsulation.setField(auth, "callbackSuccess", "/something/#/success");
        // production is default
        
        // @PostConstruct validation
        Deencapsulation.invoke(auth, "verifyInit");
        auth.doGet(request, response);
        
        // Make sure all is well
        new Verifications() {{
            String url;
            Assert.assertEquals("Callback fragment should be set", "#/success", Deencapsulation.getField(auth, "callbackFragment"));
            response.sendRedirect(url = withCapture()); times = 1;
            
            Assert.assertEquals("Redirect string should be exactly what was passed in", "/something/#/success/null", url);
        }};
    }
}
