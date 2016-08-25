package org.tap4j.plugin.issue17947;

import junit.framework.TestCase;

import org.tap4j.model.TestSet;
import org.tap4j.parser.Tap13Parser;

public class TestIssue17947 extends TestCase {

    public void testSubtestsIssue17947() {
        // tap stream provided by issue reporter
        String tap = "1..3\n" + 
                "    1..1\n" + 
                "    ok 1 - subtest 1\n" + 
                "ok 1 - test 1\n" + 
                "    1..4\n" + 
                "    ok 1 - subtest 1\n" + 
                "    ok 2 - subtest 2\n" + 
                "    ok 3 - subtest 3\n" + 
                "    ok 4 - subtest 4\n" + 
                "ok 2 - test 2\n" + 
                "    1..15\n" + 
                "    Bail out!\n" + 
                "    not ok 1 - test 3";
        
        Tap13Parser parser = new Tap13Parser(true);
        TestSet ts = parser.parseTapStream(tap);
        System.out.println(ts.getNumberOfTestResults());
    }
    
}
