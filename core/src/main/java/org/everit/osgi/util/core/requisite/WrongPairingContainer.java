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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;

/**
 * Storing dependentObject-reference pairs that surely not work together.
 */
public class WrongPairingContainer<D> {

  private Map<D, Set<ServiceReference>> wrongReferencesOfDependentObjects = new HashMap<D, Set<ServiceReference>>();

  private Map<ServiceReference, Set<D>> notSatisfyingObjectsOfReferences = new HashMap<ServiceReference, Set<D>>();

  private Object helper = new Object();

  public boolean isWrongPairing(D dependentObject, ServiceReference reference) {
    synchronized (helper) {
      Set<ServiceReference> wrongReferences = wrongReferencesOfDependentObjects.get(dependentObject);
      if (wrongReferences != null) {
        return wrongReferences.contains(reference);
      } else {
        return false;
      }
    }
  }

  public void addWrongPairing(D dependentObject, ServiceReference reference) {
    synchronized (helper) {
      Set<ServiceReference> references = wrongReferencesOfDependentObjects.get(dependentObject);
      if (references == null) {
        references = new HashSet<ServiceReference>();
        wrongReferencesOfDependentObjects.put(dependentObject, references);
      }
      references.add(reference);

      // Other way
      Set<D> dependentObjects = notSatisfyingObjectsOfReferences.get(reference);
      if (dependentObjects == null) {
        dependentObjects = new HashSet<D>();
        notSatisfyingObjectsOfReferences.put(reference, dependentObjects);
      }
      dependentObjects.add(dependentObject);
    }
  }

  public void removeReference(ServiceReference reference) {
    synchronized (helper) {
      Set<D> dependentObjects = notSatisfyingObjectsOfReferences.get(reference);
      if (dependentObjects != null) {
        for (D d : dependentObjects) {
          Set<ServiceReference> references = wrongReferencesOfDependentObjects.get(d);
          if (references != null) {
            references.remove(reference);
          }
        }
      }
      notSatisfyingObjectsOfReferences.remove(reference);
    }
  }

  public void removeDependentObject(D dependentObject) {
    synchronized (helper) {
      Set<ServiceReference> references = wrongReferencesOfDependentObjects.get(dependentObject);
      if (references != null) {
        for (ServiceReference reference : references) {
          Set<D> dependentObjects = notSatisfyingObjectsOfReferences.get(reference);
          if (dependentObjects != null) {
            dependentObjects.remove(dependentObject);
          }
        }
      }
      wrongReferencesOfDependentObjects.remove(dependentObject);
    }
  }
}
