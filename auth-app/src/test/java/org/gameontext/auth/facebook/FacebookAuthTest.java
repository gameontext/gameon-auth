package org.gameontext.auth.facebook;

import mockit.Deencapsulation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class FacebookAuthTest {

    @Rule
    public TestName test = new TestName();

    @Before
    public void before() {
        System.out.println("\n====== " + test.getMethodName());
    }

    @Test
    public void testGetQueryString() throws Exception {
        String accessTokenRespose = "{\"access_token\":\"abc\",\"token_type\":\"bearer\",\"expires_in\":123}";
        String queryString = Deencapsulation.invoke(FacebookCallback.class, "getQueryString", accessTokenRespose);
        Assert.assertEquals(queryString, "access_token=abc&expires=123");
    }
}