/**
 * Application layer — use case orchestration.
 *
 * <p>Implements inbound ports defined in {@code domain.port.in}. Coordinates
 * domain objects and outbound ports (repositories, publishers) to fulfil a
 * use case. Should remain framework-light; only Spring stereotypes
 * ({@code @Service}, {@code @Transactional}) are allowed here.
 */
package com.akulaku.transaction.application;
