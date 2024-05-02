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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.IMarkerResolution;
import org.junit.Test;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Not;

import de.ovgu.featureide.Commons;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.analysis.FeatureProperties.FeatureStatus;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.explanations.Reason;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class TestDeadFeatureResolutions {

	FeatureModelManager fmManager;
	IFeatureModel featureModel;
	FeatureModelAnalyzer analyzer;
	Set<Reason<?>> reasons;
	DefectResolutionProvider resolutionProvider;
	Set<IMarkerResolution> resolutions;
	DefectQuickFixHandler dqh;

	//
	private void loadFeatureModelAndInitialize(String filename, FeatureStatus defectType, String defectFeatureName) {
		fmManager = Commons.loadTestFeatureModelFromFile(filename);
		featureModel = fmManager.getPersistentFormula().getFeatureModel();
		analyzer = fmManager.getPersistentFormula().getAnalyzer();

		switch (defectType) {
		case DEAD:
			reasons = analyzer.getDeadFeatureExplanation(featureModel.getFeature(defectFeatureName)).getReasons();
			break;
		case FALSE_OPTIONAL:
			reasons = analyzer.getFalseOptionalFeatureExplanation(featureModel.getFeature(defectFeatureName)).getReasons();
			break;
		default:
			return;
		}

		analyzer.analyzeFeatureModel(null);
		resolutions = new HashSet<>();
		dqh = new DefectQuickFixHandler();
		dqh.analyzer = analyzer;
		dqh.featureModelFormula = fmManager.getPersistentFormula();
	}

	private IConstraint getConstraintForNode(Node node) {
		return (IConstraint) featureModel.getConstraints().stream().filter(x -> x.getNode().equals(node)).toArray()[0];
	}

	/**
	 *
	 */
	private void getDeadFeatureResolutions(String featureName) {
		final long t0 = System.currentTimeMillis();
		resolutionProvider =
			new DefectResolutionProvider(fmManager.getPersistentFormula().getFeatureModel(), fmManager, fmManager.getPersistentFormula().getAnalyzer(), dqh);
		dqh.getDeadFeatureResolutions(resolutionProvider, resolutions, featureModel.getFeature(featureName));
		System.out.println("DURATION: " + (System.currentTimeMillis() - t0) + "ms ");

	}

	@Test
	public void testExcludedOptionalDead() {

		loadFeatureModelAndInitialize("dead_optional_excluded.xml", FeatureStatus.DEAD, "Adjective");

		getDeadFeatureResolutions("Adjective");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), null)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Not(new Literal("Adjective"))), fmManager)));
		assertTrue(resolutions.contains(
				new ResolutionEditConstraint(getConstraintForNode(new Implies(new Literal("Hello"), new Not(new Literal("Adjective")))), fmManager, "")));
	}

	@Test
	public void testAlternativeImplicationDead() {

		loadFeatureModelAndInitialize("dead_imply_alternative.xml", FeatureStatus.DEAD, "Exclamation Mark");

		getDeadFeatureResolutions("Exclamation Mark");

		assertTrue(resolutions.contains(new ResolutionMakeOptional(fmManager, featureModel.getFeature("Hello"), null)));
		assertTrue(resolutions.contains(new ResolutionDeleteConstraint(new Implies(new Literal("Hello"), new Literal("Period")), fmManager)));

	}

}
