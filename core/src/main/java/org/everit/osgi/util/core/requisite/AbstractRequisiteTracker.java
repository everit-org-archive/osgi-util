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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tracks requisites for an component. When the requisite is available or removed it calls the provided
 * {@link RequisiteListener}. In case of this bundle requisites are Services in the OSGI container.
 */
public abstract class AbstractRequisiteTracker<D> extends ServiceTracker {

    /** 
     * Logger. 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("org.apache.aries.jpa.container");

    /**
     * Stores the saved comparable for the currently tracked service references.
     */
    Map<ServiceReference, Comparable<ServiceReference>> referencesWithComparators = new ConcurrentHashMap<ServiceReference, Comparable<ServiceReference>>();

    /**
     * Components that are waiting for a good service reference.
     */
    private Map<D, Boolean> awaitingObjects = new ConcurrentHashMap<D, Boolean>();

    /**
     * Components that got requirements from this manager and till now they accepted it.
     */
    private Map<D, ServiceReference> referenceByUsingObject = new ConcurrentHashMap<D, ServiceReference>();

    /**
     * Components by the requirements they use.
     */
    private Map<ServiceReference, Set<D>> objectsThatUseReferences = new ConcurrentHashMap<ServiceReference, Set<D>>();

    private Map<D, RequisiteListener> requisiteListenersOfDependentObjets = new ConcurrentHashMap<D, RequisiteListener>();

    private WrongPairingContainer<D> wrongPairingContainer = new WrongPairingContainer<D>();

    public AbstractRequisiteTracker(final BundleContext context, final String filter)
            throws InvalidSyntaxException {
        super(context, context.createFilter(filter), null);
    }

    /**
     * From now on the manager will take care of the requirements of this component.
     * 
     * The parsed persistence units of this component.
     */
    public void addDependentObject(final D dependentObject,
            final RequisiteListener<D> requisiteListener) {
        if (objectsThatUseReferences.containsKey(dependentObject)) {
            LOGGER.warn("addDependentObject was called with an already satisfied object. Do nothing: "
                    + dependentObject.toString());
            return;
        }
        if (awaitingObjects.put(dependentObject, Boolean.TRUE) != null) {
            LOGGER.warn("addDependentObject was called with an already waiting object: "
                    + dependentObject.toString());
        }
        requisiteListenersOfDependentObjets.put(dependentObject,
                requisiteListener);

        foundReqiurmentForDependObject(dependentObject);
    }

    private void foundReqiurmentForDependObject(final D dependentObject) {
        Iterator<ServiceReference> referenceIterator = referencesWithComparators
                .keySet().iterator();
        boolean foundRequirement = false;
        while (referenceIterator.hasNext() && !foundRequirement) {
            ServiceReference requirement = referenceIterator.next();
            if (!wrongPairingContainer.isWrongPairing(dependentObject,
                    requirement)) {
                foundRequirement = tryPairing(dependentObject, requirement);
            }
        }
    }

    /**
     * Clever subclasses may override this function and instead of iterating they simply call tryRequirement with the
     * components they know as well.
     */
    @Override
    public Object addingService(final ServiceReference reference) {
        for (D component : awaitingObjects.keySet()) {
            if (!wrongPairingContainer.isWrongPairing(component, reference)) {
                tryPairing(component, reference);
            }
        }
        referencesWithComparators.put(reference,
                createComparableFromReference(reference));
        return reference;
    }

    /**
     * When the {@link ServiceTrackerCustomizer#modifiedService(ServiceReference, Object)} is called we want to decide
     * if we should care about it. Therefore this function must create a Comparable object that checks the new
     * {@link ServiceReference} with the modified properties and decides if it is equal to the old one from the
     * requirement point of view. For example in case of a DataSourceFactory we only care if the property that shows the
     * name of the jdbc driver changes.
     * 
     * @param reference
     *            The modified service reference.
     * @return An object that can check itself against a ServiceReference (but the object does not have to be itself a
     *         ServiceReference as this object is not used as the key of any collection).
     */
    protected abstract Comparable<ServiceReference> createComparableFromReference(
            ServiceReference reference);

    public ServiceReference getServiceReferenceByComponent(final D component) {
        return referenceByUsingObject.get(component);
    }

    /**
     * Deciding whether the requirement could be good for the dependent object.
     * 
     * @param dependentObject
     *            The component.
     * @param reference
     *            The requisite that should be checked.
     * @param parsedPersistenceUnit
     *            The persistence unit that belongs to the component.
     * @return whether the requirement could be used to this component or not.
     */
    protected abstract boolean isReferenceSuitable(D dependentObject,
            ServiceReference reference);

    @Override
    public void modifiedService(final ServiceReference reference, final Object service) {
        Comparable<ServiceReference> comparable = referencesWithComparators
                .get(reference);
        if (comparable.compareTo(reference) != 0) {
            removedService(reference, service);
            addingService(reference);
        }
    }

    public void referenceAcceptanceCancelled(final D dependentObject,
            final ServiceReference reference) {
        referenceByUsingObject.remove(dependentObject);
        objectsThatUseReferences.remove(reference);
        awaitingObjects.put(dependentObject, Boolean.TRUE);
        wrongPairingContainer.addWrongPairing(dependentObject, reference);
    }

    /**
     * Should be called when this manager should not take care of the requirements of this component anymore.
     * 
     * @param dependentObject
     *            The component.
     */
    public void removeDependentObject(final D dependentObject) {
        RequisiteListener requisiteListener = requisiteListenersOfDependentObjets.remove(dependentObject);
        ServiceReference reference = referenceByUsingObject
                .remove(dependentObject);

        if ((reference != null) && (requisiteListener != null)) {
            requisiteListener.requisiteRemoved(dependentObject, reference);
        } else {
            awaitingObjects.remove(dependentObject);
        }

        if (reference != null) {
            objectsThatUseReferences.get(reference).remove(dependentObject);
        }
        wrongPairingContainer.removeDependentObject(dependentObject);
    }

    @Override
    public void removedService(final ServiceReference reference, final Object service) {
        wrongPairingContainer.removeReference(reference);

        Set<D> objectsThatUseReference = objectsThatUseReferences
                .remove(reference);
        if (objectsThatUseReference != null) {
            for (D dependentObject : objectsThatUseReference) {
                requisiteListenersOfDependentObjets.get(dependentObject)
                        .requisiteRemoved(dependentObject, reference);
                referenceByUsingObject.remove(dependentObject);
                awaitingObjects.put(dependentObject, Boolean.TRUE);

            }
        }
        referencesWithComparators.remove(reference);
        if (objectsThatUseReference != null) {
            for (D dependentObject : objectsThatUseReference) {
                foundReqiurmentForDependObject(dependentObject);
            }
        }
    }

    protected boolean tryPairing(final D dependentObject, final ServiceReference reference) {
        try {
            if (isReferenceSuitable(dependentObject, reference)) {
                requisiteListenersOfDependentObjets.get(dependentObject)
                        .requisiteAvailable(dependentObject, reference);
                referenceByUsingObject.put(dependentObject, reference);
                Set<D> objectsThatUseReference = objectsThatUseReferences
                        .get(reference);
                if (objectsThatUseReference == null) {
                    objectsThatUseReference = new HashSet<D>();
                    objectsThatUseReferences.put(reference,
                            objectsThatUseReference);
                }
                objectsThatUseReference.add(dependentObject);
                awaitingObjects.remove(dependentObject);
                return true;
            } else {
                referenceAcceptanceCancelled(dependentObject, reference);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Exception during trying if a requirement matches for persistence component: [requirement: "
                            + reference.toString() + ", compoenent: " + dependentObject.toString() + "]", e);
            referenceAcceptanceCancelled(dependentObject, reference);
            return false;
        }
    }
}
