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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.explanations.Reason;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class DefectQuickFixHandler implements IMarkerResolutionGenerator {

	private FeatureModelAnalyzer analyzer;
	private FeatureModelFormula featureModel;

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {

		IFeatureProject project = null;

		if (marker != null) {
			project = CorePlugin.getFeatureProject(marker.getResource());
			if (project == null) {
				featureModel = null;
			} else {
				featureModel = project.getFeatureModelManager().getPersistentFormula();
			}
		} else {
			featureModel = null;
		}

		if (featureModel != null) {

			analyzer = featureModel.getAnalyzer();
			final String affectedFeatureName = marker.getAttribute(IMarker.MESSAGE, "").split("''")[1];
			IFeature affectedFeature = null;
			for (final IFeature feat : featureModel.getFeatureModel().getFeatures()) {
				if (feat.getName().equals(affectedFeatureName)) {
					affectedFeature = feat;
					break;
				}
			}

			if (affectedFeature != null) {

				final Set<Reason<?>> reasons = analyzer.getDeadFeatureExplanation(affectedFeature).getReasons();

				final String message = marker.getAttribute(IMarker.MESSAGE, "");

				if (message.startsWith(IFeatureProject.MARKER_DEAD) && !analyzer.getDeadFeatures(null).isEmpty()) {

					final ArrayList<IMarkerResolution> offeredResolutions = new ArrayList<>();

					offeredResolutions.add(new ResolutionDeleteFeature(marker, affectedFeatureName));

					offeredResolutions.add(new DeadFeatureResolution2(marker));

					final Set<String> falseOptionals = getExcludingFalseOptionals(reasons, affectedFeature);

					if (falseOptionals.size() > 0) {
						offeredResolutions.add(new ResolutionAwaitOther(marker, affectedFeatureName, falseOptionals));
					}

					System.out.println("EXPL: " + analyzer.getDeadFeatureExplanation(affectedFeature));
					analyzer.getDeadFeatureExplanation(affectedFeature).getReasons()
							.forEach(r -> System.out.println("RSN: " + r + " " + r.toNode() + " " + r.toNode().getContainedFeatures()));

					return offeredResolutions.toArray(new IMarkerResolution[offeredResolutions.size()]);

				} else if (message.startsWith(IFeatureProject.MARKER_FALSE_OPTIONAL)) {
					return new IMarkerResolution[] { new FalseOptionalResolution(marker) };

				} else if (message.startsWith(IFeatureProject.MARKER_REDUNDANCY)) {
					return new IMarkerResolution[] { new RedundancyResolution(marker) };

				} else if (message.startsWith(IFeatureProject.MARKER_TAUTOLOGY)) {
					return new IMarkerResolution[] { new TautologyResolution(marker) };

				}
			}
		}
		return new IMarkerResolution[0];
	}

	/**
	 *
	 * @param deadFeature
	 * @return
	 */
	private Set<String> getExcludingFalseOptionals(Set<Reason<?>> reasons, IFeature deadFeature) {

		List<String> falseOptionals = new ArrayList<String>(analyzer.getCoreFeatures(null).stream().map(f -> f.getName()).toList());

		final List<String> toRemove = new ArrayList<>();
		for (final String fName : falseOptionals) {
			if (!analyzer.getFalseOptionalFeatures(null).stream().map(ft -> ft.getName()).toList().contains(fName)) {
				toRemove.add(fName);
			}
		}

		for (final String f : toRemove) {
			falseOptionals.remove(f);
		}

		final Map<String, Boolean> isAlwaysExcluded = new HashMap<>();

		for (final String f : falseOptionals) {
			isAlwaysExcluded.put(f, true);
		}

		for (final Reason<?> r : reasons) {
			final Set<Map<Object, Boolean>> set = r.toNode().getSatisfyingAssignments();
			for (final Map<Object, Boolean> map : set) {
				if ((map.get(deadFeature.getName()) != null) && map.get(deadFeature.getName())) {
					for (final Map.Entry<Object, Boolean> entry : map.entrySet()) {
						if (falseOptionals.contains(entry.getKey().toString()) && entry.getValue()) {
							isAlwaysExcluded.put(entry.getKey().toString(), false); // Assignment was found, where feature is not excluded if dead feature was
																					 // selected
						}
					}
				}
			}
		}
		falseOptionals = null;

		final Set<String> result = new HashSet<>();
		isAlwaysExcluded.forEach((featureName, isExcluded) -> {
			if (isExcluded) {
				result.add(featureName);
			}
		});

		return result;
	}
}
