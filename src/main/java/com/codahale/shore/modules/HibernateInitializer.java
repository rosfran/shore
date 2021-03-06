package com.codahale.shore.modules;

import static com.google.common.base.Preconditions.*;
import net.jcip.annotations.Immutable;

import com.google.inject.Inject;
import com.wideplay.warp.persist.PersistenceService;

/**
 * Allows Warp-Persist to start Hibernate.
 * 
 * @author coda
 */
@Immutable
public class HibernateInitializer {
	@Inject
	public HibernateInitializer(PersistenceService service) {
		checkNotNull(service).start();
	}
}
