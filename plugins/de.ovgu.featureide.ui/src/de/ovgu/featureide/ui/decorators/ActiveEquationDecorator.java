/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2010  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.ui.decorators;

import java.net.URL;
import java.util.LinkedList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.listeners.ICurrentEquationListener;
import de.ovgu.featureide.ui.UIPlugin;


/**
 * A decorator that marks the equation files that is selected for building.
 * 
 * @author Marcus Leich
 *
 */
public class ActiveEquationDecorator implements ILightweightLabelDecorator, ICurrentEquationListener {
	
	private ImageDescriptor icon;
	private LinkedList<ILabelProviderListener> listeners;
	
	public ActiveEquationDecorator() {
		URL url = de.ovgu.featureide.ui.UIPlugin.getDefault().getBundle().getEntry("/icons/currentconfiguration.gif");
		icon = ImageDescriptor.createFromURL(url);
	    listeners = new LinkedList<ILabelProviderListener> ();
	    
	    // add Listener to Activator
	    de.ovgu.featureide.core.CorePlugin.getDefault().addCurrentEquationListener(this); 
	}

	public void decorate(Object element, IDecoration decoration) {
		IFeatureProject pd = CorePlugin.getFeatureProject ((IResource)element);
		if (pd != null && ((IResource)element).equals(pd.getCurrentConfiguration())) {
			decoration.addOverlay(icon, IDecoration.TOP_LEFT);
		}
	}

	public void addListener(ILabelProviderListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void dispose() {
		de.ovgu.featureide.core.CorePlugin.getDefault().removeCurrentEquationListener(this);
	}
	
	private void refresh (IResource[] res) {
		LabelProviderChangedEvent e = new LabelProviderChangedEvent (this, res);
		for (ILabelProviderListener l : listeners) {
			l.labelProviderChanged(e);
		}
	}

	public boolean isLabelProperty(Object element, String property) {
		// this is not a property dependant label
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		listeners.remove(listener);
	}

	public void currentEquationChanged(IFeatureProject data) {
		try {
			refresh(data.getEquationFolder().members());
		} catch (CoreException e) {
			UIPlugin.getDefault().logError(e);
		}
	}

}
