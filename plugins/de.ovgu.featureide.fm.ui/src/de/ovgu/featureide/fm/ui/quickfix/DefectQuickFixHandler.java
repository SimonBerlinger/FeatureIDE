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
 * This class is responsible for returning the set of resolutions for a feature model defect.
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
				// void status

				fmManager = (FeatureModelManager) project.getFeatureModelManager();
				final DefectResolutionProvider resolutionProvider =
					new DefectResolutionProvider(featureModelFormula.getFeatureModel(), fmManager, analyzer, this);
				final String affectedElementString = splitMessage[1];

				// if the feature model is not void or the root is the affected feature
				if (!analyzer.getFeatureModelProperties().hasStatus(FeatureModelStatus.VOID)
					|| affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {

					final Set<IMarkerResolution> offeredResolutions = new HashSet<>();

					// if marker type is dead feature and feature is contained in the model and actually dead, or the affected feature is root
					if (splitMessage[0].startsWith(IFeatureProject.MARKER_DEAD)
						&& (analyzer.getDeadFeatures(null).contains(featureModelFormula.getFeatureModel().getFeature(affectedElementString))
							|| (affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())
								&& analyzer.getFeatureModelProperties().hasStatus(FeatureModelStatus.VOID)))) {

						final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);
						originalDefectName = affectedFeature.getName();
						encounteredFeatures = new HashMap<>();

						if (affectedFeature != null) {

							return getDeadFeatureResolutions(resolutionProvider, offeredResolutions, affectedFeature);
						}

					} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_FALSE_OPTIONAL)
						&& analyzer.getFalseOptionalFeatures(null).contains(featureModelFormula.getFeatureModel().getFeature(affectedElementString))) {

							final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);
							originalDefectName = affectedFeature.getName();
							encounteredFeatures = new HashMap<>();

							if (affectedFeature != null) {

								return getFalseOptionalResolutions(resolutionProvider, offeredResolutions, affectedFeature);
							}

						} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_REDUNDANCY) && (analyzer.getRedundantConstraints(null).stream()
								.map(x -> x.getDisplayName()).filter(y -> y.equals(affectedElementString)).toList().size() > 0)) {

									final List<IConstraint> affectedConstraint = featureModelFormula.getFeatureModel().getConstraints().stream()
											.filter(x -> x.getDisplayName().equals(affectedElementString)).toList();

									if (affectedConstraint.size() > 0) {

										return getRedundancyResolutions(resolutionProvider, offeredResolutions, affectedConstraint.get(0));
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

					// When the feature-model is void, always return fixes for the dead root
					final Set<IMarkerResolution> offeredResolutions = new HashSet<>();
					final IFeature root = featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature();
					getDeadFeatureResolutions(resolutionProvider, offeredResolutions, root);
					return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
				}
			}
		}

		return new IMarkerResolution[0];

	}

	/**
	 * This method is responsible of generating and returning of a set of resolutions for a redundant constraint.
	 *
	 * @param resolutionProvider The resolution provider which offers the required check-methods.
	 * @param offeredResolutions The set of offered resolutions.
	 * @param affectedConstraint The redundant constraint.
	 * @return The set of offered resolutions.
	 */
	IMarkerResolution[] getRedundancyResolutions(final DefectResolutionProvider resolutionProvider, final Set<IMarkerResolution> offeredResolutions,
			final IConstraint affectedConstraint) {

		final Set<Reason<?>> reasons = analyzer.getRedundantConstraintExplanation(affectedConstraint).getReasons();

		final Set<Map<Object, Boolean>> satisfyingAssignments = affectedConstraint.getNode().getSatisfyingAssignments();
		final Reason<?> reason = ((Reason<?>) reasons.toArray()[0]);

		if ((reasons.size() == 1) && reason.toNode().getContainedFeatures().equals(affectedConstraint.getNode().getContainedFeatures())) {

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

				return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);

			} else if (resolutionProvider.checkClausesContained(reason.toNode(), affectedConstraint.getNode(), offeredResolutions,
					resolutionProvider.isReasonConstraint(reason), false)) {

						return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);

					} else {
						// neither the affected constraint nor the reason constraint is fully contained in the other
						// TODO determine redundant clauses
					}

		} else {

			resolutionProvider.checkMultipleRedundanycReasons(affectedConstraint, reasons, offeredResolutions);

		}

		return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
	}

	/**
	 * This method is responsible of generating and returning of a set of resolutions for a false-optional feature.
	 *
	 * @param resolutionProvider The resolution provider which offers the required check-methods.
	 * @param offeredResolutions The set of offered resolutions.
	 * @param affectedFeature The false-optional feature.
	 * @return The set of offered resolutions.
	 */
	IMarkerResolution[] getFalseOptionalResolutions(final DefectResolutionProvider resolutionProvider, final Set<IMarkerResolution> offeredResolutions,
			final IFeature affectedFeature) {

		if (encounteredFeatures.get(affectedFeature.getName()) != null) {
			return new IMarkerResolution[0];
		}
		encounteredFeatures.put(affectedFeature.getName(), true);
		final Set<Reason<?>> reasons = analyzer.getFalseOptionalFeatureExplanation(affectedFeature).getReasons();
		final Set<String> involvedFeatures = determineInvolvedFeatures(reasons);

		// only add fixes if the currently regarded feature is the original defect to be fixed or the first false-optional without
		// another false-optional possibly being responsible for it
		if (!resolutionProvider.checkForImplyingFalseOptionals(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature),
				involvedFeatures)
			|| originalDefectName.equals(affectedFeature.getName())) {

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
				resolutionProvider.checkAlternativeExclusion(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature));
			}

		}
		return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
	}

	/**
	 * This method is responsible of generating and returning of a set of resolutions for a dead feature.
	 *
	 * @param resolutionProvider The resolution provider which offers the required check-methods.
	 * @param offeredResolutions The set of offered resolutions.
	 * @param affectedFeature The dead feature.
	 * @return The set of offered resolutions.
	 */
	IMarkerResolution[] getDeadFeatureResolutions(final DefectResolutionProvider resolutionProvider, final Set<IMarkerResolution> offeredResolutions,
			final IFeature affectedFeature) {
		if (encounteredFeatures.get(affectedFeature.getName()) != null) {
			return new IMarkerResolution[0];
		}
		encounteredFeatures.put(affectedFeature.getName(), true);

		final Set<Reason<?>> reasons = analyzer.getDeadFeatureExplanation(affectedFeature).getReasons();
		final Set<String> involvedFeatures = determineInvolvedFeatures(reasons);

		// Only return resolutions, if the feature is either the first or the last in a chain of implications of dead features
		if (!resolutionProvider.checkImpliedDeadFeatures(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature), involvedFeatures)
			|| originalDefectName.equals(affectedFeature.getName())) {

			// check, if there exists a constraint -f or equivalent to -f for the feature ( -> intentionally dead?)
			final Node notFeature = new Not(affectedFeature.getName());
			for (final Reason<?> r : reasons) {

				if (r.toNode().equals(notFeature)) {

					offeredResolutions.add(new ResolutionDeleteConstraint(notFeature, fmManager, getFeaturePrefix(affectedFeature)));
					return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
				}

				// check if the feature excludes itself or the root
				if (resolutionProvider.isExcluding(affectedFeature.getName(),
						featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName(), r.toNode())
					|| resolutionProvider.isExcluding(affectedFeature.getName(), affectedFeature.getName(), r.toNode())) {
					offeredResolutions.add(new ResolutionChangeConstraint(fmManager, r.toNode(), notFeature, ""));
				}

				// check if feature is excluded by deactivated feature
				for (final String f : r.toNode().getContainedFeatures()) {
					if (reasons.stream().map(x -> x.toNode().toString()).toList().contains("-" + f)
						&& !resolutionProvider.canBeSelectedInConstraint(affectedFeature.getName(), Node.replaceLiterals(r.toNode(), Arrays.asList(f), true))) {
						offeredResolutions.add(new ResolutionCreateConstraint(new Not(affectedFeature), fmManager));
					}
				}

			}

//			// Add delete feature action if not root
//			if (!affectedFeature.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature())) {
//				offeredResolutions.add(new ResolutionDeleteFeature(affectedFeature.getName(), fmManager, getFeaturePrefix(affectedFeature)));
//			}

			resolutionProvider.checkImpliesMultiAlt(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature));

			resolutionProvider.checkForExcludingFalseOptionals(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature),
					involvedFeatures);
			// All mandatory features, where the fact that they are mandatory is a reason
			for (final Reason<?> reason : reasons) {
				resolutionProvider.checkMandatoryChildReason(reason, offeredResolutions, getFeaturePrefix(affectedFeature));
			}
			// All exclusions, where a mandatory feature excludes the affected feature
			for (final String f : involvedFeatures) {
				resolutionProvider.checkExclusion(reasons, offeredResolutions, affectedFeature.getName(), f, getFeaturePrefix(affectedFeature));
			}
			// checks if the affected feature is implying its own exclusion
			resolutionProvider.checkImpliesOwnExclusion(reasons, offeredResolutions, affectedFeature, getFeaturePrefix(affectedFeature));

			resolutionProvider.checkParentExclusion(reasons, offeredResolutions, affectedFeature.getName(), involvedFeatures,
					getFeaturePrefix(affectedFeature));
			return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
		}
		return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
	}

	/**
	 *
	 * This method returns the prefix, that is added to the label of the {@code IMarkerResolutions}, that are added to the set of resolutions. This is used to
	 * indicate, that the respective fix is used to resolve another feature, that may cause the original defect.
	 *
	 * @param currentFeature The feature, for which the prefix is to be returned.
	 * @return The prefix to be added to the label of the resolution.
	 */
	private String getFeaturePrefix(final IFeature currentFeature) {
		return (currentFeature.getName().equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName()) ? "[Root] "
			: originalDefectName.equals(currentFeature.getName()) ? "" : "[Possible cause ''" + currentFeature.getName() + "''] ");
	}

	/**
	 * Determines all features, that are o´contained in a set of reasons.
	 *
	 * @param reasons The set of reasons.
	 * @return The set of features, that are contained.
	 */
	private Set<String> determineInvolvedFeatures(Set<Reason<?>> reasons) {
		final Set<String> containedFeatures = new HashSet<>();
		for (final Reason<?> r : reasons) {
			containedFeatures.addAll(r.toNode().getContainedFeatures());
		}
		return containedFeatures;
	}

	/**
	 *
	 * Sets the name for the original defect. This is used to determine, if the prefix added in {@link DefectQuickFixHandler#getFeaturePrefix(IFeature)} is
	 * empty or not.
	 *
	 * @param originalDefectName the originalDefectName to set
	 */
	public void setOriginalDefectName(String originalDefectName) {
		this.originalDefectName = originalDefectName;
	}
}
