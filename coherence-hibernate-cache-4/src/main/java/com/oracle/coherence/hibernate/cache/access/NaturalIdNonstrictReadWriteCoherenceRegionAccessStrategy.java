/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceNaturalIdRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * A NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy is a CoherenceRegionAccessStrategy
 * implementing Hibernate's nonstrict-read-write cache concurrency strategy for a natural ID region.
 *
 * @author Randy Stafford
 */
public class NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceNaturalIdRegion>
implements NaturalIdRegionAccessStrategy
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceNaturalIdRegion the CoherenceNaturalIdRegion for this NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy(CoherenceNaturalIdRegion coherenceNaturalIdRegion, Settings settings)
    {
        super(coherenceNaturalIdRegion, settings);
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
    public boolean insert(Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        debugf("%s.insert(%s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        //But in nonstrict-read-write cache concurrency strategies, don't put newly inserted natural IDs, to force
        //subsequent putFromLoad.
        debugf("%s.afterInsert(%s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence lockItem() -> remove() -> update() -> afterUpdate() when updating a natural ID.
        //"Synchronous" (i.e. transactional) access strategies should update the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should update it in afterUpdate instead.
        debugf("%s.update(%s, %s, %s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence lockItem() -> remove() -> update() -> afterUpdate() when updating a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should invalidate or update the cache entry here and release the lock,
        //as appropriate for the kind of strategy (nonstrict-read-write vs. read-write).
        //In the nonstrict-read-write strategy we remove the cache entry to force subsequent putFromLoad.
        debugf("%s.afterUpdate(%s, %s, %s)", this, key, value, lock);
        remove(key);
        unlockItem(key, lock);
        return false;
    }


}