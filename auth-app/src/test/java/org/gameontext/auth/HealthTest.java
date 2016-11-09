/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.auth;

import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;


public class HealthTest {
    @Mocked
    Context context;
	
	@Mocked
    Kafka kafka;

    @Rule
    public TestName test = new TestName();

    @Before
    public void before() throws NamingException {
        System.out.println("\n====== " + test.getMethodName());
        System.setProperty("java.naming.factory.initial", MockInitialContextFactory.class.getName());
        MockInitialContextFactory.setContext(context);
    }
    
    @Test
    public void testAllIsWell() throws Exception {
        new Expectations() {{
            context.lookup("facebookAppID"); result = "facebookAppID";
            context.lookup("facebookSecret"); result = "facebookSecret";
            context.lookup("gitHubOAuthKey"); result = "gitHubOAuthKey";
            context.lookup("gitHubOAuthSecret");result = "gitHubOAuthSecret";
            context.lookup("googleOAuthConsumerKey"); result = "googleOAuthConsumerKey";
            context.lookup("googleOAuthConsumerSecret"); result = "googleOAuthConsumerSecret";
            context.lookup("twitterOAuthConsumerKey"); result = "twitterOAuthConsumerKey";
            context.lookup("twitterOAuthConsumerSecret"); result = "twitterOAuthConsumerSecret";
        }};

        Health health = new Health();
        
        // CDI injected resources (post constructor)
        Deencapsulation.setField(health, "callbackFailure", "/something/#/fail");
        Deencapsulation.setField(health, "callbackSuccess", "/something/#/success");
        Deencapsulation.setField(health, "kafka", kafka);

        // @PostConstruct validation
        Deencapsulation.invoke(health, "verifyInit");
        
        // Make sure all is well
        new Verifications() {{
            Assert.assertTrue("All should be well: " + health.toString(), Deencapsulation.getField(health, "allIsWell"));
            Assert.assertEquals("Development mode should default to 'production'", "production", Deencapsulation.getField(health, "developmentMode"));
        }};
    }

    @Test
    public void testAuthDevelopmentModeOk() throws Exception {
        // In development mode, sign-on keys can be missing
        Health health = new Health();
        
        // CDI injected resources (post constructor)
        Deencapsulation.setField(health, "callbackFailure", "/something/#/fail");
        Deencapsulation.setField(health, "callbackSuccess", "/something/#/success");
        Deencapsulation.setField(health, "developmentMode", "development");
        Deencapsulation.setField(health, "kafka", kafka);

        // @PostConstruct validation
        Deencapsulation.invoke(health, "verifyInit");
        
        // Make sure all is well
        new Verifications() {{
            Assert.assertTrue("All should be well: " + health.toString(), Deencapsulation.getField(health, "allIsWell"));
        }};
    }
    
    @Test
    public void testAuthOtherModeFails() throws Exception {
        // In development mode, sign-on keys can be missing
        Health health = new Health();
        
        // CDI injected resources (post constructor)
        Deencapsulation.setField(health, "callbackFailure", "/something/#/fail");
        Deencapsulation.setField(health, "callbackSuccess", "/something/#/success");
        Deencapsulation.setField(health, "developmentMode", "production");
        Deencapsulation.setField(health, "kafka", kafka);

        // @PostConstruct validation
        Deencapsulation.invoke(health, "verifyInit");
        
        String result = health.toString();
        new Verifications() {{
            Assert.assertFalse("All should not be well: " + health.toString(), Deencapsulation.getField(health, "allIsWell"));
            Assert.assertTrue(result.contains("facebookAppID=false"));
            Assert.assertTrue(result.contains("facebookSecret=false"));
            Assert.assertTrue(result.contains("gitHubOAuthKey=false"));
            Assert.assertTrue(result.contains("gitHubOAuthSecret=false"));
            Assert.assertTrue(result.contains("googleOAuthConsumerKey=false"));
            Assert.assertTrue(result.contains("googleOAuthConsumerSecret=false"));
            Assert.assertTrue(result.contains("twitterOAuthConsumerKey=false"));
            Assert.assertTrue(result.contains("twitterOAuthConsumerSecret=false"));
        }};
    }

    @Test
    public void testMissingRequiredPieces() throws Exception {
        Health health = new Health();
        
        // CDI injected resources (post constructor)
        Deencapsulation.setField(health, "developmentMode", "development");

        // @PostConstruct validation
        Deencapsulation.invoke(health, "verifyInit");
        
        String result = health.toString();
        new Verifications() {{
            Assert.assertFalse("All should not be well: " + health.toString(), Deencapsulation.getField(health, "allIsWell"));
            Assert.assertTrue(result.contains("authCallbackURLSuccess=false"));
            Assert.assertTrue(result.contains("authCallbackURLFailure=false"));
            Assert.assertTrue(result.contains("kafka=false"));
        }};
    }
}
