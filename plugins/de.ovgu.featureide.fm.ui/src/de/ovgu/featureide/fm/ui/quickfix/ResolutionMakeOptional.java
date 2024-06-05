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

import java.util.Objects;

import org.eclipse.core.resources.IMarker;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * A defect resolution which makes a mandatory feature optional.
 *
 * @author Simon Berlinger
 */
public class ResolutionMakeOptional extends AbstractResolution {

	/**
	 * The name of the feature to make optional
	 */
	private final String featureName;

	/**
	 *
	 * @param fmManager The FeatureModelManager
	 * @param feature The feature to make optional
	 * @param prefix The prefix for the label to indicate the defect
	 */
	public ResolutionMakeOptional(FeatureModelManager fmManager, IFeature feature, String prefix) {
		super(fmManager);
		featureName = feature.getName();
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {

		return prefix + "Make the feature ''" + featureName + "'' optional";
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {

			final IFeature affectedFeature = featureModel.getFeature(featureName);

			if (affectedFeature != null) {
				affectedFeature.getStructure().setMandatory(false);
			}
		});

		fmManager.save();
		fmManager.overwrite();
	}

	@Override
	public int hashCode() {
		return Objects.hash(featureName);
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
		final ResolutionMakeOptional other = (ResolutionMakeOptional) obj;
		return Objects.equals(featureName, other.featureName);
	}

	@Override
	public String toString() {
		return "ResolutionMakeOptional [featureName=" + featureName + "]";
	}

}
