package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TwoPassParamParserTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testBasicPositionalParams() throws IllegalCommandLineArgumentException {
        String[] args = {"bi", "class info"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertNotNull("Request should not be null", request);
        assertEquals("bi", request.getName());
        assertEquals("class info", request.getCommand());
    }

    @Test
    public void testFlagsAtBeginning() throws IllegalCommandLineArgumentException {
        String[] args = {"-v", "-f", "bi", "class info"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertTrue("verbose should be true", request.isVerbose());
        assertTrue("force should be true", request.isForce());
        assertEquals("bi", request.getName());
        assertEquals("class info", request.getCommand());
    }

    @Test
    public void testFlagsAtEnd() throws IllegalCommandLineArgumentException {
        String[] args = {"bi", "class info", "--verbose"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertEquals("bi", request.getName());
        assertEquals("class info", request.getCommand());
        assertTrue("verbose should be true", request.isVerbose());
        assertFalse("force should be false by default", request.isForce());
    }

    @Test
    public void testFlagsInMiddle() throws IllegalCommandLineArgumentException {
        String[] args = {"bi", "-v", "--force", "class info"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertEquals("bi", request.getName());
        assertTrue("verbose should be true", request.isVerbose());
        assertTrue("force should be true", request.isForce());
        assertEquals("class info", request.getCommand());
    }

    @Test
    public void testLongFormFlags() throws IllegalCommandLineArgumentException {
        String[] args = {"--verbose", "--force", "bi", "cmd"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertTrue("verbose should be true (long form)", request.isVerbose());
        assertTrue("force should be true (long form)", request.isForce());
    }

    @Test
    public void testKeywordParam() throws IllegalCommandLineArgumentException {
        String[] args = {"bi", "cmd", "--description=This is a test alias"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertEquals("bi", request.getName());
        assertEquals("cmd", request.getCommand());
        assertEquals("This is a test alias", request.getDescription());
    }

    @Test
    public void testKeywordParamAtBeginning() throws IllegalCommandLineArgumentException {
        String[] args = {"--description=test_description", "bi", "cmd"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertEquals("test_description", request.getDescription());
        assertEquals("bi", request.getName());
    }

    @Test
    public void testMixedAllTypes() throws IllegalCommandLineArgumentException {
        String[] args = {
            "-v",
            "bi",
            "--description=complex_test",
            "--force",
            "class info with spaces"
        };
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertTrue("verbose flag should be true", request.isVerbose());
        assertTrue("force flag should be true", request.isForce());
        assertEquals("bi", request.getName());
        assertEquals("class info with spaces", request.getCommand());
        assertEquals("complex_test", request.getDescription());
    }

    @Test
    public void testDefaultFlagValues() throws IllegalCommandLineArgumentException {
        String[] args = {"bi", "cmd"};
        
        TestAliasAddRequest request = ParamParser.parse(TestAliasAddRequest.class, args);
        
        assertFalse("verbose should default to false", request.isVerbose());
        assertFalse("force should default to false", request.isForce());
        assertNull("description should default to null", request.getDescription());
    }

    @Test(expected = IllegalCommandLineArgumentException.class)
    public void testMissingRequiredParam() throws IllegalCommandLineArgumentException {
        String[] args = {};
        
        ParamParser.parse(TestAliasAddRequest.class, args);
    }
}
