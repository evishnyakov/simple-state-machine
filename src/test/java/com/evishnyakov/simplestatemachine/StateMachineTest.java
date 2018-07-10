package com.evishnyakov.simplestatemachine;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StateMachineTest {

    private enum State {
        A, B, C
    }

    private enum Transition {
        T1, T2, T3
    }

    private StateMachine.StateMachineBuilder<State, Transition> builder;

    @Before
    public void setUp() {
        builder = StateMachine.builder();
    }

    @Test
    public void noStateBuilder() {
        assertEquals("{}", builder.build().toString());
    }

    @Test
    public void oneStateBuilder() {
        assertEquals("{A{T1=B},B{}}", builder
                .init(State.A)
                .transition(State.A, Transition.T1, State.B)
                .build().toString());
    }

    @Test
    public void complexStateBuilder() {
        assertEquals("{A{T1=B, T2=C},B{T1=A, T2=C},C{T1=A, T2=B, T3=C}}", builder
                .init(State.A).init(State.B)
                .transition(State.A, Transition.T1, State.B)
                .transition(State.A, Transition.T2, State.C)
                .transition(State.B, Transition.T1, State.A)
                .transition(State.B, Transition.T2, State.C)
                .transition(State.C, Transition.T1, State.A)
                .transition(State.C, Transition.T2, State.B)
                .transition(State.C, Transition.T3, State.C)
                .build().toString());
    }

    @Test(expected = IllegalStateException.class)
    public void noAccessibleState() {
        builder.transition(State.A, Transition.T1, State.B).build();
    }

    @Test
    public void nextState() {
        StateMachine<State, Transition> stateMachine = builder.init(State.A).init(State.B)
                .transition(State.A, Transition.T1, State.B)
                .transition(State.A, Transition.T2, State.C)
                .transition(State.B, Transition.T1, State.A)
                .transition(State.B, Transition.T2, State.C)
                .transition(State.C, Transition.T1, State.A)
                .transition(State.C, Transition.T2, State.B)
                .transition(State.C, Transition.T3, State.C)
                .build();

        assertEquals(State.B, stateMachine.nextState(State.A, Transition.T1).get());
        assertEquals(State.C, stateMachine.nextState(State.A, Transition.T2).get());
        assertEquals(State.A, stateMachine.nextState(State.C, Transition.T1).get());
        assertEquals(State.C, stateMachine.nextState(State.C, Transition.T3).get());

        assertFalse(stateMachine.nextState(State.A, Transition.T3).isPresent());
    }


}

