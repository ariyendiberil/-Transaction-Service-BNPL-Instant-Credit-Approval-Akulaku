/**
 * Infrastructure layer — driven adapters.
 *
 * <p>Concrete implementations of outbound ports ({@code domain.port.out}) using
 * specific technologies: JPA/Hibernate for persistence, Lettuce for Redis,
 * Spring Kafka for messaging. These adapters translate between the domain
 * model and external systems.
 */
package com.akulaku.transaction.infrastructure;
