package org.everit.osgi.util.tests.core;

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

import org.everit.osgi.util.core.requisite.AbstractRequisiteTracker;
import org.everit.osgi.util.core.requisite.MultiRequisiteListener;
import org.everit.osgi.util.core.requisite.MultiRequisiteManager;
import org.everit.osgi.util.core.requisite.RequisiteListener;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Implementation of the {@link SampleRequisite}.
 */
public class SampleRequisiteImpl implements SampleRequisite {
    /**
     * Simple implementation of {@link MultiRequisiteListener}. Increase the mulultiServiceCounter when call the
     * startDependentObject and decrease when call the stopDependentObject.
     * 
     * @param <D>
     */
    private class MultiRequisiteListenerImpl<D> implements MultiRequisiteListener<D> {

        @Override
        public void startDependentObject(final D dependentObject, final Map<String, ServiceReference> references) {
            multiServiceListenerCounter++;
        }

        @Override
        public void stopDependentObject(final D dependentObject) {
            multiServiceListenerCounter--;
        }

    }

    /**
     * Simple implementation of {@link RequisiteListener}. Increase the serviceListenerCounter when call the
     * requisiteAvailable and decrease when call the requisiteRemoved.
     * 
     * @param <D>
     */
    private class TestRequisiteListener<D> implements RequisiteListener<D> {

        @Override
        public void requisiteAvailable(final D dependentObject, final ServiceReference requisite) {
            serviceListenerCounter++;

        }

        @Override
        public void requisiteRemoved(final D dependentObject, final ServiceReference requisite) {
            serviceListenerCounter--;

        }

    }

    private static final String SECOND = "SECOND";

    private static final String FIRST = "FIRST";

    private int serviceListenerCounter = 0;

    private int multiServiceListenerCounter = 0;
    /**
     * The BundleContext.
     */
    private BundleContext bundleContext;

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void testAbstractRequisiteTracker() throws InvalidSyntaxException {
        TestService testService = new TestService() {
            @Override
            public int getANumber() {
                return 3;
            }
        };
        AbstractRequisiteTrackerImpl abstractRequisiteTrackerImpl = new AbstractRequisiteTrackerImpl(bundleContext,
                "(" + Constants.OBJECTCLASS + "=" + TestService.class.getName() + ")");
        abstractRequisiteTrackerImpl.open();
        abstractRequisiteTrackerImpl.addDependentObject(new Integer(1), new TestRequisiteListener<Object>());
        ServiceRegistration serviceRegistration = bundleContext.registerService(TestService.class.getName(),
                testService, null);
        TestService serviceFromBundle = (TestService) bundleContext.getService(serviceRegistration.getReference());
        Assert.assertEquals(3, serviceFromBundle.getANumber());
        Assert.assertEquals(1, serviceListenerCounter);
        serviceRegistration.unregister();
        Assert.assertEquals(0, serviceListenerCounter);
        abstractRequisiteTrackerImpl.close();
    }

    @Override
    public void testAbstractRequisiteTrackerWhitTwoService() throws InvalidSyntaxException {
        TestService testService = new TestService() {
            @Override
            public int getANumber() {
                return 3;
            }
        };
        TestService testServiceTwo = new TestService() {
            @Override
            public int getANumber() {
                return 6;
            }
        };
        AbstractRequisiteTrackerImpl abstractRequisiteTrackerImpl = new AbstractRequisiteTrackerImpl(bundleContext,
                "(" + Constants.OBJECTCLASS + "=" + TestService.class.getName() + ")");
        abstractRequisiteTrackerImpl.open();
        abstractRequisiteTrackerImpl.addDependentObject(new Integer(2), new TestRequisiteListener<Object>());
        ServiceRegistration firstServiceRegistration = bundleContext.registerService(TestService.class.getName(),
                testService, null);
        ServiceRegistration secondServiceRegistration = bundleContext.registerService(TestService.class.getName(),
                testServiceTwo, null);
        Assert.assertEquals(1, serviceListenerCounter);
        firstServiceRegistration.unregister();
        Assert.assertEquals(1, serviceListenerCounter);
        secondServiceRegistration.unregister();
        Assert.assertEquals(0, serviceListenerCounter);
        abstractRequisiteTrackerImpl.close();
    }

    @Override
    public void testMultiRequisite() throws InvalidSyntaxException {
        OtherMultiTestService ortherMultiTestService = new OtherMultiTestService() {
            @Override
            public int getANumber() {
                return 3;
            }
        };
        MultiTestService multiTestSerice = new MultiTestService() {
            @Override
            public int getANumber() {
                return 10;
            }
        };
        MultiTestService multiTestSericeTwo = new MultiTestService() {
            @Override
            public int getANumber() {
                return 20;
            }
        };
        AbstractRequisiteTrackerImpl abstractRequisiteTrackerImplForMultiTestService = new
                AbstractRequisiteTrackerImpl(
                        bundleContext,
                        "(" + Constants.OBJECTCLASS + "=" + MultiTestService.class.getName() + ")");
        abstractRequisiteTrackerImplForMultiTestService.open();
        AbstractRequisiteTrackerImpl abstractRequisiteTrackerImplForOtherMultiTestService = new AbstractRequisiteTrackerImpl(
                bundleContext,
                "(" + Constants.OBJECTCLASS + "=" + OtherMultiTestService.class.getName() + ")");
        abstractRequisiteTrackerImplForOtherMultiTestService.open();

        MultiRequisiteManager multiRequisiteManager = new MultiRequisiteManager(new MultiRequisiteListenerImpl());
        Map<String, AbstractRequisiteTracker> requisiteTrackers = new HashMap<String, AbstractRequisiteTracker>();
        requisiteTrackers.put(FIRST, abstractRequisiteTrackerImplForMultiTestService);
        requisiteTrackers.put(SECOND, abstractRequisiteTrackerImplForOtherMultiTestService);
        multiRequisiteManager.registerDependentObject(new Integer(3), requisiteTrackers);

        ServiceRegistration serviceRegistrationOne = bundleContext.registerService(MultiTestService.class.getName(),
                multiTestSerice, null);
        Assert.assertEquals(0, multiServiceListenerCounter);
        ServiceRegistration secondServiceRegistration = bundleContext.registerService(
                OtherMultiTestService.class.getName(),
                ortherMultiTestService, null);
        Assert.assertEquals(1, multiServiceListenerCounter);
        ServiceRegistration thirdServiceRegistration = bundleContext.registerService(MultiTestService.class.getName(),
                multiTestSericeTwo, null);
        Assert.assertEquals(1, multiServiceListenerCounter);
        serviceRegistrationOne.unregister();
        Assert.assertEquals(1, multiServiceListenerCounter);
        thirdServiceRegistration.unregister();
        Assert.assertEquals(0, multiServiceListenerCounter);
        abstractRequisiteTrackerImplForMultiTestService.close();
        abstractRequisiteTrackerImplForOtherMultiTestService.close();
    }
}
