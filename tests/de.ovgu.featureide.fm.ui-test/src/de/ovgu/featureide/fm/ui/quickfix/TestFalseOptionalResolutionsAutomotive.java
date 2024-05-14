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
import org.prop4j.Not;

import de.ovgu.featureide.fm.core.analysis.FeatureProperties.FeatureStatus;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class TestFalseOptionalResolutionsAutomotive extends AbstractResolutionTest {

	@Test
	public void testImpliedByMandatoryAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "N_100130__F_100160", "testImpliedByMandatoryAutomotiveA");

		getFalseOptionalResolutions("N_100130__F_100160");

		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100130__F_100133"), new Literal("N_100130__F_100160")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100130__F_100133"), new Literal("N_100130__F_100160"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100130__F_100133"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, "N_100130__F_100160", "N_100130__F_100132", "")));
	}

	@Test
	public void testExcludeOtherOptionsAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "N_100130__F_100183", "testExcludeOtherOptionsAutomotiveA");

		getFalseOptionalResolutions("N_100130__F_100183");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100130__F_100133"), "")));
		assertTrue(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100130__F_100133"), new Not("N_100130__F_100175")), fmManager)));
		assertTrue(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100130__F_100133"), new Not("N_100130__F_100179")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100130__F_100133"), new Not("N_100130__F_100175"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100130__F_100133"), new Not("N_100130__F_100179"))), fmManager, "")));
	}

	@Test
	public void testImplicationChainAutomotiveA() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "N_100130__F_100217", "testImplicationChainAutomotiveA");

		getFalseOptionalResolutions("N_100130__F_100217");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100130__F_100133"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, "N_100130__F_100170", "N_100130__F_100132", "")));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100130__F_100133"), new Literal("N_100130__F_100170")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100130__F_100133"), new Literal("N_100130__F_100170"))), fmManager, "")));
	}

	@Test
	public void testImpliedByMandatoryAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "N_102383__I_104038_i_F_104267", "testImpliedByMandatoryAutomotiveB");

		getFalseOptionalResolutions("N_102383__I_104038_i_F_104267");

		assertTrue(resolutions.contains(
				new ResolutionDeleteConstraint(new Implies(new Literal("N_100002__F_100106"), new Literal("N_102383__I_104038_i_F_104267")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100002__F_100106"), new Literal("N_102383__I_104038_i_F_104267"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100002__F_100106"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, "N_102383__I_104038_i_F_104267", "N_102383__I_104038_i_F_104225", "")));
	}

	@Test
	public void testExcludeOtherOptionsAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "N_104649__F_104666", "testExcludeOtherOptionsAutomotiveB");

		getFalseOptionalResolutions("N_104649__F_104666");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100130__F_100133"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_100130__F_100133"), "")));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100000__F_100467"), new Not("N_104649__F_104667_2")), fmManager)));
		assertTrue(
				resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_100300__F_100323"), new Not("N_104649__F_104667")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100000__F_100467"), new Not("N_104649__F_104667_2"))), fmManager, "")));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_100300__F_100323"), new Not("N_104649__F_104667"))), fmManager, "")));
	}

	@Test
	public void testImplicationChainAutomotiveB() {
		analyzeFeatureModel("automotive01_defects.xml", FeatureStatus.FALSE_OPTIONAL, "N_100300__F_100352", "testImplicationChainAutomotiveB");

		getFalseOptionalResolutions("N_100300__F_100352");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("N_104649__F_104839"), "")));
		assertTrue(resolutions.contains(new ResolutionMakeMandatory(fmManager, "N_100130__F_100286", "N_100130__F_100131", "")));
		assertTrue(resolutions
				.contains(new ResolutionDeleteConstraint(new Implies(new Literal("N_104649__F_104839"), new Literal("N_100130__F_100286")), fmManager)));
		assertTrue(resolutions.contains(new ResolutionEditConstraint(
				getConstraintForNode(new Implies(new Literal("N_104649__F_104839"), new Literal("N_100130__F_100286"))), fmManager, "")));
	}
}
