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

import java.util.List;

import org.eclipse.core.resources.IMarker;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class ResolutionMakeMandatory extends AbstractResolution {

	private final String featureName;
	private final String parentName;

	/**
	 * @param marker
	 * @param manager
	 */
	public ResolutionMakeMandatory(FeatureModelManager manager, String featureName, String parentName, String prefix) {
		super(manager);
		this.featureName = featureName;
		this.parentName = parentName;
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {

		return prefix + "Make the feature ''" + featureName + "'' mandatory under ''" + parentName + "''";
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {

			IFeature currentFeature = featureModel.getFeature(featureName);
			IFeature currentParent = currentFeature.getStructure().getParent().getFeature();

			while (!currentFeature.getName().equals(parentName)) {
				currentParent = currentFeature.getStructure().getParent().getFeature();
				if (currentParent.getStructure().isAlternative()) {

					final List<IFeatureStructure> siblings =
						currentParent.getStructure().getChildren().stream().filter(x -> !x.getFeature().getName().equals(featureName)).toList();
					currentParent.getStructure().changeToAnd();

					if (siblings.size() > 1) {

						// TODO implement

					} else {
						currentParent.getStructure().changeToAnd();
						siblings.get(0).setMandatory(false);
						currentFeature.getStructure().setMandatory(true);
					}

				} else if (currentParent.getStructure().isOr()) {
					currentParent.getStructure().setAnd();
					for (final IFeatureStructure f : currentParent.getStructure().getChildren()) {
						if (f.getFeature().getName().equals(featureName)) {
							f.setMandatory(true);
						} else {
							f.setMandatory(false);
						}
					}
				} else {
					currentFeature.getStructure().setMandatory(true);
				}
				currentFeature = currentFeature.getStructure().getParent().getFeature();
			}

		});
//		System.out.println("SUCCESS?: " + fmManager.getVariableFormula().getFeatureModel().getFeatures());
//		System.out.println("SUCCESS?: " + fmManager.getPersistentFormula().getFeatureModel().getFeatures());
		fmManager.save();
		fmManager.overwrite();
//		System.out.println("SUCCESS?: " + fmManager.getVariableFormula().getFeatureModel().getFeatures());
//		System.out.println("SUCCESS?: " + fmManager.getPersistentFormula().getFeatureModel().getFeatures());
	}

}
