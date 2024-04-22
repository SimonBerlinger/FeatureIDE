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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.prop4j.Node;
import org.prop4j.Not;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.PluginID;
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

	private FeatureModelAnalyzer analyzer;
	private FeatureModelFormula featureModelFormula;
	private FeatureModelManager fmManager;
	private String originalDefectName = "";
	private boolean notOriginFlag = false;

	private static final String MODEL_MARKER = PluginID.PLUGIN_ID + ".featureModelMarker";

	public IMarkerResolution[] getResolutionsShowPrefix(IMarker marker) {
		notOriginFlag = true;
		final IMarkerResolution[] resolutions = getResolutions(marker);
		notOriginFlag = false;
		return resolutions;
	}

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
					new DefectResolutionProvider(featureModelFormula, marker, fmManager, analyzer, this, project.getModelFile());
				final String affectedElementString = splitMessage[1];

				// When other causes are checked and we arrive at the original defect, abort to avoid infinite loops
				if (notOriginFlag && affectedElementString.equals(originalDefectName)) {
					return new IMarkerResolution[] {};
				}

				System.out.println("AFFECTED ELEMENT: " + affectedElementString + " IS ORIGINAL: " + originalDefectName);

				// if feature model not void or the root is the affected feature
				if (!analyzer.getFeatureModelProperties().hasStatus(FeatureModelStatus.VOID)
					|| affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {

					final Set<IMarkerResolution> offeredResolutions = new HashSet<>();

					// if marker type is dead feature and feature is contained in the model and actually dead or the affected feature is root
					if (splitMessage[0].startsWith(IFeatureProject.MARKER_DEAD)
						&& (analyzer.getDeadFeatures(null).contains(featureModelFormula.getFeatureModel().getFeature(affectedElementString))
							|| (affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())
								&& analyzer.getFeatureModelProperties().hasStatus(FeatureModelStatus.VOID)))) {

						if (!notOriginFlag) {
							originalDefectName = affectedElementString;
						}

						final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);
						System.out.println("DF?: " + affectedElementString);
						System.out.println("DF: " + analyzer.getDeadFeatureExplanation(affectedFeature));

						if (affectedFeature != null) {

							final Set<Reason<?>> reasons = analyzer.getDeadFeatureExplanation(affectedFeature).getReasons();
							final Set<String> involvedFeatures = determineInvolvedFeatures(reasons);

							System.out.println(affectedElementString + "  Has no implied dead? " + !resolutionProvider.checkImpliedDeadFeatures(reasons,
									affectedFeature, offeredResolutions, getFeaturePrefix(affectedFeature), involvedFeatures));
							System.out.println(" Is origin? " + originalDefectName.equals(affectedElementString));
							// Only return resolutions, if the feature is either the first or the last in a chain of implications of dead features
							if (!resolutionProvider.checkImpliedDeadFeatures(reasons, affectedFeature, offeredResolutions, getFeaturePrefix(affectedFeature),
									involvedFeatures)
								|| originalDefectName.equals(affectedElementString)) {

								// check, if there exists a constraint -f or equivalent to -f for the feature ( -> intentionally dead?)
								final Node notFeature = new Not(affectedFeature.getName());

								for (final Reason<?> r : reasons) {
									if (r.toNode().equals(notFeature)) {
										return new IMarkerResolution[] { new ResolutionDeleteConstraint(marker, notFeature, fmManager,
												getFeaturePrefix(affectedFeature), " to reactivate the feature.") };
									}

									// check if feature excludes itself or the root
									if (resolutionProvider.isExcluding(affectedElementString,
											featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName(), r.toNode())
										|| resolutionProvider.isExcluding(affectedElementString, affectedElementString, r.toNode())) {
										offeredResolutions.add(new ResolutionSetConstraint(marker, fmManager, r.toNode(), notFeature, "",
												" to explicitly exclude ''" + affectedElementString + "''"));
									}

									// check if feature is excluded by deactivated feature
									for (final String f : r.toNode().getContainedFeatures()) {
										if (r.toNode().getContainedFeatures().contains(affectedElementString)
											&& !resolutionProvider.canBeSelectedInConstraint(affectedElementString,
													Node.replaceLiterals(r.toNode(), Arrays.asList(f), true))
											&& featureModelFormula.getFeatureModel().getConstraints().stream().map(x -> x.getDisplayName()).toList()
													.contains("-" + f)) {
											offeredResolutions.add(new ResolutionCreateConstraint(marker, fmManager, new Not(affectedFeature),
													" to explicitly exclude ''" + affectedElementString + "''"));
										}
									}

								}

								// Add delete feature action if not root
								if (!affectedFeature.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature())) {
									offeredResolutions
											.add(new ResolutionDeleteFeature(marker, affectedElementString, fmManager, getFeaturePrefix(affectedFeature)));
								}

								resolutionProvider.checkImpliesMultiAlt(marker, reasons, affectedFeature, offeredResolutions,
										getFeaturePrefix(affectedFeature));

								resolutionProvider.checkForExcludingFalseOptionals(reasons, affectedFeature, offeredResolutions,
										getFeaturePrefix(affectedFeature), involvedFeatures);

								// All mandatory features, where the fact that they are mandatory is a reason
								for (final Reason<?> reason : reasons) {
									resolutionProvider.checkMandatoryChildReason(reason, offeredResolutions, getFeaturePrefix(affectedFeature));
								}

								// All exclusions, where a mandatory feature excludes the affected feature
								for (final String f : involvedFeatures) {
									resolutionProvider.checkExclusion(reasons, offeredResolutions, affectedFeature.getName(), f,
											getFeaturePrefix(affectedFeature));
								}

								// checks if the affected feature is implying its own exclusion
								resolutionProvider.checkImpliesOwnExclusion(reasons, affectedFeature, offeredResolutions, getFeaturePrefix(affectedFeature));

								return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
							}
							return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
						}

					} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_FALSE_OPTIONAL)
						&& analyzer.getFalseOptionalFeatures(null).contains(featureModelFormula.getFeatureModel().getFeature(affectedElementString))) {

							if (!notOriginFlag) {
								originalDefectName = affectedElementString;
							}

							final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);

							System.out.println("FO: " + analyzer.getFalseOptionalFeatureExplanation(affectedFeature));

							if (affectedFeature != null) {

								final Set<Reason<?>> reasons = analyzer.getFalseOptionalFeatureExplanation(affectedFeature).getReasons();
								final Set<String> involvedFeatures = determineInvolvedFeatures(reasons);

								System.out.println(affectedElementString + " is original " + originalDefectName);

								// only add fixes if the currently regarded feature is the original defect to be fixed or the first false-optional without
								// another false-optional possibly being responsible for it
								if (!resolutionProvider.checkForImplyingFalseOptionals(reasons, affectedFeature, offeredResolutions,
										getFeaturePrefix(affectedFeature), involvedFeatures)
									|| originalDefectName.equals(affectedElementString)) {

									System.out.println(affectedFeature + " get fixes");

									// All mandatory features, where the fact that they are mandatory is a reason
									final List<IFeature> mandatoryReasons = new ArrayList<IFeature>();
									for (final Reason<?> reason : reasons) {
										final IFeature mandatoryReason =
											resolutionProvider.checkMandatoryChildReason(reason, offeredResolutions, getFeaturePrefix(affectedFeature));
										if (mandatoryReason != null) {
											mandatoryReasons.add(mandatoryReason);
										}
									}

									// All exclusions, where a feature implies the affected feature
									for (final String f : mandatoryReasons.stream().map(x -> x.getName()).toList()) {
										if (!f.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {
											offeredResolutions.add(new ResolutionMakeOptional(marker, f, fmManager, getFeaturePrefix(affectedFeature)));
											resolutionProvider.checkImplicationConstraint(reasons, offeredResolutions, affectedElementString, f,
													getFeaturePrefix(affectedFeature));
										}
									}

									// Situations, where enough other alternatives are excluded (dead) to make the affected feature false-optional
									if (affectedFeature.getStructure().getParent().isAlternative() || affectedFeature.getStructure().getParent().isOr()) {
										resolutionProvider.checkAlternativeExclusion(reasons, offeredResolutions, affectedFeature,
												getFeaturePrefix(affectedFeature), "POSTFIX");
									}

								}
								return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
							}

						} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_REDUNDANCY)) {
							System.out.println("REDUNDANT: " + analyzer.getRedundantConstraintExplanation(featureModelFormula.getFeatureModel().getConstraints()
									.stream().filter(x -> x.getDisplayName().equals(affectedElementString)).toList().get(0)));

							return new IMarkerResolution[] {};

						} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_TAUTOLOGY)) {

							for (final IConstraint c : featureModelFormula.getFeatureModel().getConstraints()) {
								if (c.getDisplayName().equals(affectedElementString)) {
									notOriginFlag = false;
									return new IMarkerResolution[] { new ResolutionDeleteConstraint(marker, c.getNode(), fmManager),
										new ResolutionEditConstraint(marker, c, fmManager) };
								}
							}
						}
				} else {
					System.out.println("VOID");

					// When the feature-model is void, always return fixes for the dead root
					IMarker[] markers;
					try {
						markers = project.getModelFile().findMarkers(MODEL_MARKER, false, 0);

						final List<IMarkerResolution> offeredResolutions = new ArrayList<>();
						boolean foundRoot = false;

						for (final IMarker marker1 : markers) {
							final String message = (String) marker1.getAttribute(IMarker.MESSAGE);
							if (message.startsWith(IFeatureProject.MARKER_DEAD)
								&& message.split("''")[1].equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {
								foundRoot = true;
								for (final IMarkerResolution resolution : getResolutionsShowPrefix(marker1)) {
									offeredResolutions.add(resolution);
									System.out.println("ADD RES " + resolution);
								}
							}
						}

						System.out.println("DEVIATE TO ROOT SUCCESS: " + foundRoot);
						return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);

					} catch (final CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println("NONE");
		return new IMarkerResolution[0];
	}

	/**
	 * @param affectedFeature
	 * @return
	 */
	private String getFeaturePrefix(final IFeature affectedFeature) {
		return originalDefectName.equals(affectedFeature.getName()) ? ""
			: (affectedFeature.getName().equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName()) ? "[Root] "
				: "[Possible cause ''" + affectedFeature.getName() + "''] ");
	}

	private Set<String> determineInvolvedFeatures(Set<Reason<?>> reasons) {
		final Set<String> containedFeatures = new HashSet<>();
		for (final Reason<?> r : reasons) {
			containedFeatures.addAll(r.toNode().getContainedFeatures());
		}
		return containedFeatures;
	}

}
