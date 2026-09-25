package com.my.televip.obfuscate.resolve;

import com.my.televip.obfuscate.dex.DexClass;
import com.my.televip.obfuscate.dex.DexNames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One thing a call site needs from the client - a class, a field or a method - described by its
 * original name and, for when the build renamed it, by a fingerprint.
 */
public abstract class Symbol {

    abstract String id();

    abstract Resolver.Attempt attempt(Resolver r);

    abstract void store(Resolver r, Resolver.Attempt attempt, Mapping mapping);

    // =================================================================== class

    /** Something true or false about a candidate class. Null means "not decidable yet". */
    public interface ClassFact {
        Boolean test(Resolver r, DexClass c);
    }

    /** Where to look for a renamed class. Null means "depends on something not resolved yet". */
    public interface ClassSource {
        Collection<DexClass> candidates(Resolver r);
    }

    public static ClassSymbol cls(String originalName) {
        return new ClassSymbol(originalName);
    }

    public static final class ClassSymbol extends Symbol {
        final String original;
        final List<ClassSource> sources = new ArrayList<>();
        final List<ClassFact> facts = new ArrayList<>();

        ClassSymbol(String original) {
            this.original = original;
        }

        /**
         * Where the candidates come from. Several sources are tried in order, and the first to
         * single out one class wins - so a precise anchor can come first and a broader shape match
         * after it, for builds where the anchor is gone.
         */
        public ClassSymbol from(ClassSource... sources) {
            for (ClassSource source : sources) this.sources.add(source);
            return this;
        }

        /** Every fact must hold for a candidate to count. */
        public ClassSymbol where(ClassFact... facts) {
            for (ClassFact f : facts) this.facts.add(f);
            return this;
        }

        @Override
        String id() {
            return original;
        }

        @Override
        Resolver.Attempt attempt(Resolver r) {
            DexClass kept = r.index.findClass(original);
            if (kept != null) return Resolver.Attempt.of(kept, true);
            Resolver.Attempt ambiguous = null;
            boolean waiting = false;
            for (ClassSource source : sources) {
                Collection<DexClass> candidates = source.candidates(r);
                if (candidates == null) {
                    waiting = true;
                    continue;
                }
                List<DexClass> matching = new ArrayList<>();
                boolean undecided = false;
                for (DexClass c : candidates) {
                    if (c == null) continue;
                    Boolean ok = holds(r, c);
                    if (ok == null) {
                        undecided = true;
                        break;
                    }
                    if (ok) matching.add(c);
                }
                if (undecided) {
                    waiting = true;
                    continue;
                }
                Resolver.Attempt attempt = Resolver.Attempt.single(matching);
                if (attempt.kind == Resolver.Attempt.Kind.FOUND) return attempt;
                if (attempt.kind == Resolver.Attempt.Kind.AMBIGUOUS && ambiguous == null) ambiguous = attempt;
            }
            if (waiting) return Resolver.Attempt.waiting();
            return ambiguous != null ? ambiguous : Resolver.Attempt.notFound();
        }

        private Boolean holds(Resolver r, DexClass c) {
            for (ClassFact f : facts) {
                Boolean b = f.test(r, c);
                if (b == null) return null;
                if (!b) return false;
            }
            return true;
        }

        @Override
        void store(Resolver r, Resolver.Attempt attempt, Mapping mapping) {
            DexClass c = (DexClass) attempt.found;
            r.classes.put(original, c);
            mapping.classes.put(original, c.javaName());
        }
    }

    // ================================================================== method

    public static MethodSymbol method(String owner, String key) {
        return new MethodSymbol(owner, key);
    }

    public static final class MethodSymbol extends Symbol {
        final String owner, key;
        String name;
        String returnType;
        String[] params;
        Boolean isStatic;
        int[] readPositions;
        boolean voidable, narrowedStrings;
        final List<Body> facts = new ArrayList<>();

        MethodSymbol(String owner, String key) {
            this.owner = owner;
            this.key = key;
            this.name = key;
        }

        /** The method's real name, when the table key adds an overload suffix to it. */
        public MethodSymbol named(String realName) {
            this.name = realName;
            return this;
        }

        /** Signature in source types. Original app class names are mapped through the resolution. */
        public MethodSymbol sig(String returnType, String... params) {
            this.returnType = returnType;
            this.params = params;
            return this;
        }

        public MethodSymbol isStatic(boolean value) {
            this.isStatic = value;
            return this;
        }

        /**
         * The source parameter positions the call site actually reads. R8 deletes parameters a
         * method never uses, so the build may have fewer than the source; that is accepted only if
         * each of these positions is still there at the same index - otherwise a hook reading
         * {@code args[i]} would get a different argument. Without this, the signature must be
         * exactly the source one.
         */
        public MethodSymbol reads(int... sourcePositions) {
            this.readPositions = sourcePositions;
            return this;
        }

        /**
         * R8 turns a return type into void when no caller uses the result - typically a builder's
         * {@code return this}. Accept that, for call sites that ignore what the method returns.
         */
        public MethodSymbol voidable() {
            this.voidable = true;
            return this;
        }

        /**
         * R8 narrows a parameter type when every caller passes something more specific, and in
         * practice that means {@code CharSequence} becoming {@code String}. Accept that, for call
         * sites that only ever pass strings.
         */
        public MethodSymbol narrowedStrings() {
            this.narrowedStrings = true;
            return this;
        }

        public MethodSymbol where(Body... facts) {
            for (Body f : facts) this.facts.add(f);
            return this;
        }

        @Override
        String id() {
            return owner + "#" + key;
        }

        @Override
        Resolver.Attempt attempt(Resolver r) {
            DexClass cls = r.cls(r.fullName(owner));
            if (cls == null) {
                // The owner itself is unresolved - maybe for good, maybe not yet.
                return r.pendingOrResolvable(r.fullName(owner))
                        ? Resolver.Attempt.waiting() : Resolver.Attempt.notFound();
            }
            String[] want = null;
            String ret = null;
            if (returnType != null) {
                ret = r.descriptor(returnType);
                want = new String[params.length];
                for (int i = 0; i < params.length; i++) {
                    want[i] = r.descriptor(params[i]);
                    if (want[i] == null) return Resolver.Attempt.waiting();
                }
                if (ret == null) return Resolver.Attempt.waiting();
            }

            // Kept name: conclusive for a descriptive name, but a short one could equally be
            // something R8 generated, so for those the signature has to agree as well.
            List<DexClass.Method> named = cls.methodsNamed(name);
            if (!named.isEmpty()) {
                if (want == null) {
                    if (name.length() > 3) return Resolver.Attempt.of(named.get(0), true);
                } else {
                    for (DexClass.Method m : named) {
                        if (signatureFits(m, ret, want) && staticMatches(m)) {
                            return Resolver.Attempt.of(m, true);
                        }
                    }
                }
            }
            if (want == null) return Resolver.Attempt.notFound();

            List<DexClass.Method> matching = new ArrayList<>();
            for (DexClass.Method m : cls.methods) {
                if (m.isConstructor() || !staticMatches(m) || !signatureFits(m, ret, want)) continue;
                Boolean ok = holds(r, m);
                if (ok == null) return Resolver.Attempt.waiting();
                if (ok) matching.add(m);
            }
            return Resolver.Attempt.single(matching);
        }

        /**
         * Exact signature, or - when the call site declared what it reads - the source signature
         * with unused parameters deleted, as long as every read position keeps its index.
         */
        boolean signatureFits(DexClass.Method m, String ret, String[] want) {
            if (!m.returnType().equals(ret) && !(voidable && m.returnType().equals("V"))) return false;
            String[] actual = m.parameterTypes();
            if (actual.length == want.length) {
                for (int i = 0; i < want.length; i++) {
                    if (!paramFits(want[i], actual[i])) return false;
                }
                return true;
            }
            if (readPositions == null || actual.length > want.length) return false;
            // Greedy subsequence alignment: actual[j] is source parameter kept[j].
            int[] kept = new int[actual.length];
            int j = 0;
            for (int i = 0; i < want.length && j < actual.length; i++) {
                if (paramFits(want[i], actual[j])) kept[j++] = i;
            }
            if (j != actual.length) return false;
            for (int p : readPositions) {
                if (p >= actual.length || kept[p] != p) return false;
            }
            return true;
        }

        private boolean paramFits(String declared, String actual) {
            return declared.equals(actual) || (narrowedStrings
                    && declared.equals("Ljava/lang/CharSequence;") && actual.equals("Ljava/lang/String;"));
        }

        private boolean staticMatches(DexClass.Method m) {
            return isStatic == null || isStatic == m.isStatic();
        }

        private Boolean holds(Resolver r, DexClass.Method m) {
            for (Body f : facts) {
                Boolean b = f.test(r, m);
                if (b == null) return null;
                if (!b) return false;
            }
            return true;
        }

        @Override
        void store(Resolver r, Resolver.Attempt attempt, Mapping mapping) {
            DexClass.Method m = (DexClass.Method) attempt.found;
            r.methods.put(id(), m);
            mapping.methods.put(Mapping.memberKey(owner, key), m.name());
            // Deleted parameters: the call site asks for the source list, so hand it the real one.
            if (params != null && m.parameterTypes().length != params.length) {
                String[] actual = m.parameterTypes();
                String[] names = new String[actual.length];
                for (int i = 0; i < actual.length; i++) names[i] = javaName(actual[i]);
                mapping.putParameters(name, names);
            }
        }
    }

    // =================================================================== field

    public static FieldSymbol field(String owner, String name) {
        return new FieldSymbol(owner, name);
    }

    public static final class FieldSymbol extends Symbol {
        final String owner, name;
        String type;
        Boolean isStatic;
        String accessedBy;
        boolean uniqueOfType;
        String writtenBy;
        int writeOrdinal;
        boolean ordinalOfReads;

        FieldSymbol(String owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        public FieldSymbol type(String sourceType) {
            this.type = sourceType;
            return this;
        }

        public FieldSymbol isStatic(boolean value) {
            this.isStatic = value;
            return this;
        }

        /** Renamed field: the class declares exactly one field of this type, so it is that one. */
        public FieldSymbol onlyOneOfType() {
            this.uniqueOfType = true;
            return this;
        }

        /**
         * Renamed field: the {@code ordinal}-th distinct field of this type that the given
         * (resolved) method writes, in the order it writes them. For initialisers that assign a
         * class's fields one after another, which R8 does not reorder.
         */
        public FieldSymbol writtenBy(String methodSymbolId, int ordinal) {
            this.writtenBy = methodSymbolId;
            this.writeOrdinal = ordinal;
            return this;
        }

        /**
         * Renamed field: the {@code ordinal}-th distinct field of this type that the given
         * (resolved) method reads, in order - e.g. the TextView a setter sets text on first.
         */
        public FieldSymbol readBy(String methodSymbolId, int ordinal) {
            this.writtenBy = methodSymbolId;
            this.writeOrdinal = ordinal;
            this.ordinalOfReads = true;
            return this;
        }

        /** Renamed field: the one of this type that the given (resolved) method touches. */
        public FieldSymbol accessedBy(String methodSymbolId) {
            this.accessedBy = methodSymbolId;
            return this;
        }

        @Override
        String id() {
            return owner + "." + name;
        }

        @Override
        Resolver.Attempt attempt(Resolver r) {
            DexClass cls = r.cls(r.fullName(owner));
            if (cls == null) {
                return r.pendingOrResolvable(r.fullName(owner))
                        ? Resolver.Attempt.waiting() : Resolver.Attempt.notFound();
            }
            String typeDesc = type == null ? null : r.descriptor(type);
            if (type != null && typeDesc == null) return Resolver.Attempt.waiting();

            DexClass.Field kept = cls.fieldNamed(name);
            if (kept != null && (name.length() > 3 || typeDesc == null || typeDesc.equals(kept.type()))) {
                return Resolver.Attempt.of(kept, true);
            }
            if (typeDesc != null && uniqueOfType) {
                List<DexClass.Field> ofType = new ArrayList<>();
                for (DexClass.Field f : cls.fields) {
                    if (typeDesc.equals(f.type()) && (isStatic == null || isStatic == f.isStatic())) ofType.add(f);
                }
                return Resolver.Attempt.single(ofType);
            }
            if (typeDesc != null && writtenBy != null) {
                DexClass.Method writer = r.methods.get(writtenBy);
                if (writer == null) return r.isResolvedOrPending(writtenBy)
                        ? Resolver.Attempt.waiting() : Resolver.Attempt.notFound();
                List<String> order = new ArrayList<>();
                for (Body.Refs.FieldRef f : Body.Refs.of(r, writer).fields) {
                    if (f.write != ordinalOfReads && f.owner.equals(cls.descriptor) && f.type.equals(typeDesc)
                            && !order.contains(f.name)) order.add(f.name);
                }
                if (writeOrdinal >= order.size()) return Resolver.Attempt.notFound();
                DexClass.Field declared = cls.fieldNamed(order.get(writeOrdinal));
                return declared == null ? Resolver.Attempt.notFound() : Resolver.Attempt.of(declared, false);
            }
            if (typeDesc == null || accessedBy == null) return Resolver.Attempt.notFound();
            DexClass.Method method = r.methods.get(accessedBy);
            if (method == null) return Resolver.Attempt.waiting();

            List<DexClass.Field> matching = new ArrayList<>();
            Body.Refs refs = Body.Refs.of(r, method);
            for (Body.Refs.FieldRef f : refs.fields) {
                if (!f.owner.equals(cls.descriptor) || !f.type.equals(typeDesc)) continue;
                DexClass.Field declared = cls.fieldNamed(f.name);
                if (declared != null && (isStatic == null || isStatic == declared.isStatic())) {
                    matching.add(declared);
                }
            }
            return Resolver.Attempt.single(matching);
        }

        @Override
        void store(Resolver r, Resolver.Attempt attempt, Mapping mapping) {
            DexClass.Field f = (DexClass.Field) attempt.found;
            r.fields.put(id(), f);
            mapping.fields.put(Mapping.memberKey(owner, name), f.name());
        }
    }

    // ------------------------------------------------------------------ utils

    static String javaName(String descriptor) {
        return DexNames.toJavaName(descriptor);
    }
}
