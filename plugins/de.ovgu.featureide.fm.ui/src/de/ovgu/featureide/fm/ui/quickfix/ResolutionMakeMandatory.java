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
import java.util.Objects;

import org.eclipse.core.resources.IMarker;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.Feature;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * A defect resolution, that makes a non-mandatory feature mandatory.
 *
 * @author Simon Berlinger
 */
public class ResolutionMakeMandatory extends AbstractResolution {

	/**
	 * The name of the feature to make mandatory
	 */
	private IFeature feature;
	/**
	 * The name of the parent of the affected feature
	 */
	private final IFeature parent;

	/**
	 *
	 * @param fmManager The FeatureModelManager
	 * @param feature The feature to make mandatory
	 * @param parent The parent of the affected feature
	 * @param prefix The prefix for the label to indicate the defect
	 */
	public ResolutionMakeMandatory(FeatureModelManager fmManager, IFeature feature, IFeature parent, String prefix) {
		super(fmManager);
		this.feature = feature;
		this.parent = parent;
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {

		return prefix + "Make the feature ''" + feature + "'' mandatory under ''" + parent + "''";
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {

			IFeature parentStructure = feature.getStructure().getParent().getFeature();

			while (!feature.getName().equals(parent.getName())) {
				parentStructure = feature.getStructure().getParent().getFeature();
				if (parentStructure.getStructure().isAlternative()) {

					final List<IFeatureStructure> siblings =
						parentStructure.getStructure().getChildren().stream().filter(x -> !x.getFeature().getName().equals(feature)).toList();

					if (siblings.size() > 1) {

						parentStructure.getStructure().changeToAnd();

						int idx = 0;

						while (featureModel.getFeature("ALT_GROUP_" + idx) != null) {
							idx++;
						}

						final IFeature newFeature = new Feature(featureModel, "ALT_GROUP_" + idx);

						for (final IFeatureStructure sibling : siblings) {
							newFeature.getStructure().addChild(sibling);
							parentStructure.getStructure().removeChild(sibling);
						}

						newFeature.getStructure().setMandatory(false);
						newFeature.getStructure().setAlternative();

						parentStructure.getStructure().addChild(newFeature.getStructure());

					} else {
						siblings.get(0).setMandatory(false);
					}

					feature.getStructure().setMandatory(true);

				} else if (parentStructure.getStructure().isOr()) {
					parentStructure.getStructure().setAnd();
					for (final IFeatureStructure f : parentStructure.getStructure().getChildren()) {
						if (f.getFeature().getName().equals(feature)) {
							f.setMandatory(true);
						} else {
							f.setMandatory(false);
						}
					}
				} else {
					feature.getStructure().setMandatory(true);
				}
				feature = feature.getStructure().getParent().getFeature();
			}

		});
		fmManager.save();
		fmManager.overwrite();
	}

	@Override
	public int hashCode() {
		return Objects.hash(feature, parent);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ResolutionMakeMandatory other = (ResolutionMakeMandatory) obj;
		return Objects.equals(feature, other.feature) && Objects.equals(parent, other.parent);
	}

	@Override
	public String toString() {
		return "ResolutionMakeMandatory [featureName=" + feature + ", parentName=" + parent + "]";
	}

}
