package com.intuit.karate;

import com.intuit.karate.core.Engine;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureParser;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.exception.KarateException;
import com.intuit.karate.runtime.FeatureRuntime;
import java.io.File;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
@KarateOptions(tags = {"~@ignore"})
class RunnerTest {

    static final Logger logger = LoggerFactory.getLogger(RunnerTest.class);

    boolean contains(String reportPath, String textToFind) {
        String contents = FileUtils.toString(new File(reportPath));
        return contents.contains(textToFind);
    }

    static String resultXml(String name) {
        Feature feature = FeatureParser.parse("classpath:com/intuit/karate/" + name);
        FeatureRuntime fr = new FeatureRuntime(new SuiteRuntime(), feature, null);
        fr.run();
        File file = Engine.saveResultXml("target", fr.result, null);
        return FileUtils.toString(file);
    }

    @Test
    void testScenario() throws Exception {
        String contents = resultXml("core/scenario.feature");
        assertTrue(contents.contains("Then match b == { foo: 'bar'}"));
    }

    @Test
    void testScenarioOutline() throws Exception {
        String contents = resultXml("core/outline.feature");
        assertTrue(contents.contains("When def a = 55"));
    }

    @Test
    void testParallel() {
        Results results = Runner.path(
                "classpath:com/intuit/karate/multi-scenario-fail.feature",
                "classpath:com/intuit/karate/no-scenario-name.feature",
                "classpath:com/intuit/karate/core/scenario.feature",
                "classpath:com/intuit/karate/core/outline.feature",
                "classpath:com/intuit/karate/core/stackoverflow-error.feature"
        ).parallel(1);
        assertEquals(3, results.getFailCount());
        String pathBase = "target/surefire-reports/com.intuit.karate.";
        assertTrue(contains(pathBase + "core.scenario.xml", "Then match b == { foo: 'bar'}"));
        assertTrue(contains(pathBase + "core.outline.xml", "Then assert a == 55"));
        // a scenario failure should not stop other features from running
        assertTrue(contains(pathBase + "multi-scenario-fail.xml", "Then assert a != 2 ........................................................ passed"));
        assertEquals(3, results.getFailedMap().size());
        assertTrue(results.getFailedMap().keySet().contains("com.intuit.karate.no-scenario-name"));
        assertTrue(results.getFailedMap().keySet().contains("com.intuit.karate.multi-scenario-fail"));
        assertTrue(results.getFailedMap().keySet().contains("com.intuit.karate.core.stackoverflow-error"));
    }

    @Test
    void testRunningFeatureFromJavaApi() {
        Map<String, Object> result = Runner.runFeature(getClass(), "core/scenario.feature", null, true);
        assertEquals(1, result.get("a"));
        Map<String, Object> temp = (Map) result.get("b");
        assertEquals("bar", temp.get("foo"));
        assertEquals("normal", result.get("configSource"));
    }

    @Test
    void testRunningFeatureFailureFromJavaApi() {
        try {
            Runner.runFeature(getClass(), "multi-scenario-fail.feature", null, true);
            fail("expected exception to be thrown");
        } catch (Exception e) {
            assertTrue(e instanceof KarateException);
        }
    }

    @Test
    void testRunningFeatureFailureFromRunner() {
        Results results = Runner.path("classpath:com/intuit/karate/multi-scenario-fail.feature").parallel(1);
        assertEquals(1, results.getFailCount());
    }

    @Test
    void testRunningRelativePathFeatureFromJavaApi() {
        Map<String, Object> result = Runner.runFeature("classpath:com/intuit/karate/test-called.feature", null, true);
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals("normal", result.get("configSource"));
    }

    @Test
    void testCallerArg() throws Exception {
        String contents = resultXml("caller-arg.feature");
        assertFalse(contents.contains("failed"));
        assertTrue(contents.contains("* def result = call read('called-arg-null.feature')"));
    }

}
