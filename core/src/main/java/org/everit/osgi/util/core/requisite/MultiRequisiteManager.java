package org.everit.osgi.util.core.requisite;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.ServiceReference;

public class MultiRequisiteManager<D> {

    private class InnerRequisiteListener implements RequisiteListener<D> {

        private final String requisiteId;

        public InnerRequisiteListener(final String requisiteId) {
            this.requisiteId = requisiteId;
        }

        @Override
        public void requisiteAvailable(final D dependentObject, final ServiceReference reference) {
            Map<String, ServiceReference> allReferences = null;
            synchronized (helper) {
                LOGGER.info("Service reference '" + reference.toString() + "' available for "
                        + dependentObject.toString() + " in multi requisiite tracker");
                Map<String, ServiceReference> references = dependentObjectWithReferences.get(dependentObject);
                if (references == null) {
                    references = new HashMap<String, ServiceReference>();
                    dependentObjectWithReferences.put(dependentObject, references);
                }
                references.put(requisiteId, reference);
                if (references.size() == dependentObjectWithRequisiteTrackers.get(dependentObject).size()) {
                    allReferences = references;
                }
            }
            if (allReferences != null) {
                multiRequisiteListener.startDependentObject(dependentObject, allReferences);
            }
        }

        @Override
        public void requisiteRemoved(final D dependentObject, final ServiceReference reference) {
            if (reference == null) {
               LOGGER.info("Requisite removed from dependent object: " + dependentObject.toString()
                        + " when removed bundle from tracking");
            } else {
                LOGGER.info("Requisite '" +reference.toString() + "' removed that was registered for dependent object " + dependentObject.toString());
            }

            boolean stoppingEvent = false;
            synchronized (helper) {
                Map<String, ServiceReference> references = dependentObjectWithReferences.get(dependentObject);
                int referencesSize = references.size();
                int requisiteTrackersCount = dependentObjectWithRequisiteTrackers.get(dependentObject).size();
                if (referencesSize == requisiteTrackersCount) {
                    stoppingEvent = true;
                }
                if (reference != null) {
                    references.remove(requisiteId);
                }
            }
            if (stoppingEvent) {
                multiRequisiteListener.stopDependentObject(dependentObject);
            }
        }

    }

    /** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger("org.apache.aries.jpa.container");

    private MultiRequisiteListener<D> multiRequisiteListener;

    private Map<D, Map<String, ServiceReference>> dependentObjectWithReferences = new ConcurrentHashMap<D, Map<String, ServiceReference>>();

    private Map<D, Map<String, AbstractRequisiteTracker<D>>> dependentObjectWithRequisiteTrackers = new ConcurrentHashMap<D, Map<String, AbstractRequisiteTracker<D>>>();

    /**
     * Helper object for thread synchronization.
     */
    private Object helper = new Object();

    public MultiRequisiteManager(final MultiRequisiteListener<D> multiRequisiteListener) {
        this.multiRequisiteListener = multiRequisiteListener;
    }

    public void registerDependentObject(final D dependentObject,
            final Map<String, AbstractRequisiteTracker<D>> requisiteTrackers) {
        synchronized (helper) {
            dependentObjectWithRequisiteTrackers.put(dependentObject, requisiteTrackers);
            for (Entry<String, AbstractRequisiteTracker<D>> requisiteTrackerWithId : requisiteTrackers.entrySet()) {
                requisiteTrackerWithId.getValue().addDependentObject(dependentObject,
                        new InnerRequisiteListener(requisiteTrackerWithId.getKey()));
            }
        }
    }

    public void removeDependentObject(final D dependentObject) {
        synchronized (helper) {
            Map<String, AbstractRequisiteTracker<D>> requisiteTrackers = dependentObjectWithRequisiteTrackers
                    .get(dependentObject);
            if (requisiteTrackers != null) {
                for (Entry<String, AbstractRequisiteTracker<D>> requisiteTrackerWithId : requisiteTrackers.entrySet()) {
                    requisiteTrackerWithId.getValue().removeDependentObject(dependentObject);
                }
                dependentObjectWithReferences.remove(dependentObject);
                dependentObjectWithRequisiteTrackers.remove(dependentObject);
            } else {
                LOGGER.warn("Removing was called on a dependentObject that is not "
                        + "part of the MultiRequisiteManager: "
                        + dependentObject.toString());
            }
        }
    }

}
