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
import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.prop4j.Node;
import org.prop4j.True;

import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;

/**
 * TODO description
 *
 * @author Simon Berlinger
 */
public class ResolutionDeleteFeature extends AbstractResolution {

	private final String featureName;

	public ResolutionDeleteFeature(String affectedFeature, FeatureModelManager fmManager, String prefix) {
		super(fmManager);
		featureName = affectedFeature;
		this.prefix = prefix;
	}

	@Override
	public String getLabel() {

		return prefix + "Delete the affected feature";
	}

	@Override
	public void run(IMarker marker) {

		fmManager.overwrite();
		fmManager.editObject(featureModel -> {

			final IFeature deadFeature = featureModel.getFeature(featureName);

			if (deadFeature != null) {
				final ArrayList<IConstraint> toDelete = new ArrayList<>();
				for (final IConstraint c : featureModel.getConstraints()) {
					c.setNode(Node.replaceLiterals(c.getNode(), Arrays.asList(deadFeature.getName()), true));
					if (c.getNode() instanceof True) {
						toDelete.add(c);
					}
				}
				toDelete.forEach(c -> featureModel.removeConstraint(c));
				featureModel.deleteFeature(deadFeature);
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
		final ResolutionDeleteFeature other = (ResolutionDeleteFeature) obj;
		return Objects.equals(featureName, other.featureName);
	}

}
