/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceNaturalIdRegion;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A NaturalIdReadOnlyCoherenceRegionAccessStrategy is a CoherenceRegionAccessStrategy
 * implementing Hibernate's read-only cache concurrency strategy for a natural ID region.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class NaturalIdReadOnlyCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceNaturalIdRegion>
implements NaturalIdRegionAccessStrategy
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceNaturalIdRegion the CoherenceNaturalIdRegion for this NaturalIdReadOnlyCoherenceRegionAccessStrategy
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions object
     */
    public NaturalIdReadOnlyCoherenceRegionAccessStrategy(CoherenceNaturalIdRegion coherenceNaturalIdRegion, SessionFactoryOptions sessionFactoryOptions)
    {
        super(coherenceNaturalIdRegion, sessionFactoryOptions);
    }


    // ---- interface org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalIdRegion getRegion()
    {
        debugf("%s.getRegion()", this);
        return getCoherenceRegion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        debugf("%s.insert(%s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        debugf("%s.afterInsert(%s, %s)", getClass().getName(), key, value);
        getCoherenceRegion().putValue(key, newCacheValue(value, null));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.update(%s, %s)", this, key, value);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.afterUpdate(%s, %s, %s)", this, key, value, lock);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public Object generateCacheKey(Object[] naturalIdValues, EntityPersister entityPersister, SharedSessionContractImplementor session)
    {
        return DefaultCacheKeysFactory.staticCreateNaturalIdKey(naturalIdValues, entityPersister, session);
    }

    @Override
    public Object[] getNaturalIdValues(Object cacheKey)
    {
        return DefaultCacheKeysFactory.staticGetNaturalIdValues(cacheKey);
    }
}
