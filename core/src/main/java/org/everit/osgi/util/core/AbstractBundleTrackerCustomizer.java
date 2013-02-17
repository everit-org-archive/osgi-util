package org.everit.osgi.util.core;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * An abstract bundle tracker customizer that handles the bundle state and its change.
 */
public abstract class AbstractBundleTrackerCustomizer implements BundleTrackerCustomizer {

    private static final String SYMBOLIC_NAME = "symbolicName";

    private static final String BUNDLE_ID = "bundleId";

    private static final String PROCESSD_BY = "processdBy";

    /**
     * The set of the processed bundle IDs. The bundle id will be added if addingBundle does not fail and the bundle id
     * will be removed if removedBundle invoked.
     */
    private final Set<Long> processedBundles = new ConcurrentSkipListSet<Long>();

    private final Map<Long, ServiceRegistration> registeredServices = new ConcurrentHashMap<Long, ServiceRegistration>();

    private final BundleContext bundleContext;

    private final String id;

    /**
     * Default constructor.
     */
    public AbstractBundleTrackerCustomizer(final BundleContext bundleContext, final String id) {
        super();
        this.bundleContext = bundleContext;
        this.id = id;
    }

    @Override
    public Object addingBundle(final Bundle bundle, final BundleEvent event) {
        Long bundleId = Long.valueOf(bundle.getBundleId());
        if (processedBundles.contains(bundleId)) {
            // bundle processed already, we are not interested in
            return bundle;
        }
        if ((event == null) || (bundle.getState() == Bundle.ACTIVE) || (bundle.getState() == Bundle.STARTING)) {
            boolean registerService = handleBundleAdded(bundle);
            if (registerService) {
                registerService(bundle);
            }
        } else if ((event.getType() == BundleEvent.LAZY_ACTIVATION) || (event.getType() == BundleEvent.STARTED)) {
            unregisterService(event.getBundle());
            boolean registerService = handleBundleChanged(event);
            if (registerService) {
                registerService(event.getBundle());
            }
        }
        processedBundles.add(bundleId);
        return bundle;
    }

    /**
     * Event handler invoked when the bundle is added. Will be invoked if the event is null, the bundle state is ACTIVE
     * or STARTING.
     * 
     * @param bundle
     *            The bundle itself.
     * @return <code>true</code> if a {@link TrackedBundle} service should be created, otherwise <code>false</code>.
     */
    protected abstract boolean handleBundleAdded(Bundle bundle);

    /**
     * Event handler invoked when the bundle is changed. Will be invoked if the event type is LAZY_ACTIVATION or
     * STARTED.
     * 
     * @param event
     *            The bundle event. The source bundle can be read by {@link BundleEvent#getBundle()}.
     * @return <code>true</code> if a {@link TrackedBundle} service should be created, otherwise <code>false</code>.
     */
    protected abstract boolean handleBundleChanged(BundleEvent event);

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Object object) {
        if (event == null) {
            // cannot think of why we would be interested in a modified bundle with no bundle event
            return;
        }
        unregisterService(event.getBundle());
        handleBundleChanged(event);
        registerService(event.getBundle());
    }

    private void registerService(final Bundle bundle) {
        Dictionary<String, String> props = new Hashtable<String, String>();
        long bundleId = bundle.getBundleId();
        props.put(BUNDLE_ID, "" + bundleId);
        props.put(PROCESSD_BY, id);
        props.put(SYMBOLIC_NAME, bundle.getSymbolicName());
        props.put(TrackedBundleState.class.getName(), TrackedBundleState.PROCESSED.toString());
        // FIXME handle more TrackedBundleState
        ServiceRegistration serviceRegistration =
                bundleContext.registerService(TrackedBundle.class.getName(), new TrackedBundle() {
                }, props);
        registeredServices.put(bundleId, serviceRegistration);
    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Object object) {
        unregisterService(bundle);
        processedBundles.remove(bundle.getBundleId());
    }

    private void unregisterService(final Bundle bundle) {
        long bundleId = bundle.getBundleId();
        if (!registeredServices.containsKey(bundleId)) {
            return;
        }
        ServiceRegistration serviceRegistration = registeredServices.get(bundleId);
        ServiceReference reference = serviceRegistration.getReference();
        bundleContext.ungetService(reference);
        registeredServices.remove(bundleId);
    }

}
