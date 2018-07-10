package com.evishnyakov.simplestatemachine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

/**
 * Model a system as a set of explicit states with transitions between them.
 *
 * S - State
 * T - transition
 */
public class StateMachine<S extends Enum<S>, T extends Enum<T>> {

    private final Map<S, State<S, T>> states = new HashMap<>();

    private StateMachine(Map<S, State<S, T>> states) {
        this.states.putAll(states);
    }

    public Optional<S> nextState(S currentState, T transition) {
        State<S, T> state = this.states.get(currentState);
        if(state == null) {
            return Optional.empty();
        }
        State<S, T> nextState = state.getTransitions().get(transition);
        if(nextState == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nextState.getSource());
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",", "{", "}");

        List<S> allStates = new ArrayList<>(states.keySet());
        Collections.sort(allStates);
        allStates.forEach(t -> joiner.add(states.get(t).print()));

        return joiner.toString();
    }

    public static <S extends Enum<S>, T extends Enum<T>> StateMachineBuilder<S, T> builder() {
        return new StateMachineBuilder<>();
    }

    public static class StateMachineBuilder<S extends Enum<S>, T extends Enum<T>> {

        private final Set<S> init = new HashSet<>();
        private final List<Transition<S, T>> transitions = new ArrayList<>();

        public StateMachineBuilder<S, T> init(S source) {
            this.init.add(source);
            return this;
        }
        public StateMachineBuilder<S, T> transition(S source, T transition, S target) {
            this.transitions.add(new Transition<>(source, transition, target));
            return this;
        }

        private void addRecursive(State<S,T> state, Set<State<S,T>> storage) {
            if(storage.add(state)) {
                state.getTransitions().values().forEach(t -> addRecursive(t, storage));
            }
        }

        private void checkAccessibility(Collection<State<S,T>> initStates, Collection<State<S,T>> allStates) {
            Set<State<S,T>> storage = new HashSet<>();
            initStates.forEach(s -> addRecursive(s,  storage));

            Set<State<S,T>> allStatesSet = new HashSet<>(allStates);
            allStatesSet.removeAll(storage);
            if(!allStatesSet.isEmpty()) {
                throw new IllegalStateException("Some states are not accessible from initial states: " + allStatesSet);
            }
        }

        public StateMachine<S, T> build() {
            Map<S, State<S, T>> all = new HashMap<>();
            transitions.stream()
                    .flatMap(t -> of(t.getSource(), t.getTarget()))
                    .forEach(s -> all.put(s, new State<>(s)));

            this.transitions.forEach(t -> {
                State<S,T> source = all.get(t.getSource());
                State<S,T> target = all.get(t.getTarget());
                source.transition(t.getTransition(), target);
            });

            Collection<State<S, T>> initStates = all.values().stream().filter(s -> init.contains(s.getSource())).collect(toList());

            checkAccessibility(initStates, all.values());
            return new StateMachine<>(all);
        }

    }

    @Getter
    @RequiredArgsConstructor
    private static class Transition<S extends Enum<S>, T extends Enum<T>> {
        private final S source;
        private final T transition;
        private final S target;
    }

    @Getter
    public static class State<S extends Enum<S>, T extends Enum<T>> {

        private final S source;
        private final Map<T, State<S,T>> transitions = new HashMap<>();

        public State(S source) {
            this.source = source;
        }

        /**
         * Define transition
         * @return this
         */
        public void transition(T transition, State s) {
            this.transitions.put(requireNonNull(transition), requireNonNull(s));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(source);
        }
        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if(!(obj instanceof State)) {
                return false;
            }
            return this.getSource() == ((State) obj).getSource();
        }

        @Override
        public String toString() {
            return Objects.toString(source);
        }

        public String print() {
            StringJoiner joiner = new StringJoiner(", ", toString() + "{", "}");

            List<T> allTransitions = new ArrayList<>(transitions.keySet());
            Collections.sort(allTransitions);
            allTransitions.forEach(t -> joiner.add(Objects.toString(t) + "=" + transitions.get(t)));

            return joiner.toString();
        }

    }

}
