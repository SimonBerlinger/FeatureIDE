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

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
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
public class ResolutionDeleteFeature extends QuickFixDefect implements IMarkerResolution {

	private final String deadFeatureName;

	@Override
	public String getLabel() {

		return "Delete the affected feature";
	}

	@Override
	public void run(IMarker marker) {
		System.out.println("FIX DEAD FEATURE");

		final FeatureModelManager fm = (FeatureModelManager) project.getFeatureModelManager();
		fm.overwrite();
		fm.editObject(featureModel -> {

			IFeature deadFeature = null;

			for (final IFeature f : featureModel.getFeatures()) {
				if (f.getName().equals(deadFeatureName)) {
					deadFeature = f;
					break;
				}
			}

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

		fm.save();
	}

	public ResolutionDeleteFeature(IMarker marker, String affectedFeature) {
		super(marker);
		deadFeatureName = affectedFeature;
	}

}
