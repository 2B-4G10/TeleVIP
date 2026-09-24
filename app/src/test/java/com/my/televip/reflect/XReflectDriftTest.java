package com.my.televip.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Locks down how far a hook is allowed to bend when Telegram moves something.
 *
 * <p>These rules are a safety boundary, not a convenience: attaching a privacy feature to the
 * wrong overload is worse than leaving that feature off, because it looks like it works. Loosening
 * anything here should be a deliberate decision, so each rule has a test.</p>
 *
 * <p>{@link XReflect} is plain reflection with no Android dependency, so this runs on the JVM.</p>
 */
public class XReflectDriftTest {

    /** The shape a call site was written against. */
    static class Original {
        public boolean allowScreenshots() {
            return true;
        }
    }

    /** The same method, one client release later, having gained a parameter. */
    static class GrewAParameter {
        public boolean allowScreenshots(boolean forced) {
            return true;
        }
    }

    static class Overloaded {
        public void foo(int a) {
        }

        public void foo(String a) {
        }
    }

    static class OneParameter {
        public void setListener(Runnable listener) {
        }
    }

    static class Parent {
        public void shared() {
        }
    }

    static class Child extends Parent {
        @Override
        public void shared() {
        }
    }

    // ------------------------------------------------------------ exact first

    @Test
    public void exactMatchStillWins() {
        assertNotNull(XReflect.findMethodExactIfExists(Original.class, "allowScreenshots"));
    }

    @Test
    public void exactMatchFailsOnceTheSignatureGrew() {
        assertNull(XReflect.findMethodExactIfExists(GrewAParameter.class, "allowScreenshots"));
    }

    // ------------------------------------------------------- tolerated drift

    @Test
    public void aNoArgumentCallSiteAcceptsAnyUniqueSignature() {
        // The call site asked for no parameters, so its callback cannot be reading param.args.
        Method method =
                XReflect.findMethodCompatibleIfExists(GrewAParameter.class, "allowScreenshots");
        assertNotNull(method);
        assertEquals(1, method.getParameterTypes().length);
    }

    @Test
    public void anUnresolvableParameterTypeIsAWildcard() {
        // ClassLoad returns null for a class the client renamed; arity still has to match.
        assertNotNull(XReflect.findMethodCompatibleIfExists(
                OneParameter.class, "setListener", new Class<?>[]{null}));
    }

    @Test
    public void anInheritedMethodIsFound() {
        assertNotNull(XReflect.findMethodExactIfExists(Child.class, "shared"));
    }

    @Test
    public void anOverrideIsNotCountedAsASecondCandidate() {
        assertNotNull(XReflect.findMethodCompatibleIfExists(Child.class, "shared"));
    }

    // ----------------------------------------------------------- refused guesses

    @Test
    public void ambiguousOverloadsAreRefused() {
        assertNull(XReflect.findMethodCompatibleIfExists(Overloaded.class, "foo"));
    }

    @Test
    public void twoCandidatesOfTheRequestedArityAreRefused() {
        assertNull(XReflect.findMethodCompatibleIfExists(
                Overloaded.class, "foo", new Class<?>[]{null}));
    }

    @Test
    public void arityIsEnforcedSoArgumentPositionsStayAligned() {
        assertNull(XReflect.findMethodCompatibleIfExists(
                OneParameter.class, "setListener", new Class<?>[]{null, null}));
    }

    @Test
    public void aTypeThatIsKnownAndWrongIsRefused() {
        assertNull(XReflect.findMethodCompatibleIfExists(
                OneParameter.class, "setListener", new Class<?>[]{String.class}));
    }

    @Test
    public void aNameThatDoesNotExistIsNotInvented() {
        assertNull(XReflect.findMethodCompatibleIfExists(Original.class, "gone"));
    }
}
