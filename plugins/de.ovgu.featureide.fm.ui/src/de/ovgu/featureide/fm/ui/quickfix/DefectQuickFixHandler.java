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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.PluginID;
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
	private static final String MODEL_MARKER = PluginID.PLUGIN_ID + ".featureModelMarker";

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

			analyzer = featureModelFormula.getAnalyzer();
			fmManager = (FeatureModelManager) project.getFeatureModelManager();
			final DefectResolutionProvider resolutionProvider =
				new DefectResolutionProvider(featureModelFormula, marker, fmManager, analyzer, this, project.getModelFile());

			final String[] splitMessage = marker.getAttribute(IMarker.MESSAGE, "").split("''");

			if (splitMessage.length > 1) {

				final String affectedElementString = marker.getAttribute(IMarker.MESSAGE, "").split("''")[1];

				if (analyzer.hasDeadFeatures(List.of(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature()))
					|| affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {

					if (splitMessage[0].startsWith(IFeatureProject.MARKER_DEAD)
						&& (analyzer.hasDeadFeatures(List.of(featureModelFormula.getFeatureModel().getFeature(affectedElementString)))
							|| affectedElementString.equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName()))) {

						final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);

						if (affectedFeature != null) {

							final Set<Reason<?>> reasons = analyzer.getDeadFeatureExplanation(affectedFeature).getReasons();
							final Set<IMarkerResolution> offeredResolutions = new HashSet<>();

							// Add possible resolutions to the set
							offeredResolutions.add(new ResolutionDeleteFeature(marker, affectedElementString, fmManager));

							resolutionProvider.checkForExcludingFalseOptionals(reasons, affectedFeature, offeredResolutions);

							// All mandatory features, where the fact that they are mandatory is a reason
							final List<IFeature> mandatoryReasons = new ArrayList<IFeature>();
							for (final Reason<?> reason : reasons) {
								final IFeature mandatoryReason = resolutionProvider.checkMandatoryChildReason(reason, offeredResolutions);
								if (mandatoryReason != null) {
									mandatoryReasons.add(mandatoryReason);
								}
							}

							// All exclusions, where a mandatory feature excludes the affected feature
							for (final Reason<?> reason : reasons) {
								for (final IFeature f : mandatoryReasons) {
									resolutionProvider.checkMandatoryExclusionConstraint(reason, offeredResolutions, affectedFeature, f);
								}
							}

							// checks if the affected feature is implying its own exclusion
							resolutionProvider.checkForSimultaneousImplExcl(reasons, affectedFeature, offeredResolutions);

							return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);
						}

					} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_FALSE_OPTIONAL)
						&& !analyzer.getFalseOptionalFeatures(null).contains(featureModelFormula.getFeatureModel().getFeature(affectedElementString))) {

							final IFeature affectedFeature = featureModelFormula.getFeatureModel().getFeature(affectedElementString);

							if (affectedFeature != null) {
								return new IMarkerResolution[] { new FalseOptionalResolution(marker, fmManager) };
							}

						} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_REDUNDANCY)) {
							return new IMarkerResolution[] { new RedundancyResolution(marker, fmManager) };

						} else if (splitMessage[0].startsWith(IFeatureProject.MARKER_TAUTOLOGY)) {

							for (final IConstraint c : featureModelFormula.getFeatureModel().getConstraints()) {
								if (c.getDisplayName().equals(affectedElementString)) {
									return new IMarkerResolution[] { new ResolutionDeleteConstraint(marker, c.getNode(), fmManager),
										new ResolutionEditConstraint(marker, c, fmManager) };
								}
							}
						}
				} else {

					// When the feature-model is void, always return fixes for the dead root
					IMarker[] markers;
					try {
						markers = project.getModelFile().findMarkers(MODEL_MARKER, false, 0);

						final List<IMarkerResolution> offeredResolutions = new ArrayList<>();

						for (final IMarker marker1 : markers) {
							final String message = (String) marker1.getAttribute(IMarker.MESSAGE);
							if (message.startsWith(IFeatureProject.MARKER_DEAD)
								&& message.split("''")[1].equals(featureModelFormula.getFeatureModel().getStructure().getRoot().getFeature().getName())) {

								for (final IMarkerResolution resolution : getResolutions(marker1)) {
									offeredResolutions.add(resolution);
								}
							}
						}

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

}
