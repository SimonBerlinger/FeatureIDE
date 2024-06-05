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
import org.prop4j.And;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Not;
import org.prop4j.Or;

/**
 * This class tests redundancy resolutions for the {@code Automotive01} feature model.
 *
 * @author Simon Berlinger
 */
public class TestRedundancyResolutionsAutomotive extends AbstractResolutionTest {

	@Test
	public void testConstraintsEquivalentAutomotiveA() {
		final Node redundantNode = new Or(new Not("RC_EQUIVALENT_IMPLYING"), new Literal("RC_EQUIVALENT_IMPLIED"));

		analyzeFeatureModelRedundancy("automotive01_defects.xml", redundantNode, "testConstraintsEquivalentAutomotive");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("RC_EQUIVALENT_IMPLYING"), new Literal("RC_EQUIVALENT_IMPLIED")), fmManager)));
	}

	@Test
	public void testConstraintContainedAutomotiveA() {
		final Node redundantNode = new Literal("RC_CONTAINED_CONTAINED");

		analyzeFeatureModelRedundancy("automotive01_defects.xml", redundantNode, "testConstraintContainedAutomotive");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
	}

	@Test
	public void testConstraintContainedMultiAutomotiveA() {
		final Node redundantNode = new Implies(new Literal("RC_CONTAINED_MULTI_IMPLYING"), new Literal("RC_CONTAINED_MULTI_IMPLIED-PARENT"));

		analyzeFeatureModelRedundancy("automotive01_defects.xml", redundantNode, "testConstraintContainedMultiAutomotive");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(getConstraintForNode(redundantNode), fmManager, "")));
	}

	@Test
	public void testConstraintsEquivalentAutomotiveB() {
		final Node redundantNode = new Implies(
				new And(new Literal("RC_EQUIVALENT_1_B"),
						new And(new Literal("RC_EQUIVALENT_2_B"),
								new And(new Literal("RC_EQUIVALENT_3_B"), new And(new Literal("RC_EQUIVALENT_4_B"), new Literal("RC_EQUIVALENT_5_B"))))),
				new And(new Literal("RC_EQUIVALENT_6_B"), new And(new Literal("RC_EQUIVALENT_7_B"),
						new And(new Literal("RC_EQUIVALENT_8_B"), new And(new Literal("RC_EQUIVALENT_9_B"), new Literal("RC_EQUIVALENT_10_B"))))));

		final Node otherNode = new Not(new And(
				new And(new Literal("RC_EQUIVALENT_1_B"),
						new And(new Literal("RC_EQUIVALENT_2_B"),
								new And(new Literal("RC_EQUIVALENT_3_B"), new And(new Literal("RC_EQUIVALENT_4_B"), new Literal("RC_EQUIVALENT_5_B"))))),
				new Not(new And(new Literal("RC_EQUIVALENT_6_B"), new And(new Literal("RC_EQUIVALENT_7_B"),
						new And(new Literal("RC_EQUIVALENT_8_B"), new And(new Literal("RC_EQUIVALENT_9_B"), new Literal("RC_EQUIVALENT_10_B"))))))));

		analyzeFeatureModelRedundancy("automotive01_defects.xml", redundantNode, "testConstraintsEquivalentAutomotiveB");
		getRedundancyResolutions(redundantNode);
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(otherNode, fmManager)));
	}

	@Test
	public void testConstraintContainedAutomotiveB() {
		final Node redundantNode = new Or(new Literal("RC_CONTAINED_1_B"), new Implies(new Literal("RC_CONTAINED_2_B"),
				new Implies(new Literal("RC_CONTAINED_3_B"), new And(new Literal("RC_CONTAINED_4_B"), new Literal("RC_CONTAINED_5_B")))));

		analyzeFeatureModelRedundancy("automotive01_defects.xml", redundantNode, "testConstraintContainedAutomotiveB");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
	}

	@Test
	public void testConstraintContainedMultiAutomotiveB() {
		final Node redundantNode = new Implies(
				new Or(new Literal("RC_CONTAINED_MULTI_IMPLYING_1_B"),
						new Or(new Literal("RC_CONTAINED_MULTI_IMPLYING_2_B"),
								new Or(new Literal("RC_CONTAINED_MULTI_IMPLYING_3_B"),
										new Or(new Literal("RC_CONTAINED_MULTI_IMPLYING_4_B"), new Literal("RC_CONTAINED_MULTI_IMPLYING_5_B"))))),
				new Or(new Literal("RC_CONTAINED_MULTI_IMPLIED2_1_B"),
						new Or(new Literal("RC_CONTAINED_MULTI_IMPLIED2_2_B"), new Or(new Literal("RC_CONTAINED_MULTI_IMPLIED2_3_B"),
								new Or(new Literal("RC_CONTAINED_MULTI_IMPLIED2_4_B"), new Literal("RC_CONTAINED_MULTI_IMPLIED2_5_B"))))));

		analyzeFeatureModelRedundancy("automotive01_defects.xml", redundantNode, "testConstraintContainedMultiAutomotiveB");

		getRedundancyResolutions(redundantNode);

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(redundantNode, fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(getConstraintForNode(redundantNode), fmManager, "")));
	}
}
