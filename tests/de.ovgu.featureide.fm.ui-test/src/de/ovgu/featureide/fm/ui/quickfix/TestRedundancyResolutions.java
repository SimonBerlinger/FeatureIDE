/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.ui.quickfix;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Not;
import org.prop4j.Or;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class TestRedundancyResolutions extends AbstractResolutionTest {

	@Test
	public void testConstraintsEquivalent() {

		final Node redundantNode = new Or(new Not("Hello"), new Literal("Beautiful"));

		analyzeFeatureModelRedundancy("redundancies.xml", redundantNode, "testConstraintsEquivalent");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Literal("Beautiful")), fmManager)));
	}

	@Test
	public void testConstraintContained() {
		final Node redundantNode = new Literal("Period");

		analyzeFeatureModelRedundancy("redundancies.xml", redundantNode, "testConstraintContained");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
	}

	@Test
	public void testConstraintContainedMulti() {
		final Node redundantNode = new Implies(new Literal("Hello"), new Literal("Adjective"));

		analyzeFeatureModelRedundancy("redundancies.xml", redundantNode, "testConstraintContainedMulti");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(getConstraintForNode(redundantNode), fmManager, "")));

	}

	@Test
	public void testConstraintOverlap() {

	}
}
