/**
 * Domain layer — pure business logic.
 *
 * <p><b>Hexagonal rule:</b> this package and its sub-packages MUST NOT depend on
 * any framework (Spring, JPA, Kafka, Redis, Jackson, etc.). It contains only
 * business entities, value objects, domain services, and ports (interfaces).
 *
 * <p>Sub-packages:
 * <ul>
 *   <li>{@code model}     — entities &amp; value objects (CreditAccount, Money, ...)</li>
 *   <li>{@code event}     — domain events</li>
 *   <li>{@code exception} — domain exceptions</li>
 *   <li>{@code port.in}   — inbound ports (use case interfaces)</li>
 *   <li>{@code port.out}  — outbound ports (repository, publisher interfaces)</li>
 * </ul>
 */
package com.akulaku.transaction.domain;
