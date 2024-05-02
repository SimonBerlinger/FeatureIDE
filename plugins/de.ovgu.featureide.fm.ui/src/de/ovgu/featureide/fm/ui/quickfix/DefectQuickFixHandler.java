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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.prop4j.Node;
import org.prop4j.Not;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.analysis.FeatureModelProperties.FeatureModelStatus;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.explanations.Reason;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class DefectQuickFixHandler implements IMarkerResolutionGenerator {

	FeatureModelAnalyzer analyzer;
	FeatureModelFormula featureModelFormula;
	private FeatureModelManager fmManager;
	private String originalDefectName = "";
	private HashMap<String, Boolean> encounteredFeatures = new HashMap<>();

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {

		System.out.println("START FIX");

		IFeatureProject project = null;

		if (marker != null) {
			project = CorePlugin.getFeatureProject(marker.getResource());
			if (project == null) {
				featureModelFormula = null;
			} else {
				featureModelFormula = project.getFeatureModelManager().getPersistentFormula();
			}
		} else {
			featureModelFormula = null;
		}

		if (featureModelFormula != null) {

			final String[] splitMessage = marker.getAttribute(IMarker.MESSAGE, "").split("''");

			if (splitMessage.length > 1) {

				analyzer = featureModelFormula.getAnalyzer();
				analyzer.analyzeFeatureModel(null); // TODO find possibility without calling analysis. Somehow, without this, void feature models do not have
				// void status without

				fmManager = (FeatureModelManager) project.getFeatureModelManager();
				final DefectResolutionProvider resolutionProvider =
					new DefectResolutionProvider(featureModelFormula.getFeatureModel(), fmManager, analyzer, this);
				final String affectedElementString = splitMessage[1];

				System.out.println("AFFECTED ELEMENT: " + affectedElementString + " ORIGINAL: " + originalDefectName);

				// if feature model not void or the root is the affected feature
				if (!analyzer.getFeatureModelProperties().hasStatus(FeatureModelStatus.VOID)
					|| affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {

					final Set<IMarkerResolution> offeredResolutions = new HashSet<>();

					// if marker type is dead feature and feature is contained in the model and actually dead or the affected feature is root
					if (splitMessage[0].startsWith(IFeatureProject.MARKER_DEAD)
						&& (analyzer.getDeadFeatures(null).contains(featureModelFormula.getFeatureModel().getFeature(affectedElementString))
							|| (affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())
								&& analyzer.getFeatureModelProperties().hasStatus(FeatureModelStatus.VOID)))) {

						final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);
						originalDefectName = affectedFeature.getName();
						encounteredFeatures = new HashMap<>();
						System.out.println("DF?: " + affectedElementString);
						System.out.println("DF: " + analyzer.getDeadFeatureExplanation(affectedFeature));

						if (affectedFeature != null) {

							return getDeadFeatureResolutions(resolutionProvider, offeredResolutions, affectedFeature);
						}

					} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_FALSE_OPTIONAL)
						&& analyzer.getFalseOptionalFeatures(null).contains(featureModelFormula.getFeatureModel().getFeature(affectedElementString))) {

							final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);
							originalDefectName = affectedFeature.getName();
							encounteredFeatures = new HashMap<>();
							System.out.println("FO: " + analyzer.getFalseOptionalFeatureExplanation(affectedFeature));

							if (affectedFeature != null) {

								return getFalseOptionalResolutions(resolutionProvider, offeredResolutions, affectedFeature);
							}

						} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_REDUNDANCY) && (analyzer.getRedundantConstraints(null).stream()
								.map(x -> x.getDisplayName()).filter(y -> y.equals(affectedElementString)).toList().size() > 0)) {
									System.out.println("REDUNDANT: " + analyzer.getRedundantConstraintExplanation(featureModelFormula.getFeatureModel()
											.getConstraints().stream().filter(x -> x.getDisplayName().equals(affectedElementString)).toList().get(0)));

									final List<IConstraint> affectedConstraint = featureModelFormula.getFeatureModel().getConstraints().stream()
											.filter(x -> x.getDisplayName().equals(affectedElementString)).toList();
									System.out.println("AC: " + affectedConstraint);
									if (affectedConstraint.size() > 0) {

										return getRedundancyResolutions(resolutionProvider, offeredResolutions, affectedConstraint.get(0));
									} else {
										offeredResolutions.add(new ResolutionDeleteConstraint(affectedConstraint.get(0).getNode(), fmManager));
										System.out.println("NO REASON CONSTRAINT");
									}
									return new IMarkerResolution[] {};

								} else
							if (splitMessage[0].startsWith(IFeatureProject.MARKER_TAUTOLOGY)) {

								for (final IConstraint c : featureModelFormula.getFeatureModel().getConstraints()) {
									if (c.getDisplayName().equals(affectedElementString)) {
										return new IMarkerResolution[] { new ResolutionDeleteConstraint(c.getNode(), fmManager),
											new ResolutionEditConstraint(c, fmManager, "") };
									}
								}
							}
				} else {
					System.out.println("VOID " + affectedElementString);

					// When the feature-model is void, always return fixes for the dead root

					final Set<IMarkerResolution> offeredResolutions = new HashSet<>();

					final IFeature root = featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature();

					getDeadFeatureResolutions(resolutionProvider, offeredResolutions, root);

					System.out.println("   GOT: " + offeredResolutions);

					return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);

				}
			}
		}
		System.out.println("NONE");
		return new IMarkerResolution[0];

	}

	/**
	 * @param resolutionProvider
	 * @param offeredResolutions
	 * @param affectedConstraint
	 * @return
	 */
	IMarkerResolution[] getRedundancyResolutions(final DefectResolutionProvider resolutionProvider, final Set<IMarkerResolution> offeredResolutions,
			final IConstraint affectedConstraint) {
		final Set<Reason<?>> reasons = analyzer.getRedundantConstraintExplanation(affectedConstraint).getReasons();

		final Set<Map<Object, Boolean>> satisfyingAssignments = affectedConstraint.getNode().getSatisfyingAssignments();
		final Reason<?> reason = ((Reason<?>) reasons.toArray()[0]);

		if ((reasons.size() == 1) && reason.toNode().getContainedFeatures().equals(affectedConstraint.getNode().getContainedFeatures())) {
			System.out.println("REDUNDANCY ONE REASON EQUAL FEATURES");
			// Same contained features + same satisfying assignments -> equal
			if (satisfyingAssignments.equals(reason.toNode().getSatisfyingAssignments())) {
				offeredResolutions.add(new ResolutionDeleteConstraint(affectedConstraint.getNode(), fmManager));
				if (resolutionProvider.isReasonConstraint(reason)) {
					offeredResolutions.add(new ResolutionDeleteConstraint(reason.toNode(), fmManager));
				}
			}

		} else if (reasons.size() == 1) {

			if (!resolutionProvider.isReasonConstraint(reason)) {
				offeredResolutions.add(new ResolutionDeleteConstraint(affectedConstraint.getNode(), fmManager));
				return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
			}

			// if all clauses from the constraint marked as redundant (as CNF) are contained in the reason-constraint (as CNF)
			if (resolutionProvider.checkClausesContained(affectedConstraint.getNode(), reason.toNode(), offeredResolutions, false,
					resolutionProvider.isReasonConstraint(reason))) {
				System.out.println("DEFECT IS IN REASON");
				return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);

			} else if (resolutionProvider.checkClausesContained(reason.toNode(), affectedConstraint.getNode(), offeredResolutions,
					resolutionProvider.isReasonConstraint(reason), false)) {
						System.out.println("REASON IS IN DEFECT");
						return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);

					} else {
						// neither the affected constraint nor the reason constraint is fully contained in the other
						// TODO determine redundant clauses
					}

		} else {
			System.out.println("ELSE: reasons size: " + reasons.size());

			resolutionProvider.checkMultipleRedunanycReasons(affectedConstraint, reasons, offeredResolutions);

		}

		return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
	}

	/**
	 * @param marker
	 * @param resolutionProvider
	 * @param affectedElementString
	 * @param offeredResolutions
	 * @param affectedFeature
	 * @return
	 */
	IMarkerResolution[] getFalseOptionalResolutions(final DefectResolutionProvider resolutionProvider, final Set<IMarkerResolution> offeredResolutions,
			final IFeature affectedFeature) {
		System.out.println("FO resolution for " + affectedFeature.getName());

		if (encounteredFeatures.get(affectedFeature.getName()) != null) {
			return new IMarkerResolution[0];
		}
		encounteredFeatures.put(affectedFeature.getName(), true);
		final Set<Reason<?>> reasons = analyzer.getFalseOptionalFeatureExplanation(affectedFeature).getReasons();
		final Set<String> involvedFeatures = determineInvolvedFeatures(reasons);

		System.out.println(affectedFeature.getName() + " is original " + originalDefectName);

		// only add fixes if the currently regarded feature is the original defect to be fixed or the first false-optional without
		// another false-optional possibly being responsible for it

		if (!resolutionProvider.checkForImplyingFalseOptionals(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature),
				involvedFeatures)
			|| originalDefectName.equals(affectedFeature.getName())) {

			System.out.println(affectedFeature + " get fixes");

			// All mandatory features, where the fact that they are mandatory is a reason
			final List<IFeature> mandatoryReasons = new ArrayList<IFeature>();
			for (final Reason<?> reason : reasons) {
				final IFeature mandatoryReason = resolutionProvider.checkMandatoryChildReason(reason, offeredResolutions, getFeaturePrefix(affectedFeature));
				if (mandatoryReason != null) {
					mandatoryReasons.add(mandatoryReason);
				}
			}

			// All reasons, where a mandatory feature implies the affected feature
			for (final IFeature f : mandatoryReasons) {
				if (!f.getName().equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {
					offeredResolutions.add(new ResolutionMakeOptional(fmManager, f, getFeaturePrefix(affectedFeature)));
					resolutionProvider.checkImplicationConstraintForFalseOptional(reasons, offeredResolutions, affectedFeature, f,
							getFeaturePrefix(affectedFeature));
				}
			}

			// Situations, where enough other alternatives are excluded (dead) to make the affected feature false-optional
			if (affectedFeature.getStructure().getParent().isAlternative() || affectedFeature.getStructure().getParent().isOr()) {
				resolutionProvider.checkAlternativeExclusion(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature), "POSTFIX");
			}

		}
		return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
	}

	/**
	 * @param marker
	 * @param resolutionProvider
	 * @param affectedElementString
	 * @param offeredResolutions
	 * @param affectedFeature
	 * @return
	 */
	IMarkerResolution[] getDeadFeatureResolutions(final DefectResolutionProvider resolutionProvider, final Set<IMarkerResolution> offeredResolutions,
			final IFeature affectedFeature) {
		System.out.println("BEGIN " + affectedFeature.getName() + ": " + offeredResolutions);

		if (encounteredFeatures.get(affectedFeature.getName()) != null) {
			return new IMarkerResolution[0];
		}
		encounteredFeatures.put(affectedFeature.getName(), true);

		final Set<Reason<?>> reasons = analyzer.getDeadFeatureExplanation(affectedFeature).getReasons();
		final Set<String> involvedFeatures = determineInvolvedFeatures(reasons);

//		System.out.println(affectedFeature.getName() + "  Has no implied dead? "
//			+ !resolutionProvider.checkImpliedDeadFeatures(reasons, affectedFeature, offeredResolutions, getFeaturePrefix(affectedFeature), involvedFeatures));
//		System.out.println(" Is origin? " + originalDefectName.equals(affectedFeature.getName()));
		// Only return resolutions, if the feature is either the first or the last in a chain of implications of dead features

		if (!resolutionProvider.checkImpliedDeadFeatures(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature), involvedFeatures)
			|| originalDefectName.equals(affectedFeature.getName())) {

			// check, if there exists a constraint -f or equivalent to -f for the feature ( -> intentionally dead?)
			final Node notFeature = new Not(affectedFeature.getName());

			for (final Reason<?> r : reasons) {
				if (r.toNode().equals(notFeature)) {
					System.out.println("END DEACTIVATED " + affectedFeature.getName() + " with res: " + offeredResolutions);
					offeredResolutions.add(new ResolutionDeleteConstraint(notFeature, fmManager, getFeaturePrefix(affectedFeature)));
					return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
				}

				// check if the feature excludes itself or the root
				if (resolutionProvider.isExcluding(affectedFeature.getName(),
						featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName(), r.toNode())
					|| resolutionProvider.isExcluding(affectedFeature.getName(), affectedFeature.getName(), r.toNode())) {
					offeredResolutions.add(new ResolutionSetConstraint(fmManager, r.toNode(), notFeature, ""));
				}

				// check if feature is excluded by deactivated feature
				for (final String f : r.toNode().getContainedFeatures()) {
					if (r.toNode().getContainedFeatures().contains(affectedFeature.getName())
						&& !resolutionProvider.canBeSelectedInConstraint(affectedFeature.getName(), Node.replaceLiterals(r.toNode(), Arrays.asList(f), true))
						&& featureModelFormula.getFeatureModel().getConstraints().stream().map(x -> x.getDisplayName()).toList().contains("-" + f)) {
						offeredResolutions.add(new ResolutionCreateConstraint(new Not(affectedFeature), fmManager));
					}
				}

			}

			// Add delete feature action if not root
			if (!affectedFeature.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature())) {
				offeredResolutions.add(new ResolutionDeleteFeature(affectedFeature.getName(), fmManager, getFeaturePrefix(affectedFeature)));
			}

			resolutionProvider.checkImpliesMultiAlt(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature));

			resolutionProvider.checkForExcludingFalseOptionals(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature),
					involvedFeatures);

			// All mandatory features, where the fact that they are mandatory is a reason
			for (final Reason<?> reason : reasons) {
				resolutionProvider.checkMandatoryChildReason(reason, offeredResolutions, getFeaturePrefix(affectedFeature));
			}

			// All exclusions, where a mandatory feature excludes the affected feature
			for (final String f : involvedFeatures) {
				System.out.println("CHECK EXCLUSION FOR " + affectedFeature.getName());
				resolutionProvider.checkExclusion(reasons, offeredResolutions, affectedFeature.getName(), f, getFeaturePrefix(affectedFeature));
			}

			// checks if the affected feature is implying its own exclusion
			resolutionProvider.checkImpliesOwnExclusion(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature));

			System.out.println("END " + affectedFeature.getName() + " with res: " + offeredResolutions);
			return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
		}
		System.out.println("END NONE" + affectedFeature.getName() + " with res: " + offeredResolutions);
		return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
	}

	/**
	 * @param affectedFeature
	 * @return
	 */
	private String getFeaturePrefix(final IFeature affectedFeature) {
		return (affectedFeature.getName().equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName()) ? "[Root] "
			: originalDefectName.equals(affectedFeature.getName()) ? "" : "[Possible cause ''" + affectedFeature.getName() + "''] ");
	}

	private Set<String> determineInvolvedFeatures(Set<Reason<?>> reasons) {
		final Set<String> containedFeatures = new HashSet<>();
		for (final Reason<?> r : reasons) {
			containedFeatures.addAll(r.toNode().getContainedFeatures());
		}
		return containedFeatures;
	}
}
