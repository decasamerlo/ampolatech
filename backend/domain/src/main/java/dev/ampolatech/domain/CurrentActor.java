package dev.ampolatech.domain;

/**
 * The actor of the current request (ADR 0004). It never returns null: with nobody signed in, it
 * throws. The pharmacy a request acts for is the actor's, never one the request names (ADR 0003).
 * Application code never asks for it: a driving port receives the actor as a parameter, or inside
 * its command.
 */
public interface CurrentActor {

  Actor get();
}
