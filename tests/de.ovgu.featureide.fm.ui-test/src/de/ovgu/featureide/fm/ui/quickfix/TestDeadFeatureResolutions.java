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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Not;

import de.ovgu.featureide.fm.core.analysis.FeatureProperties.FeatureStatus;

/**
 * This class tests the generation of dead feature resolutions for the {@code HelloWorld} feature models.
 *
 * @author Simon Berlinger
 */
public class TestDeadFeatureResolutions extends AbstractResolutionTest {

	@Test
	public void testExcludeOptional() {

		analyzeFeatureModel("dead_optional_excluded.xml", FeatureStatus.DEAD, "Adjective", "testExcludeOptional");

		getDeadFeatureResolutions("Adjective");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), null)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Not(new Literal("Adjective"))), fmManager)));
		assertTrue(resolutions.contains(
				new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Not(new Literal("Adjective")))), fmManager, "")));
	}

	@Test
	public void testAlternativeImplication() {

		analyzeFeatureModel("dead_imply_alternative.xml", FeatureStatus.DEAD, "Exclamation Mark", "testAlternativeImplication");

		getDeadFeatureResolutions("Exclamation Mark");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), null)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Literal("Period")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Literal("Period"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("Punctuation"), "")));
	}

	@Test
	public void testAltImplyAlt() {

		analyzeFeatureModel("dead_alternative_imply_alternative.xml", FeatureStatus.DEAD, "Period", "testAltImplyAlt");

		getDeadFeatureResolutions("Period");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Period"), new Literal("Exclamation Mark")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("Punctuation"), "")));

		// Exclusion results from the alternative group and cannot be deleted
		assertFalse(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Exclamation Mark"), new Not("Period")), fmManager)));
	}

	@Test
	public void testImplyMultiAlt() {

		analyzeFeatureModel("dead_imply_multi_alternative.xml", FeatureStatus.DEAD, "Adjective", "testImplyMultiAlt");

		getDeadFeatureResolutions("Adjective");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Adjective"), new Literal("Period")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Adjective"), new Literal("Exclamation Mark")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Adjective"), new Literal("Period"))), fmManager, "")));
		assertTrue(resolutions.contains(
				new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Adjective"), new Literal("Exclamation Mark"))), fmManager, "")));
	}

	@Test
	public void testSimultaneousImplyExclude() {
		analyzeFeatureModel("dead_simultaneous_imply_exclude.xml", FeatureStatus.DEAD, "Adjective", "testSimultaneousImplyExclude");

		getDeadFeatureResolutions("Adjective");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Adjective"), new Literal("Exclamation Mark")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Exclamation Mark"), new Not("Adjective")), fmManager)));
		assertTrue(resolutions.contains(
				new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Adjective"), new Literal("Exclamation Mark"))), fmManager, "")));
		assertTrue(resolutions.contains(
				new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Exclamation Mark"), new Not("Adjective"))), fmManager, "")));
	}

	@Test
	public void testExcludeParent() {
		analyzeFeatureModel("dead_exclude_parent.xml", FeatureStatus.DEAD, "Truly", "testExcludeParent");

		getDeadFeatureResolutions("Truly");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Truly"), new Literal("My")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("My"), new Not("Adverb")), fmManager)));
		assertTrue(
				resolutions.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Truly"), new Literal("My"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("My"), new Not("Adverb"))), fmManager, "")));

	}

	@Test
	public void testDeadFeatureImplicationChain() {
		analyzeFeatureModel("dead_implication_chain.xml", FeatureStatus.DEAD, "My", "testDeadFeatureImplicationChain");

		getDeadFeatureResolutions("My");

		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("My"), new Literal("Absolutely")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Not("Exclamation Mark")), fmManager)));

		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("My"), new Literal("Absolutely"))), fmManager, "")));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Not("Exclamation Mark"))), fmManager, "")));

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), "")));
	}

	@Test
	public void testExcludedByFalseOptional() {
		analyzeFeatureModel("false_optional_implication_chain.xml", FeatureStatus.DEAD, "Exclamation Mark", "testExcludedByFalseOptional");

		getDeadFeatureResolutions("Exclamation Mark");

		assertTrue(resolutions.contains(new ResolutionConvertAlternativeToOr(fmManager, featureModel.getFeature("Punctuation"), "")));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Literal("Beautiful")), fmManager)));
		assertTrue(resolutions
				.contains(new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Literal("Beautiful"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), "")));

	}

	@Test
	public void testDeactivatedFeature() {
		analyzeFeatureModel("dead_deactivated_features.xml", FeatureStatus.DEAD, "Period", "testDeactivatedFeature");

		getDeadFeatureResolutions("Period");

		assertTrue(resolutions.size() == 1);
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Not("Period"), fmManager)));
	}

	@Test
	public void testDeactivateExcludeSelf() {

		analyzeFeatureModel("dead_deactivated_features.xml", FeatureStatus.DEAD, "Hello", "testDeactivateExcludeSelf");
		getDeadFeatureResolutions("Hello");
		assertTrue(resolutions.contains(new ResolutionReplaceConstraint(fmManager, new Implies(new Literal("Hello"), new Not("Hello")), new Not("Hello"), "")));
	}

	@Test
	public void testDeactivateExcludeRoot() {
		analyzeFeatureModel("dead_deactivated_features.xml", FeatureStatus.DEAD, "Beautiful", "testDeactivateExcludeRoot");
		getDeadFeatureResolutions("Beautiful");
		assertTrue(resolutions
				.contains(new ResolutionReplaceConstraint(fmManager, new Implies(new Literal("Beautiful"), new Not("Sentence")), new Not("Beautiful"), "")));
	}

	@Test
	public void testDeactivateExcludedByRoot() {
		analyzeFeatureModel("dead_deactivated_features.xml", FeatureStatus.DEAD, "Wonderful", "testDeactivateExcludedByRoot");
		getDeadFeatureResolutions("Wonderful");
		assertTrue(resolutions
				.contains(new ResolutionReplaceConstraint(fmManager, new Implies(new Literal("Sentence"), new Not("Wonderful")), new Not("Wonderful"), "")));
	}

	@Test
	public void testDeactivateExcludedByDeactivated() {
		analyzeFeatureModel("dead_deactivated_features.xml", FeatureStatus.DEAD, "World", "testDeactivateExcludedByDeactivated");
		getDeadFeatureResolutions("World");
		assertTrue(resolutions.stream()
				.anyMatch(x -> (x instanceof ResolutionCreateConstraint) && ((ResolutionCreateConstraint) x).toCreateNode.toString().equals("-World")));
	}

}
