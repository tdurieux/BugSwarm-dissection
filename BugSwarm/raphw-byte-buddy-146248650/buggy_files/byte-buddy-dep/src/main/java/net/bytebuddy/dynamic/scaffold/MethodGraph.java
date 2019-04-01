package net.bytebuddy.dynamic.scaffold;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;

/**
 * A method graph represents a view on a set of methods as they are seen from a given type. Any method is represented as a node that represents
 * a method, its bridge methods, its resolution state and information on if it was made visible by a visibility bridge.
 */
public interface MethodGraph {

    /**
     * Locates a node in this graph which represents the provided method token.
     *
     * @param token A method token that represents the method to be located.
     * @return The node representing the given token.
     */
    Node locate(MethodDescription.SignatureToken token);

    /**
     * Lists all nodes of this method graph.
     *
     * @return A list of all nodes of this method graph.
     */
    NodeList listNodes();

    /**
     * A canonical implementation of an empty method graph.
     */
    enum Empty implements MethodGraph.Linked, MethodGraph.Compiler {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Node locate(MethodDescription.SignatureToken token) {
            return Node.Unresolved.INSTANCE;
        }

        @Override
        public NodeList listNodes() {
            return new NodeList(Collections.<Node>emptyList());
        }

        @Override
        public MethodGraph getSuperClassGraph() {
            return this;
        }

        @Override
        public MethodGraph getInterfaceGraph(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public Linked compile(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
            return this;
        }

        @Override
        public String toString() {
            return "MethodGraph.Empty." + name();
        }
    }

    /**
     * A linked method graph represents a view that additionally exposes information of a given type's super type view and a
     * view on this graph's directly implemented interfaces.
     */
    interface Linked extends MethodGraph {

        /**
         * Returns a graph representing the view on this represented type's super type.
         *
         * @return A graph representing the view on this represented type's super type.
         */
        MethodGraph getSuperClassGraph();

        /**
         * Returns a graph representing the view on this represented type's directly implemented interface type.
         *
         * @param typeDescription The interface type for which a view is to be returned.
         * @return A graph representing the view on this represented type's directly implemented interface type.
         */
        MethodGraph getInterfaceGraph(TypeDescription typeDescription);

        /**
         * A simple implementation of a linked method graph that exposes views by delegation to given method graphs.
         */
        class Delegation implements Linked {

            /**
             * The represented type's method graph.
             */
            private final MethodGraph methodGraph;

            /**
             * The super class's method graph.
             */
            private final MethodGraph superClassGraph;

            /**
             * A mapping of method graphs of the represented type's directly implemented interfaces to their graph representatives.
             */
            private final Map<TypeDescription, MethodGraph> interfaceGraphs;

            /**
             * Creates a new delegation method graph.
             *
             * @param methodGraph     The represented type's method graph.
             * @param superClassGraph      The super class's method graph.
             * @param interfaceGraphs A mapping of method graphs of the represented type's directly implemented interfaces to their graph representatives.
             */
            public Delegation(MethodGraph methodGraph, MethodGraph superClassGraph, Map<TypeDescription, MethodGraph> interfaceGraphs) {
                this.methodGraph = methodGraph;
                this.superClassGraph = superClassGraph;
                this.interfaceGraphs = interfaceGraphs;
            }

            @Override
            public MethodGraph getSuperClassGraph() {
                return superClassGraph;
            }

            @Override
            public MethodGraph getInterfaceGraph(TypeDescription typeDescription) {
                MethodGraph interfaceGraph = interfaceGraphs.get(typeDescription);
                return interfaceGraph == null
                        ? Empty.INSTANCE
                        : interfaceGraph;
            }

            @Override
            public Node locate(MethodDescription.SignatureToken token) {
                return methodGraph.locate(token);
            }

            @Override
            public NodeList listNodes() {
                return methodGraph.listNodes();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Delegation that = (Delegation) other;
                return methodGraph.equals(that.methodGraph)
                        && superClassGraph.equals(that.superClassGraph)
                        && interfaceGraphs.equals(that.interfaceGraphs);
            }

            @Override
            public int hashCode() {
                int result = methodGraph.hashCode();
                result = 31 * result + superClassGraph.hashCode();
                result = 31 * result + interfaceGraphs.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodGraph.Linked.Delegation{" +
                        "methodGraph=" + methodGraph +
                        ", superClassGraph=" + superClassGraph +
                        ", interfaceGraphs=" + interfaceGraphs +
                        '}';
            }
        }
    }

    /**
     * Represents a node within a method graph.
     */
    interface Node {

        /**
         * Returns the sort of this node.
         *
         * @return The sort of this node.
         */
        Sort getSort();

        /**
         * Returns the method that is represented by this node.
         *
         * @return The method that is represented by this node.
         */
        MethodDescription getRepresentative();

        /**
         * Returns a set of type tokens that this method represents. This set contains the actual method's type including the
         * types of all bridge methods.
         *
         * @return A set of type tokens that this method represents.
         */
        Set<MethodDescription.TypeToken> getMethodTypes();

        /**
         * Represents a {@link net.bytebuddy.dynamic.scaffold.MethodGraph.Node}'s state.
         */
        enum Sort {

            /**
             * Represents a resolved node that was made visible by a visibility bridge.
             */
            VISIBLE(true, true, true),

            /**
             * Represents a resolved node that was not made visible by a visibility bridge.
             */
            RESOLVED(true, true, false),

            /**
             * Represents an ambiguous node, i.e. a node that might refer to several methods.
             */
            AMBIGUOUS(true, false, false),

            /**
             * Represents an unresolved node.
             */
            UNRESOLVED(false, false, false);

            /**
             * {@code true} if this sort represents a resolved node.
             */
            private final boolean resolved;

            /**
             * {@code true} if this sort represents a non-ambiguous node.
             */
            private final boolean unique;

            /**
             * {@code true} if this sort represents a node that was made by a visibility bridge.
             */
            private final boolean madeVisible;

            /**
             * Creates a new sort.
             *
             * @param resolved    {@code true} if this sort represents a resolved node.
             * @param unique      {@code true} if this sort represents a non-ambiguous node.
             * @param madeVisible {@code true} if this sort represents a node that was made by a visibility bridge.
             */
            Sort(boolean resolved, boolean unique, boolean madeVisible) {
                this.resolved = resolved;
                this.unique = unique;
                this.madeVisible = madeVisible;
            }

            /**
             * Verifies if this sort represents a resolved node.
             *
             * @return {@code true} if this sort represents a resolved node.
             */
            public boolean isResolved() {
                return resolved;
            }

            /**
             * Verifies if this sort represents a non-ambiguous node.
             *
             * @return {@code true} if this sort represents a non-ambiguous node.
             */
            public boolean isUnique() {
                return unique;
            }

            /**
             * Verifies if this sort represents a node that was made visible by a visibility bridge.
             *
             * @return {@code true} if this sort represents a node that was made visible by a visibility bridge.
             */
            public boolean isMadeVisible() {
                return madeVisible;
            }

            @Override
            public String toString() {
                return "MethodGraph.Node.Sort." + name();
            }
        }

        /**
         * A canonical implementation of an unresolved node.
         */
        enum Unresolved implements Node {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Sort getSort() {
                return Sort.UNRESOLVED;
            }

            @Override
            public MethodDescription getRepresentative() {
                throw new IllegalStateException("Cannot resolve the method of an illegal node");
            }

            @Override
            public Set<MethodDescription.TypeToken> getMethodTypes() {
                throw new IllegalStateException("Cannot resolve bridge method of an illegal node");
            }

            @Override
            public String toString() {
                return "MethodGraph.Node.Unresolved." + name();
            }
        }

        /**
         * A simple implementation of a resolved node of a method without bridges.
         */
        class Simple implements Node {

            /**
             * The represented method.
             */
            private final MethodDescription methodDescription;

            /**
             * Creates a simple node.
             *
             * @param methodDescription The represented method.
             */
            public Simple(MethodDescription methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public Sort getSort() {
                return Sort.RESOLVED;
            }

            @Override
            public MethodDescription getRepresentative() {
                return methodDescription;
            }

            @Override
            public Set<MethodDescription.TypeToken> getMethodTypes() {
                return Collections.emptySet();
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodDescription.equals(((Simple) other).methodDescription);
            }

            @Override
            public int hashCode() {
                return methodDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodGraph.Node.Simple{" +
                        "methodDescription=" + methodDescription +
                        '}';
            }
        }
    }

    /**
     * A compiler to produce a {@link MethodGraph} from a given type.
     */
    @SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "No circularity, initialization is safe")
    interface Compiler {

        /**
         * The default compiler for compiling Java methods.
         */
        Compiler DEFAULT = MethodGraph.Compiler.Default.forJavaHierarchy();

        /**
         * Compiles the given type into a method graph considering the type to be the viewpoint.
         *
         * @param typeDescription The type to be compiled.
         * @return A linked method graph representing the given type.
         */
        MethodGraph.Linked compile(TypeDescription typeDescription);

        /**
         * Compiles the given type into a method graph.
         *
         * @param typeDefinition The type to be compiled.
         * @param viewPoint      The view point that determines the method's visibility.
         * @return A linked method graph representing the given type.
         */
        MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint);

        /**
         * An abstract base implementation of a method graph compiler.
         */
        abstract class AbstractBase implements Compiler {

            @Override
            public Linked compile(TypeDescription typeDescription) {
                return compile(typeDescription, typeDescription);
            }
        }

        /**
         * A default implementation of a method graph.
         *
         * @param <T> The type of the harmonizer token to be used for linking methods of different types.
         */
        class Default<T> extends AbstractBase {

            /**
             * The harmonizer to be used.
             */
            private final Harmonizer<T> harmonizer;

            /**
             * The merger to be used.
             */
            private final Merger merger;

            /**
             * Creates a new default method graph compiler.
             *
             * @param harmonizer The harmonizer to be used.
             * @param merger     The merger to be used.
             */
            protected Default(Harmonizer<T> harmonizer, Merger merger) {
                this.harmonizer = harmonizer;
                this.merger = merger;
            }

            /**
             * Creates a default compiler using the given harmonizer and merger.
             *
             * @param harmonizer The harmonizer to be used for creating tokens that uniquely identify a method hierarchy.
             * @param merger     The merger to be used for identifying a method to represent an ambiguous method resolution.
             * @param <S>        The type of the harmonizer token.
             * @return A default compiler for the given harmonizer and merger.
             */
            public static <S> Compiler of(Harmonizer<S> harmonizer, Merger merger) {
                return new Default<S>(harmonizer, merger);
            }

            /**
             * <p>
             * Creates a default compiler for a method hierarchy following the rules of the Java programming language. According
             * to these rules, two methods of the same name are only different if their parameter types represent different raw
             * types. The return type is not considered as a part of the signature.
             * </p>
             * <p>
             * Ambiguous methods are merged by considering the method that was discovered first.
             * </p>
             *
             * @return A compiler for resolving a method hierarchy following the rules of the Java programming language.
             */
            public static Compiler forJavaHierarchy() {
                return of(Harmonizer.ForJavaMethod.INSTANCE, Merger.Directional.LEFT);
            }

            /**
             * <p>
             * Creates a default compiler for a method hierarchy following the rules of the Java virtual machine. According
             * to these rules, two methods of the same name are different if their parameter types and return types represent
             * different type erasures.
             * </p>
             * <p>
             * Ambiguous methods are merged by considering the method that was discovered first.
             * </p>
             *
             * @return A compiler for resolving a method hierarchy following the rules of the Java programming language.
             */
            public static Compiler forJVMHierarchy() {
                return of(Harmonizer.ForJVMMethod.INSTANCE, Merger.Directional.LEFT);
            }

            @Override
            public MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
                Map<TypeDefinition, Key.Store<T>> snapshots = new HashMap<TypeDefinition, Key.Store<T>>();
                Key.Store<?> rootStore = doAnalyze(typeDefinition, snapshots, isVirtual().and(isVisibleTo(viewPoint)));
                TypeDescription.Generic superClass = typeDefinition.getSuperClass();
                List<TypeDescription.Generic> interfaceTypes = typeDefinition.getInterfaces();
                Map<TypeDescription, MethodGraph> interfaceGraphs = new HashMap<TypeDescription, MethodGraph>();
                for (TypeDescription.Generic interfaceType : interfaceTypes) {
                    interfaceGraphs.put(interfaceType.asErasure(), snapshots.get(interfaceType).asGraph(merger));
                }
                return new Linked.Delegation(rootStore.asGraph(merger),
                        superClass == null
                                ? Empty.INSTANCE
                                : snapshots.get(superClass).asGraph(merger),
                        interfaceGraphs);
            }

            /**
             * Analyzes the given type description without checking if the end of the type hierarchy was reached.
             *
             * @param typeDefinition   The type to analyze.
             * @param snapshots        A map containing snapshots of key stores for previously analyzed types.
             * @param relevanceMatcher A matcher for filtering methods that should be included in the graph.
             * @return A key store describing the provided type.
             */
            protected Key.Store<T> analyze(TypeDefinition typeDefinition,
                                           Map<TypeDefinition, Key.Store<T>> snapshots,
                                           ElementMatcher<? super MethodDescription> relevanceMatcher) {
                Key.Store<T> store = snapshots.get(typeDefinition);
                if (store == null) {
                    store = doAnalyze(typeDefinition, snapshots, relevanceMatcher);
                    snapshots.put(typeDefinition, store);
                }
                return store;
            }

            /**
             * Analyzes the given type description.
             *
             * @param typeDefinition   The type to analyze.
             * @param snapshots        A map containing snapshots of key stores for previously analyzed types.
             * @param relevanceMatcher A matcher for filtering methods that should be included in the graph.
             * @return A key store describing the provided type.
             */
            protected Key.Store<T> analyzeNullable(TypeDefinition typeDefinition,
                                                   Map<TypeDefinition, Key.Store<T>> snapshots,
                                                   ElementMatcher<? super MethodDescription> relevanceMatcher) {
                return typeDefinition == null
                        ? new Key.Store<T>()
                        : analyze(typeDefinition, snapshots, relevanceMatcher);
            }

            /**
             * Analyzes the given type description without checking if it is already presented in the key store.
             *
             * @param typeDefinition   The type to analyze.
             * @param snapshots        A map containing snapshots of key stores for previously analyzed types.
             * @param relevanceMatcher A matcher for filtering methods that should be included in the graph.
             * @return A key store describing the provided type.
             */
            protected Key.Store<T> doAnalyze(TypeDefinition typeDefinition,
                                             Map<TypeDefinition, Key.Store<T>> snapshots,
                                             ElementMatcher<? super MethodDescription> relevanceMatcher) {
                Key.Store<T> store = analyzeNullable(typeDefinition.getSuperClass(), snapshots, relevanceMatcher);
                Key.Store<T> interfaceStore = new Key.Store<T>();
                for (TypeDescription.Generic interfaceType : typeDefinition.getInterfaces()) {
                    interfaceStore = interfaceStore.combineWith(analyze(interfaceType, snapshots, relevanceMatcher));
                }
                store = store.inject(interfaceStore);
                for (MethodDescription methodDescription : typeDefinition.getDeclaredMethods().filter(relevanceMatcher)) {
                    store = store.registerTopLevel(methodDescription, harmonizer);
                }
                return store;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && harmonizer.equals(((Default<?>) other).harmonizer)
                        && merger.equals(((Default<?>) other).merger);
            }

            @Override
            public int hashCode() {
                return harmonizer.hashCode() + 31 * merger.hashCode();
            }

            @Override
            public String toString() {
                return "MethodGraph.Compiler.Default{" +
                        "harmonizer=" + harmonizer +
                        ", merger=" + merger +
                        '}';
            }

            /**
             * A harmonizer is responsible for creating a token that identifies a method's relevant attributes for considering
             * two methods of being equal or not.
             *
             * @param <S> The type of the token that is created by the implementing harmonizer.
             */
            public interface Harmonizer<S> {

                /**
                 * Harmonizes the given type token.
                 *
                 * @param typeToken The type token to harmonize.
                 * @return A token representing the given type token.
                 */
                S harmonize(MethodDescription.TypeToken typeToken);

                /**
                 * A harmonizer for the Java programming language that identifies a method by its parameter types only.
                 */
                enum ForJavaMethod implements Harmonizer<ForJavaMethod.Token> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Token harmonize(MethodDescription.TypeToken typeToken) {
                        return new Token(typeToken);
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod." + name();
                    }

                    /**
                     * A token that identifies a Java method's type by its parameter types only.
                     */
                    protected static class Token {

                        /**
                         * The represented type token.
                         */
                        private final MethodDescription.TypeToken typeToken;

                        /**
                         * Creates a new type token for a Java method.
                         *
                         * @param typeToken The represented type token.
                         */
                        protected Token(MethodDescription.TypeToken typeToken) {
                            this.typeToken = typeToken;
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || other instanceof Token
                                    && typeToken.getParameterTypes().equals(((Token) other).typeToken.getParameterTypes());
                        }

                        @Override
                        public int hashCode() {
                            return typeToken.getParameterTypes().hashCode();
                        }

                        @Override
                        public String toString() {
                            return "MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod.Token{" +
                                    "typeToken=" + typeToken +
                                    '}';
                        }
                    }
                }

                /**
                 * A harmonizer for the Java virtual machine's method dispatching rules that identifies a method by its parameter types and return type.
                 */
                enum ForJVMMethod implements Harmonizer<ForJVMMethod.Token> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Token harmonize(MethodDescription.TypeToken typeToken) {
                        return new Token(typeToken);
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod." + name();
                    }

                    /**
                     * A token that identifies a Java method's type by its parameter types and return type.
                     */
                    protected static class Token {

                        /**
                         * The represented type token.
                         */
                        private final MethodDescription.TypeToken typeToken;

                        /**
                         * Creates a new type token for a JVM method.
                         *
                         * @param typeToken The represented type token.
                         */
                        public Token(MethodDescription.TypeToken typeToken) {
                            this.typeToken = typeToken;
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || other instanceof Token
                                    && typeToken.getReturnType().equals(((Token) other).typeToken.getReturnType())
                                    && typeToken.getParameterTypes().equals(((Token) other).typeToken.getParameterTypes());
                        }

                        @Override
                        public int hashCode() {
                            return typeToken.getReturnType().hashCode() + 31 * typeToken.getParameterTypes().hashCode();
                        }

                        @Override
                        public String toString() {
                            return "MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.Token{" +
                                    "typeToken=" + typeToken +
                                    '}';
                        }
                    }
                }
            }

            /**
             * Implementations are responsible for identifying a representative method for a {@link net.bytebuddy.dynamic.scaffold.MethodGraph.Node}
             * between several ambiguously resolved methods.
             */
            public interface Merger {

                /**
                 * Merges two ambiguously resolved methods to yield a single representative.
                 *
                 * @param left  The left method description, i.e. the method that was discovered first or was previously merged.
                 * @param right The right method description, i.e. the method that was discovered last.
                 * @return A method description compatible to both method's types that is used as a representative.
                 */
                MethodDescription merge(MethodDescription left, MethodDescription right);

                /**
                 * A directional merger that always returns either the left or right method description.
                 */
                enum Directional implements Merger {

                    /**
                     * A merger that always returns the left method, i.e. the method that was discovered first or was previously merged.
                     */
                    LEFT(true),

                    /**
                     * A merger that always returns the right method, i.e. the method that was discovered last.
                     */
                    RIGHT(false);

                    /**
                     * {@code true} if the left method should be returned when merging methods.
                     */
                    private final boolean left;

                    /**
                     * Creates a directional merger.
                     *
                     * @param left {@code true} if the left method should be returned when merging methods.
                     */
                    Directional(boolean left) {
                        this.left = left;
                    }

                    @Override
                    public MethodDescription merge(MethodDescription left, MethodDescription right) {
                        return this.left
                                ? left
                                : right;
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Merger.Directional." + name();
                    }
                }
            }

            /**
             * A key represents a collection of methods within a method graph to later yield a node representing a collection of methods,
             * i.e. a method representative including information on the required method bridges.
             *
             * @param <S> The type of the token used for deciding on method equality.
             */
            protected abstract static class Key<S> {

                /**
                 * The internal name of the method this key identifies.
                 */
                protected final String internalName;

                /**
                 * Creates a new key.
                 *
                 * @param internalName The internal name of the method this key identifies.
                 */
                protected Key(String internalName) {
                    this.internalName = internalName;
                }

                /**
                 * Returns a set of all identifiers of this key.
                 *
                 * @return A set of all identifiers of this key.
                 */
                protected abstract Set<S> getIdentifiers();

                @Override
                public boolean equals(Object other) {
                    return other == this || (other instanceof Key
                            && internalName.equals(((Key) other).internalName)
                            && !Collections.disjoint(getIdentifiers(), ((Key) other).getIdentifiers()));
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode();
                }

                /**
                 * A harmonized key represents a key where equality is decided based on tokens that are returned by a
                 * {@link net.bytebuddy.dynamic.scaffold.MethodGraph.Compiler.Default.Harmonizer}.
                 *
                 * @param <V> The type of the tokens yielded by a harmonizer.
                 */
                protected static class Harmonized<V> extends Key<V> {

                    /**
                     * A mapping of identifiers to the type tokens they represent.
                     */
                    private final Map<V, Set<MethodDescription.TypeToken>> identifiers;

                    /**
                     * Creates a new harmonized key.
                     *
                     * @param internalName The internal name of the method this key identifies.
                     * @param identifiers  A mapping of identifiers to the type tokens they represent.
                     */
                    protected Harmonized(String internalName, Map<V, Set<MethodDescription.TypeToken>> identifiers) {
                        super(internalName);
                        this.identifiers = identifiers;
                    }

                    /**
                     * Creates a new harmonized key for the given method description.
                     *
                     * @param methodDescription The method description to represent as a harmonized key.
                     * @param harmonizer        The harmonizer to use.
                     * @param <Q>               The type of the token yielded by a harmonizer.
                     * @return A harmonized key representing the provided method.
                     */
                    protected static <Q> Harmonized<Q> of(MethodDescription methodDescription, Harmonizer<Q> harmonizer) {
                        MethodDescription.TypeToken typeToken = methodDescription.asTypeToken();
                        return new Harmonized<Q>(methodDescription.getInternalName(),
                                Collections.singletonMap(harmonizer.harmonize(typeToken), Collections.<MethodDescription.TypeToken>emptySet()));
                    }

                    /**
                     * Creates a detached version of this key.
                     *
                     * @param typeToken The type token of the representative method.
                     * @return The detached version of this key.
                     */
                    protected Detached detach(MethodDescription.TypeToken typeToken) {
                        Set<MethodDescription.TypeToken> identifiers = new HashSet<MethodDescription.TypeToken>();
                        for (Set<MethodDescription.TypeToken> typeTokens : this.identifiers.values()) {
                            identifiers.addAll(typeTokens);
                        }
                        identifiers.add(typeToken);
                        return new Detached(internalName, identifiers);
                    }

                    /**
                     * Combines this key with the given key.
                     *
                     * @param key The key to be merged with this key.
                     * @return A harmonized key representing the merger of this key and the given key.
                     */
                    protected Harmonized<V> combineWith(Harmonized<V> key) {
                        Map<V, Set<MethodDescription.TypeToken>> identifiers = new HashMap<V, Set<MethodDescription.TypeToken>>(this.identifiers);
                        for (Map.Entry<V, Set<MethodDescription.TypeToken>> entry : key.identifiers.entrySet()) {
                            Set<MethodDescription.TypeToken> typeTokens = identifiers.get(entry.getKey());
                            if (typeTokens == null) {
                                identifiers.put(entry.getKey(), entry.getValue());
                            } else {
                                typeTokens = new HashSet<MethodDescription.TypeToken>(typeTokens);
                                typeTokens.addAll(entry.getValue());
                                identifiers.put(entry.getKey(), typeTokens);
                            }
                        }
                        return new Harmonized<V>(internalName, identifiers);
                    }

                    /**
                     * Extends this key by the given method description.
                     *
                     * @param methodDescription The method to extend this key with.
                     * @param harmonizer        The harmonizer to use for determining method equality.
                     * @return The harmonized key representing the extension of this key with the provided method.
                     */
                    protected Harmonized<V> extend(MethodDescription.InDefinedShape methodDescription, Harmonizer<V> harmonizer) {
                        Map<V, Set<MethodDescription.TypeToken>> identifiers = new HashMap<V, Set<MethodDescription.TypeToken>>(this.identifiers);
                        MethodDescription.TypeToken typeToken = methodDescription.asTypeToken();
                        V identifier = harmonizer.harmonize(typeToken);
                        Set<MethodDescription.TypeToken> typeTokens = identifiers.get(identifier);
                        if (typeTokens == null) {
                            identifiers.put(identifier, Collections.singleton(typeToken));
                        } else {
                            typeTokens = new HashSet<MethodDescription.TypeToken>(typeTokens);
                            typeTokens.add(typeToken);
                            identifiers.put(identifier, typeTokens);
                        }
                        return new Harmonized<V>(internalName, identifiers);
                    }

                    @Override
                    protected Set<V> getIdentifiers() {
                        return identifiers.keySet();
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Key.Harmonized{" +
                                "internalName='" + internalName + '\'' +
                                ", identifiers=" + identifiers +
                                '}';
                    }
                }

                /**
                 * A detached version of a key that identifies methods by their JVM signature, i.e. parameter types and return type.
                 */
                protected static class Detached extends Key<MethodDescription.TypeToken> {

                    /**
                     * The type tokens represented by this key.
                     */
                    private final Set<MethodDescription.TypeToken> identifiers;

                    /**
                     * Creates a new detached key.
                     *
                     * @param internalName The internal name of the method this key identifies.
                     * @param identifiers  The type tokens represented by this key.
                     */
                    protected Detached(String internalName, Set<MethodDescription.TypeToken> identifiers) {
                        super(internalName);
                        this.identifiers = identifiers;
                    }

                    /**
                     * Creates a new detached key of the given method token.
                     *
                     * @param token The method token to represent as a key.
                     * @return A detached key representing the given method token..
                     */
                    protected static Detached of(MethodDescription.SignatureToken token) {
                        return new Detached(token.getName(), Collections.singleton(token.asTypeToken()));
                    }

                    @Override
                    protected Set<MethodDescription.TypeToken> getIdentifiers() {
                        return identifiers;
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Key.Detached{" +
                                "internalName='" + internalName + '\'' +
                                ", identifiers=" + identifiers +
                                '}';
                    }
                }

                /**
                 * A store for collected methods that are identified by keys.
                 *
                 * @param <V> The type of the token used for deciding on method equality.
                 */
                protected static class Store<V> {

                    /**
                     * Size of an empty map to improve code readability.
                     */
                    private static final int EMPTY = 0;

                    /**
                     * A mapping of harmonized keys to their represented entry.
                     */
                    private final LinkedHashMap<Harmonized<V>, Entry<V>> entries;

                    /**
                     * Creates an empty store.
                     */
                    protected Store() {
                        this(new LinkedHashMap<Harmonized<V>, Entry<V>>(EMPTY));
                    }

                    /**
                     * Creates a new store representing the given entries.
                     *
                     * @param entries A mapping of harmonized keys to their represented entry.
                     */
                    private Store(LinkedHashMap<Harmonized<V>, Entry<V>> entries) {
                        this.entries = entries;
                    }

                    /**
                     * Combines the two given stores.
                     *
                     * @param left  The left store to be combined.
                     * @param right The right store to be combined.
                     * @param <W>   The type of the harmonized key of both stores.
                     * @return An entry representing the combination of both stores.
                     */
                    private static <W> Entry<W> combine(Entry<W> left, Entry<W> right) {
                        Set<MethodDescription> leftMethods = left.getCandidates(), rightMethods = right.getCandidates();
                        LinkedHashSet<MethodDescription> combined = new LinkedHashSet<MethodDescription>(leftMethods.size() + rightMethods.size());
                        combined.addAll(leftMethods);
                        combined.addAll(rightMethods);
                        for (MethodDescription leftMethod : leftMethods) {
                            TypeDescription leftType = leftMethod.getDeclaringType().asErasure();
                            for (MethodDescription rightMethod : rightMethods) {
                                TypeDescription rightType = rightMethod.getDeclaringType().asErasure();
                                if (leftType.equals(rightType)) {
                                    break;
                                } else if (leftType.isAssignableTo(rightType)) {
                                    combined.remove(rightMethod);
                                    break;
                                } else if (leftType.isAssignableFrom(rightType)) {
                                    combined.remove(leftMethod);
                                    break;
                                }
                            }
                        }
                        Key.Harmonized<W> key = left.getKey().combineWith(right.getKey());
                        return combined.size() == 1
                                ? new Entry.Resolved<W>(key, combined.iterator().next(), false)
                                : new Entry.Ambiguous<W>(key, combined);
                    }

                    /**
                     * Registers a new top level method within this store.
                     *
                     * @param methodDescription The method to register.
                     * @param harmonizer        The harmonizer to use for determining method equality.
                     * @return A store with the given method registered as a top-level method.
                     */
                    protected Store<V> registerTopLevel(MethodDescription methodDescription, Harmonizer<V> harmonizer) {
                        Harmonized<V> key = Harmonized.of(methodDescription, harmonizer);
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        Entry<V> currentEntry = entries.remove(key);
                        Entry<V> extendedEntry = (currentEntry == null
                                ? new Entry.Initial<V>(key)
                                : currentEntry).extendBy(methodDescription, harmonizer);
                        entries.put(extendedEntry.getKey(), extendedEntry);
                        return new Store<V>(entries);
                    }

                    /**
                     * Combines this store with the given store.
                     *
                     * @param store The store to combine with this store.
                     * @return A store representing a combination of this store and the given store.
                     */
                    protected Store<V> combineWith(Store<V> store) {
                        Store<V> combinedStore = this;
                        for (Entry<V> entry : store.entries.values()) {
                            combinedStore = combinedStore.combineWith(entry);
                        }
                        return combinedStore;
                    }

                    /**
                     * Combines this store with the given entry.
                     *
                     * @param entry The entry to combine with this store.
                     * @return A store representing a combination of this store and the given entry.
                     */
                    protected Store<V> combineWith(Entry<V> entry) {
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        Entry<V> previousEntry = entries.remove(entry.getKey());
                        Entry<V> injectedEntry = previousEntry == null
                                ? entry
                                : combine(previousEntry, entry);
                        entries.put(injectedEntry.getKey(), injectedEntry);
                        return new Store<V>(entries);
                    }

                    /**
                     * Injects the given store into this store.
                     *
                     * @param store The key store to inject into this store.
                     * @return A store that represents this store with the given store injected.
                     */
                    protected Store<V> inject(Store<V> store) {
                        Store<V> injectedStore = this;
                        for (Entry<V> entry : store.entries.values()) {
                            injectedStore = injectedStore.inject(entry);
                        }
                        return injectedStore;
                    }

                    /**
                     * Injects the given entry into this store.
                     *
                     * @param entry The entry to be injected into this store.
                     * @return A store that represents this store with the given entry injected.
                     */
                    protected Store<V> inject(Entry<V> entry) {
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        Entry<V> dominantEntry = entries.remove(entry.getKey());
                        Entry<V> injectedEntry = dominantEntry == null
                                ? entry
                                : dominantEntry.inject(entry.getKey());
                        entries.put(injectedEntry.getKey(), injectedEntry);
                        return new Store<V>(entries);
                    }

                    /**
                     * Transforms this store into a method graph by applying the given merger.
                     *
                     * @param merger The merger to apply for resolving the representative for ambiguous resolutions.
                     * @return The method graph that represents this key store.
                     */
                    protected MethodGraph asGraph(Merger merger) {
                        LinkedHashMap<Key<MethodDescription.TypeToken>, Node> entries = new LinkedHashMap<Key<MethodDescription.TypeToken>, Node>();
                        for (Entry<V> entry : this.entries.values()) {
                            Node node = entry.asNode(merger);
                            entries.put(entry.getKey().detach(node.getRepresentative().asTypeToken()), node);
                        }
                        return new Graph(entries);
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && entries.equals(((Store<?>) other).entries);
                    }

                    @Override
                    public int hashCode() {
                        return entries.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Key.Store{" +
                                "entries=" + entries +
                                '}';
                    }

                    /**
                     * An entry of a key store.
                     *
                     * @param <W> The type of the harmonized token used for determining method equality.
                     */
                    protected interface Entry<W> {

                        /**
                         * Returns the harmonized key of this entry.
                         *
                         * @return The harmonized key of this entry.
                         */
                        Harmonized<W> getKey();

                        /**
                         * Returns all candidate methods represented by this entry.
                         *
                         * @return All candidate methods represented by this entry.
                         */
                        Set<MethodDescription> getCandidates();

                        /**
                         * Extends this entry by the given method.
                         *
                         * @param methodDescription The method description to extend this entry with.
                         * @param harmonizer        The harmonizer to use for determining method equality.
                         * @return This key extended by the given method.
                         */
                        Entry<W> extendBy(MethodDescription methodDescription, Harmonizer<W> harmonizer);

                        /**
                         * Injects the given key into this entry.
                         *
                         * @param key The key to inject into this entry.
                         * @return This entry extended with the given key.
                         */
                        Entry<W> inject(Harmonized<W> key);

                        /**
                         * Transforms this entry into a node.
                         *
                         * @param merger The merger to use for determining the representative method of an ambiguous node.
                         * @return The resolved node.
                         */
                        Node asNode(Merger merger);

                        /**
                         * An entry in its initial state before registering any method as a representative.
                         *
                         * @param <U> The type of the harmonized key to determine method equality.
                         */
                        class Initial<U> implements Entry<U> {

                            /**
                             * The harmonized key this entry represents.
                             */
                            private final Harmonized<U> key;

                            /**
                             * Creates a new initial key.
                             *
                             * @param key The harmonized key this entry represents.
                             */
                            protected Initial(Harmonized<U> key) {
                                this.key = key;
                            }

                            @Override
                            public Harmonized<U> getKey() {
                                throw new IllegalStateException("Cannot extract key from initial entry:" + this);
                            }

                            @Override
                            public Set<MethodDescription> getCandidates() {
                                throw new IllegalStateException("Cannot extract method from initial entry:" + this);
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                return new Resolved<U>(key.extend(methodDescription.asDefined(), harmonizer), methodDescription, false);
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key) {
                                throw new IllegalStateException("Cannot inject into initial entry without a registered method: " + this);
                            }

                            @Override
                            public Node asNode(Merger merger) {
                                throw new IllegalStateException("Cannot transform initial entry without a registered method: " + this);
                            }

                            @Override
                            public boolean equals(Object other) {
                                return this == other || !(other == null || getClass() != other.getClass())
                                        && key.equals(((Initial<?>) other).key);
                            }

                            @Override
                            public int hashCode() {
                                return key.hashCode();
                            }

                            @Override
                            public String toString() {
                                return "MethodGraph.Compiler.Default.Key.Store.Entry.Initial{key=" + key + '}';
                            }
                        }

                        /**
                         * An entry representing a non-ambiguous node resolution.
                         *
                         * @param <U> The type of the harmonized key to determine method equality.
                         */
                        class Resolved<U> implements Entry<U> {

                            /**
                             * The harmonized key this entry represents.
                             */
                            private final Harmonized<U> key;

                            /**
                             * The non-ambiguous, representative method of this entry.
                             */
                            private final MethodDescription methodDescription;

                            /**
                             * {@code true} if this entry's representative was made visible by a visibility bridge.
                             */
                            private final boolean madeVisible;

                            /**
                             * Creates a new resolved entry.
                             *
                             * @param key               The harmonized key this entry represents.
                             * @param methodDescription The non-ambiguous, representative method of this entry.
                             * @param madeVisible       {@code true} if this entry's representative was made visible by a visibility bridge.
                             */
                            protected Resolved(Harmonized<U> key, MethodDescription methodDescription, boolean madeVisible) {
                                this.key = key;
                                this.methodDescription = methodDescription;
                                this.madeVisible = madeVisible;
                            }

                            @Override
                            public Harmonized<U> getKey() {
                                return key;
                            }

                            @Override
                            public Set<MethodDescription> getCandidates() {
                                return Collections.singleton(methodDescription);
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                Harmonized<U> key = this.key.extend(methodDescription.asDefined(), harmonizer);
                                return methodDescription.getDeclaringType().equals(this.methodDescription.getDeclaringType())
                                        ? Ambiguous.of(key, methodDescription, this.methodDescription)
                                        : new Resolved<U>(key, methodDescription.isBridge() ? this.methodDescription : methodDescription, methodDescription.isBridge());
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key) {
                                return new Resolved<U>(key.combineWith(key), methodDescription, madeVisible);
                            }

                            @Override
                            public MethodGraph.Node asNode(Merger merger) {
                                return new Node(key.detach(methodDescription.asTypeToken()), methodDescription, madeVisible);
                            }

                            @Override
                            public boolean equals(Object other) {
                                if (this == other) return true;
                                if (other == null || getClass() != other.getClass()) return false;
                                Resolved<?> resolved = (Resolved<?>) other;
                                return madeVisible == resolved.madeVisible
                                        && key.equals(resolved.key)
                                        && methodDescription.equals(resolved.methodDescription);
                            }

                            @Override
                            public int hashCode() {
                                int result = key.hashCode();
                                result = 31 * result + methodDescription.hashCode();
                                result = 31 * result + (madeVisible ? 1 : 0);
                                return result;
                            }

                            @Override
                            public String toString() {
                                return "MethodGraph.Compiler.Default.Key.Store.Entry.Resolved{" +
                                        "key=" + key +
                                        ", methodDescription=" + methodDescription +
                                        ", madeVisible=" + madeVisible +
                                        '}';
                            }

                            /**
                             * A node implementation representing a non-ambiguous method.
                             */
                            protected static class Node implements MethodGraph.Node {

                                /**
                                 * The detached key representing this node.
                                 */
                                private final Detached key;

                                /**
                                 * The representative method of this node.
                                 */
                                private final MethodDescription methodDescription;

                                /**
                                 * {@code true} if the represented method was made explicitly visible by a visibility bridge.
                                 */
                                private final boolean visible;

                                /**
                                 * Creates a new node.
                                 *
                                 * @param key               The detached key representing this node.
                                 * @param methodDescription The representative method of this node.
                                 * @param visible           {@code true} if the represented method was made explicitly visible by a visibility bridge.
                                 */
                                protected Node(Detached key, MethodDescription methodDescription, boolean visible) {
                                    this.key = key;
                                    this.methodDescription = methodDescription;
                                    this.visible = visible;
                                }

                                @Override
                                public Sort getSort() {
                                    return visible
                                            ? Sort.VISIBLE
                                            : Sort.RESOLVED;
                                }

                                @Override
                                public MethodDescription getRepresentative() {
                                    return methodDescription;
                                }

                                @Override
                                public Set<MethodDescription.TypeToken> getMethodTypes() {
                                    return key.getIdentifiers();
                                }

                                @Override
                                public boolean equals(Object other) {
                                    if (this == other) return true;
                                    if (other == null || getClass() != other.getClass()) return false;
                                    Node node = (Node) other;
                                    return visible == node.visible
                                            && key.equals(node.key)
                                            && methodDescription.equals(node.methodDescription);
                                }

                                @Override
                                public int hashCode() {
                                    int result = key.hashCode();
                                    result = 31 * result + methodDescription.hashCode();
                                    result = 31 * result + (visible ? 1 : 0);
                                    return result;
                                }

                                @Override
                                public String toString() {
                                    return "MethodGraph.Compiler.Default.Key.Store.Entry.Resolved.Node{" +
                                            "key=" + key +
                                            ", methodDescription=" + methodDescription +
                                            ", visible=" + visible +
                                            '}';
                                }
                            }
                        }

                        /**
                         * An entry representing an ambiguous node resolution.
                         *
                         * @param <U> The type of the harmonized key to determine method equality.
                         */
                        class Ambiguous<U> implements Entry<U> {

                            /**
                             * The harmonized key this entry represents.
                             */
                            private final Harmonized<U> key;

                            /**
                             * A set of ambiguous methods that this entry represents.
                             */
                            private final LinkedHashSet<MethodDescription> methodDescriptions;

                            /**
                             * Creates a new ambiguous entry.
                             *
                             * @param key                The harmonized key this entry represents.
                             * @param methodDescriptions A set of ambiguous methods that this entry represents.
                             */
                            protected Ambiguous(Harmonized<U> key, LinkedHashSet<MethodDescription> methodDescriptions) {
                                this.key = key;
                                this.methodDescriptions = methodDescriptions;
                            }

                            /**
                             * Creates a new ambiguous entry if both provided entries are not considered to be a bridge of one another.
                             *
                             * @param key   The key of the entry to be created.
                             * @param left  The left method to be considered.
                             * @param right The right method to be considered.
                             * @param <Q>   The type of the token of the harmonized key to determine method equality.
                             * @return The entry representing both methods.
                             */
                            protected static <Q> Entry<Q> of(Harmonized<Q> key, MethodDescription left, MethodDescription right) {
                                return left.isBridge() ^ right.isBridge()
                                        ? new Resolved<Q>(key, left.isBridge() ? right : left, false)
                                        : new Ambiguous<Q>(key, new LinkedHashSet<MethodDescription>(Arrays.asList(left, right)));
                            }

                            @Override
                            public Harmonized<U> getKey() {
                                return key;
                            }

                            @Override
                            public Set<MethodDescription> getCandidates() {
                                return methodDescriptions;
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                Harmonized<U> key = this.key.extend(methodDescription.asDefined(), harmonizer);
                                LinkedHashSet<MethodDescription> methodDescriptions = new LinkedHashSet<MethodDescription>(this.methodDescriptions.size() + 1);
                                TypeDescription declaringType = methodDescription.getDeclaringType().asErasure();
                                boolean bridge = methodDescription.isBridge();
                                for (MethodDescription extendedMethod : this.methodDescriptions) {
                                    if (extendedMethod.getDeclaringType().asErasure().equals(declaringType)) {
                                        if (extendedMethod.isBridge() ^ bridge) {
                                            methodDescriptions.add(bridge ? extendedMethod : methodDescription);
                                        } else {
                                            methodDescriptions.add(methodDescription);
                                            methodDescriptions.add(extendedMethod);
                                        }
                                    }
                                }
                                if (methodDescriptions.isEmpty()) {
                                    return new Resolved<U>(key, methodDescription, bridge);
                                } else if (methodDescriptions.size() == 1) {
                                    return new Resolved<U>(key, methodDescriptions.iterator().next(), false);
                                } else {
                                    return new Ambiguous<U>(key, methodDescriptions);
                                }
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key) {
                                return new Ambiguous<U>(key.combineWith(key), methodDescriptions);
                            }

                            @Override
                            public MethodGraph.Node asNode(Merger merger) {
                                Iterator<MethodDescription> iterator = methodDescriptions.iterator();
                                MethodDescription methodDescription = iterator.next();
                                while (iterator.hasNext()) {
                                    methodDescription = merger.merge(methodDescription, iterator.next());
                                }
                                return new Node(key.detach(methodDescription.asTypeToken()), methodDescription);
                            }

                            @Override
                            public boolean equals(Object other) {
                                if (this == other) return true;
                                if (other == null || getClass() != other.getClass()) return false;
                                Ambiguous<?> ambiguous = (Ambiguous<?>) other;
                                return key.equals(ambiguous.key) && methodDescriptions.equals(ambiguous.methodDescriptions);
                            }

                            @Override
                            public int hashCode() {
                                int result = key.hashCode();
                                result = 31 * result + methodDescriptions.hashCode();
                                return result;
                            }

                            @Override
                            public String toString() {
                                return "MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous{" +
                                        "key=" + key +
                                        ", methodDescriptions=" + methodDescriptions +
                                        '}';
                            }

                            /**
                             * A node implementation representing an ambiguous method resolution.
                             */
                            protected static class Node implements MethodGraph.Node {

                                /**
                                 * The detached key representing this node.
                                 */
                                private final Detached key;

                                /**
                                 * The representative method of this node.
                                 */
                                private final MethodDescription methodDescription;

                                /**
                                 * @param key               The detached key representing this node.
                                 * @param methodDescription The representative method of this node.
                                 */
                                protected Node(Detached key, MethodDescription methodDescription) {
                                    this.key = key;
                                    this.methodDescription = methodDescription;
                                }

                                @Override
                                public Sort getSort() {
                                    return Sort.AMBIGUOUS;
                                }

                                @Override
                                public MethodDescription getRepresentative() {
                                    return methodDescription;
                                }

                                @Override
                                public Set<MethodDescription.TypeToken> getMethodTypes() {
                                    return key.getIdentifiers();
                                }

                                @Override
                                public boolean equals(Object other) {
                                    if (this == other) return true;
                                    if (other == null || getClass() != other.getClass()) return false;
                                    Node node = (Node) other;
                                    return key.equals(node.key) && methodDescription.equals(node.methodDescription);
                                }

                                @Override
                                public int hashCode() {
                                    int result = key.hashCode();
                                    result = 31 * result + methodDescription.hashCode();
                                    return result;
                                }

                                @Override
                                public String toString() {
                                    return "MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous.Node{" +
                                            "key=" + key +
                                            ", methodDescription=" + methodDescription +
                                            '}';
                                }
                            }
                        }
                    }

                    /**
                     * A graph implementation based on a key store.
                     */
                    protected static class Graph implements MethodGraph {

                        /**
                         * A mapping of a node's type tokens to the represented node.
                         */
                        private final LinkedHashMap<Key<MethodDescription.TypeToken>, Node> entries;

                        /**
                         * Creates a new graph.
                         *
                         * @param entries A mapping of a node's type tokens to the represented node.
                         */
                        protected Graph(LinkedHashMap<Key<MethodDescription.TypeToken>, Node> entries) {
                            this.entries = entries;
                        }

                        @Override
                        public Node locate(MethodDescription.SignatureToken token) {
                            Node node = entries.get(Detached.of(token));
                            return node == null
                                    ? Node.Unresolved.INSTANCE
                                    : node;
                        }

                        @Override
                        public NodeList listNodes() {
                            return new NodeList(new ArrayList<Node>(entries.values()));
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && entries.equals(((Graph) other).entries);
                        }

                        @Override
                        public int hashCode() {
                            return entries.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "MethodGraph.Compiler.Default.Key.Store.Graph{" +
                                    "entries=" + entries +
                                    '}';
                        }
                    }
                }
            }
        }
    }

    /**
     * A list of nodes.
     */
    class NodeList extends FilterableList.AbstractBase<Node, NodeList> {

        /**
         * The represented nodes.
         */
        private final List<? extends Node> nodes;

        /**
         * Creates a list of nodes.
         *
         * @param nodes The represented nodes.
         */
        public NodeList(List<? extends Node> nodes) {
            this.nodes = nodes;
        }

        @Override
        public Node get(int index) {
            return nodes.get(index);
        }

        @Override
        public int size() {
            return nodes.size();
        }

        @Override
        protected NodeList wrap(List<Node> values) {
            return new NodeList(values);
        }

        /**
         * Transforms this list of nodes into a list of the node's representatives.
         *
         * @return A list of these node's representatives.
         */
        public MethodList<?> asMethodList() {
            List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(size());
            for (Node node : nodes) {
                methodDescriptions.add(node.getRepresentative());
            }
            return new MethodList.Explicit<MethodDescription>(methodDescriptions);
        }
    }

    /**
     * A simple implementation of a method graph.
     */
    class Simple implements MethodGraph {

        /**
         * The nodes represented by this method graph.
         */
        private final LinkedHashMap<MethodDescription.SignatureToken, Node> nodes;

        /**
         * Creates a new simple method graph.
         *
         * @param nodes The nodes represented by this method graph.
         */
        public Simple(LinkedHashMap<MethodDescription.SignatureToken, Node> nodes) {
            this.nodes = nodes;
        }

        /**
         * Returns a method graph that contains all of the provided methods as simple nodes.
         *
         * @param methodDescriptions A list of method descriptions to be represented as simple nodes.
         * @return A method graph that represents all of the provided methods as simple nodes.
         */
        public static MethodGraph of(List<? extends MethodDescription> methodDescriptions) {
            LinkedHashMap<MethodDescription.SignatureToken, Node> nodes = new LinkedHashMap<MethodDescription.SignatureToken, Node>();
            for (MethodDescription methodDescription : methodDescriptions) {
                nodes.put(methodDescription.asSignatureToken(), new Node.Simple(methodDescription));
            }
            return new Simple(nodes);
        }

        @Override
        public Node locate(MethodDescription.SignatureToken token) {
            Node node = nodes.get(token);
            return node == null
                    ? Node.Unresolved.INSTANCE
                    : node;
        }

        @Override
        public NodeList listNodes() {
            return new NodeList(new ArrayList<Node>(nodes.values()));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && nodes.equals(((Simple) other).nodes);
        }

        @Override
        public int hashCode() {
            return nodes.hashCode();
        }

        @Override
        public String toString() {
            return "MethodGraph.Simple{" +
                    "nodes=" + nodes +
                    '}';
        }
    }
}
