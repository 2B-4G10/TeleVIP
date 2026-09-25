package com.my.televip.obfuscate.resolve;

import com.my.televip.obfuscate.dex.CodeScanner;
import com.my.televip.obfuscate.dex.DexClass;
import com.my.televip.obfuscate.dex.DexFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A fact about what a method's code does - which methods it calls, which fields it touches, which
 * strings and constants it loads. Used to tell apart members whose signatures alone are identical.
 *
 * <p>Facts are phrased in terms of other symbols' original names where possible, so they describe
 * the source rather than one build: "calls getStoriesFromFullPeer", not "calls y". A fact that
 * mentions a symbol not resolved yet answers null, which the resolver treats as "ask again later".</p>
 */
public interface Body {

    /** True or false, or null when it depends on something not resolved yet. */
    Boolean test(Resolver r, DexClass.Method method);

    // ------------------------------------------------------------- combinators

    static Body all(final Body... facts) {
        return (r, m) -> {
            for (Body f : facts) {
                Boolean b = f.test(r, m);
                if (b == null) return null;
                if (!b) return false;
            }
            return true;
        };
    }

    static Body not(final Body fact) {
        return (r, m) -> {
            Boolean b = fact.test(r, m);
            return b == null ? null : !b;
        };
    }

    // ------------------------------------------------------------------ facts

    /** Loads the given string constant. */
    static Body string(final String value) {
        return (r, m) -> Refs.of(r, m).strings.contains(value);
    }

    /** Loads the given integer constant. */
    static Body constant(final long value) {
        return (r, m) -> Refs.of(r, m).constants.contains(value);
    }

    /** Calls a method of its own class with this signature. */
    static Body callsSibling(final String returnType, final String... params) {
        return (r, m) -> {
            String proto = r.proto(returnType, params);
            if (proto == null) return null;
            for (Refs.Call c : Refs.of(r, m).calls) {
                if (c.owner.equals(m.owner.descriptor) && c.proto.equals(proto)) return true;
            }
            return false;
        };
    }

    /** Calls a method of {@code owner} named {@code name} (a name the build keeps), any signature. */
    static Body callsNamed(final String owner, final String name) {
        return (r, m) -> {
            String ownerDesc = r.descriptor(owner);
            if (ownerDesc == null) return null;
            for (Refs.Call c : Refs.of(r, m).calls) {
                if (c.owner.equals(ownerDesc) && c.name.equals(name)) return true;
            }
            return false;
        };
    }

    /** Calls a method of {@code owner} with this signature, whatever it is named. */
    static Body calls(final String owner, final String returnType, final String... params) {
        return (r, m) -> {
            String ownerDesc = r.descriptor(owner);
            String proto = r.proto(returnType, params);
            if (ownerDesc == null || proto == null) return null;
            for (Refs.Call c : Refs.of(r, m).calls) {
                if (c.owner.equals(ownerDesc) && c.proto.equals(proto)) return true;
            }
            return false;
        };
    }

    /** Calls the method another symbol resolved to, e.g. {@code "ChatActivity#createView"}. */
    static Body callsSymbol(final String methodSymbolId) {
        return (r, m) -> {
            DexClass.Method target = r.methods.get(methodSymbolId);
            if (target == null) return null;
            String proto = Resolver.protoOf(target);
            for (Refs.Call c : Refs.of(r, m).calls) {
                if (c.owner.equals(target.owner.descriptor) && c.name.equals(target.name())
                        && c.proto.equals(proto)) return true;
            }
            return false;
        };
    }

    /** Reads or writes a field of {@code owner} named {@code name} (a name the build keeps). */
    static Body touchesField(final String owner, final String name) {
        return (r, m) -> {
            String ownerDesc = r.descriptor(owner);
            if (ownerDesc == null) return null;
            for (Refs.FieldRef f : Refs.of(r, m).fields) {
                if (f.owner.equals(ownerDesc) && f.name.equals(name)) return true;
            }
            return false;
        };
    }

    /** Reads or writes some field of {@code owner} whose type is {@code type}. */
    static Body touchesFieldOfType(final String owner, final String type) {
        return (r, m) -> {
            String ownerDesc = r.descriptor(owner);
            String typeDesc = r.descriptor(type);
            if (ownerDesc == null || typeDesc == null) return null;
            for (Refs.FieldRef f : Refs.of(r, m).fields) {
                if (f.owner.equals(ownerDesc) && f.type.equals(typeDesc)) return true;
            }
            return false;
        };
    }

    /** Calls any method with this name, on any class - for names the platform fixes. */
    static Body callsAnyNamed(final String name) {
        return (r, m) -> {
            for (Refs.Call c : Refs.of(r, m).calls) if (c.name.equals(name)) return true;
            return false;
        };
    }

    /** Calls no method at all - a plain getter or setter. */
    static Body callsNothing() {
        return (r, m) -> Refs.of(r, m).calls.isEmpty();
    }

    /**
     * Writes a field of {@code listenerType} on {@code owner} that some method of {@code owner}
     * reads and then invokes with the integer constant {@code which} - e.g. the AlertDialog
     * listener that gets called with BUTTON_POSITIVE (-1). Pins a setter to what it means rather
     * than to what R8 happened to call it.
     */
    static Body writesListenerInvokedWith(final String owner, final String listenerType, final int which) {
        return (r, m) -> {
            DexClass ownerClass = r.cls(owner);
            String listenerDesc = r.descriptor(listenerType);
            if (ownerClass == null || listenerDesc == null) return null;
            Set<String> invokedWithWhich = new HashSet<>();
            for (DexClass.Method candidate : ownerClass.methods) {
                Refs refs = Refs.of(r, candidate);
                if (!refs.constants.contains((long) which)) continue;
                boolean callsListener = false;
                for (Refs.Call call : refs.calls) {
                    if (call.owner.equals(listenerDesc)) callsListener = true;
                }
                if (!callsListener) continue;
                Set<String> read = new HashSet<>();
                for (Refs.FieldRef f : refs.fields) {
                    if (!f.write && f.type.equals(listenerDesc)) read.add(f.owner + "." + f.name);
                }
                if (read.size() == 1) invokedWithWhich.addAll(read);   // one listener per handler
            }
            for (Refs.FieldRef f : Refs.of(r, m).fields) {
                if (f.write && f.type.equals(listenerDesc) && invokedWithWhich.contains(f.owner + "." + f.name)) return true;
            }
            return false;
        };
    }

    /** Has a switch with this case key. */
    static Body switchKey(final int key) {
        return (r, m) -> Refs.of(r, m).switchKeys.contains(key);
    }

    /**
     * Writes a field of this type that another (resolved) method also writes. Ties a renamed
     * setter to the field a kept method is known to use, e.g. Builder.setTitle to whatever field
     * the dialog's own framework-named setTitle writes.
     */
    static Body writesFieldWrittenBy(final String methodSymbolId, final String type) {
        return (r, m) -> {
            DexClass.Method other = r.methods.get(methodSymbolId);
            String typeDesc = r.descriptor(type);
            if (other == null || typeDesc == null) return null;
            Set<String> theirs = new HashSet<>();
            for (Refs.FieldRef f : Refs.of(r, other).fields) {
                if (f.write && f.type.equals(typeDesc)) theirs.add(f.owner + "." + f.name);
            }
            for (Refs.FieldRef f : Refs.of(r, m).fields) {
                if (f.write && f.type.equals(typeDesc) && theirs.contains(f.owner + "." + f.name)) return true;
            }
            return false;
        };
    }

    /** Writes some field of {@code owner} whose type is {@code type}. */
    static Body writesFieldOfType(final String owner, final String type) {
        return (r, m) -> {
            String ownerDesc = r.descriptor(owner);
            String typeDesc = r.descriptor(type);
            if (ownerDesc == null || typeDesc == null) return null;
            for (Refs.FieldRef f : Refs.of(r, m).fields) {
                if (f.write && f.owner.equals(ownerDesc) && f.type.equals(typeDesc)) return true;
            }
            return false;
        };
    }

    /** new-instance, check-cast, instance-of or const-class of the given type. */
    static Body usesType(final String type) {
        return (r, m) -> {
            String desc = r.descriptor(type);
            if (desc == null) return null;
            return Refs.of(r, m).types.contains(desc);
        };
    }

    // ------------------------------------------------------------------- refs

    /** Everything one scan of a method yields, cached per method for the life of a resolution. */
    final class Refs {
        static final class Call {
            final String owner, name, proto;

            Call(String owner, String name, String proto) {
                this.owner = owner;
                this.name = name;
                this.proto = proto;
            }
        }

        static final class FieldRef {
            final String owner, name, type;
            final boolean write;

            FieldRef(String owner, String name, String type, boolean write) {
                this.owner = owner;
                this.name = name;
                this.type = type;
                this.write = write;
            }
        }

        final List<Call> calls = new ArrayList<>();
        final List<FieldRef> fields = new ArrayList<>();
        final Set<String> strings = new HashSet<>();
        final Set<Long> constants = new HashSet<>();
        final Set<String> types = new HashSet<>();
        final List<String> newInstances = new ArrayList<>();
        final Set<Integer> switchKeys = new HashSet<>();

        static Refs of(Resolver r, DexClass.Method method) {
            Refs cached = r.refs.get(method);
            if (cached != null) return cached;
            final Refs refs = new Refs();
            final DexFile dex = method.owner.dex;
            method.scan(new CodeScanner.Visitor() {
                @Override
                public void string(int i) {
                    refs.strings.add(dex.string(i));
                }

                @Override
                public void constant(long value) {
                    refs.constants.add(value);
                }

                @Override
                public void invoke(int opcode, int i) {
                    int proto = dex.methodProto(i);
                    refs.calls.add(new Call(dex.methodClass(i), dex.methodName(i),
                            Resolver.protoOf(dex.protoReturnType(proto), dex.protoParameters(proto))));
                }

                @Override
                public void field(int opcode, int i, boolean write) {
                    refs.fields.add(new FieldRef(dex.fieldClass(i), dex.fieldName(i), dex.fieldType(i), write));
                }

                @Override
                public void type(int opcode, int i) {
                    refs.types.add(dex.type(i));
                    if (opcode == 0x22) refs.newInstances.add(dex.type(i));
                }

                @Override
                public void switchKey(int key) {
                    refs.switchKeys.add(key);
                }
            });
            r.refs.put(method, refs);
            return refs;
        }

        @Override
        public String toString() {
            return "calls=" + calls.size() + " fields=" + fields.size()
                    + " strings=" + Arrays.toString(strings.toArray());
        }
    }
}
