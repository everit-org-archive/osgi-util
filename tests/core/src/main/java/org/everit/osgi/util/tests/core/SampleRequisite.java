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

import org.everit.osgi.util.core.requisite.AbstractRequisiteTracker;
import org.everit.osgi.util.core.requisite.MultiRequisiteManager;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Test for the util.core.requisite package.
 */
public interface SampleRequisite {
    /**
     * Test the {@link AbstractRequisiteTracker} whit one service.
     * 
     * @throws InvalidSyntaxException
     *             If invalid filter.
     */
    @Test
    void testAbstractRequisiteTracker() throws InvalidSyntaxException;

    /**
     * Test the {@link AbstractRequisiteTracker} whit two same service.
     * 
     * @throws InvalidSyntaxException
     *             If invalid filter.
     */
    @Test
    void testAbstractRequisiteTrackerWhitTwoService() throws InvalidSyntaxException;

    /**
     * Test the {@link MultiRequisiteManager} whit services.
     * 
     * @throws InvalidSyntaxException
     *             If invalid filter.
     */
    @Test
    void testMultiRequisite() throws InvalidSyntaxException;
}
