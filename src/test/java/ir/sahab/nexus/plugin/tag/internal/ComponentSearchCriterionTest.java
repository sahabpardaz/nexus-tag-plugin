package ir.sahab.nexus.plugin.tag.internal;

import static org.junit.Assert.*;

import ir.sahab.nexus.plugin.tag.internal.ComponentSearchCriterion.Operator;
import ir.sahab.nexus.plugin.tag.internal.ComponentSearchCriterion.Version;
import ir.sahab.nexus.plugin.tag.internal.dto.AssociatedComponent;
import org.junit.Test;

public class ComponentSearchCriterionTest {

    @Test
    public void testParse() {
        ComponentSearchCriterion criterion = ComponentSearchCriterion.parse("r1:g1:n1 > 1");
        assertEquals("r1", criterion.getRepository());
        assertEquals("g1", criterion.getGroup());
        assertEquals("n1", criterion.getName());
        assertEquals(Operator.GT, criterion.getVersionOperator());
        assertEquals("1", criterion.getVersionValue());

        criterion = ComponentSearchCriterion.parse("r2::n2 =< 10.1");
        assertEquals("r2", criterion.getRepository());
        assertEquals("", criterion.getGroup());
        assertEquals("n2", criterion.getName());
        assertEquals(Operator.LTE, criterion.getVersionOperator());
        assertEquals("10.1", criterion.getVersionValue());
    }

    @Test
    public void testMatches() {
        ComponentSearchCriterion criterion = ComponentSearchCriterion.parse("r1:g1:n1 > 1");
        assertTrue(criterion.matches(new AssociatedComponent("r1", "g1", "n1", "2")));
        assertFalse(criterion.matches(new AssociatedComponent("r2", "g1", "n1", "2")));
        assertFalse(criterion.matches(new AssociatedComponent("r1", "", "n1", "2")));
        assertFalse(criterion.matches(new AssociatedComponent("r1", "g1", "n2", "2")));

        criterion = ComponentSearchCriterion.parse("r1:g1:n1 = 3");
        assertFalse(criterion.matches(new AssociatedComponent("r1", "g1", "n1", "2")));
        assertTrue(criterion.matches(new AssociatedComponent("r1", "g1", "n1", "3")));
    }

    @Test
    public void testVersionCompare() {
        assertTrue(new Version("1.0.0").compare(Operator.EQ, new Version("1.0.0")));

        assertTrue(new Version("1.0.0").compare(Operator.GTE, new Version("1.0.0")));
        assertTrue(new Version("1.0.1").compare(Operator.GT, new Version("1.0.0")));
        assertFalse(new Version("1.0.0").compare(Operator.GT, new Version("1.0.0")));

        assertTrue(new Version("1.0.0").compare(Operator.LTE, new Version("1.0.0")));
        assertTrue(new Version("1.0.0").compare(Operator.LT, new Version("1.1.0")));
        assertFalse(new Version("1.0.0").compare(Operator.LT, new Version("1.0.0")));

    }
}